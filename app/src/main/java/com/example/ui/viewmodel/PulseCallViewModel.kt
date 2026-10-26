package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cv.FaceBlendshapes
import com.example.cv.GestureDetectionEvaluation
import com.example.cv.GestureReactionEngine
import com.example.cv.HandLandmarks
import com.example.data.model.CallRoom
import com.example.data.model.GestureType
import com.example.data.model.ReactionEvent
import com.example.data.repository.CallRoomRepository
import com.example.ui.components.ReactionBurstTrigger
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

data class ActiveCallUiState(
  val room: CallRoom? = null,
  val isSoloDemoRoom: Boolean = false,
  val isMuted: Boolean = false,
  val isCameraOn: Boolean = true,
  val isFrontCamera: Boolean = true,
  val isScreenSharing: Boolean = false,
  val showLandmarkMesh: Boolean = true,
  val showCvInspector: Boolean = true,
  val activePresetPose: GestureType? = null,
  val customMouthPucker: Float = 0.08f,
  val customThumbIndexGap: Float = 0.25f,
  val currentHands: List<HandLandmarks> = emptyList(),
  val currentFace: FaceBlendshapes = FaceBlendshapes(),
  val latestEvaluation: GestureDetectionEvaluation = GestureReactionEngine.evaluateFrame(emptyList(), FaceBlendshapes()),
  val cooldownRemainingMs: Long = 0L,
  val burstTriggers: List<ReactionBurstTrigger> = emptyList(),
  val recentReactions: List<ReactionEvent> = emptyList(),
  val statusBanner: String? = null,
)

class PulseCallViewModel(
  private val repository: CallRoomRepository,
  private val currentUserId: String,
) : ViewModel() {

  val hostedRoomsState: StateFlow<UiState<List<CallRoom>>> =
    repository.observeMyHostedRooms(currentUserId)
      .map<List<CallRoom>, UiState<List<CallRoom>>> { list ->
        val sorted = list.sortedByDescending { it.createdAt?.seconds ?: 0L }
        UiState.Success(sorted)
      }
      .catch { error ->
        Log.w(TAG, "Error observing hosted rooms", error)
        emit(UiState.Error(error.message ?: "Failed to load rooms"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  private val _callState = MutableStateFlow(ActiveCallUiState())
  val callState: StateFlow<ActiveCallUiState> = _callState.asStateFlow()

  private val _lobbyMessage = MutableStateFlow<String?>(null)
  val lobbyMessage: StateFlow<String?> = _lobbyMessage.asStateFlow()

  private var roomJob: Job? = null
  private var reactionsJob: Job? = null
  private var cooldownJob: Job? = null
  private var lastAutoTriggerTimestamp: Long = 0L
  private val seenFirestoreReactionIds = mutableSetOf<String>()

  fun generateRoomCode(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return "PLS" + (1..3).map { chars.random() }.joinToString("")
  }

  fun clearLobbyMessage() {
    _lobbyMessage.value = null
  }

  fun createAndEnterRoom(roomTitle: String, isSoloDemo: Boolean = false) {
    val code = generateRoomCode()
    viewModelScope.launch {
      val result = repository.createRoom(code, roomTitle)
      result.onSuccess { createdRoom ->
        enterRoomInternal(createdRoom, isSoloDemo = isSoloDemo)
      }.onFailure { err ->
        _lobbyMessage.value = err.localizedMessage ?: "Could not create room"
      }
    }
  }

  fun joinAndEnterRoom(rawCode: String) {
    val code = rawCode.trim().uppercase()
    if (code.length < 4) {
      _lobbyMessage.value = "Enter a valid 4–16 character room code"
      return
    }
    viewModelScope.launch {
      val result = repository.joinRoom(code)
      result.onSuccess { joinedRoom ->
        enterRoomInternal(joinedRoom, isSoloDemo = false)
      }.onFailure { err ->
        _lobbyMessage.value = err.localizedMessage ?: "Unable to join room $code"
      }
    }
  }

  fun openExistingHostedRoom(room: CallRoom) {
    enterRoomInternal(room, isSoloDemo = false)
  }

  fun deleteHostedRoom(roomCode: String) {
    viewModelScope.launch {
      repository.deleteRoom(roomCode).onFailure { err ->
        _lobbyMessage.value = err.localizedMessage ?: "Failed to delete room"
      }
    }
  }

  private fun enterRoomInternal(room: CallRoom, isSoloDemo: Boolean) {
    roomJob?.cancel()
    reactionsJob?.cancel()
    seenFirestoreReactionIds.clear()

    _callState.value = ActiveCallUiState(
      room = room,
      isSoloDemoRoom = isSoloDemo,
      statusBanner = if (isSoloDemo) {
        "AI Gesture Studio Active • Perform or tap a pose to trigger 60fps synced reactions"
      } else {
        "Room ${room.roomCode} Connected • Waiting for peer or perform AI gesture"
      },
    )

    roomJob = viewModelScope.launch {
      repository.observeRoom(room.roomCode)
        .catch { e -> Log.w(TAG, "Error observing room ${room.roomCode}", e) }
        .collect { updatedRoom ->
          if (updatedRoom != null) {
            val isHost = updatedRoom.hostUid == currentUserId
            _callState.value = _callState.value.copy(
              room = updatedRoom,
              isMuted = if (isHost) updatedRoom.hostMuted else updatedRoom.guestMuted,
              isCameraOn = if (isHost) updatedRoom.hostCameraOn else updatedRoom.guestCameraOn,
              isScreenSharing = if (isHost) updatedRoom.hostScreenSharing else updatedRoom.guestScreenSharing,
            )
          }
        }
    }

    reactionsJob = viewModelScope.launch {
      repository.observeReactions(room.roomCode, currentUserId)
        .catch { e -> Log.w(TAG, "Error observing reactions for ${room.roomCode}", e) }
        .collect { list ->
          val sorted = list.sortedBy { it.createdAt?.seconds ?: 0L }
          val newBursts = mutableListOf<ReactionBurstTrigger>()
          for (rx in sorted) {
            if (rx.id.isNotBlank() && seenFirestoreReactionIds.add(rx.id)) {
              newBursts.add(
                ReactionBurstTrigger(
                  id = rx.id,
                  gestureType = GestureType.fromId(rx.gestureType),
                  originXPercent = rx.originX.toInt(),
                  originYPercent = rx.originY.toInt(),
                  senderName = rx.senderName,
                )
              )
            }
          }
          _callState.value = _callState.value.copy(
            recentReactions = sorted.takeLast(8),
            burstTriggers = (_callState.value.burstTriggers + newBursts).takeLast(24),
          )
        }
    }
  }

  /**
   * Applies a MediaPipe CV pose preset (Two-Hand Heart, Kiss Face, Thumbs Up, Double Thumbs Fireworks,
   * Peace Sign, or Laugh) and runs the mathematical landmark & blendshape evaluator with the
   * 2,000ms cooldown gate.
   */
  fun selectCvPosePreset(preset: GestureType?) {
    val (hands, face) = GestureReactionEngine.buildPresetPose(preset)
    val evaluation = GestureReactionEngine.evaluateFrame(hands, face)
    _callState.value = _callState.value.copy(
      activePresetPose = preset,
      currentHands = hands,
      currentFace = face,
      customMouthPucker = face.mouthPucker,
      customThumbIndexGap = evaluation.thumbGapDistance,
      latestEvaluation = evaluation,
    )
    maybeEmitAutoDetectedReaction(evaluation)
  }

  /**
   * Updates the live facial blendshape `mouthPucker` slider in real time to demonstrate
   * the `mouthPucker > 0.62` Kiss Face detection threshold.
   */
  fun updateMouthPuckerBlendshape(puckerValue: Float) {
    val clamped = puckerValue.coerceIn(0f, 1f)
    val updatedFace = _callState.value.currentFace.copy(
      mouthPucker = clamped,
      mouthFunnel = (clamped * 0.42f).coerceIn(0.05f, 0.50f),
      jawOpen = 0.08f,
    )
    val evaluation = GestureReactionEngine.evaluateFrame(_callState.value.currentHands, updatedFace)
    _callState.value = _callState.value.copy(
      activePresetPose = null,
      customMouthPucker = clamped,
      currentFace = updatedFace,
      latestEvaluation = evaluation,
    )
    maybeEmitAutoDetectedReaction(evaluation)
  }

  /**
   * Updates the live two-hand thumb/index distance slider in real time to demonstrate
   * the `dist < 0.085` Hand-Heart detection threshold.
   */
  fun updateHandHeartDistance(gapValue: Float) {
    val clamped = gapValue.coerceIn(0.01f, 0.35f)
    val halfGap = clamped / 2f
    val left = listOf(
      com.example.cv.Landmark3D(0.28f, 0.76f),
      com.example.cv.Landmark3D(0.35f, 0.72f),
      com.example.cv.Landmark3D(0.41f, 0.69f),
      com.example.cv.Landmark3D(0.45f, 0.67f),
      com.example.cv.Landmark3D(0.50f - halfGap, 0.66f), // 4 thumb tip
      com.example.cv.Landmark3D(0.34f, 0.56f),
      com.example.cv.Landmark3D(0.39f, 0.45f),
      com.example.cv.Landmark3D(0.45f, 0.49f),
      com.example.cv.Landmark3D(0.50f - halfGap, 0.54f), // 8 index tip
      com.example.cv.Landmark3D(0.32f, 0.58f),
      com.example.cv.Landmark3D(0.37f, 0.48f),
      com.example.cv.Landmark3D(0.43f, 0.53f),
      com.example.cv.Landmark3D(0.46f, 0.58f),
      com.example.cv.Landmark3D(0.30f, 0.62f),
      com.example.cv.Landmark3D(0.34f, 0.56f),
      com.example.cv.Landmark3D(0.38f, 0.60f),
      com.example.cv.Landmark3D(0.40f, 0.64f),
      com.example.cv.Landmark3D(0.28f, 0.66f),
      com.example.cv.Landmark3D(0.31f, 0.62f),
      com.example.cv.Landmark3D(0.34f, 0.65f),
      com.example.cv.Landmark3D(0.36f, 0.68f),
    )
    val right = left.map { pt -> pt.copy(x = 1.0f - pt.x) }
    val hands = listOf(HandLandmarks("Left", left), HandLandmarks("Right", right))
    val neutralFace = _callState.value.currentFace.copy(mouthPucker = 0.08f)
    val evaluation = GestureReactionEngine.evaluateFrame(hands, neutralFace)

    _callState.value = _callState.value.copy(
      activePresetPose = null,
      customThumbIndexGap = clamped,
      currentHands = hands,
      currentFace = neutralFace,
      latestEvaluation = evaluation,
    )
    maybeEmitAutoDetectedReaction(evaluation)
  }

  private fun maybeEmitAutoDetectedReaction(evaluation: GestureDetectionEvaluation) {
    val gesture = evaluation.detectedGesture ?: return
    val now = System.currentTimeMillis()
    val elapsed = now - lastAutoTriggerTimestamp
    if (elapsed < GestureReactionEngine.COOLDOWN_MS) {
      return
    }
    lastAutoTriggerTimestamp = now
    startCooldownCountdown()
    dispatchReaction(
      gestureType = gesture,
      source = "AI_GESTURE",
      originX = evaluation.originX,
      originY = evaluation.originY,
      confidencePercent = evaluation.confidencePercent,
    )
  }

  private fun startCooldownCountdown() {
    cooldownJob?.cancel()
    cooldownJob = viewModelScope.launch {
      val end = System.currentTimeMillis() + GestureReactionEngine.COOLDOWN_MS
      while (true) {
        val remaining = (end - System.currentTimeMillis()).coerceAtLeast(0L)
        _callState.value = _callState.value.copy(cooldownRemainingMs = remaining)
        if (remaining <= 0L) break
        delay(50L)
      }
    }
  }

  fun sendManualReaction(gestureType: GestureType) {
    dispatchReaction(
      gestureType = gestureType,
      source = "MANUAL_BAR",
      originX = 50,
      originY = if (gestureType == GestureType.KISS) 42 else 85,
      confidencePercent = 100,
    )
  }

  private fun dispatchReaction(
    gestureType: GestureType,
    source: String,
    originX: Int,
    originY: Int,
    confidencePercent: Int,
  ) {
    val currentRoom = _callState.value.room
    // Spawn local burst immediately for zero-latency 60fps feedback
    val localBurst = ReactionBurstTrigger(
      id = "local_${UUID.randomUUID()}",
      gestureType = gestureType,
      originXPercent = originX,
      originYPercent = originY,
      senderName = "You",
    )
    _callState.value = _callState.value.copy(
      burstTriggers = (_callState.value.burstTriggers + localBurst).takeLast(24),
      statusBanner = "${gestureType.primaryEmoji} ${gestureType.label} triggered ($source)",
    )

    if (currentRoom != null) {
      viewModelScope.launch {
        repository.sendReaction(
          room = currentRoom,
          gestureType = gestureType,
          source = source,
          originX = originX,
          originY = originY,
          confidencePercent = confidencePercent,
        ).onSuccess { docId ->
          // Mark as already rendered locally so Firestore echo doesn't double-trigger on same device
          seenFirestoreReactionIds.add(docId)
        }
      }
    }
  }

  fun toggleMute() {
    val state = _callState.value
    val newMuted = !state.isMuted
    _callState.value = state.copy(isMuted = newMuted)
    state.room?.let { room ->
      viewModelScope.launch {
        repository.updateMediaState(room, newMuted, state.isCameraOn, state.isScreenSharing)
      }
    }
  }

  fun toggleCamera() {
    val state = _callState.value
    val newCamera = !state.isCameraOn
    _callState.value = state.copy(isCameraOn = newCamera)
    state.room?.let { room ->
      viewModelScope.launch {
        repository.updateMediaState(room, state.isMuted, newCamera, state.isScreenSharing)
      }
    }
  }

  fun flipCameraLens() {
    _callState.value = _callState.value.copy(isFrontCamera = !_callState.value.isFrontCamera)
  }

  fun toggleScreenShare() {
    val state = _callState.value
    val newShare = !state.isScreenSharing
    _callState.value = state.copy(
      isScreenSharing = newShare,
      statusBanner = if (newShare) "Screen Share Active • Broadcasting viewport to peer" else "Screen Share Stopped",
    )
    state.room?.let { room ->
      viewModelScope.launch {
        repository.updateMediaState(room, state.isMuted, state.isCameraOn, newShare)
      }
    }
  }

  fun toggleLandmarkMesh() {
    _callState.value = _callState.value.copy(showLandmarkMesh = !_callState.value.showLandmarkMesh)
  }

  fun toggleCvInspector() {
    _callState.value = _callState.value.copy(showCvInspector = !_callState.value.showCvInspector)
  }

  fun leaveAndEndCall() {
    val currentRoom = _callState.value.room
    roomJob?.cancel()
    reactionsJob?.cancel()
    cooldownJob?.cancel()
    _callState.value = ActiveCallUiState()
    if (currentRoom != null) {
      viewModelScope.launch {
        repository.endCall(currentRoom.roomCode)
      }
    }
  }

  private companion object {
    const val TAG = "PulseCallVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}

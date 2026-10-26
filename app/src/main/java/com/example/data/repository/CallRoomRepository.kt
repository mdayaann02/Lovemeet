package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.CallRoom
import com.example.data.model.GestureType
import com.example.data.model.ReactionEvent
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CallRoomRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  private fun currentDisplayName(): String {
    val raw = auth.currentUser?.displayName?.trim().orEmpty()
    return if (raw.isNotEmpty()) raw.take(60) else "Pulse Caller"
  }

  suspend fun createRoom(
    roomCode: String,
    roomTitle: String,
    sdpOffer: String = "v=0\r\no=- 461173 2 IN IP4 127.0.0.1\r\ns=PulseCall-WebRTC\r\nt=0 0\r\nm=video 9 UDP/TLS/RTP/SAVPF 96",
  ): Result<CallRoom> {
    val uid = requireUserId()
    val normalizedCode = roomCode.trim().uppercase()
    val safeTitle = roomTitle.trim().ifEmpty { "Instant 1-on-1 Video Call" }.take(60)
    val hostName = currentDisplayName()
    val docRef = db.collection("rooms").document(normalizedCode)

    val payload = mapOf(
      "roomCode" to normalizedCode,
      "roomTitle" to safeTitle,
      "hostUid" to uid,
      "hostName" to hostName,
      "guestUid" to "",
      "guestName" to "",
      "status" to "waiting",
      "hostMuted" to false,
      "hostCameraOn" to true,
      "hostScreenSharing" to false,
      "guestMuted" to false,
      "guestCameraOn" to true,
      "guestScreenSharing" to false,
      "sdpOffer" to sdpOffer.take(5000),
      "sdpAnswer" to "",
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )

    return try {
      docRef.set(payload).await()
      val created = docRef.get().await().toObject(
        CallRoom::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
      ) ?: CallRoom(
        id = normalizedCode,
        roomCode = normalizedCode,
        roomTitle = safeTitle,
        hostUid = uid,
        hostName = hostName,
        status = "waiting",
      )
      Result.success(created)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun joinRoom(
    roomCode: String,
    sdpAnswer: String = "v=0\r\no=- 892311 2 IN IP4 127.0.0.1\r\ns=PulseCall-Answer\r\nt=0 0\r\nm=video 9 UDP/TLS/RTP/SAVPF 96",
  ): Result<CallRoom> {
    val uid = requireUserId()
    val normalizedCode = roomCode.trim().uppercase()
    val guestName = currentDisplayName()
    val docRef = db.collection("rooms").document(normalizedCode)

    return try {
      // Check if user is already the host or joined guest first
      val existingRoom = try {
        docRef.get().await().toObject(
          CallRoom::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
      } catch (_: Exception) {
        null
      }

      if (existingRoom != null && (existingRoom.hostUid == uid || existingRoom.guestUid == uid)) {
        return Result.success(existingRoom)
      }

      val updates = mapOf(
        "guestUid" to uid,
        "guestName" to guestName,
        "status" to "active",
        "sdpAnswer" to sdpAnswer.take(5000),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      docRef.update(updates).await()
      val updated = docRef.get().await().toObject(
        CallRoom::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
      ) ?: error("Room $normalizedCode not found after join")
      Result.success(updated)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun getRoomByCode(roomCode: String): Result<CallRoom> {
    val normalizedCode = roomCode.trim().uppercase()
    val docRef = db.collection("rooms").document(normalizedCode)
    return try {
      val snapshot = docRef.get().await()
      val room = snapshot.toObject(
        CallRoom::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
      ) ?: error("Room $normalizedCode does not exist")
      Result.success(room)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      Result.failure(e)
    }
  }

  fun observeMyHostedRooms(hostUid: String): Flow<List<CallRoom>> {
    val path = "rooms"
    return db.collection(path)
      .whereEqualTo("hostUid", hostUid)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          CallRoom::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeRoom(roomCode: String): Flow<CallRoom?> {
    val normalizedCode = roomCode.trim().uppercase()
    val docRef = db.collection("rooms").document(normalizedCode)
    return docRef.snapshots()
      .map { snapshot ->
        snapshot.toObject(
          CallRoom::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.GET, docRef.path)
        throw error
      }
  }

  suspend fun updateMediaState(
    room: CallRoom,
    isMuted: Boolean,
    isCameraOn: Boolean,
    isScreenSharing: Boolean,
  ): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("rooms").document(room.roomCode)
    val updates = if (uid == room.hostUid) {
      mapOf(
        "hostMuted" to isMuted,
        "hostCameraOn" to isCameraOn,
        "hostScreenSharing" to isScreenSharing,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    } else {
      mapOf(
        "guestMuted" to isMuted,
        "guestCameraOn" to isCameraOn,
        "guestScreenSharing" to isScreenSharing,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    }
    return try {
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun endCall(roomCode: String): Result<Unit> {
    requireUserId()
    val normalizedCode = roomCode.trim().uppercase()
    val docRef = db.collection("rooms").document(normalizedCode)
    return try {
      docRef.update(
        mapOf(
          "status" to "ended",
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteRoom(roomCode: String): Result<Unit> {
    requireUserId()
    val normalizedCode = roomCode.trim().uppercase()
    val docRef = db.collection("rooms").document(normalizedCode)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun sendReaction(
    room: CallRoom,
    gestureType: GestureType,
    source: String = "AI_GESTURE",
    originX: Int = 50,
    originY: Int = 80,
    confidencePercent: Int = 95,
  ): Result<String> {
    val uid = requireUserId()
    val senderName = currentDisplayName()
    val allowedUids = buildList {
      if (room.hostUid.isNotBlank()) add(room.hostUid)
      if (room.guestUid.isNotBlank() && room.guestUid != room.hostUid) add(room.guestUid)
      if (uid !in this) add(uid)
    }.take(2)

    val reactionsRef = db.collection("rooms").document(room.roomCode).collection("reactions")
    val docRef = reactionsRef.document()
    val payload = mapOf(
      "roomId" to room.roomCode,
      "senderUid" to uid,
      "senderName" to senderName,
      "gestureType" to gestureType.id,
      "emoji" to gestureType.primaryEmoji,
      "source" to source,
      "originX" to originX.coerceIn(0, 100),
      "originY" to originY.coerceIn(0, 100),
      "confidencePercent" to confidencePercent.coerceIn(0, 100),
      "allowedUids" to allowedUids,
      "createdAt" to FieldValue.serverTimestamp(),
    )

    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  fun observeReactions(roomCode: String, currentUserId: String): Flow<List<ReactionEvent>> {
    val normalizedCode = roomCode.trim().uppercase()
    val path = "rooms/$normalizedCode/reactions"
    return db.collection("rooms")
      .document(normalizedCode)
      .collection("reactions")
      .whereArrayContains("allowedUids", currentUserId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          ReactionEvent::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }
}

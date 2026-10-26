package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.automirrored.filled.StopScreenShare
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.cv.GestureReactionEngine
import com.example.data.model.GestureType
import com.example.ui.components.DeliverablesCodeSheet
import com.example.ui.components.FloatingEmojiOverlay
import com.example.ui.components.LandmarkMeshOverlay
import com.example.ui.theme.DangerRed
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.PulseRose
import com.example.ui.theme.SignalEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VisionCyan
import com.example.ui.viewmodel.ActiveCallUiState
import kotlin.math.roundToInt

@Composable
fun VideoCallScreen(
  callState: ActiveCallUiState,
  onSelectCvPreset: (GestureType?) -> Unit,
  onUpdateMouthPucker: (Float) -> Unit,
  onUpdateHandHeartGap: (Float) -> Unit,
  onManualReaction: (GestureType) -> Unit,
  onToggleMute: () -> Unit,
  onToggleCamera: () -> Unit,
  onFlipCamera: () -> Unit,
  onToggleScreenShare: () -> Unit,
  onToggleLandmarkMesh: () -> Unit,
  onToggleCvInspector: () -> Unit,
  onEndCall: () -> Unit,
) {
  BackHandler {
    onEndCall()
  }

  val context = LocalContext.current
  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    )
  }
  var showDeliverables by remember { mutableStateOf(false) }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
  }

  LaunchedEffect(Unit) {
    if (!hasCameraPermission) {
      permissionLauncher.launch(
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
      )
    }
  }

  val roomCode = callState.room?.roomCode ?: "SOLO"
  val peerName = when {
    callState.room?.guestName?.isNotBlank() == true -> callState.room.guestName
    callState.isSoloDemoRoom -> "Maya Lin (AI CV Studio Peer)"
    else -> "Waiting for Peer..."
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(ObsidianBg)
  ) {
    val density = LocalDensity.current
    val maxWidthPx = with(density) { maxWidth.toPx() }
    val maxHeightPx = with(density) { maxHeight.toPx() }
    val pipWidthPx = with(density) { 134.dp.toPx() }
    val pipHeightPx = with(density) { 192.dp.toPx() }

    var pipOffsetX by remember(maxWidthPx) {
      mutableFloatStateOf((maxWidthPx - pipWidthPx - 36f).coerceAtLeast(24f))
    }
    var pipOffsetY by remember { mutableFloatStateOf(180f) }

    // 1. Full-Screen Remote Video Feed Canvas
    RemoteParticipantVideoBackdrop(
      peerName = peerName,
      isConnected = callState.isSoloDemoRoom || callState.room?.status == "active",
      isScreenSharing = callState.isScreenSharing,
      roomCode = roomCode,
    )

    // 2. 60fps Floating 3D-Style Emoji Particle Overlay Canvas (Synced across Peer & Local)
    FloatingEmojiOverlay(
      burstTriggers = callState.burstTriggers,
      modifier = Modifier.fillMaxSize(),
    )

    // 3. Top Call Room HUD Bar (Room Code + Copy + Cooldown Indicator + Code Sheet)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.TopCenter)
        .windowInsetsPadding(WindowInsets.statusBars)
        .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = ObsidianCard.copy(alpha = 0.86f),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp)),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
          ) {
            Box(
              modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(SignalEmerald)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "ROOM $roomCode",
                  fontFamily = JetBrainsMonoFamily,
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = VisionCyan,
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                  onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Room Code", roomCode))
                    Toast.makeText(context, "Copied room code $roomCode", Toast.LENGTH_SHORT).show()
                  },
                  modifier = Modifier
                    .size(28.dp)
                    .testTag("call_copy_room_code_button"),
                ) {
                  Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Room Code",
                    tint = TextSecondary,
                    modifier = Modifier.size(15.dp),
                  )
                }
              }
              Text(
                text = callState.latestEvaluation.diagnosticFormula,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 10.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          // 2.0s Cooldown Progress Ring + Inspector & Code Toggles
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            val cooldownProgress =
              (callState.cooldownRemainingMs.toFloat() / GestureReactionEngine.COOLDOWN_MS.toFloat())
                .coerceIn(0f, 1f)
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier.size(38.dp),
            ) {
              CircularProgressIndicator(
                progress = { if (cooldownProgress > 0f) cooldownProgress else 1f },
                modifier = Modifier.size(36.dp),
                color = if (cooldownProgress > 0f) PulseRose else SignalEmerald,
                strokeWidth = 3.dp,
              )
              Text(
                text = if (cooldownProgress > 0f) {
                  "${"%.1f".format(callState.cooldownRemainingMs / 1000f)}s"
                } else {
                  "RDY"
                },
                fontFamily = JetBrainsMonoFamily,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
              )
            }

            IconButton(
              onClick = onToggleCvInspector,
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (callState.showCvInspector) VisionCyan.copy(alpha = 0.22f) else Color.Transparent)
                .testTag("toggle_cv_inspector_button"),
            ) {
              Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Toggle AI Gesture Inspector",
                tint = VisionCyan,
                modifier = Modifier.size(20.dp),
              )
            }

            IconButton(
              onClick = { showDeliverables = true },
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .testTag("open_deliverables_call_button"),
            ) {
              Icon(
                imageVector = Icons.Default.Code,
                contentDescription = "View Code Deliverables",
                tint = TextPrimary,
                modifier = Modifier.size(20.dp),
              )
            }
          }
        }
      }
    }

    // 4. Draggable Picture-in-Picture (PiP) Local Video Feed + 21-Point Landmark Mesh Overlay
    Surface(
      modifier = Modifier
        .offset { IntOffset(pipOffsetX.roundToInt(), pipOffsetY.roundToInt()) }
        .size(width = 134.dp, height = 192.dp)
        .pointerInput(maxWidthPx, maxHeightPx) {
          detectDragGestures { change, dragAmount ->
            change.consume()
            pipOffsetX = (pipOffsetX + dragAmount.x).coerceIn(16f, (maxWidthPx - pipWidthPx - 16f).coerceAtLeast(16f))
            pipOffsetY = (pipOffsetY + dragAmount.y).coerceIn(120f, (maxHeightPx - pipHeightPx - 220f).coerceAtLeast(120f))
          }
        }
        .border(
          width = 2.dp,
          brush = Brush.linearGradient(listOf(VisionCyan, PulseRose)),
          shape = RoundedCornerShape(20.dp),
        )
        .testTag("draggable_pip_local_video"),
      shape = RoundedCornerShape(20.dp),
      color = Color(0xFF0B1324),
      shadowElevation = 12.dp,
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        if (callState.isCameraOn && hasCameraPermission) {
          CameraXLocalPreview(
            isFrontCamera = callState.isFrontCamera,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          // Simulated local camera backdrop when camera is toggled off or in headless emulator
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                )
              ),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = if (callState.isCameraOn) "LIVE CV RIG" else "CAM OFF",
              fontFamily = JetBrainsMonoFamily,
              fontSize = 10.sp,
              color = TextSecondary,
            )
          }
        }

        // 21-Point Hand Skeleton & Face Blendshape Mesh Overlay
        if (callState.showLandmarkMesh) {
          LandmarkMeshOverlay(
            hands = callState.currentHands,
            face = callState.currentFace,
            modifier = Modifier.fillMaxSize(),
          )
        }

        // PiP Top Badge
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "YOU (DRAG PiP)",
            fontFamily = JetBrainsMonoFamily,
            fontSize = 9.sp,
            color = VisionCyan,
          )
          Icon(
            imageVector = Icons.Default.Cameraswitch,
            contentDescription = "Flip Camera",
            tint = TextPrimary,
            modifier = Modifier
              .size(14.dp)
              .pointerInput(Unit) {
                // Tap handled by bottom control bar as well
              },
          )
        }

        // PiP Bottom Detected Pose Badge
        Surface(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth(),
          color = Color.Black.copy(alpha = 0.65f),
        ) {
          Text(
            text = callState.latestEvaluation.detectedGesture?.let {
              "${it.primaryEmoji} ${it.id} (${callState.latestEvaluation.confidencePercent}%)"
            } ?: "CV Tracking 60fps",
            fontFamily = JetBrainsMonoFamily,
            fontSize = 10.sp,
            color = if (callState.latestEvaluation.detectedGesture != null) PulseRose else TextPrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            maxLines = 1,
          )
        }
      }
    }

    // 5. Bottom Controls Stack: AI Gesture & Blendshape Inspector + Manual Emoji Fallback Bar + Call Controls
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomCenter)
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      // A. Collapsible Client-Side AI Gesture & Facial Blendshape Inspector
      AnimatedVisibility(visible = callState.showCvInspector) {
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = ObsidianCard.copy(alpha = 0.90f),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, VisionCyan.copy(alpha = 0.28f), RoundedCornerShape(20.dp)),
        ) {
          Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "AI GESTURE & BLENDSHAPE SIMULATOR (MEDIAPIPE MATH)",
                style = MaterialTheme.typography.labelSmall,
                color = VisionCyan,
              )
              Text(
                text = if (callState.cooldownRemainingMs > 0L) {
                  "Cooldown: ${"%.1f".format(callState.cooldownRemainingMs / 1000f)}s"
                } else {
                  "Ready (0.0s)"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (callState.cooldownRemainingMs > 0L) PulseRose else SignalEmerald,
              )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Pose Presets Row
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              GestureType.entries.forEach { gesture ->
                val isSelected = callState.activePresetPose == gesture
                FilterChip(
                  selected = isSelected,
                  onClick = { onSelectCvPreset(gesture) },
                  label = { Text("${gesture.primaryEmoji} ${gesture.label}") },
                  modifier = Modifier.testTag("cv_preset_${gesture.id.lowercase()}"),
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PulseRose.copy(alpha = 0.25f),
                    selectedLabelColor = TextPrimary,
                  ),
                )
              }
            }

            // Live Threshold Sliders for Hand-Heart Gap (<0.085) and Kiss MouthPucker (>0.62)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Hand-Heart Gap: ${"%.3f".format(callState.customThumbIndexGap)} (<0.085 ❤️)",
                  fontFamily = JetBrainsMonoFamily,
                  fontSize = 10.sp,
                  color = if (callState.customThumbIndexGap < 0.085f) PulseRose else TextSecondary,
                )
                Slider(
                  value = callState.customThumbIndexGap,
                  onValueChange = onUpdateHandHeartGap,
                  valueRange = 0.02f..0.30f,
                  colors = SliderDefaults.colors(thumbColor = PulseRose, activeTrackColor = PulseRose),
                  modifier = Modifier.height(26.dp),
                )
              }

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "mouthPucker: ${"%.2f".format(callState.customMouthPucker)} (>0.62 😘)",
                  fontFamily = JetBrainsMonoFamily,
                  fontSize = 10.sp,
                  color = if (callState.customMouthPucker >= 0.62f) PulseRose else TextSecondary,
                )
                Slider(
                  value = callState.customMouthPucker,
                  onValueChange = onUpdateMouthPucker,
                  valueRange = 0.0f..1.0f,
                  colors = SliderDefaults.colors(thumbColor = VisionCyan, activeTrackColor = VisionCyan),
                  modifier = Modifier.height(26.dp),
                )
              }
            }
          }
        }
      }

      // B. Manual Emoji Reaction Fallback Bar
      Surface(
        shape = RoundedCornerShape(50),
        color = ObsidianCard.copy(alpha = 0.88f),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(50)),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          GestureType.entries.forEach { gesture ->
            Surface(
              onClick = { onManualReaction(gesture) },
              shape = CircleShape,
              color = Color.White.copy(alpha = 0.06f),
              modifier = Modifier
                .size(48.dp)
                .testTag("manual_reaction_${gesture.id.lowercase()}"),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(text = gesture.primaryEmoji, fontSize = 22.sp)
              }
            }
          }
        }
      }

      // C. Bottom Video Call Control Bar (Mute/Unmute, Toggle Camera, Flip/Mesh, Share Screen, End Call)
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = ObsidianCard.copy(alpha = 0.94f),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          CallControlIconButton(
            active = !callState.isMuted,
            activeColor = Color.White.copy(alpha = 0.12f),
            inactiveColor = DangerRed.copy(alpha = 0.25f),
            icon = if (callState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
            label = if (callState.isMuted) "Unmute" else "Mute",
            testTag = "control_mute_button",
            onClick = onToggleMute,
          )

          CallControlIconButton(
            active = callState.isCameraOn,
            activeColor = Color.White.copy(alpha = 0.12f),
            inactiveColor = DangerRed.copy(alpha = 0.25f),
            icon = if (callState.isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
            label = if (callState.isCameraOn) "Camera" else "Cam Off",
            testTag = "control_camera_button",
            onClick = onToggleCamera,
          )

          CallControlIconButton(
            active = callState.showLandmarkMesh,
            activeColor = VisionCyan.copy(alpha = 0.22f),
            inactiveColor = Color.White.copy(alpha = 0.08f),
            icon = Icons.Default.GridOn,
            label = "CV Mesh",
            testTag = "control_mesh_button",
            onClick = onToggleLandmarkMesh,
          )

          CallControlIconButton(
            active = callState.isScreenSharing,
            activeColor = SignalEmerald.copy(alpha = 0.25f),
            inactiveColor = Color.White.copy(alpha = 0.12f),
            icon = if (callState.isScreenSharing) Icons.AutoMirrored.Filled.StopScreenShare else Icons.AutoMirrored.Filled.ScreenShare,
            label = if (callState.isScreenSharing) "Sharing" else "Share",
            testTag = "control_screenshare_button",
            onClick = onToggleScreenShare,
          )

          CallControlIconButton(
            active = true,
            activeColor = Color.White.copy(alpha = 0.12f),
            inactiveColor = Color.White.copy(alpha = 0.12f),
            icon = Icons.Default.Cameraswitch,
            label = "Flip",
            testTag = "control_flip_camera_button",
            onClick = onFlipCamera,
          )

          // End Call Button
          Surface(
            onClick = onEndCall,
            shape = CircleShape,
            color = DangerRed,
            modifier = Modifier
              .size(52.dp)
              .testTag("control_end_call_button"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.CallEnd,
                contentDescription = "End Call",
                tint = Color.White,
                modifier = Modifier.size(24.dp),
              )
            }
          }
        }
      }
    }

    if (showDeliverables) {
      DeliverablesCodeSheet(onDismiss = { showDeliverables = false })
    }
  }
}

@Composable
private fun CallControlIconButton(
  active: Boolean,
  activeColor: Color,
  inactiveColor: Color,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  testTag: String,
  onClick: () -> Unit,
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Surface(
      onClick = onClick,
      shape = CircleShape,
      color = if (active) activeColor else inactiveColor,
      modifier = Modifier
        .size(48.dp)
        .testTag(testTag),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = label,
          tint = TextPrimary,
          modifier = Modifier.size(22.dp),
        )
      }
    }
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      fontSize = 10.sp,
      color = TextSecondary,
    )
  }
}

@Composable
private fun RemoteParticipantVideoBackdrop(
  peerName: String,
  isConnected: Boolean,
  isScreenSharing: Boolean,
  roomCode: String,
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.radialGradient(
          colors = listOf(
            Color(0xFF1E293B),
            Color(0xFF0F172A),
            ObsidianBg,
          ),
          radius = 1200f,
        )
      ),
    contentAlignment = Alignment.Center,
  ) {
    // Stylized Remote Video Feed Canvas with studio lighting & face target zone for Kiss reactions
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height

      // Ambient studio key light glow
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            PulseRose.copy(alpha = 0.16f),
            VisionCyan.copy(alpha = 0.08f),
            Color.Transparent,
          ),
          center = Offset(w * 0.5f, h * 0.40f),
          radius = w * 0.65f,
        ),
        radius = w * 0.65f,
        center = Offset(w * 0.5f, h * 0.40f),
      )

      // Stylized Remote Caller Silhouette (Head & Shoulders in 9:16 Video Frame)
      val headCenter = Offset(w * 0.5f, h * 0.39f)
      val headRadius = w.coerceAtMost(h) * 0.14f

      drawCircle(
        color = Color(0xFF334155).copy(alpha = 0.55f),
        radius = headRadius,
        center = headCenter,
      )
      drawCircle(
        color = VisionCyan.copy(alpha = 0.35f),
        radius = headRadius * 1.08f,
        center = headCenter,
        style = Stroke(width = 3f),
      )

      // Shoulders arc
      drawOval(
        color = Color(0xFF1E293B).copy(alpha = 0.75f),
        topLeft = Offset(w * 0.24f, h * 0.53f),
        size = androidx.compose.ui.geometry.Size(w * 0.52f, h * 0.26f),
      )
    }

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .align(Alignment.TopStart)
        .padding(start = 18.dp, top = 110.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.50f),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(if (isConnected) SignalEmerald else Color(0xFFF59E0B))
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = peerName,
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary,
          )
        }
      }

      if (isScreenSharing) {
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = SignalEmerald.copy(alpha = 0.22f),
        ) {
          Text(
            text = "🖥️ LIVE SCREEN SHARE ACTIVE (ROOM $roomCode)",
            fontFamily = JetBrainsMonoFamily,
            fontSize = 10.sp,
            color = SignalEmerald,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun CameraXLocalPreview(
  isFrontCamera: Boolean,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val previewView = remember {
    PreviewView(context).apply {
      implementationMode = PreviewView.ImplementationMode.COMPATIBLE
      scaleType = PreviewView.ScaleType.FILL_CENTER
    }
  }

  LaunchedEffect(isFrontCamera) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener(
      {
        try {
          val cameraProvider = cameraProviderFuture.get()
          val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
          }
          val selector = if (isFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
          } else {
            CameraSelector.DEFAULT_BACK_CAMERA
          }
          cameraProvider.unbindAll()
          cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
        } catch (_: Exception) {
          // Gracefully handle emulators without physical camera hardware
        }
      },
      ContextCompat.getMainExecutor(context)
    )
  }

  AndroidView(
    factory = { previewView },
    modifier = modifier,
  )
}

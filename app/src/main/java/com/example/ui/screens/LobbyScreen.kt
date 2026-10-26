package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CallRoom
import com.example.data.model.GestureType
import com.example.ui.components.DeliverablesCodeSheet
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.PulseRose
import com.example.ui.theme.SignalEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VisionCyan
import com.example.ui.viewmodel.UiState

@Composable
fun LobbyScreen(
  userDisplayName: String,
  hostedRoomsState: UiState<List<CallRoom>>,
  lobbyMessage: String?,
  onDismissLobbyMessage: () -> Unit,
  onCreateRoom: (String, Boolean) -> Unit,
  onJoinRoomByCode: (String) -> Unit,
  onOpenExistingRoom: (CallRoom) -> Unit,
  onDeleteRoom: (String) -> Unit,
  onSignOut: () -> Unit,
) {
  val context = LocalContext.current
  var roomTitleInput by remember { mutableStateOf("1-on-1 Design & Emoji Sync") }
  var joinCodeInput by remember { mutableStateOf("") }
  var showDeliverables by remember { mutableStateOf(false) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(Color(0xFF0D1527), ObsidianBg, Color(0xFF130D21))
        )
      )
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      // Top Header Bar
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(PulseRose, VisionCyan))),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = "PulseCall Logo",
                tint = Color.White,
                modifier = Modifier.size(24.dp),
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "PulseCall",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
              )
              Text(
                text = "Signed in as $userDisplayName",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
              )
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconButton(
              onClick = { showDeliverables = true },
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(ObsidianCard)
                .testTag("open_deliverables_lobby_button"),
            ) {
              Icon(
                imageVector = Icons.Default.Code,
                contentDescription = "View MediaPipe & WebRTC Code",
                tint = VisionCyan,
              )
            }
            IconButton(
              onClick = onSignOut,
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(ObsidianCard)
                .testTag("sign_out_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = "Sign Out",
                tint = TextSecondary,
              )
            }
          }
        }
      }

      // Error / Info Banner
      if (lobbyMessage != null) {
        item {
          Surface(
            onClick = onDismissLobbyMessage,
            modifier = Modifier
              .fillMaxWidth()
              .widthIn(max = 600.dp),
            shape = RoundedCornerShape(14.dp),
            color = PulseRose.copy(alpha = 0.18f),
          ) {
            Text(
              text = lobbyMessage,
              color = Color(0xFFFECDD3),
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(14.dp),
            )
          }
        }
      }

      // Hero Card: Instant Solo AI Gesture Studio + 1-on-1 Room Creation
      item {
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .border(1.dp, VisionCyan.copy(alpha = 0.28f), RoundedCornerShape(24.dp)),
          shape = RoundedCornerShape(24.dp),
          color = ObsidianCard,
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = VisionCyan.copy(alpha = 0.18f),
              ) {
                Text(
                  text = "CLIENT-SIDE AI VISION + WEBRTC SYNC",
                  style = MaterialTheme.typography.labelSmall,
                  color = VisionCyan,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
              text = "Start a 1-on-1 Video Call with Live Gesture Reactions",
              style = MaterialTheme.typography.headlineMedium,
              color = TextPrimary,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = "Form a two-hand heart ❤️, pucker for a kiss 😘, give one or two thumbs up 👍🎆, or flash a peace sign ✌️ to launch synchronized 60fps 3D emoji animations.",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
              value = roomTitleInput,
              onValueChange = { roomTitleInput = it },
              label = { Text("Call Room Topic") },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("room_title_input"),
              shape = RoundedCornerShape(14.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PulseRose,
                unfocusedBorderColor = ObsidianSurfaceVariant,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
              ),
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Button(
                onClick = { onCreateRoom(roomTitleInput, false) },
                modifier = Modifier
                  .weight(1f)
                  .height(52.dp)
                  .testTag("create_room_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PulseRose),
              ) {
                Icon(Icons.Default.VideoCall, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create 1-on-1 Room", fontWeight = FontWeight.Bold)
              }

              Button(
                onClick = { onCreateRoom(roomTitleInput, true) },
                modifier = Modifier
                  .weight(1f)
                  .height(52.dp)
                  .testTag("launch_solo_studio_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = VisionCyan,
                  contentColor = ObsidianBg,
                ),
              ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Gesture Studio", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // Join Existing Room by Code Card
      item {
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp)),
          shape = RoundedCornerShape(22.dp),
          color = ObsidianCard,
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            Text(
              text = "Join a Friend's Room by Code",
              style = MaterialTheme.typography.titleLarge,
              color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Enter the 6-character shareable room code (e.g., PLS842) to connect.",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              OutlinedTextField(
                value = joinCodeInput,
                onValueChange = { joinCodeInput = it.uppercase().take(16) },
                placeholder = { Text("PLS842") },
                singleLine = true,
                modifier = Modifier
                  .weight(1f)
                  .testTag("join_room_code_input"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = VisionCyan,
                  unfocusedBorderColor = ObsidianSurfaceVariant,
                  focusedTextColor = TextPrimary,
                  unfocusedTextColor = TextPrimary,
                ),
              )
              Button(
                onClick = { onJoinRoomByCode(joinCodeInput) },
                modifier = Modifier
                  .height(54.dp)
                  .testTag("join_room_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SignalEmerald),
              ) {
                Icon(Icons.Default.MeetingRoom, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Join Call", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // Gesture Reference Matrix Card
      item {
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(22.dp)),
          shape = RoundedCornerShape(22.dp),
          color = ObsidianCard,
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "MediaPipe Gesture & Blendshape Thresholds",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
              )
              Text(
                text = "2.0s Cooldown",
                style = MaterialTheme.typography.labelSmall,
                color = VisionCyan,
              )
            }
            Spacer(modifier = Modifier.height(12.dp))
            GestureType.entries.forEach { gesture ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Box(
                  modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianSurfaceVariant),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(text = gesture.primaryEmoji, fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = gesture.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                  )
                  Text(
                    text = gesture.blendshapeFormula,
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 11.sp,
                    color = VisionCyan,
                  )
                }
              }
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
              onClick = { showDeliverables = true },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("view_full_code_button"),
              shape = RoundedCornerShape(12.dp),
            ) {
              Icon(Icons.Default.Code, contentDescription = null, tint = VisionCyan, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Inspect useGestureReactions.js & Full Project Structure", color = VisionCyan)
            }
          }
        }
      }

      // Recent Hosted Rooms Section
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Your Hosted Call Rooms",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
          )
        }
      }

      when (hostedRoomsState) {
        is UiState.Loading -> {
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator(color = VisionCyan)
            }
          }
        }

        is UiState.Error -> {
          item {
            Text(
              text = hostedRoomsState.message,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodyMedium,
            )
          }
        }

        is UiState.Success -> {
          val rooms = hostedRoomsState.data
          if (rooms.isEmpty()) {
            item {
              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .widthIn(max = 600.dp),
                shape = RoundedCornerShape(18.dp),
                color = ObsidianCard.copy(alpha = 0.6f),
              ) {
                Column(
                  modifier = Modifier.padding(24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Text(text = "📹 ✨ ❤️", fontSize = 28.sp)
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    text = "No rooms created yet. Tap 'Create 1-on-1 Room' or 'AI Gesture Studio' above to launch!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                  )
                }
              }
            }
          } else {
            items(rooms, key = { it.roomCode }) { room ->
              HostedRoomCard(
                room = room,
                onRejoin = { onOpenExistingRoom(room) },
                onCopyCode = {
                  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                  clipboard.setPrimaryClip(ClipData.newPlainText("PulseCall Room Code", room.roomCode))
                  Toast.makeText(context, "Copied room code ${room.roomCode}", Toast.LENGTH_SHORT).show()
                },
                onDelete = { onDeleteRoom(room.roomCode) },
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
private fun HostedRoomCard(
  room: CallRoom,
  onRejoin: () -> Unit,
  onCopyCode: () -> Unit,
  onDelete: () -> Unit,
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .widthIn(max = 600.dp)
      .clip(RoundedCornerShape(18.dp))
      .clickable(onClick = onRejoin)
      .testTag("hosted_room_card_${room.roomCode}"),
    shape = RoundedCornerShape(18.dp),
    color = ObsidianCard,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = VisionCyan.copy(alpha = 0.16f),
          ) {
            Text(
              text = room.roomCode,
              fontFamily = JetBrainsMonoFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = VisionCyan,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          val statusColor = when (room.status) {
            "active" -> SignalEmerald
            "waiting" -> Color(0xFFF59E0B)
            else -> TextMuted
          }
          Text(
            text = "• ${room.status.uppercase()}",
            style = MaterialTheme.typography.labelSmall,
            color = statusColor,
          )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = room.roomTitle,
          style = MaterialTheme.typography.titleMedium,
          color = TextPrimary,
        )
        Text(
          text = if (room.guestName.isNotBlank()) "Peer: ${room.guestName}" else "Waiting for peer to join...",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onCopyCode,
          modifier = Modifier.testTag("copy_room_code_${room.roomCode}"),
        ) {
          Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = "Copy Room Code",
            tint = VisionCyan,
          )
        }
        IconButton(
          onClick = onDelete,
          modifier = Modifier.testTag("delete_room_${room.roomCode}"),
        ) {
          Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = "Delete Room",
            tint = TextSecondary,
          )
        }
      }
    }
  }
}

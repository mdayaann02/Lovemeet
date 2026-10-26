package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.data.model.GestureType
import com.example.ui.components.DeliverablesCodeSheet
import com.example.ui.components.FloatingEmojiOverlay
import com.example.ui.components.ReactionBurstTrigger
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.PulseRose
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VisionCyan
import java.util.UUID

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }

  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var showDeliverables by remember { mutableStateOf(false) }
  var previewBursts by remember {
    mutableStateOf(
      listOf(
        ReactionBurstTrigger("init_heart", GestureType.HEART, 50, 88, "Preview")
      )
    )
  }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = {},
      scope = scope,
    )
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(Color(0xFF0F172A), ObsidianBg, Color(0xFF160B1E))
        )
      )
  ) {
    FloatingEmojiOverlay(burstTriggers = previewBursts)

    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Column(
        modifier = Modifier.widthIn(max = 520.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // Glowing Emblem
        Box(
          modifier = Modifier
            .size(88.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(
              Brush.linearGradient(listOf(PulseRose, VisionCyan))
            )
            .padding(2.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(ObsidianCard),
          contentAlignment = Alignment.Center,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Videocam,
              contentDescription = "Video Call",
              tint = VisionCyan,
              modifier = Modifier.size(34.dp),
            )
            Icon(
              imageVector = Icons.Default.Favorite,
              contentDescription = "Heart Gesture",
              tint = PulseRose,
              modifier = Modifier.size(28.dp),
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
          text = stringResource(R.string.app_name),
          style = MaterialTheme.typography.displayLarge,
          color = TextPrimary,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "1-on-1 Live Video Calling with Client-Side AI Hand Gesture & Facial Expression 3D Emoji Reactions",
          style = MaterialTheme.typography.bodyLarge,
          color = TextSecondary,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Interactive Gesture Preview Pills (works right on the sign-in screen!)
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp)),
          shape = RoundedCornerShape(20.dp),
          color = ObsidianCard.copy(alpha = 0.88f),
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text(
              text = "LIVE 60FPS EMOJI PARTICLE ENGINE — TAP TO PREVIEW",
              style = MaterialTheme.typography.labelSmall,
              color = VisionCyan,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
              listOf(
                GestureType.HEART,
                GestureType.KISS,
                GestureType.THUMBS_UP,
                GestureType.FIREWORKS,
                GestureType.PEACE,
              ).forEach { gesture ->
                Surface(
                  onClick = {
                    previewBursts = previewBursts.takeLast(12) + ReactionBurstTrigger(
                      id = UUID.randomUUID().toString(),
                      gestureType = gesture,
                      originXPercent = 50,
                      originYPercent = if (gesture == GestureType.KISS) 42 else 85,
                      senderName = "Preview",
                    )
                  },
                  shape = CircleShape,
                  color = Color.White.copy(alpha = 0.07f),
                  modifier = Modifier
                    .size(52.dp)
                    .testTag("auth_preview_gesture_${gesture.id.lowercase()}"),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(text = gesture.primaryEmoji, fontSize = 24.sp)
                  }
                }
              }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "❤️ Two-Hand Heart • 😘 Kiss Face • 👍 Thumbs Up • 🎆 Fireworks • ✌️ Peace",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondary,
              textAlign = TextAlign.Center,
            )
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (errorMessage != null) {
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 14.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
          ) {
            Text(
              text = errorMessage!!,
              color = Color(0xFFFCA5A5),
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(12.dp),
              textAlign = TextAlign.Center,
            )
          }
        }

        Button(
          onClick = {
            errorMessage = null
            isLoading = true
            onGoogleSignInClicked(
              context = context,
              credentialManager = credentialManager,
              onAuthSuccess = {
                isLoading = false
                onAuthSuccess()
              },
              onAuthError = { msg ->
                isLoading = false
                errorMessage = msg
              },
              scope = scope,
              onAuthCancelled = {
                isLoading = false
              },
            )
          },
          enabled = !isLoading,
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("google_sign_in_button"),
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = PulseRose,
            contentColor = TextPrimary,
          ),
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(24.dp),
              color = TextPrimary,
              strokeWidth = 2.5.dp,
            )
          } else {
            Text(
              text = "Sign in with Google",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
          onClick = { showDeliverables = true },
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("open_deliverables_auth_button"),
          shape = RoundedCornerShape(16.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Code,
            contentDescription = "View Code & Math Spec",
            tint = VisionCyan,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "View MediaPipe Hook & WebRTC Code",
            color = VisionCyan,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }

    if (showDeliverables) {
      DeliverablesCodeSheet(onDismiss = { showDeliverables = false })
    }
  }
}

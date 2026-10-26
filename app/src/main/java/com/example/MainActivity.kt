package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.repository.CallRoomRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.LobbyScreen
import com.example.ui.screens.VideoCallScreen
import com.example.ui.theme.PulseCallTheme
import com.example.ui.viewmodel.PulseCallViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      PulseCallTheme {
        PulseCallAppNavigation()
      }
    }
  }
}

@Composable
fun PulseCallAppNavigation(auth: FirebaseAuth = Firebase.auth) {
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser

  if (user == null) {
    AuthScreen(onAuthSuccess = {})
  } else {
    AuthenticatedPulseCallApp(
      currentUserId = user.uid,
      userDisplayName = user.displayName?.takeIf { it.isNotBlank() } ?: user.email ?: "Pulse Caller",
    )
  }
}

@Composable
private fun AuthenticatedPulseCallApp(
  currentUserId: String,
  userDisplayName: String,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }

  val viewModel: PulseCallViewModel = viewModel(
    key = currentUserId,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) {
          "APPLICATION_KEY missing from CreationExtras"
        }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        PulseCallViewModel(CallRoomRepository(db), currentUserId)
      }
    }
  )

  val hostedRoomsState by viewModel.hostedRoomsState.collectAsStateWithLifecycle()
  val callState by viewModel.callState.collectAsStateWithLifecycle()
  val lobbyMessage by viewModel.lobbyMessage.collectAsStateWithLifecycle()

  if (callState.room != null) {
    VideoCallScreen(
      callState = callState,
      onSelectCvPreset = viewModel::selectCvPosePreset,
      onUpdateMouthPucker = viewModel::updateMouthPuckerBlendshape,
      onUpdateHandHeartGap = viewModel::updateHandHeartDistance,
      onManualReaction = viewModel::sendManualReaction,
      onToggleMute = viewModel::toggleMute,
      onToggleCamera = viewModel::toggleCamera,
      onFlipCamera = viewModel::flipCameraLens,
      onToggleScreenShare = viewModel::toggleScreenShare,
      onToggleLandmarkMesh = viewModel::toggleLandmarkMesh,
      onToggleCvInspector = viewModel::toggleCvInspector,
      onEndCall = viewModel::leaveAndEndCall,
    )
  } else {
    LobbyScreen(
      userDisplayName = userDisplayName,
      hostedRoomsState = hostedRoomsState,
      lobbyMessage = lobbyMessage,
      onDismissLobbyMessage = viewModel::clearLobbyMessage,
      onCreateRoom = viewModel::createAndEnterRoom,
      onJoinRoomByCode = viewModel::joinAndEnterRoom,
      onOpenExistingRoom = viewModel::openExistingHostedRoom,
      onDeleteRoom = viewModel::deleteHostedRoom,
      onSignOut = {
        signOutUser(
          credentialManager = credentialManager,
          onSignOutComplete = {},
          scope = scope,
        )
      },
    )
  }
}

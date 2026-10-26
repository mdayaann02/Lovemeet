package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.PulseRose
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VisionCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliverablesCodeSheet(
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val context = LocalContext.current
  var selectedTab by remember { mutableIntStateOf(0) }

  val tabs = listOf(
    "1. Folder Structure",
    "2. useGestureReactions.js",
    "3. Call & Emoji UI",
    "4. Setup & Math Spec",
  )

  val activeContent = remember(selectedTab) {
    when (selectedTab) {
      0 -> FOLDER_STRUCTURE_DOC
      1 -> USE_GESTURE_REACTIONS_HOOK_CODE
      2 -> VIDEO_CALL_AND_CANVAS_CODE
      else -> SETUP_AND_MATH_GUIDE
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = ObsidianCard,
    contentColor = TextPrimary,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Full-Stack & MediaPipe Deliverables",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
          )
          Text(
            text = "Complete Android + React/Vite/MediaPipe/WebRTC reference code",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
          )
        }
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.testTag("close_deliverables_button")
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close deliverables", tint = TextPrimary)
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        tabs.forEachIndexed { index, title ->
          FilterChip(
            selected = selectedTab == index,
            onClick = { selectedTab = index },
            label = { Text(title) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = PulseRose.copy(alpha = 0.22f),
              selectedLabelColor = PulseRose,
            ),
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
      ) {
        Button(
          onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(tabs[selectedTab], activeContent))
            Toast.makeText(context, "Copied ${tabs[selectedTab]} to clipboard", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = VisionCyan, contentColor = ObsidianBg),
          modifier = Modifier.testTag("copy_deliverable_code_button"),
        ) {
          Icon(Icons.Default.ContentCopy, contentDescription = "Copy code", modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Copy Code", fontWeight = FontWeight.Bold)
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        shape = RoundedCornerShape(14.dp),
        color = ObsidianBg,
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .horizontalScroll(rememberScrollState())
            .padding(14.dp)
        ) {
          Text(
            text = activeContent,
            fontFamily = JetBrainsMonoFamily,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = TextPrimary,
          )
        }
      }
    }
  }
}

private const val FOLDER_STRUCTURE_DOC = """
# Complete Project Folder Structure (Native Android + Web/Vite Reference)

## 1. Native Android App (PulseCall — Kotlin + Jetpack Compose + CameraX + Firestore Sync)
pulsecall-android/
├── app/src/main/java/com/example/
│   ├── MainActivity.kt                        # Edge-to-Edge host & Auth-gated root navigation
│   ├── auth/
│   │   └── AuthManager.kt                     # Google Sign-In via Jetpack CredentialManager
│   ├── cv/
│   │   ├── GestureReactionEngine.kt           # 21-point Hand & 52-Blendshape math + 2s cooldown
│   │   └── CameraFrameAnalyzer.kt             # CameraX ImageAnalysis pipeline for live video
│   ├── data/
│   │   ├── FirestoreErrorHandler.kt           # Structured JSON diagnostic error logging
│   │   ├── model/CallModels.kt                # CallRoom, ReactionEvent, GestureType models
│   │   └── repository/CallRoomRepository.kt   # Real-time Firestore signaling & reaction sync
│   └── ui/
│       ├── components/
│       │   ├── FloatingEmojiOverlay.kt        # 60fps 3D emoji particle fountain & mesh HUD
│       │   └── DeliverablesCodeSheet.kt       # Full-stack MediaPipe / WebRTC code reference
│       ├── screens/
│       │   ├── AuthScreen.kt                  # Google Sign-In onboarding screen
│       │   ├── LobbyScreen.kt                 # Create/Join room by code + Solo Studio launch
│       │   └── VideoCallScreen.kt             # Full-screen remote + draggable PiP + CV HUD
│       └── viewmodel/
│           └── PulseCallViewModel.kt          # Room state, WebRTC signaling, & 2s CV cooldown
├── firebase-blueprint.json                    # Firestore schema blueprint
├── firestore.rules                            # Hardened zero-trust ABAC security rules
└── firestore.test.js                          # Rapid Node.js security rules test suite

## 2. Web Stack (React + Vite + Tailwind + @mediapipe/tasks-vision + WebRTC/Socket.io)
pulsecall-web/
├── server/
│   ├── package.json
│   └── index.js                               # Node.js + Socket.io WebRTC signaling & reaction relay
└── client/
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── App.jsx
        ├── hooks/
        │   ├── useGestureReactions.js         # MediaPipe HandLandmarker + FaceLandmarker hook
        │   └── useWebRTCCall.js               # RTCPeerConnection + RTCDataChannel("reactions")
        └── components/
            ├── VideoCallRoom.jsx              # Full-screen remote + draggable PiP + control bar
            └── EmojiOverlayCanvas.jsx         # 60fps requestAnimationFrame 3D emoji engine
"""

private const val USE_GESTURE_REACTIONS_HOOK_CODE = """
// src/hooks/useGestureReactions.js
// Client-side AI Gesture & Facial Expression Detection with @mediapipe/tasks-vision
import { useEffect, useRef, useState, useCallback } from "react";
import {
  FilesetResolver,
  HandLandmarker,
  FaceLandmarker,
} from "@mediapipe/tasks-vision";

const COOLDOWN_MS = 2000; // 2-second cooldown between auto-detections

// Euclidean distance in normalized [0..1] image coordinates
const dist2D = (a, b) => Math.hypot(a.x - b.x, a.y - b.y);

export function useGestureReactions({ videoRef, enabled = true, onReactionDetected }) {
  const handLandmarkerRef = useRef(null);
  const faceLandmarkerRef = useRef(null);
  const lastTriggerTimeRef = useRef(0);
  const rafIdRef = useRef(null);
  const [isReady, setIsReady] = useState(false);
  const [diagnostics, setDiagnostics] = useState({ gesture: null, mouthPucker: 0 });

  // 1. Initialize MediaPipe HandLandmarker (2 hands) & FaceLandmarker (blendshapes enabled)
  useEffect(() => {
    let cancelled = false;
    async function initMediaPipe() {
      const vision = await FilesetResolver.forVisionTasks(
        "https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@0.10.14/wasm"
      );
      const [handLandmarker, faceLandmarker] = await Promise.all([
        HandLandmarker.createFromOptions(vision, {
          baseOptions: {
            modelAssetPath:
              "https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task",
            delegate: "GPU",
          },
          runningMode: "VIDEO",
          numHands: 2,
          minHandDetectionConfidence: 0.6,
          minTrackingConfidence: 0.6,
        }),
        FaceLandmarker.createFromOptions(vision, {
          baseOptions: {
            modelAssetPath:
              "https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task",
            delegate: "GPU",
          },
          runningMode: "VIDEO",
          numFaces: 1,
          outputFaceBlendshapes: true,
        }),
      ]);
      if (!cancelled) {
        handLandmarkerRef.current = handLandmarker;
        faceLandmarkerRef.current = faceLandmarker;
        setIsReady(true);
      }
    }
    initMediaPipe();
    return () => {
      cancelled = true;
      cancelAnimationFrame(rafIdRef.current);
      handLandmarkerRef.current?.close();
      faceLandmarkerRef.current?.close();
    };
  }, []);

  // 2. Exact Math Helpers for Hand Heart, Finger Heart, Thumbs Up, Fireworks, Peace Sign
  const isFingerCurled = (pts, tipIdx, pipIdx) =>
    dist2D(pts[tipIdx], pts[0]) < dist2D(pts[pipIdx], pts[0]) * 1.04;

  const isThumbsUp = (pts) => {
    const thumbVertical =
      pts[4].y < pts[3].y - 0.03 && pts[4].y < pts[5].y - 0.06;
    const fourCurled =
      isFingerCurled(pts, 8, 6) &&
      isFingerCurled(pts, 12, 10) &&
      isFingerCurled(pts, 16, 14) &&
      isFingerCurled(pts, 20, 18);
    return thumbVertical && fourCurled;
  };

  const isPeaceSign = (pts) => {
    const indexUp = pts[8].y < pts[6].y - 0.045;
    const middleUp = pts[12].y < pts[10].y - 0.045;
    const ringCurled = isFingerCurled(pts, 16, 14);
    const pinkyCurled = isFingerCurled(pts, 20, 18);
    const spreadDist = dist2D(pts[8], pts[12]);
    return indexUp && middleUp && ringCurled && pinkyCurled && spreadDist > 0.06;
  };

  const detectFrame = useCallback(() => {
    const video = videoRef.current;
    if (
      enabled &&
      isReady &&
      video &&
      video.readyState >= 2 &&
      handLandmarkerRef.current &&
      faceLandmarkerRef.current
    ) {
      const nowMs = performance.now();
      const handRes = handLandmarkerRef.current.detectForVideo(video, nowMs);
      const faceRes = faceLandmarkerRef.current.detectForVideo(video, nowMs);

      const hands = handRes.landmarks || [];
      const blendshapes = faceRes.faceBlendshapes?.[0]?.categories || [];
      const getScore = (name) =>
        blendshapes.find((b) => b.categoryName === name)?.score ?? 0;

      const mouthPucker = getScore("mouthPucker");
      const mouthFunnel = getScore("mouthFunnel");
      const jawOpen = getScore("jawOpen");

      let reaction = null;

      // A. Two-Hand Heart Detection:
      // Index tips (8) meet at top arch, Thumb tips (4) meet at bottom point, PIPs (6) arch higher
      if (hands.length >= 2) {
        const [h1, h2] = hands;
        const indexGap = dist2D(h1[8], h2[8]);
        const thumbGap = dist2D(h1[4], h2[4]);
        const archValid =
          h1[6].y < h1[4].y - 0.04 &&
          h2[6].y < h2[4].y - 0.04 &&
          h1[4].y > h1[8].y + 0.03;

        if (indexGap < 0.085 && thumbGap < 0.09 && archValid) {
          reaction = { type: "HEART", emoji: "❤️", originX: 50, originY: 88 };
        }
      }

      // B. Single-Hand Finger Heart Fallback
      if (!reaction) {
        for (const pts of hands) {
          const fingerHeartDist = dist2D(pts[4], pts[8]);
          const othersCurled =
            isFingerCurled(pts, 12, 10) &&
            isFingerCurled(pts, 16, 14) &&
            isFingerCurled(pts, 20, 18);
          if (fingerHeartDist < 0.055 && othersCurled) {
            reaction = { type: "HEART", emoji: "❤️", originX: 50, originY: 88 };
            break;
          }
        }
      }

      // C. Bonus: Two Thumbs Up (Fireworks 🎆) vs Single Thumbs Up (👍)
      if (!reaction && hands.length > 0) {
        const thumbsHands = hands.filter(isThumbsUp);
        if (thumbsHands.length >= 2) {
          reaction = { type: "FIREWORKS", emoji: "🎆", originX: 50, originY: 50 };
        } else if (thumbsHands.length === 1) {
          reaction = {
            type: "THUMBS_UP",
            emoji: "👍",
            originX: Math.round((1 - thumbsHands[0][4].x) * 100),
            originY: Math.round(thumbsHands[0][4].y * 100),
          };
        }
      }

      // D. Bonus: Peace Sign (✌️)
      if (!reaction) {
        const peaceHand = hands.find(isPeaceSign);
        if (peaceHand) {
          reaction = {
            type: "PEACE",
            emoji: "✌️",
            originX: Math.round((1 - peaceHand[8].x) * 100),
            originY: Math.round(peaceHand[8].y * 100),
          };
        }
      }

      // E. Kiss Facial Expression (mouthPucker > 0.62)
      if (!reaction && mouthPucker > 0.62 && (mouthFunnel > 0.18 || jawOpen < 0.25)) {
        const noseTip = faceRes.faceLandmarks?.[0]?.[1] || { x: 0.5, y: 0.4 };
        reaction = {
          type: "KISS",
          emoji: "😘",
          originX: Math.round((1 - noseTip.x) * 100),
          originY: Math.round(noseTip.y * 100),
        };
      }

      setDiagnostics({ gesture: reaction?.type || null, mouthPucker });

      // Enforce 2000ms cooldown before triggering & syncing across WebRTC / Socket.io
      if (reaction && Date.now() - lastTriggerTimeRef.current >= COOLDOWN_MS) {
        lastTriggerTimeRef.current = Date.now();
        onReactionDetected?.(reaction);
      }
    }
    rafIdRef.current = requestAnimationFrame(detectFrame);
  }, [enabled, isReady, onReactionDetected, videoRef]);

  useEffect(() => {
    rafIdRef.current = requestAnimationFrame(detectFrame);
    return () => cancelAnimationFrame(rafIdRef.current);
  }, [detectFrame]);

  return { isReady, diagnostics };
}
"""

private const val VIDEO_CALL_AND_CANVAS_CODE = """
// src/components/VideoCallRoom.jsx
// Full-screen Remote Video + Draggable PiP Local Video + WebRTC DataChannel Reaction Sync
import React, { useRef, useState, useCallback } from "react";
import { useGestureReactions } from "../hooks/useGestureReactions";
import { EmojiOverlayCanvas } from "./EmojiOverlayCanvas";

export function VideoCallRoom({ roomCode, peerDataChannel, socket, onLeave }) {
  const localVideoRef = useRef(null);
  const remoteVideoRef = useRef(null);
  const [bursts, setBursts] = useState([]);
  const [muted, setMuted] = useState(false);
  const [cameraOn, setCameraOn] = useState(true);

  const triggerAndSyncReaction = useCallback(
    (reaction) => {
      const event = { ...reaction, id: crypto.randomUUID(), ts: Date.now() };
      setBursts((prev) => [...prev.slice(-20), event]);

      // Instant peer-to-peer sync via WebRTC DataChannel with Socket.io fallback
      if (peerDataChannel?.readyState === "open") {
        peerDataChannel.send(JSON.stringify({ type: "EMOJI_REACTION", payload: event }));
      } else {
        socket?.emit("reaction:send", { roomCode, payload: event });
      }
    },
    [peerDataChannel, socket, roomCode]
  );

  const { isReady, diagnostics } = useGestureReactions({
    videoRef: localVideoRef,
    enabled: cameraOn,
    onReactionDetected: triggerAndSyncReaction,
  });

  return (
    <div className="relative w-screen h-screen bg-slate-950 overflow-hidden select-none">
      {/* Full-Screen Remote Video Feed */}
      <video
        ref={remoteVideoRef}
        autoPlay
        playsInline
        className="w-full h-full object-cover"
      />

      {/* 60fps Floating 3D Emoji Particle Overlay Canvas */}
      <EmojiOverlayCanvas bursts={bursts} />

      {/* Draggable Picture-in-Picture (PiP) Local Video Feed */}
      <div className="absolute top-16 right-4 w-36 h-52 rounded-2xl overflow-hidden border-2 border-cyan-400/60 shadow-2xl bg-slate-900">
        <video
          ref={localVideoRef}
          autoPlay
          muted
          playsInline
          className="w-full h-full object-cover scale-x-[-1]"
        />
        <div className="absolute bottom-2 left-2 right-2 text-[11px] font-mono bg-black/60 text-cyan-300 px-2 py-0.5 rounded">
          {isReady ? "CV Ready • Pucker " + (diagnostics.mouthPucker * 100).toFixed(0) + "%" : "Loading CV..."}
        </div>
      </div>

      {/* Manual Fallback Reaction Bar */}
      <div className="absolute bottom-24 left-1/2 -translate-x-1/2 flex gap-2 bg-slate-900/80 backdrop-blur-md px-4 py-2 rounded-full border border-white/10">
        {[
          { type: "HEART", emoji: "❤️" },
          { type: "KISS", emoji: "😘" },
          { type: "THUMBS_UP", emoji: "👍" },
          { type: "FIREWORKS", emoji: "🎆" },
          { type: "PEACE", emoji: "✌️" },
        ].map((item) => (
          <button
            key={item.type}
            onClick={() => triggerAndSyncReaction({ ...item, originX: 50, originY: 85 })}
            className="text-2xl hover:scale-125 transition-transform px-2"
          >
            {item.emoji}
          </button>
        ))}
      </div>
    </div>
  );
}
"""

private const val SETUP_AND_MATH_GUIDE = """
# Step-by-Step Local Installation & Mathematical Threshold Specification

## 1. Mathematical & Blendshape Thresholds
- **Two-Hand Heart (`❤️`, `💖`)**:
  - Euclidean distance between Left & Right Index Fingertips (`landmark[8]`): `dist < 0.085`
  - Euclidean distance between Left & Right Thumb Tips (`landmark[4]`): `dist < 0.090`
  - Heart Arch Curvature: `indexPIP[6].y < thumbTip[4].y - 0.04` AND `thumbTip[4].y > indexTip[8].y + 0.03`
- **Single-Hand Finger Heart (`❤️`)**:
  - Crossed Thumb Tip (`[4]`) & Index Tip (`[8]`): `dist < 0.055` while Middle (`12`), Ring (`16`), Pinky (`20`) are curled (`dist(tip, wrist) < dist(pip, wrist)`).
- **Kiss Facial Expression (`😘`, `💋`)**:
  - MediaPipe Face Landmarker blendshape `mouthPucker >= 0.62` AND (`mouthFunnel >= 0.18` OR `jawOpen < 0.25`).
- **Thumbs Up (`👍`) & Double Thumbs Fireworks (`🎆`)**:
  - `thumbTip[4].y < thumbIP[3].y - 0.03` AND `thumbTip[4].y < indexMCP[5].y - 0.06` with all 4 fingers curled.
  - 1 Hand -> `👍` | 2 Hands Simultaneously -> `🎆` Fireworks!
- **Peace Sign (`✌️`)**:
  - Index (`8`) & Middle (`12`) extended upward (`tip.y < pip.y - 0.045`), Ring & Pinky curled, V-spread angle `>= 14°`.
- **Cooldown Timer**:
  - `COOLDOWN_MS = 2000` (2 seconds) enforced between automatic detections.

## 2. Running the Native Android App (PulseCall)
1. Sign in with Google on the welcome screen.
2. Tap **"Start Instant Call Room"** (or **"Solo AI Gesture Studio"**) to enter the video call room.
3. Grant Camera & Microphone permissions when prompted (or use the interactive **AI Pose & Blendshape Inspector** bar to test every gesture and blendshape threshold directly).
4. Share the 6-character Room Code with a second device to sync reactions in real time via Firebase Firestore!

## 3. Running the Web (React + Vite + MediaPipe + Socket.io) Reference Locally
```bash
# 1. Start the WebRTC + Reaction Signaling Server
cd pulsecall-web/server
npm install express socket.io cors
node index.js

# 2. Start the Vite + React + MediaPipe Client
cd ../client
npm install @mediapipe/tasks-vision socket.io-client framer-motion canvas-confetti tailwindcss
npm run dev
```
"""

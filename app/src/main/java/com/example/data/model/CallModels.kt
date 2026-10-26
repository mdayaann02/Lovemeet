package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class GestureType(
  val id: String,
  val label: String,
  val primaryEmoji: String,
  val secondaryEmojis: List<String>,
  val blendshapeFormula: String,
  val description: String,
) {
  HEART(
    id = "HEART",
    label = "Hand / Finger Heart",
    primaryEmoji = "❤️",
    secondaryEmojis = listOf("💖", "💘", "💗", "✨"),
    blendshapeFormula = "dist(thumbTipL, thumbTipR) < 0.08 && dist(indexTipL, indexTipR) < 0.08 && indexMidY < thumbTipY",
    description = "Two-hand heart arch or crossed thumb+index finger heart triggers a fountain of rising 3D hearts.",
  ),
  KISS(
    id = "KISS",
    label = "Kiss / Puckered Lips",
    primaryEmoji = "😘",
    secondaryEmojis = listOf("💋", "💕", "😍", "✨"),
    blendshapeFormula = "mouthPucker > 0.62 && (mouthFunnel > 0.18 || jawOpen < 0.25)",
    description = "Facial blendshape pucker triggers an outward radial burst of kissing emojis from face center.",
  ),
  THUMBS_UP(
    id = "THUMBS_UP",
    label = "Thumbs Up",
    primaryEmoji = "👍",
    secondaryEmojis = listOf("🔥", "⭐", "💯"),
    blendshapeFormula = "thumbExtendedUp && curled(index, middle, ring, pinky) [1 Hand]",
    description = "Single vertical thumb extension with 4 curled fingers launches a golden approval burst.",
  ),
  FIREWORKS(
    id = "FIREWORKS",
    label = "Double Thumbs Fireworks",
    primaryEmoji = "🎆",
    secondaryEmojis = listOf("🎇", "✨", "🎉", "🚀"),
    blendshapeFormula = "leftHand.isThumbsUp && rightHand.isThumbsUp [2 Hands]",
    description = "Simultaneous two-hand thumbs up ignites a multi-stage 60fps celebratory fireworks show.",
  ),
  PEACE(
    id = "PEACE",
    label = "Peace / Victory Sign",
    primaryEmoji = "✌️",
    secondaryEmojis = listOf("⚡", "🌟", "💫", "😎"),
    blendshapeFormula = "extended(index, middle) && curled(ring, pinky) && spreadAngle > 14°",
    description = "V-sign with index and middle fingers extended triggers a neon electric peace wave.",
  ),
  LAUGH(
    id = "LAUGH",
    label = "Joy / Big Smile",
    primaryEmoji = "😂",
    secondaryEmojis = listOf("🤣", "😆", "✨", "🎉"),
    blendshapeFormula = "(mouthSmileLeft + mouthSmileRight)/2 > 0.70 && jawOpen > 0.32",
    description = "High smile + open jaw blendshapes trigger a buoyant cascade of laughing reactions.",
  );

  companion object {
    fun fromId(id: String): GestureType = entries.firstOrNull { it.id == id } ?: HEART
  }
}

@IgnoreExtraProperties
data class CallRoom(
  @DocumentId val id: String = "",
  val roomCode: String = "",
  val roomTitle: String = "",
  val hostUid: String = "",
  val hostName: String = "",
  val guestUid: String = "",
  val guestName: String = "",
  val status: String = "waiting",
  val hostMuted: Boolean = false,
  val hostCameraOn: Boolean = true,
  val hostScreenSharing: Boolean = false,
  val guestMuted: Boolean = false,
  val guestCameraOn: Boolean = true,
  val guestScreenSharing: Boolean = false,
  val sdpOffer: String = "",
  val sdpAnswer: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ReactionEvent(
  @DocumentId val id: String = "",
  val roomId: String = "",
  val senderUid: String = "",
  val senderName: String = "",
  val gestureType: String = "HEART",
  val emoji: String = "❤️",
  val source: String = "AI_GESTURE",
  val originX: Long = 50L,
  val originY: Long = 80L,
  val confidencePercent: Long = 95L,
  val allowedUids: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
)

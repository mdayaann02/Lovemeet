package com.example.cv

import com.example.data.model.GestureType
import kotlin.math.acos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * 3D normalized landmark matching MediaPipe Tasks Vision (x, y in [0..1], z relative depth).
 * Hand indices follow the 21-point MediaPipe Hand Landmarker topology:
 * 0: WRIST
 * 1..4: THUMB (CMC, MCP, IP, TIP)
 * 5..8: INDEX (MCP, PIP, DIP, TIP)
 * 9..12: MIDDLE (MCP, PIP, DIP, TIP)
 * 13..16: RING (MCP, PIP, DIP, TIP)
 * 17..20: PINKY (MCP, PIP, DIP, TIP)
 */
data class Landmark3D(
  val x: Float,
  val y: Float,
  val z: Float = 0f,
)

data class HandLandmarks(
  val handedness: String, // "Left" or "Right"
  val points: List<Landmark3D>, // 21 points
)

/**
 * Subset of the 52 ARKit / MediaPipe Face Landmarker blendshape coefficients used for
 * real-time facial expression reaction detection.
 */
data class FaceBlendshapes(
  val mouthPucker: Float = 0.08f,
  val mouthFunnel: Float = 0.05f,
  val jawOpen: Float = 0.06f,
  val mouthSmileLeft: Float = 0.15f,
  val mouthSmileRight: Float = 0.15f,
  val eyeSquintLeft: Float = 0.10f,
  val eyeSquintRight: Float = 0.10f,
  val faceCenterX: Float = 0.50f,
  val faceCenterY: Float = 0.38f,
)

data class GestureDetectionEvaluation(
  val detectedGesture: GestureType?,
  val confidencePercent: Int,
  val originX: Int,
  val originY: Int,
  val thumbGapDistance: Float,
  val indexGapDistance: Float,
  val mouthPuckerScore: Float,
  val smileScore: Float,
  val peaceSpreadDegrees: Float,
  val diagnosticFormula: String,
)

object GestureReactionEngine {

  const val COOLDOWN_MS = 2000L

  // Exact MediaPipe geometric & blendshape thresholds
  const val HAND_HEART_INDEX_DIST_THRESHOLD = 0.085f
  const val HAND_HEART_THUMB_DIST_THRESHOLD = 0.090f
  const val FINGER_HEART_DIST_THRESHOLD = 0.055f
  const val KISS_PUCKER_THRESHOLD = 0.62f
  const val KISS_FUNNEL_MIN = 0.18f
  const val SMILE_THRESHOLD = 0.70f
  const val JAW_OPEN_LAUGH_THRESHOLD = 0.32f
  const val PEACE_MIN_ANGLE_DEGREES = 14.0f

  fun distance3D(a: Landmark3D, b: Landmark3D): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    val dz = a.z - b.z
    return sqrt(dx * dx + dy * dy + dz * dz)
  }

  fun distance2D(a: Landmark3D, b: Landmark3D): Float {
    return hypot(a.x - b.x, a.y - b.y)
  }

  /**
   * Evaluates 21-point hand landmarks and 52-blendshape facial coefficients to detect:
   * 1. Two-Hand Heart or Single-Hand Finger Heart -> GestureType.HEART (❤️, 💖)
   * 2. Kiss / Puckered Lips Blendshape -> GestureType.KISS (😘, 💋)
   * 3. Two-Hand Thumbs Up -> GestureType.FIREWORKS (🎆)
   * 4. Single-Hand Thumbs Up -> GestureType.THUMBS_UP (👍)
   * 5. Peace Sign (V-Sign) -> GestureType.PEACE (✌️)
   * 6. Joy / Big Laugh Blendshape -> GestureType.LAUGH (😂)
   */
  fun evaluateFrame(
    hands: List<HandLandmarks>,
    face: FaceBlendshapes,
  ): GestureDetectionEvaluation {
    var thumbGap = 0.42f
    var indexGap = 0.44f
    var peaceAngle = 4.0f

    // 1. Check Two-Hand Heart Arch (Thumb tips [4] touching + Index tips [8] touching + arch curvature)
    if (hands.size >= 2) {
      val h1 = hands[0].points
      val h2 = hands[1].points
      if (h1.size >= 21 && h2.size >= 21) {
        val thumbTip1 = h1[4]
        val thumbTip2 = h2[4]
        val indexTip1 = h1[8]
        val indexTip2 = h2[8]
        val indexPip1 = h1[6]
        val indexPip2 = h2[6]

        thumbGap = distance2D(thumbTip1, thumbTip2)
        indexGap = distance2D(indexTip1, indexTip2)

        val archFormed =
          indexPip1.y < thumbTip1.y - 0.04f &&
            indexPip2.y < thumbTip2.y - 0.04f &&
            thumbTip1.y > indexTip1.y + 0.03f

        if (indexGap < HAND_HEART_INDEX_DIST_THRESHOLD &&
          thumbGap < HAND_HEART_THUMB_DIST_THRESHOLD &&
          archFormed
        ) {
          val score = (100f - ((indexGap + thumbGap) * 280f)).roundToInt().coerceIn(88, 99)
          return GestureDetectionEvaluation(
            detectedGesture = GestureType.HEART,
            confidencePercent = score,
            originX = 50,
            originY = 88, // Rises from bottom of screen as a heart fountain
            thumbGapDistance = thumbGap,
            indexGapDistance = indexGap,
            mouthPuckerScore = face.mouthPucker,
            smileScore = (face.mouthSmileLeft + face.mouthSmileRight) / 2f,
            peaceSpreadDegrees = peaceAngle,
            diagnosticFormula = "2-Hand Heart: indexDist=${"%.3f".format(indexGap)} (<0.085), thumbDist=${"%.3f".format(thumbGap)} (<0.090)",
          )
        }
      }
    }

    // 2. Check Single-Hand Finger Heart (Thumb tip [4] & Index tip [8] crossed closely while other fingers curled)
    for (hand in hands) {
      val pts = hand.points
      if (pts.size >= 21) {
        val fingerHeartDist = distance2D(pts[4], pts[8])
        val othersCurled = isFingerCurled(pts, 12, 10) && isFingerCurled(pts, 16, 14) && isFingerCurled(pts, 20, 18)
        if (fingerHeartDist < FINGER_HEART_DIST_THRESHOLD && othersCurled && pts[8].y < pts[0].y - 0.08f) {
          val score = (98f - fingerHeartDist * 200f).roundToInt().coerceIn(86, 98)
          return GestureDetectionEvaluation(
            detectedGesture = GestureType.HEART,
            confidencePercent = score,
            originX = 50,
            originY = 88,
            thumbGapDistance = fingerHeartDist,
            indexGapDistance = fingerHeartDist,
            mouthPuckerScore = face.mouthPucker,
            smileScore = (face.mouthSmileLeft + face.mouthSmileRight) / 2f,
            peaceSpreadDegrees = peaceAngle,
            diagnosticFormula = "Finger Heart: dist(thumb[4], index[8])=${"%.3f".format(fingerHeartDist)} (<0.055)",
          )
        }
      }
    }

    // 3. Check Two-Hand Thumbs Up -> Fireworks (🎆) vs Single-Hand Thumbs Up -> (👍)
    val thumbsUpHands = hands.filter { isThumbsUp(it.points) }
    if (thumbsUpHands.size >= 2) {
      return GestureDetectionEvaluation(
        detectedGesture = GestureType.FIREWORKS,
        confidencePercent = 97,
        originX = 50,
        originY = 55,
        thumbGapDistance = thumbGap,
        indexGapDistance = indexGap,
        mouthPuckerScore = face.mouthPucker,
        smileScore = (face.mouthSmileLeft + face.mouthSmileRight) / 2f,
        peaceSpreadDegrees = peaceAngle,
        diagnosticFormula = "Double Thumbs Up: 2 hands verified -> Fireworks 🎆",
      )
    } else if (thumbsUpHands.size == 1) {
      val handTip = thumbsUpHands[0].points[4]
      return GestureDetectionEvaluation(
        detectedGesture = GestureType.THUMBS_UP,
        confidencePercent = 94,
        originX = (handTip.x * 100f).roundToInt().coerceIn(15, 85),
        originY = (handTip.y * 100f).roundToInt().coerceIn(20, 80),
        thumbGapDistance = thumbGap,
        indexGapDistance = indexGap,
        mouthPuckerScore = face.mouthPucker,
        smileScore = (face.mouthSmileLeft + face.mouthSmileRight) / 2f,
        peaceSpreadDegrees = peaceAngle,
        diagnosticFormula = "Thumbs Up: thumbTip[4].y < indexMCP[5].y - 0.06 && 4 fingers curled",
      )
    }

    // 4. Check Peace Sign (✌️)
    for (hand in hands) {
      val pts = hand.points
      if (pts.size >= 21) {
        val angle = computePeaceAngleDegrees(pts)
        peaceAngle = maxOf(peaceAngle, angle)
        if (isPeaceSign(pts, angle)) {
          return GestureDetectionEvaluation(
            detectedGesture = GestureType.PEACE,
            confidencePercent = 95,
            originX = (pts[8].x * 100f).roundToInt().coerceIn(15, 85),
            originY = (pts[8].y * 100f).roundToInt().coerceIn(20, 80),
            thumbGapDistance = thumbGap,
            indexGapDistance = indexGap,
            mouthPuckerScore = face.mouthPucker,
            smileScore = (face.mouthSmileLeft + face.mouthSmileRight) / 2f,
            peaceSpreadDegrees = angle,
            diagnosticFormula = "Peace V-Sign: spreadAngle=${"%.1f".format(angle)}° (>14°) & ring/pinky curled",
          )
        }
      }
    }

    // 5. Check Facial Kiss Expression (mouthPucker blendshape > 0.62)
    if (face.mouthPucker >= KISS_PUCKER_THRESHOLD &&
      (face.mouthFunnel >= KISS_FUNNEL_MIN || face.jawOpen < 0.25f)
    ) {
      val score = (face.mouthPucker * 100f).roundToInt().coerceIn(85, 99)
      return GestureDetectionEvaluation(
        detectedGesture = GestureType.KISS,
        confidencePercent = score,
        originX = (face.faceCenterX * 100f).roundToInt().coerceIn(20, 80),
        originY = (face.faceCenterY * 100f).roundToInt().coerceIn(20, 75),
        thumbGapDistance = thumbGap,
        indexGapDistance = indexGap,
        mouthPuckerScore = face.mouthPucker,
        smileScore = (face.mouthSmileLeft + face.mouthSmileRight) / 2f,
        peaceSpreadDegrees = peaceAngle,
        diagnosticFormula = "Kiss Face: mouthPucker=${"%.2f".format(face.mouthPucker)} (>0.62), funnel=${"%.2f".format(face.mouthFunnel)}",
      )
    }

    // 6. Check Big Smile / Laugh Expression
    val avgSmile = (face.mouthSmileLeft + face.mouthSmileRight) / 2f
    if (avgSmile >= SMILE_THRESHOLD && face.jawOpen >= JAW_OPEN_LAUGH_THRESHOLD) {
      val score = (avgSmile * 100f).roundToInt().coerceIn(85, 99)
      return GestureDetectionEvaluation(
        detectedGesture = GestureType.LAUGH,
        confidencePercent = score,
        originX = (face.faceCenterX * 100f).roundToInt().coerceIn(20, 80),
        originY = (face.faceCenterY * 100f).roundToInt().coerceIn(20, 75),
        thumbGapDistance = thumbGap,
        indexGapDistance = indexGap,
        mouthPuckerScore = face.mouthPucker,
        smileScore = avgSmile,
        peaceSpreadDegrees = peaceAngle,
        diagnosticFormula = "Laugh Expression: avgSmile=${"%.2f".format(avgSmile)} (>0.70), jawOpen=${"%.2f".format(face.jawOpen)}",
      )
    }

    return GestureDetectionEvaluation(
      detectedGesture = null,
      confidencePercent = 0,
      originX = 50,
      originY = 50,
      thumbGapDistance = thumbGap,
      indexGapDistance = indexGap,
      mouthPuckerScore = face.mouthPucker,
      smileScore = avgSmile,
      peaceSpreadDegrees = peaceAngle,
      diagnosticFormula = "Tracking neutral pose • mouthPucker=${"%.2f".format(face.mouthPucker)} • thumbGap=${"%.2f".format(thumbGap)}",
    )
  }

  private fun isFingerCurled(pts: List<Landmark3D>, tipIdx: Int, pipIdx: Int): Boolean {
    val wrist = pts[0]
    return distance2D(pts[tipIdx], wrist) < distance2D(pts[pipIdx], wrist) * 1.04f
  }

  private fun isFingerExtended(pts: List<Landmark3D>, tipIdx: Int, pipIdx: Int): Boolean {
    return pts[tipIdx].y < pts[pipIdx].y - 0.045f
  }

  private fun isThumbsUp(pts: List<Landmark3D>): Boolean {
    if (pts.size < 21) return false
    val thumbTip = pts[4]
    val thumbIp = pts[3]
    val indexMcp = pts[5]
    val thumbVertical = thumbTip.y < thumbIp.y - 0.03f && thumbTip.y < indexMcp.y - 0.06f
    val fourFingersCurled =
      isFingerCurled(pts, 8, 6) &&
        isFingerCurled(pts, 12, 10) &&
        isFingerCurled(pts, 16, 14) &&
        isFingerCurled(pts, 20, 18)
    return thumbVertical && fourFingersCurled
  }

  private fun computePeaceAngleDegrees(pts: List<Landmark3D>): Float {
    val mcpMidX = (pts[5].x + pts[9].x) / 2f
    val mcpMidY = (pts[5].y + pts[9].y) / 2f
    val v1x = pts[8].x - mcpMidX
    val v1y = pts[8].y - mcpMidY
    val v2x = pts[12].x - mcpMidX
    val v2y = pts[12].y - mcpMidY
    val mag1 = hypot(v1x, v1y).coerceAtLeast(1e-4f)
    val mag2 = hypot(v2x, v2y).coerceAtLeast(1e-4f)
    val cosTheta = ((v1x * v2x + v1y * v2y) / (mag1 * mag2)).coerceIn(-1f, 1f)
    return Math.toDegrees(acos(cosTheta.toDouble())).toFloat()
  }

  private fun isPeaceSign(pts: List<Landmark3D>, spreadAngle: Float): Boolean {
    val indexExtended = isFingerExtended(pts, 8, 6)
    val middleExtended = isFingerExtended(pts, 12, 10)
    val ringCurled = isFingerCurled(pts, 16, 14)
    val pinkyCurled = isFingerCurled(pts, 20, 18)
    return indexExtended && middleExtended && ringCurled && pinkyCurled && spreadAngle >= PEACE_MIN_ANGLE_DEGREES
  }

  /**
   * Generates realistic 21-point hand skeletons and 52-blendshape facial states
   * for any selected pose preset or custom slider configuration so users can inspect
   * the exact MediaPipe landmark geometry on screen.
   */
  fun buildPresetPose(preset: GestureType?): Pair<List<HandLandmarks>, FaceBlendshapes> {
    return when (preset) {
      GestureType.HEART -> {
        // Two-hand heart arch where index tips (8) and thumb tips (4) meet at center
        val left = listOf(
          Landmark3D(0.28f, 0.76f), // 0 wrist
          Landmark3D(0.35f, 0.72f), Landmark3D(0.41f, 0.69f), Landmark3D(0.45f, 0.67f), Landmark3D(0.485f, 0.66f), // 1..4 thumb meeting at (0.50, 0.66)
          Landmark3D(0.34f, 0.56f), Landmark3D(0.39f, 0.45f), Landmark3D(0.45f, 0.49f), Landmark3D(0.488f, 0.54f), // 5..8 index arch meeting at (0.50, 0.54)
          Landmark3D(0.32f, 0.58f), Landmark3D(0.37f, 0.48f), Landmark3D(0.43f, 0.53f), Landmark3D(0.46f, 0.58f), // 9..12
          Landmark3D(0.30f, 0.62f), Landmark3D(0.34f, 0.56f), Landmark3D(0.38f, 0.60f), Landmark3D(0.40f, 0.64f), // 13..16
          Landmark3D(0.28f, 0.66f), Landmark3D(0.31f, 0.62f), Landmark3D(0.34f, 0.65f), Landmark3D(0.36f, 0.68f), // 17..20
        )
        val right = left.map { pt -> pt.copy(x = 1.0f - pt.x) }
        Pair(
          listOf(HandLandmarks("Left", left), HandLandmarks("Right", right)),
          FaceBlendshapes(mouthSmileLeft = 0.55f, mouthSmileRight = 0.55f)
        )
      }

      GestureType.KISS -> {
        Pair(
          emptyList(),
          FaceBlendshapes(
            mouthPucker = 0.84f,
            mouthFunnel = 0.34f,
            jawOpen = 0.08f,
            eyeSquintLeft = 0.48f,
            eyeSquintRight = 0.48f,
            faceCenterX = 0.50f,
            faceCenterY = 0.40f,
          )
        )
      }

      GestureType.THUMBS_UP -> {
        val right = buildThumbsUpHand(centerX = 0.62f, centerY = 0.64f)
        Pair(listOf(HandLandmarks("Right", right)), FaceBlendshapes(mouthSmileLeft = 0.45f, mouthSmileRight = 0.45f))
      }

      GestureType.FIREWORKS -> {
        val left = buildThumbsUpHand(centerX = 0.34f, centerY = 0.64f)
        val right = buildThumbsUpHand(centerX = 0.66f, centerY = 0.64f)
        Pair(
          listOf(HandLandmarks("Left", left), HandLandmarks("Right", right)),
          FaceBlendshapes(mouthSmileLeft = 0.65f, mouthSmileRight = 0.65f, jawOpen = 0.22f)
        )
      }

      GestureType.PEACE -> {
        val right = listOf(
          Landmark3D(0.58f, 0.78f), // 0 wrist
          Landmark3D(0.52f, 0.72f), Landmark3D(0.50f, 0.66f), Landmark3D(0.53f, 0.63f), Landmark3D(0.55f, 0.62f), // thumb curled across palm
          Landmark3D(0.54f, 0.62f), Landmark3D(0.51f, 0.50f), Landmark3D(0.49f, 0.41f), Landmark3D(0.47f, 0.34f), // index extended left
          Landmark3D(0.58f, 0.61f), Landmark3D(0.61f, 0.49f), Landmark3D(0.64f, 0.40f), Landmark3D(0.66f, 0.33f), // middle extended right (spread > 22°)
          Landmark3D(0.61f, 0.64f), Landmark3D(0.62f, 0.67f), Landmark3D(0.60f, 0.71f), Landmark3D(0.59f, 0.73f), // ring curled
          Landmark3D(0.64f, 0.67f), Landmark3D(0.65f, 0.70f), Landmark3D(0.63f, 0.73f), Landmark3D(0.61f, 0.75f), // pinky curled
        )
        Pair(listOf(HandLandmarks("Right", right)), FaceBlendshapes(mouthSmileLeft = 0.50f, mouthSmileRight = 0.50f))
      }

      GestureType.LAUGH -> {
        Pair(
          emptyList(),
          FaceBlendshapes(
            mouthPucker = 0.05f,
            mouthSmileLeft = 0.86f,
            mouthSmileRight = 0.88f,
            jawOpen = 0.52f,
            eyeSquintLeft = 0.62f,
            eyeSquintRight = 0.62f,
          )
        )
      }

      null -> {
        Pair(emptyList(), FaceBlendshapes())
      }
    }
  }

  private fun buildThumbsUpHand(centerX: Float, centerY: Float): List<Landmark3D> {
    return listOf(
      Landmark3D(centerX, centerY + 0.10f), // 0 wrist
      Landmark3D(centerX - 0.03f, centerY + 0.03f),
      Landmark3D(centerX - 0.03f, centerY - 0.03f),
      Landmark3D(centerX - 0.03f, centerY - 0.09f),
      Landmark3D(centerX - 0.03f, centerY - 0.15f), // 4 thumb tip high up
      Landmark3D(centerX + 0.03f, centerY - 0.02f),
      Landmark3D(centerX + 0.06f, centerY - 0.01f),
      Landmark3D(centerX + 0.02f, centerY + 0.01f),
      Landmark3D(centerX + 0.01f, centerY + 0.01f), // 8 index curled
      Landmark3D(centerX + 0.03f, centerY + 0.01f),
      Landmark3D(centerX + 0.06f, centerY + 0.02f),
      Landmark3D(centerX + 0.02f, centerY + 0.03f),
      Landmark3D(centerX + 0.01f, centerY + 0.03f), // 12 middle curled
      Landmark3D(centerX + 0.03f, centerY + 0.04f),
      Landmark3D(centerX + 0.06f, centerY + 0.05f),
      Landmark3D(centerX + 0.02f, centerY + 0.06f),
      Landmark3D(centerX + 0.01f, centerY + 0.06f), // 16 ring curled
      Landmark3D(centerX + 0.02f, centerY + 0.07f),
      Landmark3D(centerX + 0.05f, centerY + 0.08f),
      Landmark3D(centerX + 0.02f, centerY + 0.09f),
      Landmark3D(centerX + 0.01f, centerY + 0.09f), // 20 pinky curled
    )
  }
}

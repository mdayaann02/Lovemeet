package com.example.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.example.cv.FaceBlendshapes
import com.example.cv.HandLandmarks
import com.example.data.model.GestureType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class ReactionBurstTrigger(
  val id: String,
  val gestureType: GestureType,
  val originXPercent: Int,
  val originYPercent: Int,
  val senderName: String,
)

private data class EmojiParticle(
  val emoji: String,
  var xNorm: Float,
  var yNorm: Float,
  var vx: Float,
  var vy: Float,
  val gravity: Float,
  val baseSizePx: Float,
  val wobbleFreq: Float,
  val wobbleAmp: Float,
  val phaseOffset: Float,
  val glowColor: Color,
  val createdAtMs: Long,
  val lifetimeMs: Long,
)

@Composable
fun FloatingEmojiOverlay(
  burstTriggers: List<ReactionBurstTrigger>,
  modifier: Modifier = Modifier,
) {
  val particles = remember { mutableStateListOf<EmojiParticle>() }
  val processedIds = remember { mutableSetOf<String>() }
  var frameTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

  LaunchedEffect(burstTriggers) {
    val now = System.currentTimeMillis()
    for (trigger in burstTriggers) {
      if (processedIds.add(trigger.id)) {
        spawnBurstParticles(trigger, now, particles)
      }
    }
  }

  LaunchedEffect(Unit) {
    var lastNanos = 0L
    while (true) {
      withFrameNanos { nanos ->
        val dtSec = if (lastNanos == 0L) 0.016f else ((nanos - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        lastNanos = nanos
        val now = System.currentTimeMillis()
        frameTimeMs = now

        val iterator = particles.listIterator()
        while (iterator.hasNext()) {
          val p = iterator.next()
          val age = now - p.createdAtMs
          if (age >= p.lifetimeMs) {
            iterator.remove()
          } else {
            val ageSec = age / 1000f
            p.vy += p.gravity * dtSec
            p.xNorm += (p.vx + sin(ageSec * p.wobbleFreq + p.phaseOffset) * p.wobbleAmp) * dtSec
            p.yNorm += p.vy * dtSec
          }
        }
      }
    }
  }

  val textPaint = remember {
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      textAlign = Paint.Align.CENTER
    }
  }

  Canvas(modifier = modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height
    val now = frameTimeMs

    for (p in particles) {
      val progress = ((now - p.createdAtMs).toFloat() / p.lifetimeMs.toFloat()).coerceIn(0f, 1f)
      // Smooth pop-in during first 15%, sustain, then fade out in final 30%
      val scale = when {
        progress < 0.15f -> 0.4f + (progress / 0.15f) * 0.85f
        progress < 0.70f -> 1.25f - ((progress - 0.15f) / 0.55f) * 0.25f
        else -> 1.0f - ((progress - 0.70f) / 0.30f) * 0.45f
      }
      val alpha = when {
        progress < 0.10f -> (progress / 0.10f)
        progress < 0.72f -> 1f
        else -> (1f - (progress - 0.72f) / 0.28f).coerceIn(0f, 1f)
      }

      val cx = p.xNorm * w
      val cy = p.yNorm * h
      val radius = (p.baseSizePx * scale * 0.75f).coerceAtLeast(8f)

      // 3D-style volumetric radial aura behind each floating emoji
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            p.glowColor.copy(alpha = 0.42f * alpha),
            p.glowColor.copy(alpha = 0.12f * alpha),
            Color.Transparent,
          ),
          center = Offset(cx, cy - radius * 0.18f),
          radius = radius * 1.45f,
        ),
        radius = radius * 1.45f,
        center = Offset(cx, cy - radius * 0.18f),
      )

      // Specular 3D highlight orb
      drawCircle(
        color = Color.White.copy(alpha = 0.22f * alpha),
        radius = radius * 0.28f,
        center = Offset(cx - radius * 0.25f, cy - radius * 0.35f),
      )

      drawContext.canvas.nativeCanvas.apply {
        textPaint.textSize = p.baseSizePx * scale
        textPaint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
        drawText(p.emoji, cx, cy + (p.baseSizePx * scale * 0.32f), textPaint)
      }
    }
  }
}

private fun spawnBurstParticles(
  trigger: ReactionBurstTrigger,
  now: Long,
  target: MutableList<EmojiParticle>,
) {
  val palette = listOf(trigger.gestureType.primaryEmoji) + trigger.gestureType.secondaryEmojis
  val glow = when (trigger.gestureType) {
    GestureType.HEART -> Color(0xFFF43F5E)
    GestureType.KISS -> Color(0xFFEC4899)
    GestureType.THUMBS_UP -> Color(0xFFF59E0B)
    GestureType.FIREWORKS -> Color(0xFF38BDF8)
    GestureType.PEACE -> Color(0xFF06B6D4)
    GestureType.LAUGH -> Color(0xFFFBBF24)
  }

  when (trigger.gestureType) {
    GestureType.HEART -> {
      // Particle fountain of floating red/pink hearts rising from the bottom of the screen
      val count = 28
      for (i in 0 until count) {
        val startX = (0.20f + Random.nextFloat() * 0.60f)
        val startY = 0.92f + Random.nextFloat() * 0.08f
        val vy = -(0.34f + Random.nextFloat() * 0.32f)
        val vx = (Random.nextFloat() - 0.5f) * 0.18f
        target.add(
          EmojiParticle(
            emoji = palette[i % palette.size],
            xNorm = startX,
            yNorm = startY,
            vx = vx,
            vy = vy,
            gravity = -0.04f, // Upward thermal lift
            baseSizePx = 56f + Random.nextFloat() * 34f,
            wobbleFreq = 3.2f + Random.nextFloat() * 2.5f,
            wobbleAmp = 0.08f + Random.nextFloat() * 0.08f,
            phaseOffset = Random.nextFloat() * 6.28f,
            glowColor = glow,
            createdAtMs = now + (i * 35L),
            lifetimeMs = 2400L + Random.nextLong(700L),
          )
        )
      }
    }

    GestureType.KISS -> {
      // Outward radial burst of kissing emojis floating from the user's face area
      val originX = (trigger.originXPercent / 100f).coerceIn(0.2f, 0.8f)
      val originY = (trigger.originYPercent / 100f).coerceIn(0.2f, 0.75f)
      val count = 24
      for (i in 0 until count) {
        val angle = (2.0 * PI * i / count) + (Random.nextDouble(-0.2, 0.2))
        val speed = 0.18f + Random.nextFloat() * 0.26f
        target.add(
          EmojiParticle(
            emoji = palette[i % palette.size],
            xNorm = originX,
            yNorm = originY,
            vx = (cos(angle) * speed).toFloat(),
            vy = (sin(angle) * speed).toFloat() - 0.12f,
            gravity = -0.08f, // Float gently upward after radial burst
            baseSizePx = 54f + Random.nextFloat() * 30f,
            wobbleFreq = 4.0f,
            wobbleAmp = 0.06f,
            phaseOffset = Random.nextFloat() * 6.28f,
            glowColor = glow,
            createdAtMs = now + (i * 22L),
            lifetimeMs = 2200L + Random.nextLong(600L),
          )
        )
      }
    }

    GestureType.FIREWORKS -> {
      // Multi-center fireworks bursts across upper and mid screen
      val centers = listOf(Offset(0.30f, 0.35f), Offset(0.70f, 0.32f), Offset(0.50f, 0.48f))
      val fireworkColors = listOf(Color(0xFFF59E0B), Color(0xFF06B6D4), Color(0xFFF43F5E), Color(0xFFA855F7))
      centers.forEachIndexed { cIdx, center ->
        for (i in 0 until 14) {
          val angle = (2.0 * PI * i / 14.0)
          val speed = 0.22f + Random.nextFloat() * 0.25f
          target.add(
            EmojiParticle(
              emoji = palette[(i + cIdx) % palette.size],
              xNorm = center.x,
              yNorm = center.y,
              vx = (cos(angle) * speed).toFloat(),
              vy = (sin(angle) * speed).toFloat(),
              gravity = 0.18f, // Classic firework arc gravity
              baseSizePx = 52f + Random.nextFloat() * 32f,
              wobbleFreq = 2.5f,
              wobbleAmp = 0.03f,
              phaseOffset = i.toFloat(),
              glowColor = fireworkColors[(cIdx + i) % fireworkColors.size],
              createdAtMs = now + (cIdx * 180L),
              lifetimeMs = 2300L,
            )
          )
        }
      }
    }

    else -> {
      // Thumbs Up, Peace Sign, Laugh: energetic fountain + arc from origin
      val originX = (trigger.originXPercent / 100f).coerceIn(0.18f, 0.82f)
      val originY = (trigger.originYPercent / 100f).coerceIn(0.25f, 0.85f)
      for (i in 0 until 22) {
        val spreadAngle = -PI / 2.0 + (Random.nextDouble(-0.95, 0.95))
        val speed = 0.26f + Random.nextFloat() * 0.28f
        target.add(
          EmojiParticle(
            emoji = palette[i % palette.size],
            xNorm = originX + (Random.nextFloat() - 0.5f) * 0.14f,
            yNorm = originY,
            vx = (cos(spreadAngle) * speed).toFloat(),
            vy = (sin(spreadAngle) * speed).toFloat(),
            gravity = 0.08f,
            baseSizePx = 54f + Random.nextFloat() * 30f,
            wobbleFreq = 3.5f,
            wobbleAmp = 0.06f,
            phaseOffset = Random.nextFloat() * 6.28f,
            glowColor = glow,
            createdAtMs = now + (i * 28L),
            lifetimeMs = 2100L + Random.nextLong(500L),
          )
        )
      }
    }
  }
}

/**
 * Renders the real-time 21-point MediaPipe Hand Landmarker skeleton and Face Landmarker
 * pucker/smile geometry overlay on the local camera feed.
 */
@Composable
fun LandmarkMeshOverlay(
  hands: List<HandLandmarks>,
  face: FaceBlendshapes,
  modifier: Modifier = Modifier,
) {
  val connections = remember {
    listOf(
      0 to 1, 1 to 2, 2 to 3, 3 to 4,       // Thumb
      0 to 5, 5 to 6, 6 to 7, 7 to 8,       // Index
      0 to 9, 9 to 10, 10 to 11, 11 to 12,  // Middle
      0 to 13, 13 to 14, 14 to 15, 15 to 16,// Ring
      0 to 17, 17 to 18, 18 to 19, 19 to 20,// Pinky
      5 to 9, 9 to 13, 13 to 17             // Palm knuckle bridge
    )
  }

  Canvas(modifier = modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height

    // Draw face tracking contour & mouth blendshape indicator
    val faceCx = face.faceCenterX * w
    val faceCy = face.faceCenterY * h
    val faceRadius = w.coerceAtMost(h) * 0.24f

    drawCircle(
      color = Color(0xFF06B6D4).copy(alpha = 0.45f),
      radius = faceRadius,
      center = Offset(faceCx, faceCy),
      style = Stroke(width = 3f),
    )

    // Mouth blendshape ring (contracts into a bright rose ring when mouthPucker > 0.62)
    val isKissing = face.mouthPucker >= 0.62f
    val mouthColor = if (isKissing) Color(0xFFF43F5E) else Color(0xFF22D3EE)
    val mouthWidth = faceRadius * (0.55f - face.mouthPucker * 0.34f).coerceAtLeast(0.16f)
    val mouthHeight = faceRadius * (0.14f + face.jawOpen * 0.32f + face.mouthPucker * 0.12f)

    drawOval(
      color = mouthColor.copy(alpha = 0.85f),
      topLeft = Offset(faceCx - mouthWidth / 2f, faceCy + faceRadius * 0.22f),
      size = androidx.compose.ui.geometry.Size(mouthWidth, mouthHeight),
      style = Stroke(width = if (isKissing) 5f else 3f),
    )

    // Draw 21-point hand skeletons
    for (hand in hands) {
      val pts = hand.points
      if (pts.size >= 21) {
        val boneColor = if (hand.handedness == "Left") Color(0xFF22D3EE) else Color(0xFFF43F5E)
        for ((a, b) in connections) {
          val p1 = Offset(pts[a].x * w, pts[a].y * h)
          val p2 = Offset(pts[b].x * w, pts[b].y * h)
          drawLine(
            color = boneColor.copy(alpha = 0.85f),
            start = p1,
            end = p2,
            strokeWidth = 4.5f,
            cap = StrokeCap.Round,
          )
        }
        pts.forEachIndexed { idx, lm ->
          val isFingertip = idx in listOf(4, 8, 12, 16, 20)
          drawCircle(
            color = if (isFingertip) Color.White else boneColor,
            radius = if (isFingertip) 7f else 4.5f,
            center = Offset(lm.x * w, lm.y * h),
          )
        }
      }
    }
  }
}

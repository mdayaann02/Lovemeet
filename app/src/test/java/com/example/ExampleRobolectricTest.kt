package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.cv.GestureReactionEngine
import com.example.data.model.GestureType
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("PulseCall", appName)
  }

  @Test
  fun `gesture reaction engine detects heart kiss thumbs fireworks and peace`() {
    GestureType.entries.forEach { preset ->
      val (hands, face) = GestureReactionEngine.buildPresetPose(preset)
      val evaluation = GestureReactionEngine.evaluateFrame(hands, face)
      assertEquals(preset, evaluation.detectedGesture)
    }
  }
}

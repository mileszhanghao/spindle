package com.mileszhang.spindle

import android.content.ComponentName
import android.graphics.Bitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.mileszhang.spindle.playback.PlaybackService
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * End-to-end on a real emulator against the real catalog: browse, open an
 * album, play, favorite. Screenshots of each step are saved for the demo site
 * (copied out by CI with `adb exec-out run-as com.mileszhang.spindle`).
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class SmokeTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun browsePlayAndFavorite() {
        compose.waitUntilAtLeastOneExists(hasText("Rainy Seattle mornings"), timeoutMillis = 30_000)
        settle()
        screenshot("1-home")

        compose.onNodeWithText("Rain on Banana Leaves", substring = true).performClick()
        compose.waitUntilAtLeastOneExists(hasText("Play"), timeoutMillis = 20_000)
        settle()
        screenshot("2-album")

        compose.onNodeWithText("Play").performClick()
        try {
            compose.waitUntilAtLeastOneExists(hasContentDescription("Pause"), timeoutMillis = 45_000)
        } catch (e: Throwable) {
            screenshot("x-not-playing")
            // Surface the player's own error message (shown in a snackbar) in the test failure.
            val shown = compose.onAllNodes(hasText("Couldn't play", substring = true)).fetchSemanticsNodes()
                .flatMap { node -> node.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.Text) { emptyList() } }
            throw AssertionError("Playback did not start. UI said: ${shown.joinToString().ifEmpty { "nothing" }}. Session: ${sessionState()}", e)
        }
        Thread.sleep(4_000) // let the record turn a little
        compose.waitForIdle()
        screenshot("3-playing")

        compose.onNodeWithContentDescription("Favorite").performClick()
        compose.onNodeWithText("Favorites").performClick()
        compose.waitUntilAtLeastOneExists(hasText("Zhang Peijian", substring = true), timeoutMillis = 10_000)
        settle()
        screenshot("4-favorites")
    }

    /** Connects a second MediaController to the app's session and describes the player. */
    private fun sessionState(): String {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val token = SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))
        val future = MediaController.Builder(ctx, token).buildAsync()
        return try {
            val c = future.get(10, TimeUnit.SECONDS)
            val result = arrayOfNulls<String>(1)
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                result[0] = "state=${c.playbackState} playWhenReady=${c.playWhenReady} items=${c.mediaItemCount} " +
                    "current=${c.currentMediaItem?.mediaId} uri=${c.currentMediaItem?.localConfiguration?.uri} " +
                    "error=${c.playerError?.errorCodeName}:${c.playerError?.message}:${c.playerError?.cause}"
                c.release()
            }
            result[0] ?: "unknown"
        } catch (t: Throwable) {
            "could not connect: $t"
        }
    }

    /** Give cover images a moment to load before capturing. */
    private fun settle() {
        compose.waitForIdle()
        Thread.sleep(3_000)
        compose.waitForIdle()
    }

    private fun screenshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        // App-private storage; CI copies it out with `adb exec-out run-as`.
        val dir = File(instrumentation.targetContext.filesDir, "screens").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}

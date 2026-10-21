package com.mileszhang.spindle

import android.content.ComponentName
import android.graphics.Bitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.util.concurrent.ListenableFuture
import com.mileszhang.spindle.playback.PlaybackService
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * End-to-end on a real emulator against the real catalog and real Wikimedia
 * audio: browse, open an album, play, favorite. Screenshots of each step are
 * saved for the demo site (CI copies them out with `adb exec-out run-as`).
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

        // The album is in two sections; open the first one.
        compose.onAllNodesWithText("Rain on Banana Leaves", substring = true)[0].performClick()
        compose.waitUntilAtLeastOneExists(hasText("Play"), timeoutMillis = 20_000)
        settle()
        screenshot("2-album")

        compose.onNodeWithText("Play").performScrollTo().performClick()
        // From here the UI animates without end (buffering spinner, then the turning
        // record), so Compose never goes idle. Drive its clock by hand, and ask the
        // media session rather than the UI whether music is playing.
        compose.mainClock.autoAdvance = false
        if (!waitForSession(timeoutMs = 60_000) { it.isPlaying }) {
            screenshot("x-not-playing")
            throw AssertionError("Playback did not start. Session: ${sessionState()}")
        }
        compose.mainClock.advanceTimeBy(3_000) // let the record turn a little
        Thread.sleep(1_000)
        screenshot("3-playing")
        compose.onNodeWithContentDescription("Pause").assertExists()

        compose.onNodeWithContentDescription("Favorite").performClick()
        compose.onNodeWithText("Favorites").performClick()
        compose.mainClock.advanceTimeBy(2_000)
        Thread.sleep(2_000) // cover image
        compose.mainClock.advanceTimeBy(500)
        compose.onAllNodesWithText("Zhang Peijian", substring = true)[0].assertExists()
        screenshot("4-favorites")
    }

    /** Polls the app's media session through a second controller until [done] holds or time runs out. */
    private fun waitForSession(timeoutMs: Long, done: (MediaController) -> Boolean): Boolean {
        val controller = connect()
        try {
            val deadline = System.currentTimeMillis() + timeoutMs
            while (System.currentTimeMillis() < deadline) {
                var ok = false
                InstrumentationRegistry.getInstrumentation().runOnMainSync { ok = done(controller) }
                if (ok) return true
                Thread.sleep(500)
            }
            return false
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { controller.release() }
        }
    }

    /** Describes the player, for failure messages. */
    private fun sessionState(): String = try {
        val c = connect()
        var result = "unknown"
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            result = "state=${c.playbackState} playWhenReady=${c.playWhenReady} items=${c.mediaItemCount} " +
                "current=${c.currentMediaItem?.mediaId} " +
                "error=${c.playerError?.errorCodeName}:${c.playerError?.message}:${c.playerError?.cause}"
            c.release()
        }
        result
    } catch (t: Throwable) {
        "could not connect: $t"
    }

    /** MediaControllers must be built on a thread with a Looper, so build on the main thread. */
    private fun connect(): MediaController {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val ctx = instrumentation.targetContext
        val token = SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))
        lateinit var future: ListenableFuture<MediaController>
        instrumentation.runOnMainSync { future = MediaController.Builder(ctx, token).buildAsync() }
        return future.get(10, TimeUnit.SECONDS)
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

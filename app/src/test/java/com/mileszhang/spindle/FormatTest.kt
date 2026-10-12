package com.mileszhang.spindle

import com.mileszhang.spindle.playback.albumIdOf
import com.mileszhang.spindle.ui.common.formatDuration
import com.mileszhang.spindle.ui.common.greetingFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormatTest {
    @Test
    fun `formats durations`() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:00", formatDuration(-5))
        assertEquals("3:35", formatDuration(215_000))
        assertEquals("1:02:05", formatDuration(3_725_000))
    }

    @Test
    fun `greets by time of day`() {
        assertEquals("Good morning", greetingFor(8))
        assertEquals("Good evening", greetingFor(21))
        assertEquals("Up late?", greetingFor(2))
    }

    @Test
    fun `recovers the album id from a track id`() {
        assertEquals(12, albumIdOf("12-3"))
        assertNull(albumIdOf(null))
        assertNull(albumIdOf("x-1"))
    }
}

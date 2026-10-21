package com.mileszhang.spindle.playback

/** What the UI needs to know about the player, as one immutable snapshot. */
data class NowPlaying(
    val trackId: String? = null,
    val albumId: Int? = null,
    val title: String = "",
    val artist: String = "",
    val artworkUrl: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0,
    /** 0 while unknown. The course passed C.TIME_UNSET (a negative number) straight into the Slider range. */
    val durationMs: Long = 0,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
    val error: String? = null,
) {
    val isActive: Boolean get() = trackId != null
}

/** Track ids are "<albumId>-<n>", so the album can be recovered from what is playing. */
fun albumIdOf(trackId: String?): Int? = trackId?.substringBefore('-')?.toIntOrNull()

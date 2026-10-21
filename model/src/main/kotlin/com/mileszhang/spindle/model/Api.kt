package com.mileszhang.spindle.model

import kotlinx.serialization.Serializable

// Wire format shared by the Ktor server, the static GitHub Pages API and the
// Android app, so the three can't disagree about field names or types.

/** Where a cover image or recording came from, shown in the app. */
@Serializable
data class Credit(
    val author: String,
    val license: String,
    val sourceUrl: String,
)

@Serializable
data class AlbumSummary(
    val id: Int,
    val title: String,
    val artist: String,
    val year: Int,
    val coverUrl: String,
    val description: String,
    val coverCredit: Credit,
)

@Serializable
data class Track(
    /** Stable id ("<albumId>-<n>"), so "now playing" never relies on equality of display fields. */
    val id: String,
    val title: String,
    val artist: String,
    /** Real duration in milliseconds; the UI formats it. */
    val durationMs: Long,
    val audioUrl: String,
    val credit: Credit,
)

@Serializable
data class Section(
    val title: String,
    val albums: List<AlbumSummary>,
)

/** GET feed.json */
@Serializable
data class Feed(val sections: List<Section>)

/** GET albums/{id}.json */
@Serializable
data class AlbumDetail(
    val album: AlbumSummary,
    val tracks: List<Track>,
)

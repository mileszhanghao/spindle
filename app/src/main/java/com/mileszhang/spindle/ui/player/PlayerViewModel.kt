package com.mileszhang.spindle.ui.player

import androidx.lifecycle.ViewModel
import com.mileszhang.spindle.model.AlbumSummary
import com.mileszhang.spindle.model.Track
import com.mileszhang.spindle.playback.NowPlaying
import com.mileszhang.spindle.playback.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(private val playback: PlaybackConnection) : ViewModel() {
    val nowPlaying: StateFlow<NowPlaying> = playback.state

    init { playback.connect() }

    fun play(album: AlbumSummary, tracks: List<Track>, index: Int) = playback.playAlbum(album, tracks, index)
    fun togglePlayPause() = playback.togglePlayPause()
    fun seekTo(positionMs: Long) = playback.seekTo(positionMs)
    fun next() = playback.next()
    fun previous() = playback.previous()
    fun clearError() = playback.clearError()
}

package com.mileszhang.spindle.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.mileszhang.spindle.model.AlbumSummary
import com.mileszhang.spindle.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The app's handle on [PlaybackService]: a MediaController connected on first
 * use, with the player's state mirrored into a StateFlow for Compose.
 */
@Singleton
class PlaybackConnection @Inject constructor(@ApplicationContext private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(NowPlaying())
    val state: StateFlow<NowPlaying> = _state.asStateFlow()
    private var ticker: Job? = null

    private val controller = scope.async(start = CoroutineStart.LAZY) {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        MediaController.Builder(context, token).buildAsync().await().also { controller ->
            controller.addListener(listener)
            sync(controller)
        }
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = sync(player)

        override fun onPlayerError(error: PlaybackException) {
            _state.update { it.copy(error = "Couldn't play this track (${error.errorCodeName}).") }
        }
    }

    /** Called when the app starts, so a session that is still playing in the background shows up in the UI. */
    fun connect() {
        scope.launch { controller.await() }
    }

    fun playAlbum(album: AlbumSummary, tracks: List<Track>, startIndex: Int) {
        scope.launch {
            val player = controller.await()
            player.setMediaItems(tracks.map { it.toMediaItem(album) }, startIndex, 0L)
            player.prepare()
            player.play()
        }
    }

    fun togglePlayPause() = withPlayer { if (it.isPlaying) it.pause() else it.play() }
    fun seekTo(positionMs: Long) = withPlayer {
        it.seekTo(positionMs)
        _state.update { s -> s.copy(positionMs = positionMs) }
    }
    fun next() = withPlayer { it.seekToNextMediaItem() }
    fun previous() = withPlayer { it.seekToPrevious() }
    fun clearError() = _state.update { it.copy(error = null) }

    private fun withPlayer(action: (MediaController) -> Unit) {
        scope.launch { action(controller.await()) }
    }

    private fun sync(player: Player) {
        val item = player.currentMediaItem
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L
        _state.update {
            it.copy(
                trackId = item?.mediaId,
                albumId = albumIdOf(item?.mediaId),
                title = item?.mediaMetadata?.title?.toString().orEmpty(),
                artist = item?.mediaMetadata?.artist?.toString().orEmpty(),
                artworkUrl = item?.mediaMetadata?.artworkUri?.toString(),
                isPlaying = player.isPlaying,
                isBuffering = player.playbackState == Player.STATE_BUFFERING,
                positionMs = player.currentPosition.coerceAtLeast(0),
                durationMs = duration,
                hasPrevious = item != null,
                hasNext = player.hasNextMediaItem(),
            )
        }
        updateTicker(player)
    }

    /** Position only changes on its own while playing, so only poll then. */
    private fun updateTicker(player: Player) {
        if (player.isPlaying && ticker?.isActive != true) {
            ticker = scope.launch {
                while (isActive) {
                    _state.update { it.copy(positionMs = player.currentPosition.coerceAtLeast(0)) }
                    delay(500)
                }
            }
        } else if (!player.isPlaying) {
            ticker?.cancel()
            ticker = null
        }
    }
}

private fun Track.toMediaItem(album: AlbumSummary): MediaItem {
    val uri = Uri.parse(audioUrl)
    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(uri)
        .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album.title)
                .setArtworkUri(Uri.parse(album.coverUrl))
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .build(),
        )
        .build()
}

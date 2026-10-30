package com.mileszhang.spindle.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mileszhang.spindle.R
import com.mileszhang.spindle.playback.NowPlaying
import com.mileszhang.spindle.ui.common.formatDuration

/** Persistent player above the navigation bar; tapping it opens the playing album. */
@Composable
fun MiniPlayer(
    state: NowPlaying,
    onOpen: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable(onClickLabel = "Open album", onClick = onOpen).padding(start = 12.dp, end = 4.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = state.artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)),
                )
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(state.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        state.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onPrevious, enabled = state.hasPrevious) {
                    Icon(painterResource(R.drawable.ic_skip_previous), contentDescription = "Previous track")
                }
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier.semantics { stateDescription = if (state.isPlaying) "Playing" else "Paused" },
                ) {
                    when {
                        state.isBuffering && !state.isPlaying ->
                            CircularProgressIndicator(Modifier.size(22.dp).semantics { contentDescription = "Buffering" }, strokeWidth = 2.dp)
                        state.isPlaying -> Icon(painterResource(R.drawable.ic_pause), contentDescription = "Pause")
                        else -> Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
                    }
                }
                IconButton(onClick = onNext, enabled = state.hasNext) {
                    Icon(painterResource(R.drawable.ic_skip_next), contentDescription = "Next track")
                }
            }
            SeekBar(state, onSeek)
        }
    }
}

@Composable
private fun SeekBar(state: NowPlaying, onSeek: (Long) -> Unit) {
    // While dragging, show the thumb where the finger is; seek once on release.
    var dragging by remember { mutableStateOf<Float?>(null) }
    val duration = state.durationMs.toFloat()
    val position = dragging ?: state.positionMs.toFloat().coerceIn(0f, duration.coerceAtLeast(0f))
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(formatDuration(position.toLong()), style = MaterialTheme.typography.labelSmall)
        Slider(
            value = if (duration > 0f) position else 0f,
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let { onSeek(it.toLong()) }
                dragging = null
            },
            valueRange = 0f..duration.coerceAtLeast(1f),
            enabled = duration > 0f,
            colors = SliderDefaults.colors(inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.weight(1f).height(32.dp).padding(horizontal = 8.dp)
                .semantics { contentDescription = "Seek" },
        )
        Text(formatDuration(state.durationMs), style = MaterialTheme.typography.labelSmall)
    }
}

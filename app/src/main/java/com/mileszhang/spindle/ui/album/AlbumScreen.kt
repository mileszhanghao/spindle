package com.mileszhang.spindle.ui.album

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileszhang.spindle.R
import com.mileszhang.spindle.model.AlbumDetail
import com.mileszhang.spindle.model.Credit
import com.mileszhang.spindle.model.Track
import com.mileszhang.spindle.playback.NowPlaying
import com.mileszhang.spindle.ui.common.ErrorView
import com.mileszhang.spindle.ui.common.LoadingView
import com.mileszhang.spindle.ui.common.formatDuration

@Composable
fun AlbumScreen(
    nowPlaying: NowPlaying,
    onPlay: (AlbumDetail, Int) -> Unit,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.padding(top = contentPadding.calculateTopPadding())) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Spacer(Modifier.weight(1f))
            val ready = state as? AlbumUiState.Ready
            if (ready != null) {
                IconToggleButton(
                    checked = ready.isFavorite,
                    onCheckedChange = viewModel::setFavorite,
                    modifier = Modifier.semantics { stateDescription = if (ready.isFavorite) "In favorites" else "Not in favorites" },
                ) {
                    Icon(
                        if (ready.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (ready.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        val bottom = PaddingValues(bottom = contentPadding.calculateBottomPadding())
        when (val s = state) {
            AlbumUiState.Loading -> LoadingView(Modifier.padding(bottom))
            is AlbumUiState.Failed -> ErrorView(s.message, viewModel::retry, Modifier.padding(bottom))
            is AlbumUiState.Ready -> AlbumContent(s.detail, nowPlaying, onPlay, bottom)
        }
    }
}

@Composable
fun AlbumContent(detail: AlbumDetail, nowPlaying: NowPlaying, onPlay: (AlbumDetail, Int) -> Unit, contentPadding: PaddingValues) {
    val album = detail.album
    val playingHere = nowPlaying.albumId == album.id
    LazyColumn(contentPadding = contentPadding) {
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Vinyl(
                    coverUrl = album.coverUrl,
                    spinning = playingHere && nowPlaying.isPlaying,
                    size = 260.dp,
                    contentDescription = "Cover of ${album.title}",
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    album.title,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    "${album.artist} • ${album.year}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Text(album.description, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { onPlay(detail, 0) }) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (playingHere) "Play from the start" else "Play")
                }
                Spacer(Modifier.height(12.dp))
            }
        }
        itemsIndexed(detail.tracks, key = { _, t -> t.id }) { index, track ->
            TrackRow(index, track, isCurrent = nowPlaying.trackId == track.id, isPlaying = nowPlaying.isPlaying) {
                onPlay(detail, index)
            }
        }
        item { Credits(detail) }
    }
}

@Composable
private fun TrackRow(index: Int, track: Track, isCurrent: Boolean, isPlaying: Boolean, onClick: () -> Unit) {
    val accent = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(if (isCurrent) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.background)
            .clickable(role = Role.Button, onClickLabel = "Play ${track.title}", onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(28.dp), contentAlignment = Alignment.CenterStart) {
            if (isCurrent && isPlaying) {
                Icon(painterResource(R.drawable.ic_equalizer), contentDescription = "Now playing", tint = accent, modifier = Modifier.size(18.dp))
            } else {
                Text("${index + 1}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                color = accent,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                track.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(formatDuration(track.durationMs), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Every cover and recording is credited in the app, with a link to its source. */
@Composable
private fun Credits(detail: AlbumDetail) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Credits", style = MaterialTheme.typography.titleSmall)
        CreditLine("Cover", detail.album.coverCredit)
        detail.tracks.forEach { CreditLine(it.title, it.credit) }
    }
}

@Composable
private fun CreditLine(label: String, credit: Credit) {
    val uri = LocalUriHandler.current
    Text(
        "$label: ${credit.author}, ${credit.license}",
        style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clickable(role = Role.Button, onClickLabel = "Open source page") { uri.openUri(credit.sourceUrl) },
    )
}

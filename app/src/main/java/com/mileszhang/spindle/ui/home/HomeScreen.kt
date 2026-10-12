package com.mileszhang.spindle.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.mileszhang.spindle.model.AlbumSummary
import com.mileszhang.spindle.model.Section
import com.mileszhang.spindle.ui.common.ErrorView
import com.mileszhang.spindle.ui.common.LoadingView
import com.mileszhang.spindle.ui.common.greetingFor
import java.time.LocalTime

@Composable
fun HomeScreen(
    onAlbumClick: (Int) -> Unit,
    contentPadding: PaddingValues,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        HomeUiState.Loading -> LoadingView(Modifier.padding(contentPadding))
        is HomeUiState.Failed -> ErrorView(s.message, viewModel::retry, Modifier.padding(contentPadding))
        is HomeUiState.Ready -> HomeContent(s.sections, onAlbumClick, contentPadding)
    }
}

@Composable
fun HomeContent(sections: List<Section>, onAlbumClick: (Int) -> Unit, contentPadding: PaddingValues) {
    val greeting = remember { greetingFor(LocalTime.now().hour) }
    LazyColumn(contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(28.dp)) {
        item {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp)) {
                Text(greeting, style = MaterialTheme.typography.displaySmall, modifier = Modifier.semantics { heading() })
                Text(
                    "Public-domain records for every hour.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(sections, key = { it.title }) { section ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    section.title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp).semantics { heading() },
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(section.albums, key = { it.id }) { album -> AlbumCard(album, onClick = { onAlbumClick(album.id) }) }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun AlbumCard(album: AlbumSummary, onClick: () -> Unit) {
    Column(
        Modifier
            .width(156.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClickLabel = "Open ${album.title}", onClick = onClick),
    ) {
        AsyncImage(
            model = album.coverUrl,
            contentDescription = "Cover of ${album.title}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(156.dp).clip(RoundedCornerShape(10.dp)),
        )
        Spacer(Modifier.height(8.dp))
        Text(album.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            album.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

package com.mileszhang.spindle.ui.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.mileszhang.spindle.data.FavoriteAlbum
import com.mileszhang.spindle.ui.common.LoadingView

@Composable
fun FavoritesScreen(
    onAlbumClick: (Int) -> Unit,
    contentPadding: PaddingValues,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    FavoritesContent(favorites, onAlbumClick, contentPadding)
}

@Composable
fun FavoritesContent(favorites: List<FavoriteAlbum>?, onAlbumClick: (Int) -> Unit, contentPadding: PaddingValues) {
    when {
        favorites == null -> LoadingView(Modifier.padding(contentPadding))
        favorites.isEmpty() -> Column(
            Modifier.fillMaxSize().padding(contentPadding).padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("No favorites yet", style = MaterialTheme.typography.titleLarge)
            Text(
                "Tap the heart on an album to keep it here, even offline.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        else -> LazyColumn(contentPadding = contentPadding) {
            item {
                Text(
                    "Favorites",
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 12.dp).semantics { heading() },
                )
            }
            items(favorites, key = { it.id }) { album ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button, onClickLabel = "Open ${album.title}") { onAlbumClick(album.id) }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    AsyncImage(
                        model = album.coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(60.dp).clip(RoundedCornerShape(6.dp)),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(album.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${album.artist} • ${album.year}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

package com.mileszhang.spindle.ui.album

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileszhang.spindle.data.CatalogRepository
import com.mileszhang.spindle.data.FavoritesRepository
import com.mileszhang.spindle.model.AlbumDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AlbumUiState {
    data object Loading : AlbumUiState
    data class Ready(val detail: AlbumDetail, val isFavorite: Boolean) : AlbumUiState
    data class Failed(val message: String) : AlbumUiState
}

/**
 * Gets only the album id from navigation (the course passed a whole
 * Serializable Album). Loading starts once in init; the course called
 * fetchPlaylist() from onViewCreated and started another collector on every visit.
 */
@HiltViewModel
class AlbumViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val catalog: CatalogRepository,
    private val favorites: FavoritesRepository,
) : ViewModel() {
    val albumId: Int = checkNotNull(savedStateHandle.get<Int>("id")) { "AlbumRoute.id missing" }

    private sealed interface Load {
        data object Loading : Load
        data class Done(val detail: AlbumDetail) : Load
        data class Failed(val message: String) : Load
    }

    private val load = MutableStateFlow<Load>(Load.Loading)

    val state: StateFlow<AlbumUiState> = combine(load, favorites.isFavorite(albumId)) { load, isFavorite ->
        when (load) {
            Load.Loading -> AlbumUiState.Loading
            is Load.Failed -> AlbumUiState.Failed(load.message)
            is Load.Done -> AlbumUiState.Ready(load.detail, isFavorite)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlbumUiState.Loading)

    init { fetch() }

    fun retry() = fetch()

    fun setFavorite(favorite: Boolean) {
        val detail = (load.value as? Load.Done)?.detail ?: return
        viewModelScope.launch { favorites.setFavorite(detail.album, favorite) }
    }

    private fun fetch() {
        load.value = Load.Loading
        viewModelScope.launch {
            load.value = catalog.album(albumId).fold(
                onSuccess = { Load.Done(it) },
                onFailure = { Load.Failed(it.message ?: "Couldn't load this album.") },
            )
        }
    }
}

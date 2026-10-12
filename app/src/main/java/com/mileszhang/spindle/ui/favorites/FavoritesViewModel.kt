package com.mileszhang.spindle.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileszhang.spindle.data.FavoriteAlbum
import com.mileszhang.spindle.data.FavoritesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(favorites: FavoritesRepository) : ViewModel() {
    /** null until Room's first emission, so the screen can tell "loading" from "empty". */
    val favorites: StateFlow<List<FavoriteAlbum>?> =
        favorites.favorites().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

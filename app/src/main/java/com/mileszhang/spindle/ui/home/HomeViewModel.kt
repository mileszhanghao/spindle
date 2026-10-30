package com.mileszhang.spindle.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileszhang.spindle.data.CatalogRepository
import com.mileszhang.spindle.model.Section
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Ready(val sections: List<Section>) : HomeUiState
    data class Failed(val message: String) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(private val catalog: CatalogRepository) : ViewModel() {
    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    // Loads once per ViewModel, not every time the screen is recomposed or revisited.
    init { load() }

    fun retry() = load()

    private fun load() {
        _state.value = HomeUiState.Loading
        viewModelScope.launch {
            _state.value = catalog.feed().fold(
                onSuccess = { HomeUiState.Ready(it.sections) },
                onFailure = { HomeUiState.Failed(it.message ?: "Couldn't load the catalog.") },
            )
        }
    }
}

package com.mileszhang.spindle

import androidx.lifecycle.SavedStateHandle
import com.mileszhang.spindle.ui.album.AlbumUiState
import com.mileszhang.spindle.ui.album.AlbumViewModel
import com.mileszhang.spindle.ui.home.HomeUiState
import com.mileszhang.spindle.ui.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `home loads the feed once and shows sections`() {
        val catalog = FakeCatalog()
        val vm = HomeViewModel(catalog)
        val state = vm.state.value as HomeUiState.Ready
        assertEquals(listOf(1, 2), state.sections.single().albums.map { it.id })
        assertEquals(1, catalog.feedCalls)
    }

    @Test
    fun `home shows an error with retry instead of crashing`() {
        val catalog = FakeCatalog().apply { failFeed = true }
        val vm = HomeViewModel(catalog)
        assertEquals(HomeUiState.Failed("offline"), vm.state.value)
        catalog.failFeed = false
        vm.retry()
        assertTrue(vm.state.value is HomeUiState.Ready)
    }

    @Test
    fun `album reads its id from navigation and toggles favorite`() = runTest {
        val favorites = FakeFavorites()
        val vm = AlbumViewModel(SavedStateHandle(mapOf("id" to 3)), FakeCatalog(), favorites)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        val ready = vm.state.first { it is AlbumUiState.Ready } as AlbumUiState.Ready
        assertEquals(3, ready.detail.album.id)
        assertEquals(listOf("3-1", "3-2"), ready.detail.tracks.map { it.id })
        assertEquals(false, ready.isFavorite)

        vm.setFavorite(true)
        assertEquals(true, (vm.state.first { it is AlbumUiState.Ready && it.isFavorite } as AlbumUiState.Ready).isFavorite)
        assertTrue(3 in favorites.stored.value)

        vm.setFavorite(false)
        assertTrue(favorites.stored.value.isEmpty())
    }

    @Test
    fun `missing album shows the error message`() = runTest {
        val vm = AlbumViewModel(SavedStateHandle(mapOf("id" to 404)), FakeCatalog(), FakeFavorites())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        assertEquals(AlbumUiState.Failed("Not found"), vm.state.first { it is AlbumUiState.Failed })
    }
}

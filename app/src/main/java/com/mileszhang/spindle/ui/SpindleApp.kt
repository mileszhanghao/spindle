package com.mileszhang.spindle.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mileszhang.spindle.ui.album.AlbumScreen
import com.mileszhang.spindle.ui.favorites.FavoritesScreen
import com.mileszhang.spindle.ui.home.HomeScreen
import com.mileszhang.spindle.ui.player.MiniPlayer
import com.mileszhang.spindle.ui.player.PlayerViewModel
import kotlin.reflect.KClass

private data class Tab(val label: String, val icon: ImageVector, val route: Any, val routeClass: KClass<*>)

private val tabs = listOf(
    Tab("Home", Icons.Filled.Home, HomeRoute, HomeRoute::class),
    Tab("Favorites", Icons.Filled.Favorite, FavoritesRoute, FavoritesRoute::class),
)

/**
 * Single Activity, all Compose. Replaces the course's Fragments + XML nav graph
 * + ComposeView, and its BottomNavigationView back-stack workaround.
 */
@Composable
fun SpindleApp(player: PlayerViewModel = hiltViewModel()) {
    val nav = rememberNavController()
    val nowPlaying by player.nowPlaying.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val backStack by nav.currentBackStackEntryAsState()

    LaunchedEffect(nowPlaying.error) {
        nowPlaying.error?.let {
            snackbar.showSnackbar(it)
            player.clearError()
        }
    }
    AskForNotificationsOnFirstPlay(nowPlaying.isPlaying)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Column {
                AnimatedVisibility(nowPlaying.isActive, enter = expandVertically(), exit = shrinkVertically()) {
                    MiniPlayer(
                        state = nowPlaying,
                        onOpen = { nowPlaying.albumId?.let { nav.navigate(AlbumRoute(it)) { launchSingleTop = true } } },
                        onTogglePlay = player::togglePlayPause,
                        onPrevious = player::previous,
                        onNext = player::next,
                        onSeek = player::seekTo,
                    )
                }
                NavigationBar {
                    tabs.forEach { tab ->
                        val selected = backStack?.destination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { nav.switchTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = HomeRoute) {
            composable<HomeRoute> {
                HomeScreen(onAlbumClick = { nav.navigate(AlbumRoute(it)) }, contentPadding = padding)
            }
            composable<FavoritesRoute> {
                FavoritesScreen(onAlbumClick = { nav.navigate(AlbumRoute(it)) }, contentPadding = padding)
            }
            composable<AlbumRoute> {
                AlbumScreen(
                    nowPlaying = nowPlaying,
                    onPlay = { detail, index -> player.play(detail.album, detail.tracks, index) },
                    onBack = { nav.popBackStack() },
                    contentPadding = padding,
                )
            }
        }
    }
}

private fun NavHostController.switchTab(route: Any) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

/** Android 13+ needs permission for the media notification. Ask when it is first useful, not at launch. */
@Composable
private fun AskForNotificationsOnFirstPlay(isPlaying: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(isPlaying) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (isPlaying && !asked && !granted) {
            asked = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

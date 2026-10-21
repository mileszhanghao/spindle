package com.mileszhang.spindle.ui

import kotlinx.serialization.Serializable

// Type-safe Navigation Compose routes. Only the album id is passed; the course
// passed a whole Serializable Album through Safe Args.
@Serializable data object HomeRoute
@Serializable data object FavoritesRoute
@Serializable data class AlbumRoute(val id: Int)

package com.mileszhang.spindle.server

import com.mileszhang.spindle.model.CatalogIndex
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

@Serializable
data class ApiError(val error: String)

/**
 * Catalog API. The course version re-read and re-parsed a JSON file on every
 * request, sent JSON as text/plain, and answered unknown playlists with
 * 200 "null". Here the catalog is validated once at startup, responses are
 * typed JSON, and unknown or malformed ids are 404/400.
 *
 * Paths end in .json so the same URLs work when the API is exported as static
 * files to GitHub Pages.
 */
fun Application.spindleApi(catalog: CatalogIndex) {
    install(ContentNegotiation) { json(CatalogIndex.json) }
    install(CORS) { anyHost() }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled error", cause)
            call.respond(HttpStatusCode.InternalServerError, ApiError("something went wrong"))
        }
    }
    routing {
        get("/health") { call.respond(mapOf("status" to "ok")) }
        route("/api") {
            get("/feed.json") { call.respond(catalog.feed) }
            get("/albums/{file}") {
                val id = call.parameters["file"]?.removeSuffix(".json")?.toIntOrNull()
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiError("album id must be a number"))
                val album = catalog.album(id)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ApiError("album $id not found"))
                call.respond(album)
            }
        }
    }
}

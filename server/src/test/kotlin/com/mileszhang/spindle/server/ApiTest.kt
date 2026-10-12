package com.mileszhang.spindle.server

import com.mileszhang.spindle.model.AlbumDetail
import com.mileszhang.spindle.model.CatalogIndex
import com.mileszhang.spindle.model.Feed
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApiTest {
    // The real catalog file, so a broken edit to catalog.json fails CI.
    private val catalog = CatalogIndex.parse(File(System.getProperty("spindle.catalog")).readText())

    @Test
    fun `feed is JSON and lists every section`() = testApplication {
        application { spindleApi(catalog) }
        val response = client.get("/api/feed.json")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.contentType()!!.match(ContentType.Application.Json))
        val feed = CatalogIndex.json.decodeFromString(Feed.serializer(), response.bodyAsText())
        assertEquals(catalog.feed, feed)
        assertTrue(feed.sections.isNotEmpty())
    }

    @Test
    fun `every album in the feed resolves with playable tracks`() = testApplication {
        application { spindleApi(catalog) }
        catalog.feed.sections.flatMap { it.albums }.forEach { album ->
            val response = client.get("/api/albums/${album.id}.json")
            assertEquals(HttpStatusCode.OK, response.status, "album ${album.id}")
            val detail = CatalogIndex.json.decodeFromString(AlbumDetail.serializer(), response.bodyAsText())
            assertEquals(album, detail.album)
            assertTrue(detail.tracks.isNotEmpty())
        }
    }

    @Test
    fun `unknown and malformed ids are 404 and 400, not 200 null`() = testApplication {
        application { spindleApi(catalog) }
        assertEquals(HttpStatusCode.NotFound, client.get("/api/albums/9999.json").status)
        assertEquals(HttpStatusCode.BadRequest, client.get("/api/albums/abc.json").status)
    }
}

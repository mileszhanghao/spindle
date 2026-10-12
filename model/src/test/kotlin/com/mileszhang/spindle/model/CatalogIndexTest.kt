package com.mileszhang.spindle.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class CatalogIndexTest {
    private val credit = Credit("someone", "Public domain", "https://example.org/file")
    private fun track(id: String, ms: Long = 1000, url: String = "https://example.org/$id.ogg") =
        Track(id, "Track $id", "Artist", ms, url, credit)
    private fun album(id: Int, vararg tracks: Track) =
        AlbumSource(id, "Album $id", "Artist", 1920, "https://example.org/$id.jpg", "About $id", credit, tracks.toList())

    @Test
    fun `builds the feed in section order and looks albums up by id`() {
        val index = CatalogIndex(
            Catalog(
                sections = listOf(SectionSource("Rainy mornings", listOf(2, 1))),
                albums = listOf(album(1, track("1-1")), album(2, track("2-1"), track("2-2"))),
            ),
        )
        assertEquals(listOf(2, 1), index.feed.sections.single().albums.map { it.id })
        assertEquals(listOf("2-1", "2-2"), index.album(2)!!.tracks.map { it.id })
        assertNull(index.album(99))
    }

    @Test
    fun `rejects catalogs the app could not play`() {
        val ok = album(1, track("1-1"))
        assertFailsWith<CatalogException> { CatalogIndex(Catalog(listOf(SectionSource("x", listOf(7))), listOf(ok))) }
        assertFailsWith<CatalogException> { CatalogIndex(Catalog(emptyList(), listOf(ok, album(1, track("1-2"))))) }
        assertFailsWith<CatalogException> { CatalogIndex(Catalog(emptyList(), listOf(album(1, track("1-1"), track("1-1"))))) }
        assertFailsWith<CatalogException> { CatalogIndex(Catalog(emptyList(), listOf(album(1)))) }
        assertFailsWith<CatalogException> { CatalogIndex(Catalog(emptyList(), listOf(album(1, track("1-1", ms = 0))))) }
        assertFailsWith<CatalogException> {
            CatalogIndex(Catalog(emptyList(), listOf(album(1, track("1-1", url = "http://example.org/a.ogg")))))
        }
    }
}

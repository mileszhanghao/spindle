package com.mileszhang.spindle.server

import com.mileszhang.spindle.model.AlbumDetail
import com.mileszhang.spindle.model.CatalogIndex
import com.mileszhang.spindle.model.Feed
import java.io.File

/** Usage: Export <catalog.json> <outDir>. Writes the API as static files. */
fun main(args: Array<String>) {
    require(args.size == 2) { "usage: Export <catalog.json> <outDir>" }
    val catalog = CatalogIndex.parse(File(args[0]).readText())
    val out = File(args[1]).apply { deleteRecursively(); mkdirs() }
    File(out, "feed.json").writeText(CatalogIndex.json.encodeToString(Feed.serializer(), catalog.feed))
    val albums = File(out, "albums").apply { mkdirs() }
    catalog.albumIds.sorted().forEach { id ->
        File(albums, "$id.json").writeText(CatalogIndex.json.encodeToString(AlbumDetail.serializer(), catalog.album(id)!!))
    }
    println("Exported feed and ${catalog.albumIds.size} albums to ${out.path}")
}

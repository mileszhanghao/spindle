package com.mileszhang.spindle.server

import com.mileszhang.spindle.model.CatalogIndex
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.io.File

/** Usage: ./gradlew :server:run   (serves catalog/catalog.json on http://localhost:8080) */
fun main(args: Array<String>) {
    val catalogPath = args.firstOrNull() ?: "catalog/catalog.json"
    val catalog = CatalogIndex.parse(File(catalogPath).readText())
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0") { spindleApi(catalog) }.start(wait = true)
}

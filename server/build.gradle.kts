plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

kotlin { jvmToolchain(17) }

application {
    mainClass.set("com.mileszhang.spindle.server.MainKt")
}

dependencies {
    implementation(project(":model"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.logback)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
}

val catalogFile = rootProject.file("catalog/catalog.json")

tasks.named<JavaExec>("run") {
    args(catalogFile.absolutePath)
}

tasks.test {
    systemProperty("spindle.catalog", catalogFile.absolutePath)
}

// Writes the same responses the server returns as static files, for the
// GitHub Pages demo: build/static-api/feed.json and build/static-api/albums/<id>.json.
val exportStaticApi by tasks.registering(JavaExec::class) {
    group = "distribution"
    description = "Exports the catalog API as static JSON files"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.mileszhang.spindle.server.ExportKt")
    val out = layout.buildDirectory.dir("static-api")
    args(catalogFile.absolutePath, out.get().asFile.absolutePath)
    inputs.file(catalogFile)
    outputs.dir(out)
}

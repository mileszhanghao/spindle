# Spindle

An Android record player for rainy nights. You browse albums of public-domain and Creative Commons recordings: Cantonese gaohu, Satie on guitar, Chopin after midnight, ragtime for the last ferry. Pick one and the album cover spins like a record while it plays. Music keeps playing in the background, with notification, lock-screen and headphone controls.

**Demo:** https://mileszhanghao.github.io/spindle/. Listen to the catalog in the browser, or download the APK (Android 8.0+).

**Stack:** Kotlin 2.1 · Jetpack Compose (Material 3) · Media3 (ExoPlayer + MediaSessionService) · Navigation Compose (type-safe routes) · Hilt (KSP) · Room · Retrofit + kotlinx.serialization · Coil · Ktor 3 server · JUnit + coroutines-test · GitHub Actions

## Background

I first built this in LaiOffer's Android course (2025), following a Spotify-style app. That version used Fragments with XML navigation and a `ComposeView` in each Fragment, Hilt, Retrofit/Gson, Room, ExoPlayer 2 and a local Ktor mock server. In 2026 I rebuilt it to fix the problems I found when re-reading the code, and gave it a catalog I can actually publish. The rebuild was developed with AI pair-programming assistance (Claude).

## What changed from the course version, and why

| Problem in the course version | What the rebuild does |
|---|---|
| The ExoPlayer lived in an Activity-scoped ViewModel: music stopped with the Activity, with no notification, lock-screen controls or audio focus, and the player was never released. | Playback runs in a Media3 `MediaSessionService`, and the UI talks to it through a `MediaController`. You get background playback, a media notification, lock-screen and Bluetooth controls, audio focus, and a pause when headphones are unplugged. The player is released when the service stops. |
| `GlobalScope` code left in `MainActivity` made a network call and inserted an album into Favorites on **every app launch**. | Removed. Each coroutine runs in a lifecycle-bound scope (`viewModelScope`, or the playback connection's own scope). |
| Repositories called `api.x().execute().body()!!`, so any HTTP error or empty body crashed the app. There was no error state, and a leftover `delay(3000)`. | Retrofit `suspend` functions. Failures become a `Result` with a readable message, and each screen has loading, error and retry states. |
| One `Album` class was the Gson DTO, the Room entity and a `Serializable` Safe-Args argument. | Separate wire models (shared with the server in `:model`), a Room entity, and type-safe routes that pass only the album id. |
| `fetchPlaylist()` ran from `onViewCreated`, so every visit started another network call and another Room collector. | The ViewModel loads once in `init`, combines its flows, and exposes them with `stateIn(WhileSubscribed)`. The UI collects with `collectAsStateWithLifecycle`. |
| `Song` had no id, so "now playing" was decided by comparing display fields. `length` was a display string. | Tracks have stable ids (`"<album>-<n>"`) and `durationMs`. |
| The playback position was polled forever, and `C.TIME_UNSET` (a negative number) could reach the Slider's range. | The position ticker runs only while playing. An unknown duration shows as 0 and disables the seek bar. |
| The record jumped back to 0° on pause. | It stops where it is, like a turntable. |
| Fragments, XML layouts, a nav graph, `BottomNavigationView` with a back-stack workaround, and a separate `MaterialTheme` in each Fragment. | A single Activity, all Compose: `NavigationBar` + `NavHost` in a `Scaffold`, with one app theme. |
| Icons had empty content descriptions and 28dp touch targets. | `IconButton`/`IconToggleButton` with labels and state descriptions, and 48dp targets. |
| ExoPlayer 2 (deprecated), kapt, Compose Material 1 with mismatched Compose versions, JVM 1.8. | Media3, KSP, Material 3 with the Compose BOM and the Kotlin 2.x Compose compiler, JVM 17. |
| The cleartext HTTP base URL was hard-coded, and a spoofed browser User-Agent was used to get past Wikimedia. | HTTPS by default (configurable through a Gradle property). Cleartext is allowed only to `10.0.2.2` in debug builds. One honest User-Agent covers the API, images and audio. |
| The Ktor mock re-read the JSON file on every request, sent it as `text/plain`, and answered unknown ids with `200 "null"`. | The catalog is validated once at startup and served as typed JSON, with 404/400 for unknown or malformed ids. Tests run against the real catalog file. |

## Architecture

```
app/        Android app
  ui/          Compose screens + ViewModels (Home, Album, Favorites, MiniPlayer)
  playback/    PlaybackService (MediaSessionService + ExoPlayer), PlaybackConnection (MediaController → StateFlow)
  data/        Retrofit API, repositories, Room (favorites)
model/      Kotlin/JVM: wire models + catalog validation, shared by app and server
server/     Ktor: serves the catalog API; also exports it as static JSON for GitHub Pages
catalog/    catalog.json, the albums, tracks, sections and credits
site/       the demo page
```

The app reads `feed.json` and `albums/{id}.json`. By default these come from the static copy on GitHub Pages, so the APK works without a server. Pointed at the Ktor server, the same paths return the same JSON.

## The catalog

Each album is a grouping I chose, built from recordings on Wikimedia Commons that are in the public domain, CC0, or CC BY-SA. Cover art comes from public-domain paintings (Hiroshige, Caillebotte, Van Gogh, Spitzweg, Hokusai, Toulouse-Lautrec). Each album screen links every cover and recording to its source page, author and license.

For the jazz album I used only recordings from 1925 or earlier, the ones that are in the public domain in the US. Some later recordings are labeled public domain on Commons, but the label doesn't hold under US law.

## Running it

```bash
./gradlew :app:installDebug                    # app against the GitHub Pages API
./gradlew :server:run                          # catalog API on http://localhost:8080
./gradlew :app:installDebug -Pspindle.apiBaseUrl=http://10.0.2.2:8080/api/   # app against the local server (emulator)
```

## Tests

```bash
./gradlew :model:test :server:test :app:testDebugUnitTest
```

- Catalog validation: duplicate ids, unknown albums in sections, empty albums, non-HTTPS media.
- The Ktor API, run against the real `catalog.json`.
- ViewModels with fake repositories: loading, error and retry, favorites, missing albums.
- Formatting helpers.

GitHub Actions runs the tests and builds debug and release APKs on every push. Pushes to `main` also publish the demo site.

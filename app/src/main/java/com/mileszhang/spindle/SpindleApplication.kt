package com.mileszhang.spindle

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class SpindleApplication : Application(), ImageLoaderFactory {
    @Inject lateinit var okHttp: OkHttpClient

    /** Covers load through the same OkHttp client, with the app's User-Agent. */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient(okHttp)
        .crossfade(true)
        .build()
}

package com.mileszhang.spindle.data

import com.mileszhang.spindle.model.AlbumDetail
import com.mileszhang.spindle.model.Feed
import retrofit2.http.GET
import retrofit2.http.Path

/** Suspend functions instead of the course's blocking Call<T>.execute() inside withContext(IO). */
interface SpindleApi {
    @GET("feed.json")
    suspend fun feed(): Feed

    @GET("albums/{id}.json")
    suspend fun album(@Path("id") id: Int): AlbumDetail
}

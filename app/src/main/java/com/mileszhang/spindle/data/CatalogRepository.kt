package com.mileszhang.spindle.data

import com.mileszhang.spindle.model.AlbumDetail
import com.mileszhang.spindle.model.Feed
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

interface CatalogRepository {
    suspend fun feed(): Result<Feed>
    suspend fun album(id: Int): Result<AlbumDetail>
}

/**
 * Turns network failures into a Result with a message the UI can show. The
 * course version used `body()!!`, so any HTTP error or empty body crashed the app.
 */
class NetworkCatalogRepository @Inject constructor(private val api: SpindleApi) : CatalogRepository {
    override suspend fun feed() = load { api.feed() }
    override suspend fun album(id: Int) = load { api.album(id) }

    private suspend fun <T> load(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            Result.failure(LoadException(if (e.code() == 404) "Not found" else "Server error (${e.code()})", e))
        } catch (e: IOException) {
            Result.failure(LoadException("Can't reach the music catalog. Check your connection.", e))
        } catch (e: Exception) {
            Result.failure(LoadException("Something went wrong loading the catalog.", e))
        }
}

class LoadException(message: String, cause: Throwable? = null) : Exception(message, cause)

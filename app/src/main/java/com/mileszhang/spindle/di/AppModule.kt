package com.mileszhang.spindle.di

import android.content.Context
import androidx.room.Room
import com.mileszhang.spindle.BuildConfig
import com.mileszhang.spindle.data.CatalogRepository
import com.mileszhang.spindle.data.FavoritesRepository
import com.mileszhang.spindle.data.NetworkCatalogRepository
import com.mileszhang.spindle.data.RoomFavoritesRepository
import com.mileszhang.spindle.data.SpindleApi
import com.mileszhang.spindle.data.db.FavoriteDao
import com.mileszhang.spindle.data.db.SpindleDatabase
import com.mileszhang.spindle.model.CatalogIndex
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/** Wikimedia asks API and media clients to identify themselves; one honest UA for every request. */
const val USER_AGENT = "Spindle/1.0 (Android; https://github.com/mileszhanghao/spindle)"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun okHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("User-Agent", USER_AGENT).build())
        }
        .build()

    @Provides
    @Singleton
    fun api(client: OkHttpClient): SpindleApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(CatalogIndex.json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(SpindleApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): SpindleDatabase =
        Room.databaseBuilder(context, SpindleDatabase::class.java, "spindle.db").build()

    @Provides
    fun favoriteDao(db: SpindleDatabase): FavoriteDao = db.favoriteDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun catalog(impl: NetworkCatalogRepository): CatalogRepository

    @Binds
    abstract fun favorites(impl: RoomFavoritesRepository): FavoritesRepository
}

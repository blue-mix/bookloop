package com.starry.myne.di

import android.content.Context
import com.starry.myne.api.BookAPI
import com.starry.myne.database.MyneDatabase
import com.starry.myne.epub.EpubParser
import com.starry.myne.helpers.PreferenceUtil
import com.starry.myne.helpers.book.BookDownloader
import com.starry.myne.ui.screens.welcome.viewmodels.WelcomeDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton


@InstallIn(SingletonComponent::class)
@Module
class MainModule {

    // Core application related. ==========================================================

    @Provides
    fun provideAppContext(@ApplicationContext context: Context) = context

    @Singleton
    @Provides
    fun provideMyneDatabase(@ApplicationContext context: Context) =
        MyneDatabase.getInstance(context)

    @Provides
    fun provideLibraryDao(myneDatabase: MyneDatabase) = myneDatabase.getLibraryDao()

    @Provides
    fun provideReaderDao(myneDatabase: MyneDatabase) = myneDatabase.getReaderDao()

    @Singleton
    @Provides
    fun providePreferenceUtil(@ApplicationContext context: Context) = PreferenceUtil(context)

    // Network & Books related. ==========================================================

    @Singleton
    @Provides
    fun provideBooksApi(
        @ApplicationContext context: Context,
        preferenceUtil: PreferenceUtil
    ) = BookAPI(context, preferenceUtil)

    @Singleton
    @Provides
    fun provideBookDownloader(@ApplicationContext context: Context) = BookDownloader(context)

    @Singleton
    @Provides
    fun provideEpubParser(@ApplicationContext context: Context) = EpubParser(context)

    @Provides
    @Singleton
    fun provideDataStoreRepository(
        @ApplicationContext context: Context
    ) = WelcomeDataStore(context = context)

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
            )
        }

    }
}
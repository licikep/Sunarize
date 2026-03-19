package com.banglaread.di

import android.content.Context
import androidx.room.Room
import com.banglaread.data.local.AppDatabase
import com.banglaread.data.local.dao.ArticleDao
import com.banglaread.data.local.dao.FeedDao
import com.banglaread.data.remote.parser.HtmlCleaner
import com.banglaread.data.remote.parser.OPMLParser
import com.banglaread.data.remote.parser.RSSParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, AppDatabase.DATABASE_NAME).fallbackToDestructiveMigration().build()

    @Provides fun provideArticleDao(db: AppDatabase): ArticleDao = db.articleDao()
    @Provides fun provideFeedDao(db: AppDatabase): FeedDao = db.feedDao()

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true).followSslRedirects(true)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    @Provides @Singleton fun provideHtmlCleaner(): HtmlCleaner = HtmlCleaner()
    @Provides @Singleton fun provideOPMLParser(): OPMLParser = OPMLParser()
    @Provides @Singleton fun provideRSSParser(c: HtmlCleaner): RSSParser = RSSParser(c)
}

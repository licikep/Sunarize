package com.banglaread.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.banglaread.data.local.dao.ArticleDao
import com.banglaread.data.local.dao.FeedDao
import com.banglaread.data.local.entity.Article
import com.banglaread.data.local.entity.Feed
import com.banglaread.data.local.entity.StringListConverter

@Database(entities = [Article::class, Feed::class], version = 1, exportSchema = true)
@TypeConverters(StringListConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun feedDao(): FeedDao
    companion object { const val DATABASE_NAME = "banglaread.db" }
}

package com.banglaread.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class StringListConverter {
    @TypeConverter fun fromList(list: List<String>): String = Gson().toJson(list)
    @TypeConverter fun toList(json: String): List<String> =
        Gson().fromJson(json, object : TypeToken<List<String>>() {}.type) ?: emptyList()
}

enum class TranslationStatus { PENDING, IN_PROGRESS, DONE, FAILED, NOT_NEEDED }
enum class SummaryTone        { SIMPLE, LITERARY, CASUAL }

@Entity(tableName = "articles")
@TypeConverters(StringListConverter::class)
data class Article(
    @PrimaryKey val id: String,
    val title: String,
    val originalTitle: String = title,
    val originalContent: String,
    val banglaContent: String?     = null,
    val shortSummary: String?      = null,
    val mediumSummary: String?     = null,
    val bulletSummary: String?     = null,
    val sourceUrl: String,
    val imageUrl: String?          = null,
    val feedId: String,
    val feedTitle: String,
    val category: String,
    val author: String?            = null,
    val publishedAt: Long,
    val fetchedAt: Long            = System.currentTimeMillis(),
    val wordCount: Int             = 0,
    val estimatedReadMinutes: Int  = 1,
    val language: String           = "unknown",
    val tags: List<String>         = emptyList(),
    val isRead: Boolean            = false,
    val isSaved: Boolean           = false,
    val readTimeSeconds: Long      = 0L,
    val translationStatus: TranslationStatus = TranslationStatus.PENDING
)

@Entity(tableName = "feeds")
data class Feed(
    @PrimaryKey val id: String,
    val title: String,
    val xmlUrl: String,
    val websiteUrl: String?     = null,
    val category: String,
    val description: String?   = null,
    val iconUrl: String?        = null,
    val isActive: Boolean       = true,
    val isUserAdded: Boolean    = false,
    val reliabilityScore: Float = 1.0f,
    val lastFetchedAt: Long?    = null,
    val fetchErrorCount: Int    = 0,
    val totalArticlesFetched: Int = 0
)

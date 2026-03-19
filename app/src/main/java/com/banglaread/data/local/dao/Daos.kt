package com.banglaread.data.local.dao

import androidx.room.*
import com.banglaread.data.local.entity.Article
import com.banglaread.data.local.entity.Feed
import com.banglaread.data.local.entity.TranslationStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles ORDER BY publishedAt DESC")
    fun getAllArticles(): Flow<List<Article>>

    @Query("SELECT * FROM articles WHERE category = :category ORDER BY publishedAt DESC")
    fun getArticlesByCategory(category: String): Flow<List<Article>>

    @Query("SELECT * FROM articles WHERE isSaved = 1 ORDER BY fetchedAt DESC")
    fun getSavedArticles(): Flow<List<Article>>

    @Query("SELECT * FROM articles WHERE isRead = 0 ORDER BY publishedAt DESC")
    fun getUnreadArticles(): Flow<List<Article>>

    @Query("SELECT * FROM articles WHERE title LIKE '%' || :query || '%' OR originalContent LIKE '%' || :query || '%' ORDER BY publishedAt DESC")
    fun searchArticles(query: String): Flow<List<Article>>

    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun getArticleById(id: String): Article?

    @Query("SELECT * FROM articles WHERE translationStatus = 'PENDING' LIMIT :limit")
    suspend fun getPendingTranslations(limit: Int = 10): List<Article>

    @Query("SELECT DISTINCT category FROM articles ORDER BY category ASC")
    fun getCategories(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM articles WHERE id = :id")
    suspend fun exists(id: String): Int

    @Query("SELECT COUNT(*) FROM articles WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArticles(articles: List<Article>)

    @Update
    suspend fun updateArticle(article: Article)

    @Query("UPDATE articles SET isRead = 1, readTimeSeconds = :readTime WHERE id = :id")
    suspend fun markAsRead(id: String, readTime: Long)

    @Query("UPDATE articles SET isSaved = :saved WHERE id = :id")
    suspend fun toggleSaved(id: String, saved: Boolean)

    @Query("UPDATE articles SET banglaContent = :content, translationStatus = :status WHERE id = :id")
    suspend fun updateTranslation(id: String, content: String, status: TranslationStatus)

    @Query("UPDATE articles SET shortSummary = :short, mediumSummary = :medium, bulletSummary = :bullet WHERE id = :id")
    suspend fun updateSummaries(id: String, short: String?, medium: String?, bullet: String?)

    @Query("UPDATE articles SET translationStatus = :status WHERE id = :id")
    suspend fun updateTranslationStatus(id: String, status: TranslationStatus)

    @Query("UPDATE articles SET tags = :tags WHERE id = :id")
    suspend fun updateTags(id: String, tags: String)

    @Query("DELETE FROM articles WHERE isSaved = 0 AND fetchedAt < :olderThan")
    suspend fun deleteOldUnsavedArticles(olderThan: Long)

    @Query("DELETE FROM articles WHERE feedId = :feedId AND isSaved = 0")
    suspend fun deleteUnsavedArticlesForFeed(feedId: String)
}

@Dao
interface FeedDao {
    @Query("SELECT * FROM feeds WHERE isActive = 1 ORDER BY category, title")
    fun getActiveFeeds(): Flow<List<Feed>>

    @Query("SELECT * FROM feeds ORDER BY category, title")
    fun getAllFeeds(): Flow<List<Feed>>

    @Query("SELECT * FROM feeds WHERE isActive = 1 ORDER BY category, title")
    suspend fun getActiveFeedsSync(): List<Feed>

    @Query("SELECT DISTINCT category FROM feeds ORDER BY category ASC")
    fun getCategories(): Flow<List<String>>

    @Query("SELECT * FROM feeds WHERE id = :id")
    suspend fun getFeedById(id: String): Feed?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFeeds(feeds: List<Feed>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeed(feed: Feed)

    @Update
    suspend fun updateFeed(feed: Feed)

    @Query("UPDATE feeds SET lastFetchedAt = :time WHERE id = :id")
    suspend fun updateLastFetched(id: String, time: Long)

    @Query("UPDATE feeds SET fetchErrorCount = fetchErrorCount + 1 WHERE id = :id")
    suspend fun incrementErrorCount(id: String)

    @Query("UPDATE feeds SET fetchErrorCount = 0 WHERE id = :id")
    suspend fun resetErrorCount(id: String)

    @Query("UPDATE feeds SET isActive = :active WHERE id = :id")
    suspend fun setFeedActive(id: String, active: Boolean)

    @Query("UPDATE feeds SET totalArticlesFetched = totalArticlesFetched + :count WHERE id = :id")
    suspend fun incrementArticleCount(id: String, count: Int)

    @Query("DELETE FROM feeds WHERE id = :id")
    suspend fun deleteFeed(id: String)

    @Query("SELECT COUNT(*) FROM feeds WHERE isActive = 1")
    fun getActiveFeedCount(): Flow<Int>
}

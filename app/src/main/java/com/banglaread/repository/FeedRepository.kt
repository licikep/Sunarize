package com.banglaread.repository

import android.content.Context
import com.banglaread.data.local.dao.ArticleDao
import com.banglaread.data.local.dao.FeedDao
import com.banglaread.data.local.entity.Article
import com.banglaread.data.local.entity.Feed
import com.banglaread.data.local.entity.TranslationStatus
import com.banglaread.data.remote.parser.HtmlCleaner
import com.banglaread.data.remote.parser.OPMLParser
import com.banglaread.data.remote.parser.RSSParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed class SyncResult {
    data class Success(val newArticleCount: Int, val feedsUpdated: Int) : SyncResult()
    data class PartialSuccess(val newArticleCount: Int, val errors: List<String>) : SyncResult()
    data class Failure(val message: String) : SyncResult()
}

sealed class FeedSyncResult {
    data class Success(val newArticles: Int) : FeedSyncResult()
    data class Error(val message: String) : FeedSyncResult()
}

@Singleton
class FeedRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val articleDao: ArticleDao,
    private val feedDao: FeedDao,
    private val opmlParser: OPMLParser,
    private val rssParser: RSSParser,
    private val htmlCleaner: HtmlCleaner,
    private val okHttpClient: OkHttpClient
) {
    fun getActiveFeeds(): Flow<List<Feed>>       = feedDao.getActiveFeeds()
    fun getAllFeeds(): Flow<List<Feed>>           = feedDao.getAllFeeds()
    fun getFeedCategories(): Flow<List<String>>  = feedDao.getCategories()
    fun getActiveFeedCount(): Flow<Int>          = feedDao.getActiveFeedCount()
    fun getAllArticles(): Flow<List<Article>>     = articleDao.getAllArticles()
    fun getArticlesByCategory(c: String)         = articleDao.getArticlesByCategory(c)
    fun getSavedArticles()                       = articleDao.getSavedArticles()
    fun getUnreadArticles()                      = articleDao.getUnreadArticles()
    fun searchArticles(q: String)                = articleDao.searchArticles(q)
    fun getUnreadCount()                         = articleDao.getUnreadCount()
    fun getArticleCategories()                   = articleDao.getCategories()
    suspend fun getArticleById(id: String)       = articleDao.getArticleById(id)
    suspend fun markArticleRead(id: String, t: Long = 0L) = articleDao.markAsRead(id, t)
    suspend fun toggleSaved(id: String, saved: Boolean)   = articleDao.toggleSaved(id, saved)
    suspend fun updateTranslation(id: String, content: String) = articleDao.updateTranslation(id, content, TranslationStatus.DONE)
    suspend fun updateTranslationFailed(id: String)            = articleDao.updateTranslationStatus(id, TranslationStatus.FAILED)
    suspend fun updateSummaries(id: String, s: String?, m: String?, b: String?) = articleDao.updateSummaries(id, s, m, b)
    suspend fun getPendingTranslations(limit: Int = 5)          = articleDao.getPendingTranslations(limit)

    suspend fun loadDefaultFeedsIfEmpty() = withContext(Dispatchers.IO) {
        if (feedDao.getActiveFeedsSync().isEmpty()) {
            feedDao.insertFeeds(opmlParser.parseFromAssets(context, "default_feeds.opml").feeds)
        }
    }

    suspend fun importOPML(inputStream: InputStream): OPMLParser.ParseResult = withContext(Dispatchers.IO) {
        val result = opmlParser.parse(inputStream)
        feedDao.insertFeeds(result.feeds)
        result
    }

    suspend fun addFeed(feed: Feed) = feedDao.insertFeed(feed)

    suspend fun syncAllFeeds(): SyncResult = withContext(Dispatchers.IO) {
        val feeds = feedDao.getActiveFeedsSync()
        if (feeds.isEmpty()) return@withContext SyncResult.Failure("No active feeds")
        var totalNew = 0; var updated = 0; val errors = mutableListOf<String>()
        feeds.chunked(5).forEach { batch ->
            batch.forEach { feed ->
                when (val r = syncFeed(feed)) {
                    is FeedSyncResult.Success -> { totalNew += r.newArticles; updated++ }
                    is FeedSyncResult.Error   -> errors.add("${feed.title}: ${r.message}")
                }
            }
        }
        when { errors.isEmpty() -> SyncResult.Success(totalNew, updated)
               totalNew > 0     -> SyncResult.PartialSuccess(totalNew, errors)
               else             -> SyncResult.Failure(errors.first()) }
    }

    suspend fun syncFeed(feed: Feed): FeedSyncResult = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(feed.xmlUrl)
                .header("User-Agent","BanglaRead/1.0")
                .header("Accept","application/rss+xml, application/atom+xml, text/xml").build()
            val resp = okHttpClient.newCall(req).execute()
            if (!resp.isSuccessful) { feedDao.incrementErrorCount(feed.id); return@withContext FeedSyncResult.Error("HTTP ${resp.code}") }
            val bytes = resp.body?.bytes() ?: return@withContext FeedSyncResult.Error("Empty body")
            val articles = rssParser.parse(bytes, feed)
            val newOnes  = articles.filter { articleDao.exists(it.id) == 0 }
            if (newOnes.isNotEmpty()) { articleDao.insertArticles(newOnes); feedDao.incrementArticleCount(feed.id, newOnes.size) }
            feedDao.updateLastFetched(feed.id, System.currentTimeMillis())
            feedDao.resetErrorCount(feed.id)
            FeedSyncResult.Success(newOnes.size)
        } catch (e: Exception) { feedDao.incrementErrorCount(feed.id); FeedSyncResult.Error(e.message ?: "Unknown") }
    }

    suspend fun pruneOldArticles(days: Int = 30) = withContext(Dispatchers.IO) {
        articleDao.deleteOldUnsavedArticles(System.currentTimeMillis() - days * 86_400_000L)
    }
}

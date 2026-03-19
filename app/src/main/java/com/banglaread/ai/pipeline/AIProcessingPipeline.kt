package com.banglaread.ai.pipeline

import com.banglaread.ai.model.*
import com.banglaread.data.local.dao.ArticleDao
import com.banglaread.data.local.entity.TranslationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class PipelineProgress(val total: Int=0, val processed: Int=0, val failed: Int=0, val current: String?=null) {
    val isIdle  get() = total == 0
    val percent get() = if (total == 0) 0f else processed.toFloat() / total
}

@Singleton
class AIProcessingPipeline @Inject constructor(
    private val gemini: GeminiClient,
    private val articleDao: ArticleDao
) {
    private val _progress = MutableStateFlow(PipelineProgress())
    val progress: Flow<PipelineProgress> = _progress.asStateFlow()

    suspend fun processPendingArticles(tone: BanglaTone, limit: Int = 10) = withContext(Dispatchers.IO) {
        val pending = articleDao.getPendingTranslations(limit)
        if (pending.isEmpty()) return@withContext
        _progress.value = PipelineProgress(total = pending.size)
        pending.forEachIndexed { i, article ->
            _progress.value = _progress.value.copy(current = article.title)
            articleDao.updateTranslationStatus(article.id, TranslationStatus.IN_PROGRESS)
            val ok = processSingleArticle(ArticleAIJob(article.id, article.title, article.originalContent, article.language), tone)
            _progress.value = _progress.value.copy(processed = i+1, failed = _progress.value.failed + if (ok) 0 else 1)
        }
        _progress.value = PipelineProgress()
    }

    suspend fun processSingleArticle(job: ArticleAIJob, tone: BanglaTone): Boolean = withContext(Dispatchers.IO) {
        val banglaContent: String
        when (val r = gemini.translate(job, tone)) {
            is AIResult.Success    -> { banglaContent = r.data.banglaContent; articleDao.updateTranslation(job.articleId, banglaContent, TranslationStatus.DONE) }
            is AIResult.RateLimited -> { articleDao.updateTranslationStatus(job.articleId, TranslationStatus.PENDING); return@withContext false }
            is AIResult.Error      -> { articleDao.updateTranslationStatus(job.articleId, if (r.isRetryable) TranslationStatus.PENDING else TranslationStatus.FAILED); return@withContext false }
        }
        when (val r = gemini.summarize(job, banglaContent, tone)) {
            is AIResult.Success -> with(r.data) { articleDao.updateSummaries(job.articleId, short, medium, bullet) }
            else -> {}
        }
        when (val r = gemini.generateTags(job)) {
            is AIResult.Success -> articleDao.updateTags(job.articleId, "[${r.data.tags.joinToString(",") { ""$it"" }}]")
            else -> {}
        }
        true
    }

    suspend fun getHighlights(articleId: String): List<String>? = withContext(Dispatchers.IO) {
        val content = articleDao.getArticleById(articleId)?.banglaContent ?: return@withContext null
        (gemini.extractHighlights(articleId, content) as? AIResult.Success)?.data
    }

    suspend fun askAboutArticle(articleId: String, question: String, tone: BanglaTone): String? = withContext(Dispatchers.IO) {
        val article = articleDao.getArticleById(articleId) ?: return@withContext null
        val summary = article.shortSummary ?: article.banglaContent?.take(500) ?: return@withContext null
        (gemini.askAboutArticle(article.title, summary, question, tone) as? AIResult.Success)?.data
    }

    suspend fun resuminWithTone(articleId: String, tone: BanglaTone): Boolean = withContext(Dispatchers.IO) {
        val article = articleDao.getArticleById(articleId) ?: return@withContext false
        val content = article.banglaContent ?: return@withContext false
        val job = ArticleAIJob(article.id, article.title, content)
        when (val r = gemini.summarize(job, content, tone)) {
            is AIResult.Success -> { with(r.data) { articleDao.updateSummaries(articleId, short, medium, bullet) }; true }
            else -> false
        }
    }
}

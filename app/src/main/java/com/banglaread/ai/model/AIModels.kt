package com.banglaread.ai.model

enum class BanglaTone(val label: String, val banglaLabel: String) {
    SIMPLE("Simple Bangla","সহজ বাংলা"),
    LITERARY("Literary Bangla","সাহিত্যিক বাংলা"),
    CASUAL("Casual Bangla","কথ্য বাংলা")
}

enum class SummaryType { SHORT, MEDIUM, BULLET }

data class TranslationResult(val articleId: String, val banglaTitle: String, val banglaContent: String, val detectedLanguage: String)
data class SummaryResult(val articleId: String, val short: String, val medium: String, val bullet: String, val tone: BanglaTone)
data class TagResult(val articleId: String, val tags: List<String>)
data class ArticleAIJob(val articleId: String, val title: String, val content: String, val sourceLanguage: String = "unknown")

sealed class AIResult<out T> {
    data class Success<T>(val data: T) : AIResult<T>()
    data class Error(val message: String, val isRetryable: Boolean = true) : AIResult<Nothing>()
    data object RateLimited : AIResult<Nothing>()
}

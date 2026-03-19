package com.banglaread.ai.pipeline

import com.banglaread.ai.model.*
import com.banglaread.ai.prompt.PromptBuilder
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.*
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiClient @Inject constructor(private val model: GenerativeModel) {

    suspend fun translate(job: ArticleAIJob, tone: BanglaTone): AIResult<TranslationResult> =
        call(PromptBuilder.buildTranslationPrompt(job.title, job.content, tone)) { json ->
            TranslationResult(job.articleId, json.getString("translated_title").trim(), json.getString("translated_content").trim(), json.optString("detected_language","unknown"))
        }

    suspend fun summarize(job: ArticleAIJob, banglaContent: String, tone: BanglaTone): AIResult<SummaryResult> =
        call(PromptBuilder.buildSummaryPrompt(job.title, banglaContent, tone)) { json ->
            SummaryResult(job.articleId, json.getString("short").trim(), json.getString("medium").trim(), json.getString("bullet").trim(), tone)
        }

    suspend fun generateTags(job: ArticleAIJob): AIResult<TagResult> =
        call(PromptBuilder.buildTagPrompt(job.title, job.content)) { json ->
            val arr = json.getJSONArray("tags")
            TagResult(job.articleId, (0 until arr.length()).map { arr.getString(it).trim() })
        }

    suspend fun extractHighlights(articleId: String, banglaContent: String): AIResult<List<String>> =
        call(PromptBuilder.buildHighlightPrompt(banglaContent)) { json ->
            val arr: JSONArray = json.getJSONArray("highlights")
            (0 until arr.length()).map { arr.getString(it).trim() }
        }

    suspend fun askAboutArticle(title: String, summary: String, question: String, tone: BanglaTone): AIResult<String> =
        try {
            val r = model.generateContent(PromptBuilder.buildConversationPrompt(title, summary, question, tone))
            val t = r.text?.trim() ?: ""
            if (t.isEmpty()) AIResult.Error("Empty response") else AIResult.Success(t)
        } catch (e: Exception) { handleEx(e) }

    private suspend fun <T> call(prompt: String, retries: Int = 3, parser: (JSONObject) -> T): AIResult<T> {
        var last: AIResult<T> = AIResult.Error("Unknown")
        repeat(retries) { attempt ->
            try {
                val raw = model.generateContent(prompt).text?.trim() ?: ""
                if (raw.isEmpty()) { last = AIResult.Error("Empty response"); delay(backoff(attempt)); return@repeat }
                val json = extractJson(raw) ?: run { last = AIResult.Error("JSON parse failed"); delay(backoff(attempt)); return@repeat }
                return AIResult.Success(parser(json))
            } catch (e: Exception) { last = handleEx(e); delay(if (last is AIResult.RateLimited) backoff(attempt, 5000) else backoff(attempt)) }
        }
        return last
    }

    private fun extractJson(raw: String): JSONObject? =
        runCatching { JSONObject(raw.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()) }.getOrNull()

    private fun backoff(attempt: Int, base: Long = 1000L) = base * (1L shl attempt)

    private fun <T> handleEx(e: Exception): AIResult<T> = when {
        (e.message ?: "").contains("429") || (e.message ?: "").contains("quota",true) -> AIResult.RateLimited
        (e.message ?: "").contains("timeout",true) -> AIResult.Error("Timeout", true)
        else -> AIResult.Error(e.message ?: "Unknown", false)
    }
}

object GeminiModelFactory {
    fun create(apiKey: String): GenerativeModel = GenerativeModel(
        modelName = "gemini-2.0-flash",
        apiKey    = apiKey,
        generationConfig = generationConfig { temperature = 0.3f; topK = 40; topP = 0.95f; maxOutputTokens = 4096 },
        safetySettings = listOf(
            SafetySetting(HarmCategory.HARASSMENT,        BlockThreshold.ONLY_HIGH),
            SafetySetting(HarmCategory.HATE_SPEECH,       BlockThreshold.ONLY_HIGH),
            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.ONLY_HIGH),
        )
    )
}

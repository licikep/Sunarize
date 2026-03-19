package com.banglaread.data.remote.parser

import androidx.core.text.HtmlCompat

class HtmlCleaner {
    private val stripPatterns = listOf(
        Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL),
        Regex("<script[^>]*>.*?</script>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)),
        Regex("<style[^>]*>.*?</style>",  setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)),
        Regex("<iframe[^>]*>.*?</iframe>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)),
        Regex("\\[ad[^]]*]", RegexOption.IGNORE_CASE),
    )
    private val imgPattern = Regex("""<img[^>]+src=["'']([^"'']+)["'']""", RegexOption.IGNORE_CASE)

    fun clean(rawInput: String): String {
        if (rawInput.isBlank()) return ""
        var cleaned = rawInput
        for (p in stripPatterns) cleaned = p.replace(cleaned, " ")
        cleaned = HtmlCompat.fromHtml(cleaned, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
        return cleaned.replace(Regex("[ \t]+"), " ").replace(Regex("\n{3,}"), "\n\n").trim()
    }

    fun extractFirstImageUrl(html: String): String? =
        imgPattern.find(html)?.groupValues?.getOrNull(1)?.takeIf { it.startsWith("http") }

    fun wordCount(text: String): Int = text.trim().split(Regex("\\s+")).size
    fun estimatedReadMinutes(text: String): Int = maxOf(1, wordCount(text) / 200)
    fun hasMinimumContent(text: String, minWords: Int = 40): Boolean =
        text.split(Regex("\\s+")).count { it.length > 2 } >= minWords
}

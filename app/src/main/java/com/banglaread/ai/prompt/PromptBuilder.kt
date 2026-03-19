package com.banglaread.ai.prompt

import com.banglaread.ai.model.BanglaTone

object PromptBuilder {
    private const val MAX_WORDS = 2000

    private fun toneInstruction(tone: BanglaTone) = when (tone) {
        BanglaTone.SIMPLE   -> "Write in SIMPLE, EVERYDAY Bangla (সহজ বাংলা). Use common words, short sentences, like explaining to a friend."
        BanglaTone.LITERARY -> "Write in LITERARY, ELEVATED Bangla (সাহিত্যিক বাংলা). Use refined vocabulary, graceful structures, like a quality editorial."
        BanglaTone.CASUAL   -> "Write in CASUAL, CONVERSATIONAL Bangla (কথ্য বাংলা). Warm, relaxed, like texting a friend about something you read."
    }

    fun buildTranslationPrompt(title: String, content: String, tone: BanglaTone) = """
You are an expert translator. Tone: ${toneInstruction(tone)}
Translate BOTH title and body. Preserve structure. Translate idioms meaningfully. Transliterate proper nouns phonetically.
ARTICLE TITLE: $title
ARTICLE BODY: ${truncate(content)}
Respond ONLY with valid JSON (no markdown):
{"detected_language":"<ISO code>","translated_title":"<Bangla title>","translated_content":"<Full Bangla content>"}
    """.trimIndent()

    fun buildSummaryPrompt(title: String, banglaContent: String, tone: BanglaTone) = """
You are a Bangla editor. Tone: ${toneInstruction(tone)}
Generate THREE summaries in Bangla:
1. SHORT: 2-3 sentences, most important idea.
2. MEDIUM: 5-7 sentence paragraph.
3. BULLET: Exactly 5 points starting with "•".
TITLE: $title
CONTENT: ${truncate(banglaContent)}
Respond ONLY with valid JSON (no markdown):
{"short":"<2-3 sentences>","medium":"<paragraph>","bullet":"• point1\n• point2\n• point3\n• point4\n• point5"}
    """.trimIndent()

    fun buildTagPrompt(title: String, content: String) = """
Generate 3-6 conceptual English tags for this article. Max 2 words each. Prefer specific over generic.
TITLE: $title
EXCERPT: ${truncate(content, 500)}
Respond ONLY with valid JSON (no markdown): {"tags":["Tag1","Tag2","Tag3"]}
    """.trimIndent()

    fun buildHighlightPrompt(banglaContent: String) = """
Extract the 3-5 most insightful sentences verbatim from this Bangla article.
ARTICLE: ${truncate(banglaContent, 1000)}
Respond ONLY with valid JSON (no markdown): {"highlights":["sentence1","sentence2","sentence3"]}
    """.trimIndent()

    fun buildConversationPrompt(title: String, summary: String, question: String, tone: BanglaTone) = """
You are a Bangla reading companion. ${toneInstruction(tone)}
ARTICLE: $title
SUMMARY: $summary
USER QUESTION: $question
Answer in Bangla only. Under 150 words unless needed.
    """.trimIndent()

    private fun truncate(text: String, maxWords: Int = MAX_WORDS): String {
        val w = text.split(Regex("\s+"))
        return if (w.size <= maxWords) text else w.take(maxWords).joinToString(" ") + "\n\n[...continues]"
    }
}

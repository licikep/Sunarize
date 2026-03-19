package com.banglaread.data.remote.parser

import android.util.Xml
import com.banglaread.data.local.entity.Article
import com.banglaread.data.local.entity.Feed
import com.banglaread.data.local.entity.TranslationStatus
import org.xmlpull.v1.XmlPullParser
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

class RSSParser(private val htmlCleaner: HtmlCleaner) {
    private val dateFormats = listOf(
        "EEE, dd MMM yyyy HH:mm:ss zzz","EEE, dd MMM yyyy HH:mm:ss Z",
        "yyyy-MM-dd'T'HH:mm:ssZ","yyyy-MM-dd'T'HH:mm:ss.SSSZ",
        "yyyy-MM-dd'T'HH:mm:ssXXX","yyyy-MM-dd HH:mm:ss"
    )

    fun parse(bytes: ByteArray, feed: Feed): List<Article> {
        val isAtom = detectIsAtom(bytes)
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(bytes.inputStream(), null)
        return if (isAtom) parseAtom(parser, feed) else parseRSS(parser, feed)
    }

    private fun detectIsAtom(bytes: ByteArray): Boolean {
        val p = Xml.newPullParser()
        p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        runCatching {
            p.setInput(bytes.inputStream(), null)
            var e = p.eventType
            while (e != XmlPullParser.END_DOCUMENT) {
                if (e == XmlPullParser.START_TAG) return p.name.lowercase() == "feed"
                e = p.next()
            }
        }
        return false
    }

    private fun parseRSS(parser: XmlPullParser, feed: Feed): List<Article> {
        val articles = mutableListOf<Article>()
        var inItem = false
        var title=""; var link=""; var desc=""; var contentEncoded=""; var pubDate=""; var author=""; var imageUrl: String?=null
        var e = parser.eventType
        while (e != XmlPullParser.END_DOCUMENT) {
            when (e) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name.lowercase().substringAfter(":")
                    when {
                        tag == "item" -> { inItem=true; title=""; link=""; desc=""; contentEncoded=""; pubDate=""; author=""; imageUrl=null }
                        !inItem -> {}
                        tag == "title"    -> title    = safeText(parser)
                        tag == "link"     -> link     = safeText(parser)
                        tag == "description" -> desc  = safeText(parser)
                        tag == "encoded"  -> contentEncoded = safeText(parser)
                        tag == "pubdate" || tag == "date" -> pubDate = safeText(parser)
                        tag == "creator" || tag == "author" -> author = safeText(parser)
                        (tag == "thumbnail" || tag == "content") && imageUrl == null ->
                            imageUrl = parser.getAttributeValue(null, "url")
                    }
                }
                XmlPullParser.END_TAG ->
                    if (parser.name.lowercase() == "item" && inItem) {
                        inItem = false
                        if (title.isNotEmpty() && link.isNotEmpty()) {
                            val raw = contentEncoded.ifEmpty { desc }
                            articles.add(buildArticle(title, link, raw, pubDate, author, imageUrl ?: htmlCleaner.extractFirstImageUrl(raw), feed))
                        }
                    }
            }
            e = parser.next()
        }
        return articles
    }

    private fun parseAtom(parser: XmlPullParser, feed: Feed): List<Article> {
        val articles = mutableListOf<Article>()
        var inEntry = false
        var title=""; var link=""; var content=""; var summary=""; var published=""; var author=""; var imageUrl: String?=null
        var e = parser.eventType
        while (e != XmlPullParser.END_DOCUMENT) {
            when (e) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name.lowercase().substringAfter(":")
                    when {
                        tag == "entry" -> { inEntry=true; title=""; link=""; content=""; summary=""; published=""; author=""; imageUrl=null }
                        !inEntry -> {}
                        tag == "title"   -> title   = safeText(parser)
                        tag == "content" -> content = safeText(parser)
                        tag == "summary" -> summary = safeText(parser)
                        tag == "name"    -> if (author.isEmpty()) author = safeText(parser)
                        tag == "published" || tag == "updated" -> if (published.isEmpty()) published = safeText(parser)
                        tag == "link" -> {
                            val rel  = parser.getAttributeValue(null, "rel")
                            val href = parser.getAttributeValue(null, "href")
                            if (href != null && (rel == "alternate" || rel == null)) link = href
                        }
                    }
                }
                XmlPullParser.END_TAG ->
                    if (parser.name.lowercase() == "entry" && inEntry) {
                        inEntry = false
                        if (title.isNotEmpty() && link.isNotEmpty()) {
                            val raw = content.ifEmpty { summary }
                            articles.add(buildArticle(title, link, raw, published, author, imageUrl ?: htmlCleaner.extractFirstImageUrl(raw), feed))
                        }
                    }
            }
            e = parser.next()
        }
        return articles
    }

    private fun buildArticle(title: String, url: String, rawBody: String, dateStr: String, author: String, imageUrl: String?, feed: Feed): Article {
        val clean = htmlCleaner.clean(rawBody)
        val wc    = htmlCleaner.wordCount(clean)
        return Article(
            id = md5(url), title = htmlCleaner.clean(title), originalContent = clean,
            sourceUrl = url, imageUrl = imageUrl, feedId = feed.id, feedTitle = feed.title,
            category = feed.category, author = author.ifEmpty { null },
            publishedAt = parseDate(dateStr), wordCount = wc,
            estimatedReadMinutes = htmlCleaner.estimatedReadMinutes(clean),
            translationStatus = if (feed.category == "Bangla News") TranslationStatus.NOT_NEEDED else TranslationStatus.PENDING
        )
    }

    private fun parseDate(s: String): Long {
        if (s.isBlank()) return System.currentTimeMillis()
        for (fmt in dateFormats) runCatching { return SimpleDateFormat(fmt, Locale.ENGLISH).parse(s.trim())!!.time }
        return System.currentTimeMillis()
    }

    private fun safeText(p: XmlPullParser): String = try { p.nextText().trim() } catch (_: Exception) { "" }
    private fun md5(input: String): String = MessageDigest.getInstance("MD5").digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
}

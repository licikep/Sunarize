package com.banglaread.data.remote.parser

import android.content.Context
import android.util.Xml
import com.banglaread.data.local.entity.Feed
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.security.MessageDigest

class OPMLParser {
    data class ParseResult(val title: String, val feeds: List<Feed>, val categories: List<String>)

    fun parse(inputStream: InputStream): ParseResult {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, "UTF-8")
        return doParse(parser)
    }

    fun parseFromAssets(context: Context, fileName: String): ParseResult =
        context.assets.open(fileName).use { parse(it) }

    private fun doParse(parser: XmlPullParser): ParseResult {
        val feeds = mutableListOf<Feed>()
        var opmlTitle = "My Feeds"
        val categoryStack = ArrayDeque<String>()
        var inHead = false
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> when (parser.name.lowercase()) {
                    "head" -> inHead = true
                    "title" -> if (inHead) runCatching { opmlTitle = parser.nextText().trim() }
                    "body"  -> inHead = false
                    "outline" -> {
                        val type   = parser.getAttributeValue(null, "type")
                        val text   = parser.getAttributeValue(null, "text")?.trim() ?: ""
                        val xmlUrl = parser.getAttributeValue(null, "xmlUrl")?.trim()
                        val htmlUrl = parser.getAttributeValue(null, "htmlUrl")?.trim()
                        val isFeed = (type?.lowercase() in listOf("rss","atom")) || xmlUrl != null
                        if (isFeed && xmlUrl != null) {
                            feeds.add(Feed(id = md5(xmlUrl), title = text.ifEmpty { xmlUrl }, xmlUrl = xmlUrl, websiteUrl = htmlUrl, category = categoryStack.lastOrNull() ?: "General"))
                        } else {
                            categoryStack.addLast(text.ifEmpty { "General" })
                        }
                    }
                }
                XmlPullParser.END_TAG -> when (parser.name.lowercase()) {
                    "head"    -> inHead = false
                    "outline" -> if (categoryStack.isNotEmpty()) categoryStack.removeLast()
                }
            }
            eventType = parser.next()
        }
        return ParseResult(opmlTitle, feeds, feeds.map { it.category }.distinct().sorted())
    }

    private fun md5(input: String): String =
        MessageDigest.getInstance("MD5").digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
}

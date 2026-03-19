package com.banglaread.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.banglaread.ai.model.BanglaTone
import com.banglaread.ai.model.SummaryType
import com.banglaread.data.local.entity.Article
import com.banglaread.data.local.entity.TranslationStatus
import com.banglaread.ui.theme.*
import com.banglaread.ui.viewmodel.ViewMode
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ArticleCard(article: Article, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val extras = MaterialTheme.extras
    val titleColor = if (article.isRead) extras.readTitleColor else extras.cream
    Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = extras.cardBackground), elevation = CardDefaults.cardElevation(0.dp)) {
        Column {
            article.imageUrl?.let { AsyncImage(model=it, contentDescription=null, contentScale=ContentScale.Crop, modifier=Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(topStart=16.dp,topEnd=16.dp))) }
            Column(Modifier.padding(horizontal=16.dp, vertical=14.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    CategoryPill(article.category); Spacer(Modifier.width(8.dp))
                    Text("${article.estimatedReadMinutes} মিনিট", style=MaterialTheme.typography.labelMedium, color=BanglaReadColors.Cream400)
                    Spacer(Modifier.weight(1f))
                    if (article.isSaved) Icon(Icons.Filled.Bookmark, null, tint=BanglaReadColors.Gold400, modifier=Modifier.size(16.dp))
                }
                Spacer(Modifier.height(10.dp))
                Text(article.title, style=MaterialTheme.typography.headlineSmall, color=titleColor, maxLines=3, overflow=TextOverflow.Ellipsis)
                article.shortSummary?.let { Spacer(Modifier.height(8.dp)); Text(it, style=MaterialTheme.typography.bodySmall, color=BanglaReadColors.Cream200, maxLines=2, overflow=TextOverflow.Ellipsis) }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text(article.feedTitle, style=MaterialTheme.typography.labelMedium, color=BanglaReadColors.Cream400, modifier=Modifier.weight(1f), maxLines=1, overflow=TextOverflow.Ellipsis)
                    Spacer(Modifier.width(8.dp))
                    Text(formatDate(article.publishedAt), style=MaterialTheme.typography.labelMedium, color=BanglaReadColors.Cream400)
                }
                if (article.translationStatus == TranslationStatus.PENDING || article.translationStatus == TranslationStatus.IN_PROGRESS) {
                    Spacer(Modifier.height(8.dp)); AIStatusChip(article.translationStatus == TranslationStatus.IN_PROGRESS)
                }
            }
        }
    }
}

@Composable private fun AIStatusChip(isProcessing: Boolean) {
    val pulse = rememberInfiniteTransition(label="pulse")
    val alpha by pulse.animateFloat(0.4f, 1f, infiniteRepeatable(tween(800, easing=EaseInOut), RepeatMode.Reverse), label="a")
    Row(verticalAlignment=Alignment.CenterVertically, modifier=Modifier.clip(RoundedCornerShape(20.dp)).background(BanglaReadColors.GoldDim).padding(horizontal=10.dp, vertical=4.dp)) {
        if (isProcessing) { Box(Modifier.size(6.dp).clip(CircleShape).background(BanglaReadColors.Gold400.copy(alpha=alpha))); Spacer(Modifier.width(6.dp)) }
        Text(if (isProcessing) "অনুবাদ হচ্ছে…" else "অনুবাদের অপেক্ষায়", style=MaterialTheme.typography.labelMedium, color=BanglaReadColors.Gold300)
    }
}

@Composable fun CategoryPill(category: String, modifier: Modifier=Modifier) {
    Box(modifier=modifier.clip(RoundedCornerShape(20.dp)).background(categoryColor(category).copy(alpha=0.2f)).padding(horizontal=8.dp, vertical=3.dp)) {
        Text(category, style=MaterialTheme.typography.labelMedium, color=categoryColor(category))
    }
}

@Composable fun CategoryChipRow(categories: List<String>, selected: String?, onSelect: (String?) -> Unit, modifier: Modifier=Modifier) {
    val all = listOf("সব") + categories.filter { it != "সব" }
    ScrollableTabRow(selectedTabIndex=all.indexOfFirst { it == (selected ?: "সব") }.coerceAtLeast(0), modifier=modifier, containerColor=Color.Transparent, edgePadding=16.dp, divider={}, indicator={}) {
        all.forEach { cat ->
            val sel = (cat == "সব" && selected == null) || cat == selected
            Tab(selected=sel, onClick={ onSelect(if (cat=="সব") null else cat) },
                text={ Text(cat, style=MaterialTheme.typography.labelLarge, color=if (sel) BanglaReadColors.Gold400 else BanglaReadColors.Cream400) },
                modifier=Modifier.padding(horizontal=4.dp).clip(RoundedCornerShape(20.dp)).background(if (sel) BanglaReadColors.GoldDim else Color.Transparent).padding(vertical=4.dp))
        }
    }
}

@Composable fun ViewModeToggle(current: ViewMode, onSelect: (ViewMode) -> Unit, modifier: Modifier=Modifier) {
    Row(modifier=modifier.clip(RoundedCornerShape(12.dp)).background(BanglaReadColors.Ink700).padding(4.dp), horizontalArrangement=Arrangement.spacedBy(2.dp)) {
        ViewMode.entries.forEach { mode ->
            val label = when(mode) { ViewMode.SUMMARY->"সারাংশ"; ViewMode.FULL->"পূর্ণ"; ViewMode.SIDE_BY_SIDE->"দুই পাশ" }
            val icon  = when(mode) { ViewMode.SUMMARY->Icons.Outlined.Summarize; ViewMode.FULL->Icons.Outlined.Article; ViewMode.SIDE_BY_SIDE->Icons.Outlined.ViewSidebar }
            val isSel = current == mode
            TextButton(onClick={onSelect(mode)}, shape=RoundedCornerShape(8.dp), colors=ButtonDefaults.textButtonColors(containerColor=if(isSel) BanglaReadColors.Ink900 else Color.Transparent, contentColor=if(isSel) BanglaReadColors.Gold400 else BanglaReadColors.Cream400), contentPadding=PaddingValues(horizontal=10.dp, vertical=6.dp)) {
                Icon(icon, label, Modifier.size(15.dp)); Spacer(Modifier.width(4.dp)); Text(label, style=MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable fun SummaryTypeSelector(current: SummaryType, onSelect: (SummaryType) -> Unit, modifier: Modifier=Modifier) {
    Row(modifier=modifier, horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        SummaryType.entries.forEach { type ->
            val label = when(type) { SummaryType.SHORT->"সংক্ষিপ্ত"; SummaryType.MEDIUM->"মাধ্যম"; SummaryType.BULLET->"বুলেট" }
            FilterChip(selected=current==type, onClick={onSelect(type)}, label={Text(label, style=MaterialTheme.typography.labelLarge)},
                colors=FilterChipDefaults.filterChipColors(selectedContainerColor=BanglaReadColors.GoldDim, selectedLabelColor=BanglaReadColors.Gold300, containerColor=BanglaReadColors.Ink700, labelColor=BanglaReadColors.Cream400),
                border=FilterChipDefaults.filterChipBorder(enabled=true, selected=current==type, selectedBorderColor=BanglaReadColors.Gold400.copy(0.4f), borderColor=BanglaReadColors.Ink600))
        }
    }
}

@Composable fun ToneSelector(current: BanglaTone, onSelect: (BanglaTone) -> Unit, modifier: Modifier=Modifier) {
    Column(modifier=modifier) {
        Text("ভাষার ধরন", style=MaterialTheme.typography.labelLarge, color=BanglaReadColors.Cream400)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            BanglaTone.entries.forEach { tone ->
                val isSel = current == tone
                Box(Modifier.clip(RoundedCornerShape(10.dp)).background(if(isSel) BanglaReadColors.GoldDim else BanglaReadColors.Ink700).border(1.dp, if(isSel) BanglaReadColors.Gold400.copy(0.5f) else Color.Transparent, RoundedCornerShape(10.dp)).clickable{onSelect(tone)}.padding(horizontal=14.dp, vertical=8.dp)) {
                    Text(tone.banglaLabel, style=MaterialTheme.typography.labelLarge, color=if(isSel) BanglaReadColors.Gold300 else BanglaReadColors.Cream400)
                }
            }
        }
    }
}

@Composable fun ReadingProgressBar(progress: Float, modifier: Modifier=Modifier) {
    Box(modifier=modifier.fillMaxWidth().height(2.dp).background(BanglaReadColors.Ink600).drawBehind {
        drawLine(Brush.horizontalGradient(listOf(BanglaReadColors.Gold400, BanglaReadColors.Gold200)), Offset.Zero, Offset(size.width*progress.coerceIn(0f,1f),0f), size.height, StrokeCap.Round)
    })
}

@Composable fun AIPipelineBanner(processed: Int, total: Int, currentTitle: String?, modifier: Modifier=Modifier) {
    if (total == 0) return
    val pulse = rememberInfiniteTransition(label="ai")
    val scale by pulse.animateFloat(1f, 1.15f, infiniteRepeatable(tween(600, easing=EaseInOut), RepeatMode.Reverse), label="s")
    Surface(modifier=modifier.fillMaxWidth(), color=BanglaReadColors.Ink700, tonalElevation=4.dp) {
        Column(Modifier.padding(horizontal=16.dp, vertical=10.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, null, tint=BanglaReadColors.Gold400, modifier=Modifier.size((16*scale).dp))
                Spacer(Modifier.width(8.dp))
                Text("AI অনুবাদ চলছে — $processed/$total", style=MaterialTheme.typography.labelLarge, color=BanglaReadColors.Cream200)
            }
            currentTitle?.let { Text(it, style=MaterialTheme.typography.labelMedium, color=BanglaReadColors.Cream400, maxLines=1, overflow=TextOverflow.Ellipsis, modifier=Modifier.padding(start=24.dp,top=2.dp)) }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(progress={processed.toFloat()/total}, modifier=Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)), color=BanglaReadColors.Gold400, trackColor=BanglaReadColors.Ink600)
        }
    }
}

@Composable fun HighlightedSentence(text: String, modifier: Modifier=Modifier) {
    Row(modifier=modifier.fillMaxWidth()) {
        Box(Modifier.width(3.dp).height(IntrinsicSize.Max).background(Brush.verticalGradient(listOf(BanglaReadColors.Gold400,BanglaReadColors.Gold200)), RoundedCornerShape(4.dp)))
        Spacer(Modifier.width(12.dp))
        Text(text, style=MaterialTheme.typography.bodyMedium.copy(fontSize=15.sp), color=BanglaReadColors.Cream200)
    }
}

@Composable fun ArticleCardSkeleton(modifier: Modifier=Modifier) {
    val shimmer = rememberInfiniteTransition(label="shimmer")
    val offset by shimmer.animateFloat(-1f, 2f, infiniteRepeatable(tween(1200, easing=LinearEasing)), label="o")
    fun brush() = Brush.linearGradient(listOf(BanglaReadColors.Ink700,BanglaReadColors.Ink600,BanglaReadColors.Ink700), Offset(offset*400,0f), Offset(offset*400+400,0f))
    Card(modifier=modifier.fillMaxWidth(), shape=RoundedCornerShape(16.dp), colors=CardDefaults.cardColors(containerColor=BanglaReadColors.Ink800)) {
        Column(Modifier.padding(16.dp)) {
            Box(Modifier.fillMaxWidth(0.3f).height(18.dp).clip(RoundedCornerShape(4.dp)).background(brush()))
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(22.dp).clip(RoundedCornerShape(4.dp)).background(brush()))
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth(0.8f).height(22.dp).clip(RoundedCornerShape(4.dp)).background(brush()))
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(4.dp)).background(brush()))
        }
    }
}

@Composable fun EmptyFeedState(onRefresh: () -> Unit, modifier: Modifier=Modifier) {
    Column(modifier=modifier.fillMaxSize(), horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.Center) {
        Text("📰", fontSize=56.sp); Spacer(Modifier.height(16.dp))
        Text("কোনো আর্টিকেল নেই", style=MaterialTheme.typography.headlineSmall, color=BanglaReadColors.Cream100); Spacer(Modifier.height(8.dp))
        Text("রিফ্রেশ করুন বা নতুন ফিড যোগ করুন", style=MaterialTheme.typography.bodyMedium, color=BanglaReadColors.Cream400); Spacer(Modifier.height(24.dp))
        Button(onClick=onRefresh, colors=ButtonDefaults.buttonColors(containerColor=BanglaReadColors.Gold400)) {
            Icon(Icons.Filled.Refresh, null, tint=BanglaReadColors.Ink900); Spacer(Modifier.width(8.dp))
            Text("রিফ্রেশ", color=BanglaReadColors.Ink900, style=MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable fun categoryColor(cat: String) = when (cat.lowercase()) {
    "philosophy","দর্শন"         -> BanglaReadColors.ChipPhilosophy
    "motivation","অনুপ্রেরণা"    -> BanglaReadColors.ChipMotivation
    "literature","সাহিত্য"       -> BanglaReadColors.ChipLiterature
    "psychology","মনোবিজ্ঞান"    -> BanglaReadColors.ChipPsychology
    "bangla news","বাংলা সংবাদ"  -> BanglaReadColors.ChipBangla
    else -> BanglaReadColors.ChipDefault
}

private fun formatDate(ms: Long): String {
    val d = System.currentTimeMillis() - ms
    return when { d < 3_600_000 -> "${(d/60_000).coerceAtLeast(1)} মিনিট আগে"; d < 86_400_000 -> "${d/3_600_000} ঘণ্টা আগে"; d < 604_800_000 -> "${d/86_400_000} দিন আগে"; else -> SimpleDateFormat("d MMM", Locale("bn")).format(Date(ms)) }
}

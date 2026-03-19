package com.banglaread.ui.screens.reader

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.banglaread.ai.model.SummaryType
import com.banglaread.data.local.entity.TranslationStatus
import com.banglaread.ui.components.*
import com.banglaread.ui.theme.BanglaReadColors
import com.banglaread.ui.theme.Spacing
import com.banglaread.ui.viewmodel.ReaderUiState
import com.banglaread.ui.viewmodel.ReaderViewModel
import com.banglaread.ui.viewmodel.ViewMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(articleId: String, onBack: () -> Unit, viewModel: ReaderViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current
    val readingProgress by remember { derivedStateOf { if (scrollState.maxValue == 0) 0f else scrollState.value.toFloat()/scrollState.maxValue } }

    LaunchedEffect(articleId) { viewModel.loadArticle(articleId) }
    val article = state.article

    Scaffold(containerColor=BanglaReadColors.Ink900, topBar={
        Column {
            ReadingProgressBar(readingProgress)
            TopAppBar(title={}, navigationIcon={ IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back",tint=BanglaReadColors.Cream200)} },
                actions={
                    IconButton(onClick=viewModel::toggleSaved){ Icon(if(article?.isSaved==true) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, null, tint=if(article?.isSaved==true) BanglaReadColors.Gold400 else BanglaReadColors.Cream200) }
                    IconButton(onClick={ article?.sourceUrl?.let { uriHandler.openUri(it) } }){ Icon(Icons.Outlined.OpenInBrowser,"Web",tint=BanglaReadColors.Cream200) }
                },
                colors=TopAppBarDefaults.topAppBarColors(containerColor=BanglaReadColors.Ink900))
        }
    }) { padding ->
        if (article == null) { Box(Modifier.fillMaxSize(), contentAlignment=Alignment.Center){ CircularProgressIndicator(color=BanglaReadColors.Gold400) }; return@Scaffold }
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(scrollState)) {
            article.imageUrl?.let { AsyncImage(model=it, contentDescription=null, contentScale=ContentScale.Crop, modifier=Modifier.fillMaxWidth().height(220.dp)) }
            Column(Modifier.padding(horizontal=Spacing.md)) {
                Spacer(Modifier.height(Spacing.lg))
                Row(verticalAlignment=Alignment.CenterVertically) {
                    CategoryPill(article.category); Spacer(Modifier.width(8.dp))
                    Text(article.feedTitle, style=MaterialTheme.typography.labelMedium, color=BanglaReadColors.Cream400)
                    Spacer(Modifier.weight(1f))
                    Text("${article.estimatedReadMinutes} মিনিট", style=MaterialTheme.typography.labelMedium, color=BanglaReadColors.Cream400)
                }
                Spacer(Modifier.height(Spacing.sm))
                Text(article.title, style=MaterialTheme.typography.titleLarge, color=BanglaReadColors.Cream100)
                article.author?.let { Spacer(Modifier.height(6.dp)); Text(it, style=MaterialTheme.typography.bodySmall, color=BanglaReadColors.Cream400) }
                Spacer(Modifier.height(Spacing.lg))
                if (article.translationStatus == TranslationStatus.PENDING || article.translationStatus == TranslationStatus.IN_PROGRESS) {
                    val isProc = article.translationStatus == TranslationStatus.IN_PROGRESS
                    val pulse = rememberInfiniteTransition(label="p"); val alpha by pulse.animateFloat(0.5f, 1f, infiniteRepeatable(tween(800, easing=EaseInOut), RepeatMode.Reverse), label="a")
                    Surface(color=BanglaReadColors.GoldDim, shape=RoundedCornerShape(12.dp), modifier=Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(Spacing.sm), verticalAlignment=Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Translate, null, tint=BanglaReadColors.Gold400.copy(alpha=if(isProc) alpha else 1f), modifier=Modifier.size(20.dp)); Spacer(Modifier.width(10.dp))
                            Text(if(isProc) "বাংলায় অনুবাদ হচ্ছে…" else "অনুবাদের অপেক্ষায় রয়েছে", style=MaterialTheme.typography.bodySmall, color=BanglaReadColors.Gold200)
                        }
                    }
                    Spacer(Modifier.height(Spacing.md))
                }
                ViewModeToggle(state.viewMode, viewModel::setViewMode, Modifier.fillMaxWidth())
                Spacer(Modifier.height(Spacing.sm))
                AnimatedVisibility(state.viewMode == ViewMode.SUMMARY) { SummaryTypeSelector(state.summaryType, viewModel::setSummaryType, Modifier.padding(bottom=Spacing.sm)) }
                Spacer(Modifier.height(Spacing.sm))
                Crossfade(state.viewMode, label="vm") { mode ->
                    when (mode) {
                        ViewMode.SUMMARY -> {
                            val txt = when(state.summaryType){ SummaryType.SHORT->article.shortSummary; SummaryType.MEDIUM->article.mediumSummary; SummaryType.BULLET->article.bulletSummary }
                            if (txt != null) {
                                Surface(color=BanglaReadColors.Ink800, shape=RoundedCornerShape(12.dp)) {
                                    Column(Modifier.padding(Spacing.md)) {
                                        Row(verticalAlignment=Alignment.CenterVertically){ Icon(Icons.Outlined.AutoAwesome,null,tint=BanglaReadColors.Gold400,modifier=Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("AI সারাংশ",style=MaterialTheme.typography.labelLarge,color=BanglaReadColors.Gold300) }
                                        Spacer(Modifier.height(Spacing.sm))
                                        if (state.summaryType == SummaryType.BULLET) {
                                            Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                                                txt.split("\n").filter{it.startsWith("•")}.forEach { pt ->
                                                    Row(verticalAlignment=Alignment.Top) { Text("•",color=BanglaReadColors.Gold400,style=MaterialTheme.typography.bodyLarge,modifier=Modifier.padding(top=2.dp)); Spacer(Modifier.width(10.dp)); Text(pt.removePrefix("•").trim(),style=MaterialTheme.typography.bodyLarge,color=BanglaReadColors.Cream100) }
                                                }
                                            }
                                        } else Text(txt, style=MaterialTheme.typography.bodyLarge, color=BanglaReadColors.Cream100)
                                    }
                                }
                            } else Text(article.originalContent.take(400)+"…", style=MaterialTheme.typography.bodyLarge, color=BanglaReadColors.Cream200)
                        }
                        ViewMode.FULL -> Text(article.banglaContent ?: article.originalContent, style=MaterialTheme.typography.bodyLarge, color=BanglaReadColors.Cream100)
                        ViewMode.SIDE_BY_SIDE -> Column(verticalArrangement=Arrangement.spacedBy(Spacing.md)) {
                            article.banglaContent?.let { Column{ Text("বাংলা",style=MaterialTheme.typography.labelLarge,color=BanglaReadColors.Gold400); Spacer(Modifier.height(6.dp)); Text(it,style=MaterialTheme.typography.bodyMedium,color=BanglaReadColors.Cream200) } }
                            HorizontalDivider(color=BanglaReadColors.Ink600)
                            Column{ Text("মূল",style=MaterialTheme.typography.labelLarge,color=BanglaReadColors.Cream400); Spacer(Modifier.height(6.dp)); Text(article.originalContent,style=MaterialTheme.typography.bodyMedium,color=BanglaReadColors.Cream200) }
                        }
                    }
                }
                if (article.banglaContent != null) {
                    Spacer(Modifier.height(Spacing.xl))
                    Row(verticalAlignment=Alignment.CenterVertically, modifier=Modifier.fillMaxWidth()) {
                        Text("মূল বাক্যগুলো", style=MaterialTheme.typography.titleMedium, color=BanglaReadColors.Cream100)
                        Spacer(Modifier.weight(1f))
                        if (state.highlights.isEmpty() && !state.isLoadingHighlights) TextButton(onClick=viewModel::loadHighlights){ Text("দেখুন",color=BanglaReadColors.Gold400) }
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    if (state.isLoadingHighlights) LinearProgressIndicator(modifier=Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)), color=BanglaReadColors.Gold400, trackColor=BanglaReadColors.Ink600)
                    else if (state.highlights.isNotEmpty()) Column(verticalArrangement=Arrangement.spacedBy(Spacing.md)) { state.highlights.forEach { HighlightedSentence(it) } }
                }
                Spacer(Modifier.height(Spacing.xl))
                ToneSelector(state.tone, viewModel::setTone)
                if (article.banglaContent != null) {
                    Spacer(Modifier.height(Spacing.xl))
                    var question by remember { mutableStateOf("") }
                    Row(verticalAlignment=Alignment.CenterVertically){ Icon(Icons.Outlined.Psychology,null,tint=BanglaReadColors.Gold400,modifier=Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("এই আর্টিকেল সম্পর্কে জিজ্ঞাসা করুন",style=MaterialTheme.typography.titleMedium,color=BanglaReadColors.Cream100) }
                    Spacer(Modifier.height(Spacing.sm))
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        listOf("সহজ করে বলুন","উদাহরণ দিন","সারাংশ করুন").forEach { s ->
                            AssistChip(onClick={question=s;viewModel.askQuestion(s)}, label={Text(s,style=MaterialTheme.typography.labelMedium)}, colors=AssistChipDefaults.assistChipColors(containerColor=BanglaReadColors.Ink700,labelColor=BanglaReadColors.Cream200), border=AssistChipDefaults.assistChipBorder(enabled=true,borderColor=BanglaReadColors.Ink600))
                        }
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
                        OutlinedTextField(value=question, onValueChange={question=it}, placeholder={Text("বাংলায় প্রশ্ন করুন…",color=BanglaReadColors.Cream400)}, modifier=Modifier.weight(1f), shape=RoundedCornerShape(12.dp), singleLine=true,
                            keyboardOptions=KeyboardOptions(imeAction=ImeAction.Send), keyboardActions=KeyboardActions(onSend={if(question.isNotBlank())viewModel.askQuestion(question)}),
                            colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=BanglaReadColors.Gold400,unfocusedBorderColor=BanglaReadColors.Ink600,focusedTextColor=BanglaReadColors.Cream100,unfocusedTextColor=BanglaReadColors.Cream100,cursorColor=BanglaReadColors.Gold400,focusedContainerColor=BanglaReadColors.Ink800,unfocusedContainerColor=BanglaReadColors.Ink800))
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick={if(question.isNotBlank())viewModel.askQuestion(question)}, enabled=question.isNotBlank()&&!state.isChatLoading, modifier=Modifier.clip(RoundedCornerShape(12.dp)).background(BanglaReadColors.Gold400).size(48.dp)) {
                            if (state.isChatLoading) CircularProgressIndicator(color=BanglaReadColors.Ink900,modifier=Modifier.size(18.dp),strokeWidth=2.dp)
                            else Icon(Icons.Filled.Send,null,tint=BanglaReadColors.Ink900)
                        }
                    }
                    AnimatedVisibility(state.chatResponse != null) {
                        state.chatResponse?.let { resp ->
                            Spacer(Modifier.height(Spacing.sm))
                            Surface(color=BanglaReadColors.Ink800, shape=RoundedCornerShape(12.dp)) {
                                Column(Modifier.padding(Spacing.md)) {
                                    Row(verticalAlignment=Alignment.CenterVertically){ Icon(Icons.Filled.AutoAwesome,null,tint=BanglaReadColors.Gold400,modifier=Modifier.size(14.dp)); Spacer(Modifier.width(6.dp)); Text("AI উত্তর",style=MaterialTheme.typography.labelLarge,color=BanglaReadColors.Gold300) }
                                    Spacer(Modifier.height(8.dp)); Text(resp,style=MaterialTheme.typography.bodyMedium,color=BanglaReadColors.Cream100)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.xxl))
            }
        }
    }
}

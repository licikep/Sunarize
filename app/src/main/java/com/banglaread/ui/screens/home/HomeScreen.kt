package com.banglaread.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banglaread.ai.pipeline.PipelineProgress
import com.banglaread.ui.components.*
import com.banglaread.ui.theme.BanglaReadColors
import com.banglaread.ui.theme.Spacing
import com.banglaread.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onArticleClick: (String)->Unit, onSettingsClick: ()->Unit, onSearchClick: ()->Unit, aiProgress: PipelineProgress, viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val isScrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 20 } }

    Scaffold(containerColor=BanglaReadColors.Ink900, topBar={
        TopAppBar(
            title={
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("বাংলারিড", style=MaterialTheme.typography.displayLarge.copy(fontSize=26.sp), color=BanglaReadColors.Gold400)
                    if (uiState.unreadCount > 0) { Spacer(Modifier.width(10.dp))
                        Box(Modifier.background(BanglaReadColors.Gold400, androidx.compose.foundation.shape.RoundedCornerShape(10.dp)).padding(horizontal=6.dp, vertical=2.dp)) {
                            Text(if(uiState.unreadCount>99)"99+" else "${uiState.unreadCount}", style=MaterialTheme.typography.labelMedium, color=BanglaReadColors.Ink900) }
                    }
                }
            },
            actions={ IconButton(onClick=onSearchClick){Icon(Icons.Outlined.Search,"Search",tint=BanglaReadColors.Cream200)}; IconButton(onClick=onSettingsClick){Icon(Icons.Outlined.Settings,"Settings",tint=BanglaReadColors.Cream200)} },
            colors=TopAppBarDefaults.topAppBarColors(containerColor=if(isScrolled) BanglaReadColors.Ink800 else BanglaReadColors.Ink900)
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            AnimatedVisibility(!aiProgress.isIdle, enter=slideInVertically()+fadeIn(), exit=slideOutVertically()+fadeOut()) {
                AIPipelineBanner(aiProgress.processed, aiProgress.total, aiProgress.current)
            }
            AnimatedVisibility(uiState.syncMessage != null, enter=slideInVertically()+fadeIn(), exit=slideOutVertically()+fadeOut()) {
                uiState.syncMessage?.let { msg ->
                    LaunchedEffect(msg) { delay(3000); viewModel.dismissSyncMessage() }
                    Box(Modifier.fillMaxWidth().background(BanglaReadColors.Ink700).padding(horizontal=16.dp, vertical=10.dp)) {
                        Text(msg, style=MaterialTheme.typography.bodySmall, color=BanglaReadColors.Cream200)
                    }
                }
            }
            if (uiState.categories.isNotEmpty()) CategoryChipRow(uiState.categories, uiState.selectedCategory, viewModel::selectCategory, Modifier.padding(vertical=8.dp))
            PullToRefreshBox(isRefreshing=uiState.isRefreshing, onRefresh=viewModel::refresh, modifier=Modifier.fillMaxSize()) {
                when {
                    uiState.isRefreshing && uiState.articles.isEmpty() ->
                        LazyColumn(contentPadding=PaddingValues(Spacing.md), verticalArrangement=Arrangement.spacedBy(Spacing.sm)) { items(5) { ArticleCardSkeleton() } }
                    uiState.articles.isEmpty() -> EmptyFeedState(onRefresh=viewModel::refresh)
                    else -> LazyColumn(state=listState, contentPadding=PaddingValues(start=Spacing.md,end=Spacing.md,top=Spacing.sm,bottom=Spacing.xxl), verticalArrangement=Arrangement.spacedBy(Spacing.sm)) {
                        val highlights = uiState.articles.filter { !it.isRead }.take(3)
                        if (highlights.isNotEmpty()) {
                            item { SectionHeader("আজকের সেরা পড়া") }
                            items(highlights, key={"h_${it.id}"}) { ArticleCard(it) { onArticleClick(it.id) } }
                            item { SectionHeader("সব আর্টিকেল", Modifier.padding(top=Spacing.xs)) }
                        }
                        items(if (highlights.isNotEmpty()) uiState.articles.drop(3) else uiState.articles, key={it.id}) { ArticleCard(it) { onArticleClick(it.id) } }
                    }
                }
            }
        }
    }
}

@Composable private fun SectionHeader(title: String, modifier: Modifier=Modifier) {
    Row(modifier=modifier.fillMaxWidth().padding(vertical=8.dp), verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.width(3.dp).height(18.dp).background(Brush.verticalGradient(listOf(BanglaReadColors.Gold400,BanglaReadColors.Gold200)), androidx.compose.foundation.shape.RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(10.dp))
        Text(title, style=MaterialTheme.typography.titleMedium, color=BanglaReadColors.Cream200)
    }
}

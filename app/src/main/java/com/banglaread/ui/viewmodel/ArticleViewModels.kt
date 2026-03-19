package com.banglaread.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banglaread.ai.model.BanglaTone
import com.banglaread.ai.model.SummaryType
import com.banglaread.ai.pipeline.AIProcessingPipeline
import com.banglaread.ai.pipeline.PipelineProgress
import com.banglaread.data.local.UserPreferencesRepository
import com.banglaread.data.local.entity.Article
import com.banglaread.repository.FeedRepository
import com.banglaread.repository.SyncResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(val articles: List<Article>=emptyList(), val categories: List<String>=emptyList(), val selectedCategory: String?=null, val isRefreshing: Boolean=false, val syncMessage: String?=null, val unreadCount: Int=0)
data class ReaderUiState(val article: Article?=null, val viewMode: ViewMode=ViewMode.SUMMARY, val summaryType: SummaryType=SummaryType.SHORT, val tone: BanglaTone=BanglaTone.SIMPLE, val highlights: List<String>=emptyList(), val isLoadingHighlights: Boolean=false, val chatResponse: String?=null, val isChatLoading: Boolean=false)
enum class ViewMode { SUMMARY, FULL, SIDE_BY_SIDE }

@HiltViewModel
class HomeViewModel @Inject constructor(private val feedRepo: FeedRepository, private val prefs: UserPreferencesRepository) : ViewModel() {
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _isRefreshing     = MutableStateFlow(false)
    private val _syncMessage      = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        _selectedCategory.flatMapLatest { cat -> if (cat == null) feedRepo.getAllArticles() else feedRepo.getArticlesByCategory(cat) },
        feedRepo.getArticleCategories(), _selectedCategory, _isRefreshing, _syncMessage, feedRepo.getUnreadCount()
    ) { articles, cats, sel, refresh, msg, unread ->
        HomeUiState(articles, listOf("সব") + cats, sel, refresh, msg, unread)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun selectCategory(cat: String?) { _selectedCategory.value = if (cat == "সব") null else cat }
    fun dismissSyncMessage() { _syncMessage.value = null }

    fun refresh() { viewModelScope.launch {
        _isRefreshing.value = true
        _syncMessage.value = when (val r = feedRepo.syncAllFeeds()) {
            is SyncResult.Success        -> "${r.newArticleCount} নতুন আর্টিকেল পাওয়া গেছে"
            is SyncResult.PartialSuccess -> "${r.newArticleCount} নতুন আর্টিকেল (কিছু সমস্যা হয়েছে)"
            is SyncResult.Failure        -> "সিঙ্ক করতে সমস্যা হয়েছে"
        }
        _isRefreshing.value = false
    }}
}

@HiltViewModel
class ReaderViewModel @Inject constructor(private val feedRepo: FeedRepository, private val pipeline: AIProcessingPipeline, private val prefs: UserPreferencesRepository) : ViewModel() {
    private val _state = MutableStateFlow(ReaderUiState())
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()
    val aiProgress: Flow<PipelineProgress> = pipeline.progress

    fun loadArticle(articleId: String) { viewModelScope.launch {
        val article = feedRepo.getArticleById(articleId) ?: return@launch
        _state.value = _state.value.copy(article = article, tone = prefs.getBanglaTone())
        feedRepo.markArticleRead(articleId)
    }}

    fun setViewMode(m: ViewMode)      { _state.value = _state.value.copy(viewMode = m) }
    fun setSummaryType(t: SummaryType){ _state.value = _state.value.copy(summaryType = t) }

    fun setTone(tone: BanglaTone) { viewModelScope.launch {
        _state.value = _state.value.copy(tone = tone)
        prefs.setBanglaTone(tone)
        _state.value.article?.id?.let { pipeline.resuminWithTone(it, tone); loadArticle(it) }
    }}

    fun loadHighlights() { val id = _state.value.article?.id ?: return; if (_state.value.highlights.isNotEmpty()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingHighlights = true)
            _state.value = _state.value.copy(highlights = pipeline.getHighlights(id) ?: emptyList(), isLoadingHighlights = false)
        }
    }

    fun askQuestion(q: String) { val id = _state.value.article?.id ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isChatLoading = true, chatResponse = null)
            _state.value = _state.value.copy(chatResponse = pipeline.askAboutArticle(id, q, _state.value.tone) ?: "উত্তর দিতে সমস্যা হয়েছে।", isChatLoading = false)
        }
    }

    fun toggleSaved() { val a = _state.value.article ?: return
        viewModelScope.launch { val s = !a.isSaved; feedRepo.toggleSaved(a.id, s); _state.value = _state.value.copy(article = a.copy(isSaved = s)) }
    }
}

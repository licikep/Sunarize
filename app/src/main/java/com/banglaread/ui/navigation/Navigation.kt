package com.banglaread.ui.navigation

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.banglaread.ai.model.BanglaTone
import com.banglaread.ai.pipeline.AIProcessingPipeline
import com.banglaread.ai.pipeline.PipelineProgress
import com.banglaread.data.local.UserPreferencesRepository
import com.banglaread.repository.FeedRepository
import com.banglaread.ui.screens.home.HomeScreen
import com.banglaread.ui.screens.reader.ReaderScreen
import com.banglaread.ui.screens.settings.OnboardingScreen
import com.banglaread.ui.screens.settings.SettingsScreen
import com.banglaread.worker.FeedSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class Route(val path: String) {
    object Onboarding : Route("onboarding")
    object Home       : Route("home")
    object Reader     : Route("reader/{articleId}") { fun go(id: String) = "reader/$id" }
    object Settings   : Route("settings")
}

@Composable
fun BanglaReadNavHost(startDestination: String, aiProgress: PipelineProgress, modifier: Modifier = Modifier) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = startDestination, modifier = modifier) {
        composable(Route.Onboarding.path) {
            OnboardingScreen { nav.navigate(Route.Home.path) { popUpTo(Route.Onboarding.path) { inclusive = true } } }
        }
        composable(Route.Home.path) {
            HomeScreen(onArticleClick = { nav.navigate(Route.Reader.go(it)) }, onSettingsClick = { nav.navigate(Route.Settings.path) }, onSearchClick = {}, aiProgress = aiProgress)
        }
        composable(Route.Reader.path) { back ->
            ReaderScreen(articleId = back.arguments?.getString("articleId") ?: return@composable, onBack = { nav.popBackStack() })
        }
        composable(Route.Settings.path) {
            SettingsScreen(onBack = { nav.popBackStack() }, onManageFeeds = {})
        }
    }
}

data class SettingsUiState(val geminiApiKey: String="", val hasApiKey: Boolean=false, val banglaTone: BanglaTone=BanglaTone.SIMPLE, val backgroundSyncEnabled: Boolean=true, val defaultView: String="summary", val ttsEnabled: Boolean=true, val fontScale: Float=1.0f, val activeFeedCount: Int=0)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: UserPreferencesRepository,
    private val feedRepo: FeedRepository
) : ViewModel() {
    val state: StateFlow<SettingsUiState> = combine(
        prefs.geminiApiKey, prefs.hasApiKey, prefs.banglaToneFlow,
        prefs.backgroundSyncEnabled, prefs.defaultView, prefs.ttsEnabled,
        prefs.fontScale, feedRepo.getActiveFeedCount()
    ) { v -> SettingsUiState(v[0] as? String ?: "", v[1] as Boolean, v[2] as BanglaTone, v[3] as Boolean, v[4] as? String ?: "summary", v[5] as Boolean, v[6] as Float, v[7] as Int) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun saveApiKey(k: String)           = viewModelScope.launch { prefs.setGeminiApiKey(k) }
    fun setTone(t: BanglaTone)          = viewModelScope.launch { prefs.setBanglaTone(t) }
    fun setDefaultView(v: String)       = viewModelScope.launch { prefs.setDefaultView(v) }
    fun setTtsEnabled(v: Boolean)       = viewModelScope.launch { prefs.setTtsEnabled(v) }
    fun setFontScale(v: Float)          = viewModelScope.launch { prefs.setFontScale(v) }
    fun pruneOldArticles()              = viewModelScope.launch { feedRepo.pruneOldArticles() }
    fun setBackgroundSync(v: Boolean)   = viewModelScope.launch { prefs.setBackgroundSyncEnabled(v); if (v) FeedSyncWorker.schedulePeriodic(context) else FeedSyncWorker.cancelPeriodic(context) }
    fun importOpml(context: Context, uri: Uri) = viewModelScope.launch { context.contentResolver.openInputStream(uri)?.use { feedRepo.importOPML(it) } }
    fun triggerOPMLImport() { _opmlPickerEvent.value = true }
    fun onOpmlPickerHandled() { _opmlPickerEvent.value = false }
    private val _opmlPickerEvent = MutableStateFlow(false)
    val opmlPickerEvent: StateFlow<Boolean> = _opmlPickerEvent.asStateFlow()
}

@HiltViewModel
class AppViewModel @Inject constructor(private val prefs: UserPreferencesRepository, private val feedRepo: FeedRepository, private val pipeline: AIProcessingPipeline) : ViewModel() {
    val startDestination: Flow<String> = prefs.onboardingComplete.map { if (it) Route.Home.path else Route.Onboarding.path }
    val aiProgress: StateFlow<PipelineProgress> = pipeline.progress.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PipelineProgress())
    init { viewModelScope.launch { feedRepo.loadDefaultFeedsIfEmpty() } }
}

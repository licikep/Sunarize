package com.banglaread.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.banglaread.ai.model.BanglaTone
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private object Keys {
        val GEMINI_API_KEY          = stringPreferencesKey("gemini_api_key")
        val BANGLA_TONE             = stringPreferencesKey("bangla_tone")
        val BACKGROUND_SYNC_ENABLED = booleanPreferencesKey("background_sync_enabled")
        val ONBOARDING_COMPLETE     = booleanPreferencesKey("onboarding_complete")
        val DARK_MODE               = booleanPreferencesKey("dark_mode")
        val FONT_SCALE              = floatPreferencesKey("font_scale")
        val DEFAULT_VIEW            = stringPreferencesKey("default_view")
        val TTS_ENABLED             = booleanPreferencesKey("tts_enabled")
        val PRUNE_AFTER_DAYS        = intPreferencesKey("prune_after_days")
    }
    val geminiApiKey: Flow<String?>      = context.dataStore.data.map { it[Keys.GEMINI_API_KEY] }
    val hasApiKey: Flow<Boolean>         = context.dataStore.data.map { !it[Keys.GEMINI_API_KEY].isNullOrBlank() }
    val banglaToneFlow: Flow<BanglaTone> = context.dataStore.data.map { BanglaTone.valueOf(it[Keys.BANGLA_TONE] ?: BanglaTone.SIMPLE.name) }
    val backgroundSyncEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.BACKGROUND_SYNC_ENABLED] ?: true }
    val darkMode: Flow<Boolean>          = context.dataStore.data.map { it[Keys.DARK_MODE] ?: false }
    val fontScale: Flow<Float>           = context.dataStore.data.map { it[Keys.FONT_SCALE] ?: 1.0f }
    val defaultView: Flow<String>        = context.dataStore.data.map { it[Keys.DEFAULT_VIEW] ?: "summary" }
    val ttsEnabled: Flow<Boolean>        = context.dataStore.data.map { it[Keys.TTS_ENABLED] ?: true }
    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_COMPLETE] ?: false }
    val pruneAfterDays: Flow<Int>        = context.dataStore.data.map { it[Keys.PRUNE_AFTER_DAYS] ?: 30 }

    suspend fun getGeminiApiKey(): String? = context.dataStore.data.first()[Keys.GEMINI_API_KEY]
    suspend fun setGeminiApiKey(key: String) = context.dataStore.edit { it[Keys.GEMINI_API_KEY] = key.trim() }
    suspend fun getBanglaTone(): BanglaTone = runCatching { BanglaTone.valueOf(context.dataStore.data.first()[Keys.BANGLA_TONE] ?: "") }.getOrDefault(BanglaTone.SIMPLE)
    suspend fun setBanglaTone(tone: BanglaTone) = context.dataStore.edit { it[Keys.BANGLA_TONE] = tone.name }
    suspend fun setBackgroundSyncEnabled(v: Boolean) = context.dataStore.edit { it[Keys.BACKGROUND_SYNC_ENABLED] = v }
    suspend fun setFontScale(v: Float) = context.dataStore.edit { it[Keys.FONT_SCALE] = v.coerceIn(0.8f, 1.6f) }
    suspend fun setDefaultView(v: String) = context.dataStore.edit { it[Keys.DEFAULT_VIEW] = v }
    suspend fun setTtsEnabled(v: Boolean) = context.dataStore.edit { it[Keys.TTS_ENABLED] = v }
    suspend fun setOnboardingComplete() = context.dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = true }
}

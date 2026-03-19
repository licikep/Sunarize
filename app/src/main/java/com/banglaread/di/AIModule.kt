package com.banglaread.di

import com.banglaread.ai.pipeline.GeminiClient
import com.banglaread.ai.pipeline.GeminiModelFactory
import com.banglaread.data.local.UserPreferencesRepository
import com.google.ai.client.generativeai.GenerativeModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AIModule {
    @Provides @Singleton
    fun provideGenerativeModel(prefs: UserPreferencesRepository): GenerativeModel =
        GeminiModelFactory.create(runBlocking { prefs.getGeminiApiKey() ?: "" })

    @Provides @Singleton
    fun provideGeminiClient(model: GenerativeModel): GeminiClient = GeminiClient(model)
}

package com.banglaread

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.banglaread.ui.navigation.AppViewModel
import com.banglaread.ui.navigation.BanglaReadNavHost
import com.banglaread.ui.theme.BanglaReadColors
import com.banglaread.ui.theme.BanglaReadTheme
import com.banglaread.worker.FeedSyncWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        var isReady = false
        splashScreen.setKeepOnScreenCondition { !isReady }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FeedSyncWorker.schedulePeriodic(this)
        val startDestination = runBlocking {
            appViewModel.startDestination.first()
        }.also { isReady = true }
        setContent {
            BanglaReadTheme {
                val aiProgress by appViewModel.aiProgress.collectAsStateWithLifecycle()
                BanglaReadNavHost(
                    startDestination = startDestination,
                    aiProgress       = aiProgress,
                    modifier         = Modifier.fillMaxSize().background(BanglaReadColors.Ink900)
                )
            }
        }
    }
}

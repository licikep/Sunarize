package com.banglaread.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.banglaread.ai.pipeline.AIProcessingPipeline
import com.banglaread.data.local.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class AIProcessingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val pipeline: AIProcessingPipeline,
    private val prefs: UserPreferencesRepository
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        pipeline.processPendingArticles(tone = prefs.getBanglaTone(), limit = 10)
        return Result.success()
    }
    companion object {
        const val WORK_NAME = "banglaread_ai_processing"
        private val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        fun scheduleOneTime(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<AIProcessingWorker>().setConstraints(constraints).build())
        }
    }
}

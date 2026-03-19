package com.banglaread.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.banglaread.repository.FeedRepository
import com.banglaread.repository.SyncResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class FeedSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val feedRepository: FeedRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = when (val r = feedRepository.syncAllFeeds()) {
        is SyncResult.Success        -> Result.success(workDataOf("new" to r.newArticleCount))
        is SyncResult.PartialSuccess -> Result.success(workDataOf("new" to r.newArticleCount))
        is SyncResult.Failure        -> if (runAttemptCount < 3) Result.retry() else Result.failure()
    }

    companion object {
        const val PERIODIC_WORK_NAME = "banglaread_periodic_sync"
        const val ONE_TIME_WORK_NAME = "banglaread_manual_sync"
        private val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        fun schedulePeriodic(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<FeedSyncWorker>(30, TimeUnit.MINUTES).setConstraints(constraints).build()
            )
        }

        fun scheduleOneTime(context: Context): OneTimeWorkRequest {
            val req = OneTimeWorkRequestBuilder<FeedSyncWorker>().setConstraints(constraints).addTag(ONE_TIME_WORK_NAME).build()
            WorkManager.getInstance(context).enqueueUniqueWork(ONE_TIME_WORK_NAME, ExistingWorkPolicy.REPLACE, req)
            return req
        }

        fun cancelPeriodic(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
    }
}

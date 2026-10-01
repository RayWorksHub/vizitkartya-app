package hu.rayworks.vizit.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import hu.rayworks.vizit.VizitApplication
import java.util.concurrent.TimeUnit

class WorkManagerProfileSyncScheduler(context: Context) : ProfileSyncScheduler {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    override fun enqueue() {
        val request = OneTimeWorkRequestBuilder<ProfileSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                SyncRetryPolicy.INITIAL_DELAY_MILLIS,
                TimeUnit.MILLISECONDS,
            )
            .addTag(WORK_NAME)
            .build()
        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    override fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    companion object {
        const val WORK_NAME = "vizit-profile-sync"
    }
}

class ProfileSyncWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? VizitApplication ?: return Result.failure()
        return when (app.container.profileSyncEngine.run()) {
            ProfileSyncRunResult.RETRY -> Result.retry()
            ProfileSyncRunResult.COMPLETE,
            ProfileSyncRunResult.WAITING_FOR_SESSION,
            ProfileSyncRunResult.CONFLICT -> Result.success()
        }
    }
}

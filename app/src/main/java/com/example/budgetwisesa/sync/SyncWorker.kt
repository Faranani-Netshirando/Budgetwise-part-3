package com.example.budgetwisesa.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.budgetwisesa.data.repository.BudgetRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.util.concurrent.TimeUnit

/** Durable network-constrained work survives process death and retries after reconnection.
 * Reference: https://developer.android.com/develop/background-work/background-tasks/persistent
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val uid = inputData.getString("uid") ?: return Result.failure()
        if (FirebaseAuth.getInstance().currentUser?.uid != uid) return Result.success()
        return try {
            BudgetRepository(applicationContext, uid).sync()
            Log.i("BudgetWiseSync", "Account sync completed")
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            // Do not log tokens, names, receipt URLs, amounts or raw HTTP bodies.
            Log.w("BudgetWiseSync", "Sync failed: ${error.javaClass.simpleName}")
            if (error is HttpException && error.code() in listOf(400, 401, 403, 404)) Result.failure()
            else Result.retry()
        }
    }

    companion object {
        fun name(uid: String) = "budgetwise-sync-$uid"
        fun enqueue(context: Context, uid: String) {
            if (uid == BudgetRepository.GUEST) return
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setInputData(workDataOf("uid" to uid)).setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS).build()
            WorkManager.getInstance(context).enqueueUniqueWork(name(uid), ExistingWorkPolicy.APPEND_OR_REPLACE, request)
            val periodic = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setInputData(workDataOf("uid" to uid)).setConstraints(constraints).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("periodic-$uid", ExistingPeriodicWorkPolicy.KEEP, periodic)
        }
        fun cancel(context: Context, uid: String) {
            WorkManager.getInstance(context).cancelUniqueWork(name(uid))
            WorkManager.getInstance(context).cancelUniqueWork("periodic-$uid")
        }
    }
}

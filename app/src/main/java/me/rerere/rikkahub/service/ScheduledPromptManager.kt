package me.rerere.rikkahub.service

import android.app.Application
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rerere.rikkahub.AppScope
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.model.ScheduleType
import me.rerere.rikkahub.data.model.ScheduledPromptTask
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.uuid.Uuid

private const val TAG = "ScheduledPromptManager"

class ScheduledPromptManager(
    context: Application,
    private val appScope: AppScope,
    private val settingsStore: SettingsStore,
) {
    private val workManager = WorkManager.getInstance(context)
    private val started = AtomicBoolean(false)

    fun start() {
        if (!started.compareAndSet(false, true)) return
        appScope.launch {
            settingsStore.settingsFlow.collectLatest { settings ->
                runCatching {
                    reconcile(settings)
                }.onFailure {
                    Log.e(TAG, "Failed to reconcile scheduled prompt tasks", it)
                }
            }
        }
    }

    suspend fun reconcile(settings: Settings) {
        val enabledTasks = settings.assistants.flatMap { assistant ->
            assistant.scheduledPromptTasks
                .filter { it.enabled && it.prompt.isNotBlank() }
                .map { assistant.id to it }
        }
        val expectedTaskIds = enabledTasks.map { it.second.id }.toSet()

        cancelStaleWorks(expectedTaskIds)

        val now = ZonedDateTime.now()
        enabledTasks.forEach { (assistantId, task) ->
            schedulePeriodic(assistantId, task)
            if (ScheduledPromptTime.shouldRunCatchUp(task, now)) {
                scheduleCatchUp(assistantId, task)
            }
        }
    }

    private fun schedulePeriodic(assistantId: Uuid, task: ScheduledPromptTask) {
        val repeatDays = when (task.scheduleType) {
            ScheduleType.DAILY -> 1L
            ScheduleType.WEEKLY -> 7L
        }
        val request = PeriodicWorkRequestBuilder<ScheduledPromptWorker>(
            repeatDays,
            TimeUnit.DAYS,
            15L,
            TimeUnit.MINUTES
        )
            .setInitialDelay(ScheduledPromptTime.initialDelayMillis(task), TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10L, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(scheduledPromptInputData(assistantId = assistantId, taskId = task.id))
            .addTag(SCHEDULED_PROMPT_WORK_TAG)
            .addTag(taskIdTag(task.id))
            .build()

        workManager.enqueueUniquePeriodicWork(
            periodicWorkName(task.id),
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun scheduleCatchUp(assistantId: Uuid, task: ScheduledPromptTask) {
        val request = OneTimeWorkRequestBuilder<ScheduledPromptWorker>()
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10L, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(scheduledPromptInputData(assistantId = assistantId, taskId = task.id))
            .addTag(SCHEDULED_PROMPT_WORK_TAG)
            .addTag(taskIdTag(task.id))
            .build()

        workManager.enqueueUniqueWork(
            catchUpWorkName(task.id),
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    private suspend fun cancelStaleWorks(expectedTaskIds: Set<Uuid>) {
        withContext(Dispatchers.IO) {
            val workInfos = workManager.getWorkInfosByTag(SCHEDULED_PROMPT_WORK_TAG).get()
            workInfos.forEach { info ->
                val taskId = info.tags
                    .firstNotNullOfOrNull { parseTaskIdFromTag(it) }
                    ?: return@forEach
                if (taskId !in expectedTaskIds) {
                    workManager.cancelWorkById(info.id)
                }
            }
        }
    }
}

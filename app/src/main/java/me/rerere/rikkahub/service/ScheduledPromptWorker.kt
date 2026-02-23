package me.rerere.rikkahub.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import me.rerere.rikkahub.R
import me.rerere.rikkahub.RouteActivity
import me.rerere.rikkahub.SCHEDULED_TASK_NOTIFICATION_CHANNEL_ID
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.model.ScheduledPromptTask
import me.rerere.rikkahub.data.model.TaskRunStatus
import me.rerere.rikkahub.data.datastore.getAssistantById
import me.rerere.rikkahub.utils.sendNotification
import kotlin.uuid.Uuid

private const val TAG = "ScheduledPromptWorker"

class ScheduledPromptWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val settingsStore: SettingsStore,
    private val chatService: ChatService,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val assistantId = inputData.getString(INPUT_ASSISTANT_ID)?.let { runCatching { Uuid.parse(it) }.getOrNull() }
            ?: return Result.failure()
        val taskId = inputData.getString(INPUT_TASK_ID)?.let { runCatching { Uuid.parse(it) }.getOrNull() }
            ?: return Result.failure()

        val settings = settingsStore.settingsFlow.first()
        val assistant = settings.getAssistantById(assistantId) ?: return Result.success()
        val task = assistant.scheduledPromptTasks.firstOrNull { it.id == taskId } ?: return Result.success()
        if (!task.enabled || task.prompt.isBlank()) return Result.success()

        updateTask(assistantId, taskId) {
            it.copy(lastStatus = TaskRunStatus.RUNNING, lastError = "")
        }

        val startedAt = System.currentTimeMillis()
        return runCatching {
            chatService.sendScheduledPrompt(
                assistantId = assistantId,
                conversationId = task.conversationId,
                prompt = task.prompt
            ).getOrThrow()
        }.fold(
            onSuccess = { replyPreview ->
                updateTask(assistantId, taskId) {
                    it.copy(
                        lastStatus = TaskRunStatus.SUCCESS,
                        lastRunAt = startedAt,
                        lastError = ""
                    )
                }
                maybeNotifySuccess(task, replyPreview)
                Result.success()
            },
            onFailure = { error ->
                Log.e(TAG, "Scheduled task execution failed: ${task.id}", error)
                updateTask(assistantId, taskId) {
                    it.copy(
                        lastStatus = TaskRunStatus.FAILED,
                        lastRunAt = startedAt,
                        lastError = error.message.orEmpty().take(200)
                    )
                }
                maybeNotifyFailure(task, error)
                Result.retry()
            }
        )
    }

    private suspend fun updateTask(
        assistantId: Uuid,
        taskId: Uuid,
        transform: (ScheduledPromptTask) -> ScheduledPromptTask
    ) {
        settingsStore.update { settings ->
            settings.copy(
                assistants = settings.assistants.map { assistant ->
                    if (assistant.id != assistantId) return@map assistant
                    assistant.copy(
                        scheduledPromptTasks = assistant.scheduledPromptTasks.map { task ->
                            if (task.id == taskId) transform(task) else task
                        }
                    )
                }
            )
        }
    }

    private suspend fun maybeNotifySuccess(task: ScheduledPromptTask, replyPreview: String?) {
        val settings = settingsStore.settingsFlow.first()
        if (!settings.displaySetting.enableScheduledTaskNotification) return

        applicationContext.sendNotification(
            channelId = SCHEDULED_TASK_NOTIFICATION_CHANNEL_ID,
            notificationId = notificationId(task.id)
        ) {
            title = applicationContext.getString(
                R.string.notification_scheduled_task_success_title,
                taskDisplayTitle(task)
            )
            content = replyPreview?.ifBlank {
                applicationContext.getString(R.string.notification_scheduled_task_success_fallback)
            } ?: applicationContext.getString(R.string.notification_scheduled_task_success_fallback)
            autoCancel = true
            useDefaults = true
            category = NotificationCompat.CATEGORY_REMINDER
            contentIntent = getPendingIntent(task.conversationId)
        }
    }

    private suspend fun maybeNotifyFailure(task: ScheduledPromptTask, error: Throwable) {
        val settings = settingsStore.settingsFlow.first()
        if (!settings.displaySetting.enableScheduledTaskNotification) return

        applicationContext.sendNotification(
            channelId = SCHEDULED_TASK_NOTIFICATION_CHANNEL_ID,
            notificationId = notificationId(task.id)
        ) {
            title = applicationContext.getString(
                R.string.notification_scheduled_task_failed_title,
                taskDisplayTitle(task)
            )
            content = error.message.orEmpty().ifBlank {
                applicationContext.getString(R.string.notification_scheduled_task_failed_fallback)
            }
            autoCancel = true
            useDefaults = true
            category = NotificationCompat.CATEGORY_ERROR
            contentIntent = getPendingIntent(task.conversationId)
        }
    }

    private fun getPendingIntent(conversationId: Uuid): PendingIntent {
        val intent = Intent(applicationContext, RouteActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("conversationId", conversationId.toString())
        }
        return PendingIntent.getActivity(
            applicationContext,
            conversationId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun notificationId(taskId: Uuid): Int = taskId.hashCode() + 20000

    private fun taskDisplayTitle(task: ScheduledPromptTask): String {
        return task.title.ifBlank {
            task.prompt.lineSequence().firstOrNull().orEmpty().take(24)
                .ifBlank { applicationContext.getString(R.string.assistant_schedule_untitled) }
        }
    }
}

package me.rerere.rikkahub.data.model

import kotlin.uuid.Uuid

data class ScheduledTaskRun(
    val id: Uuid = Uuid.random(),
    val taskId: Uuid,
    val taskTitleSnapshot: String,
    val assistantIdSnapshot: Uuid,
    val status: TaskRunStatus,
    val startedAt: Long,
    val finishedAt: Long = 0L,
    val durationMs: Long = 0L,
    val promptSnapshot: String,
    val resultText: String = "",
    val errorText: String = "",
    val modelIdSnapshot: Uuid? = null,
    val providerNameSnapshot: String = "",
)

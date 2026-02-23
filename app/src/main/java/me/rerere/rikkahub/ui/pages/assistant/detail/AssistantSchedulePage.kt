package me.rerere.rikkahub.ui.pages.assistant.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Trash2
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.model.ScheduleType
import me.rerere.rikkahub.data.model.ScheduledPromptTask
import me.rerere.rikkahub.data.model.TaskRunStatus
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.utils.toLocalDateTime
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.DayOfWeek
import java.time.Instant
import java.util.Locale
import kotlin.uuid.Uuid

@Composable
fun AssistantSchedulePage(id: String) {
    val vm: AssistantDetailVM = koinViewModel(
        parameters = {
            parametersOf(id)
        }
    )
    val assistant by vm.assistant.collectAsStateWithLifecycle()
    var editingTask by remember(assistant.id) { mutableStateOf<ScheduledPromptTask?>(null) }

    fun saveTask(task: ScheduledPromptTask) {
        val normalized = task.copy(
            title = task.title.ifBlank {
                task.prompt.lineSequence().firstOrNull().orEmpty().take(24)
            },
            dayOfWeek = if (task.scheduleType == ScheduleType.WEEKLY) {
                task.dayOfWeek ?: DayOfWeek.MONDAY.value
            } else {
                null
            }
        )
        val hasSameId = assistant.scheduledPromptTasks.any { it.id == normalized.id }
        val nextTasks = if (hasSameId) {
            assistant.scheduledPromptTasks.map { if (it.id == normalized.id) normalized else it }
        } else {
            assistant.scheduledPromptTasks + normalized
        }
        vm.update(assistant.copy(scheduledPromptTasks = nextTasks))
        editingTask = null
    }

    fun deleteTask(taskId: Uuid) {
        vm.update(
            assistant.copy(
                scheduledPromptTasks = assistant.scheduledPromptTasks.filter { it.id != taskId }
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.assistant_page_tab_schedule))
                },
                navigationIcon = {
                    BackButton()
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTask = ScheduledPromptTask(
                        conversationId = Uuid.random(),
                        dayOfWeek = DayOfWeek.MONDAY.value
                    )
                }
            ) {
                Icon(Lucide.Plus, contentDescription = stringResource(R.string.assistant_schedule_add_task))
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 96.dp
            )
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    ListItem(
                        headlineContent = {
                            Text(stringResource(R.string.assistant_schedule_summary_title))
                        },
                        supportingContent = {
                            val enabledCount = assistant.scheduledPromptTasks.count { it.enabled }
                            Text(stringResource(R.string.assistant_schedule_summary_desc, enabledCount))
                        }
                    )
                }
            }

            if (assistant.scheduledPromptTasks.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.assistant_schedule_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
                    )
                }
            } else {
                items(
                    items = assistant.scheduledPromptTasks,
                    key = { it.id.toString() }
                ) { task ->
                    ScheduledTaskCard(
                        task = task,
                        onToggleEnabled = { enabled ->
                            saveTask(task.copy(enabled = enabled))
                        },
                        onEdit = {
                            editingTask = task
                        },
                        onDelete = {
                            deleteTask(task.id)
                        }
                    )
                }
            }
        }
    }

    editingTask?.let { task ->
        TaskEditorSheet(
            task = task,
            quickMessages = assistant.quickMessages.map { it.content }.filter { it.isNotBlank() },
            onDismiss = { editingTask = null },
            onSave = { saveTask(it) }
        )
    }
}

@Composable
private fun ScheduledTaskCard(
    task: ScheduledPromptTask,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            ListItem(
                headlineContent = {
                    Text(
                        text = task.title.ifBlank { stringResource(R.string.assistant_schedule_untitled) },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                supportingContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = task.prompt.replace("\n", " "),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = task.scheduleSummary(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = task.statusSummary(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                trailingContent = {
                    Switch(
                        checked = task.enabled,
                        onCheckedChange = onToggleEnabled
                    )
                }
            )
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Lucide.Pencil, contentDescription = stringResource(R.string.assistant_schedule_edit))
                }
                IconButton(onClick = onDelete) {
                    Icon(Lucide.Trash2, contentDescription = stringResource(R.string.assistant_schedule_delete))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditorSheet(
    task: ScheduledPromptTask,
    quickMessages: List<String>,
    onDismiss: () -> Unit,
    onSave: (ScheduledPromptTask) -> Unit,
) {
    var title by remember(task.id) { mutableStateOf(task.title) }
    var prompt by remember(task.id) { mutableStateOf(task.prompt) }
    var scheduleType by remember(task.id) { mutableStateOf(task.scheduleType) }
    var timeMinutesOfDay by remember(task.id) { mutableStateOf(task.timeMinutesOfDay.coerceIn(0, 1439)) }
    var dayOfWeek by remember(task.id) { mutableStateOf(task.dayOfWeek ?: DayOfWeek.MONDAY.value) }
    var showTimePicker by remember(task.id) { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.assistant_schedule_editor_title),
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.assistant_schedule_task_title)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text(stringResource(R.string.assistant_schedule_task_prompt)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )

            if (quickMessages.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.assistant_schedule_insert_quick_message),
                    style = MaterialTheme.typography.labelMedium
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    quickMessages.take(8).forEach { quickMessage ->
                        AssistChip(
                            onClick = {
                                prompt = if (prompt.isBlank()) {
                                    quickMessage
                                } else {
                                    "$prompt\n$quickMessage"
                                }
                            },
                            label = {
                                Text(
                                    text = quickMessage.replace("\n", " ").take(18),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                val options = listOf(ScheduleType.DAILY, ScheduleType.WEEKLY)
                options.forEachIndexed { index, option ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        onClick = { scheduleType = option },
                        selected = scheduleType == option
                    ) {
                        Text(
                            text = if (option == ScheduleType.DAILY) {
                                stringResource(R.string.assistant_schedule_daily)
                            } else {
                                stringResource(R.string.assistant_schedule_weekly)
                            }
                        )
                    }
                }
            }

            TextButton(
                onClick = {
                    showTimePicker = true
                }
            ) {
                Icon(Lucide.Clock, contentDescription = null)
                Text(
                    text = stringResource(
                        R.string.assistant_schedule_time_at,
                        formatTime(timeMinutesOfDay)
                    ),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            if (scheduleType == ScheduleType.WEEKLY) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DayOfWeek.values().forEach { day ->
                        FilterChip(
                            selected = dayOfWeek == day.value,
                            onClick = { dayOfWeek = day.value },
                            label = {
                                Text(day.displayName())
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.assistant_schedule_cancel))
                }
                TextButton(
                    onClick = {
                        if (prompt.isBlank()) return@TextButton
                        onSave(
                            task.copy(
                                title = title.trim(),
                                prompt = prompt.trim(),
                                scheduleType = scheduleType,
                                timeMinutesOfDay = timeMinutesOfDay.coerceIn(0, 1439),
                                dayOfWeek = if (scheduleType == ScheduleType.WEEKLY) dayOfWeek else null,
                            )
                        )
                    }
                ) {
                    Text(stringResource(R.string.assistant_schedule_save))
                }
            }
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = timeMinutesOfDay / 60,
            initialMinute = timeMinutesOfDay % 60,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            text = {
                TimePicker(state = timeState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        timeMinutesOfDay = timeState.hour * 60 + timeState.minute
                        showTimePicker = false
                    }
                ) {
                    Text(stringResource(R.string.assistant_schedule_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.assistant_schedule_cancel))
                }
            }
        )
    }
}

@Composable
private fun ScheduledPromptTask.scheduleSummary(): String {
    val time = formatTime(timeMinutesOfDay)
    return when (scheduleType) {
        ScheduleType.DAILY -> stringResource(R.string.assistant_schedule_summary_daily, time)
        ScheduleType.WEEKLY -> {
            val day = runCatching {
                DayOfWeek.of(dayOfWeek ?: DayOfWeek.MONDAY.value).displayName()
            }.getOrElse { DayOfWeek.MONDAY.displayName() }
            stringResource(R.string.assistant_schedule_summary_weekly, day, time)
        }
    }
}

@Composable
private fun ScheduledPromptTask.statusSummary(): String {
    val status = when (lastStatus) {
        TaskRunStatus.IDLE -> stringResource(R.string.assistant_schedule_status_idle)
        TaskRunStatus.RUNNING -> stringResource(R.string.assistant_schedule_status_running)
        TaskRunStatus.SUCCESS -> stringResource(R.string.assistant_schedule_status_success)
        TaskRunStatus.FAILED -> stringResource(R.string.assistant_schedule_status_failed)
    }
    val timeText = if (lastRunAt > 0) {
        Instant.ofEpochMilli(lastRunAt).toLocalDateTime()
    } else {
        stringResource(R.string.assistant_schedule_status_never_run)
    }
    return stringResource(R.string.assistant_schedule_status_line, status, timeText)
}

private fun DayOfWeek.displayName(): String {
    return getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())
}

private fun formatTime(timeMinutesOfDay: Int): String {
    val hour = (timeMinutesOfDay.coerceIn(0, 1439)) / 60
    val minute = (timeMinutesOfDay.coerceIn(0, 1439)) % 60
    return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
}

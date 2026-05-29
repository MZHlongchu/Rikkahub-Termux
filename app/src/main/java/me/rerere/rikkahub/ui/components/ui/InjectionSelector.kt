package me.rerere.rikkahub.ui.components.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Link01
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.Conversation
import me.rerere.rikkahub.data.model.PromptInjection
import me.rerere.rikkahub.ui.theme.CustomColors

@Composable
fun InjectionSelector(
    modifier: Modifier = Modifier,
    assistant: Assistant,
    settings: Settings,
    onUpdate: (Assistant) -> Unit,
    conversation: Conversation? = null,
    onUpdateConversation: ((Conversation) -> Unit)? = null,
    onNavigateToModeInjections: () -> Unit = {},
) {
    if (settings.modeInjections.isEmpty()) {
        InjectionEmptyState(
            modifier = modifier,
            onNavigateToModeInjections = onNavigateToModeInjections,
        )
        return
    }

    val useConversationInjections =
        assistant.allowConversationPromptInjection && conversation != null && onUpdateConversation != null
    val selectedModeInjectionIds = if (useConversationInjections) {
        conversation.modeInjectionIds
    } else {
        assistant.modeInjectionIds
    }

    Column(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End,
        ) {
            IconButton(onClick = onNavigateToModeInjections) {
                Icon(HugeIcons.Link01, contentDescription = null)
            }
        }

        ModeInjectionsSection(
            modeInjections = settings.modeInjections,
            selectedIds = selectedModeInjectionIds,
            onToggle = { id, checked ->
                val newIds = if (checked) {
                    selectedModeInjectionIds + id
                } else {
                    selectedModeInjectionIds - id
                }
                if (useConversationInjections) {
                    onUpdateConversation(conversation.copy(modeInjectionIds = newIds))
                } else {
                    onUpdate(assistant.copy(modeInjectionIds = newIds))
                }
            },
        )
    }
}

@Composable
private fun ModeInjectionsSection(
    modeInjections: List<PromptInjection.ModeInjection>,
    selectedIds: Set<kotlin.uuid.Uuid>,
    onToggle: (kotlin.uuid.Uuid, Boolean) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(modeInjections) { injection ->
            ListItem(
                headlineContent = {
                    Text(injection.name.ifBlank { stringResource(R.string.injection_selector_unnamed) })
                },
                trailingContent = {
                    Switch(
                        checked = selectedIds.contains(injection.id),
                        onCheckedChange = { checked ->
                            onToggle(injection.id, checked)
                        }
                    )
                },
                colors = CustomColors.listItemColors
            )
        }
    }
}

@Composable
private fun InjectionEmptyState(
    modifier: Modifier = Modifier,
    onNavigateToModeInjections: () -> Unit = {},
) {
    val content: @Composable () -> Unit = {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.injection_selector_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = stringResource(R.string.injection_selector_empty_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            TextButton(onClick = onNavigateToModeInjections) {
                Icon(HugeIcons.Link01, contentDescription = null)
                Text(stringResource(R.string.prompt_page_mode_injection_title))
            }
        }
    }

    content()
}

@Composable
private fun ModeInjectionsEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.injection_selector_mode_injections_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

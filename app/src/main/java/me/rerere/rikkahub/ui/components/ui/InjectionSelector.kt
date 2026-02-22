package me.rerere.rikkahub.ui.components.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ExternalLink
import com.composables.icons.lucide.Lucide
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.Lorebook

@Composable
fun InjectionSelector(
    modifier: Modifier = Modifier,
    assistant: Assistant,
    settings: Settings,
    onUpdate: (Assistant) -> Unit,
    onNavigateToPrompts: () -> Unit = {},
) {
    if (settings.lorebooks.isEmpty()) {
        InjectionEmptyState(
            modifier = modifier,
            onNavigateToPrompts = onNavigateToPrompts,
        )
        return
    }

    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.injection_selector_lorebooks),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onNavigateToPrompts) {
                Icon(Lucide.ExternalLink, contentDescription = null)
            }
        }
        LorebooksSection(
            lorebooks = settings.lorebooks,
            selectedIds = assistant.lorebookIds,
            onToggle = { id, checked ->
                val newIds = if (checked) {
                    assistant.lorebookIds + id
                } else {
                    assistant.lorebookIds - id
                }
                onUpdate(assistant.copy(lorebookIds = newIds))
            },
        )
    }
}

@Composable
private fun LorebooksSection(
    lorebooks: List<Lorebook>,
    selectedIds: Set<kotlin.uuid.Uuid>,
    onToggle: (kotlin.uuid.Uuid, Boolean) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(lorebooks) { lorebook ->
            ListItem(
                headlineContent = {
                    Text(lorebook.name.ifBlank { stringResource(R.string.injection_selector_unnamed_lorebook) })
                },
                supportingContent = if (lorebook.description.isNotBlank()) {
                    {
                        Text(
                            text = lorebook.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else null,
                trailingContent = {
                    Switch(
                        checked = selectedIds.contains(lorebook.id),
                        onCheckedChange = { checked ->
                            onToggle(lorebook.id, checked)
                        }
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                )
            )
        }
    }
}

@Composable
private fun InjectionEmptyState(
    modifier: Modifier = Modifier,
    onNavigateToPrompts: () -> Unit = {},
) {
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
        TextButton(onClick = onNavigateToPrompts) {
            Icon(Lucide.ExternalLink, contentDescription = null)
            Text(stringResource(R.string.injection_selector_go_to_prompts))
        }
    }
}

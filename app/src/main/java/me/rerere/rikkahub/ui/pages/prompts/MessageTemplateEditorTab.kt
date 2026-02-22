package me.rerere.rikkahub.ui.pages.prompts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.GripHorizontal
import com.composables.icons.lucide.Import
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Settings2
import com.composables.icons.lucide.Trash2
import kotlinx.coroutines.launch
import me.rerere.ai.core.MessageRole
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.export.MessageTemplateSerializer
import me.rerere.rikkahub.data.export.rememberImporter
import me.rerere.rikkahub.data.model.MessageInjectionTemplate
import me.rerere.rikkahub.data.model.MessageTemplateNode
import me.rerere.rikkahub.data.model.TemplateRoleMapping
import me.rerere.rikkahub.ui.components.ui.FormItem
import me.rerere.rikkahub.ui.components.ui.Select
import me.rerere.rikkahub.ui.components.ui.Tag
import me.rerere.rikkahub.ui.components.ui.TagType
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.ui.hooks.useEditState
import me.rerere.rikkahub.utils.plus
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun MessageTemplateEditorTab(
    template: MessageInjectionTemplate,
    onUpdate: (MessageInjectionTemplate) -> Unit
) {
    val lazyListState = rememberLazyListState()
    val toaster = LocalToaster.current
    val currentTemplate by rememberUpdatedState(template)
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val newNodes = template.nodes.toMutableList()
        val moved = newNodes.removeAt(from.index)
        newNodes.add(to.index, moved)
        onUpdate(template.copy(nodes = newNodes))
    }
    val editState = useEditState<MessageTemplateNode> { edited ->
        val index = template.nodes.indexOfFirst { it.id == edited.id }
        if (index >= 0) {
            onUpdate(template.copy(nodes = template.nodes.toMutableList().apply { set(index, edited) }))
        } else {
            onUpdate(template.copy(nodes = template.nodes + edited))
        }
    }
    val importSuccessMsg = stringResource(R.string.export_import_success)
    val importFailedMsg = stringResource(R.string.export_import_failed)
    val defaultPromptName = stringResource(R.string.prompt_page_message_template_prompt_default_name)
    val importer = rememberImporter(MessageTemplateSerializer) { result ->
        result.onSuccess { imported ->
            onUpdate(imported)
            toaster.show(importSuccessMsg)
        }.onFailure { error ->
            toaster.show(importFailedMsg.format(error.message))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp) + PaddingValues(bottom = 168.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            state = lazyListState
        ) {
            if (template.nodes.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillParentMaxHeight(0.8f)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.prompt_page_message_template_tab),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.prompt_page_message_template_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                items(template.nodes, key = { it.id }) { node ->
                    ReorderableItem(
                        state = reorderableState,
                        key = node.id
                    ) { isDragging ->
                        MessageTemplateNodeCard(
                            node = node,
                            modifier = Modifier
                                .longPressDraggableHandle()
                                .graphicsLayer {
                                    if (isDragging) {
                                        scaleX = 1.03f
                                        scaleY = 1.03f
                                    }
                                },
                            onToggleEnabled = { enabled ->
                                val nextNode = when (node) {
                                    is MessageTemplateNode.PromptNode -> node.copy(enabled = enabled)
                                    is MessageTemplateNode.HistoryNode -> node.copy(enabled = enabled)
                                    is MessageTemplateNode.LastUserMessageNode -> node.copy(enabled = enabled)
                                }
                                val index = template.nodes.indexOfFirst { it.id == node.id }
                                if (index >= 0) {
                                    onUpdate(template.copy(nodes = template.nodes.toMutableList().apply { set(index, nextNode) }))
                                }
                            },
                            onEdit = { editState.open(node) },
                            onDelete = {
                                onUpdate(template.copy(nodes = template.nodes.filter { it.id != node.id }))
                            }
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { importer.importFromFile() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Lucide.Import, null)
                Spacer(modifier = Modifier.size(8.dp))
                Text(stringResource(R.string.prompt_page_message_template_import))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        editState.open(
                            MessageTemplateNode.PromptNode(
                                name = defaultPromptName
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Lucide.Plus, null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(stringResource(R.string.prompt_page_message_template_add_prompt))
                }
                Button(
                    onClick = {
                        onUpdate(currentTemplate.copy(nodes = currentTemplate.nodes + MessageTemplateNode.HistoryNode()))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Lucide.Plus, null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(stringResource(R.string.prompt_page_message_template_add_history))
                }
            }
            Button(
                onClick = {
                    onUpdate(currentTemplate.copy(nodes = currentTemplate.nodes + MessageTemplateNode.LastUserMessageNode()))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Lucide.Plus, null)
                Spacer(modifier = Modifier.size(6.dp))
                Text(stringResource(R.string.prompt_page_message_template_add_last_user))
            }
        }
    }

    if (editState.isEditing) {
        editState.currentState?.let { node ->
            MessageTemplateNodeEditSheet(
                node = node,
                onDismiss = { editState.dismiss() },
                onConfirm = { editState.confirm() },
                onEdit = { editState.currentState = it }
            )
        }
    }
}

@Composable
private fun MessageTemplateNodeCard(
    node: MessageTemplateNode,
    modifier: Modifier = Modifier,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Lucide.GripHorizontal, contentDescription = null)
            Switch(
                checked = node.enabled,
                onCheckedChange = onToggleEnabled
            )
            Tag(type = TagType.INFO) {
                Text(getNodeTypeLabel(node))
            }
            Text(
                text = getNodeDisplayName(node),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = onEdit) {
                Icon(Lucide.Settings2, stringResource(R.string.prompt_page_edit))
            }
            FilledIconButton(onClick = onDelete) {
                Icon(Lucide.Trash2, stringResource(R.string.prompt_page_delete))
            }
        }
    }
}

@Composable
private fun MessageTemplateNodeEditSheet(
    node: MessageTemplateNode,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onEdit: (MessageTemplateNode) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetGesturesEnabled = false,
        dragHandle = {
            IconButton(onClick = {
                scope.launch {
                    sheetState.hide()
                    onDismiss()
                }
            }) {
                Icon(Lucide.ChevronDown, null)
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize(0.95f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.prompt_page_message_template_edit_node),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FormItem(
                    label = { Text(stringResource(R.string.prompt_page_enabled)) },
                    tail = {
                        Switch(
                            checked = node.enabled,
                            onCheckedChange = { enabled ->
                                val nextNode = when (node) {
                                    is MessageTemplateNode.PromptNode -> node.copy(enabled = enabled)
                                    is MessageTemplateNode.HistoryNode -> node.copy(enabled = enabled)
                                    is MessageTemplateNode.LastUserMessageNode -> node.copy(enabled = enabled)
                                }
                                onEdit(nextNode)
                            }
                        )
                    }
                )

                when (node) {
                    is MessageTemplateNode.PromptNode -> {
                        OutlinedTextField(
                            value = node.name,
                            onValueChange = { onEdit(node.copy(name = it)) },
                            label = { Text(stringResource(R.string.prompt_page_message_template_name)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        MessageTemplateRoleSelector(
                            label = stringResource(R.string.prompt_page_message_template_role),
                            selected = node.role,
                            onSelect = { onEdit(node.copy(role = it)) }
                        )
                        OutlinedTextField(
                            value = node.content,
                            onValueChange = { onEdit(node.copy(content = it)) },
                            label = { Text(stringResource(R.string.prompt_page_message_template_content)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            minLines = 6
                        )
                    }

                    is MessageTemplateNode.HistoryNode -> {
                        Text(
                            text = stringResource(R.string.prompt_page_message_template_history_title),
                            style = MaterialTheme.typography.titleSmall
                        )
                        MessageTemplateRoleMappingEditor(
                            roleMapping = node.roleMapping,
                            onUpdate = { onEdit(node.copy(roleMapping = it)) }
                        )
                    }

                    is MessageTemplateNode.LastUserMessageNode -> {
                        Text(
                            text = stringResource(R.string.prompt_page_message_template_last_user_title),
                            style = MaterialTheme.typography.titleSmall
                        )
                        MessageTemplateRoleMappingEditor(
                            roleMapping = node.roleMapping,
                            onUpdate = { onEdit(node.copy(roleMapping = it)) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.prompt_page_cancel))
                }
                TextButton(onClick = onConfirm) {
                    Text(stringResource(R.string.prompt_page_confirm))
                }
            }
        }
    }
}

@Composable
private fun MessageTemplateRoleMappingEditor(
    roleMapping: TemplateRoleMapping,
    onUpdate: (TemplateRoleMapping) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MessageTemplateRoleSelector(
            label = stringResource(R.string.prompt_page_message_template_mapping_system),
            selected = roleMapping.system,
            onSelect = { onUpdate(roleMapping.copy(system = it)) }
        )
        MessageTemplateRoleSelector(
            label = stringResource(R.string.prompt_page_message_template_mapping_user),
            selected = roleMapping.user,
            onSelect = { onUpdate(roleMapping.copy(user = it)) }
        )
        MessageTemplateRoleSelector(
            label = stringResource(R.string.prompt_page_message_template_mapping_assistant),
            selected = roleMapping.assistant,
            onSelect = { onUpdate(roleMapping.copy(assistant = it)) }
        )
    }
}

@Composable
private fun MessageTemplateRoleSelector(
    label: String,
    selected: MessageRole,
    onSelect: (MessageRole) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Select(
            options = listOf(MessageRole.SYSTEM, MessageRole.USER, MessageRole.ASSISTANT),
            selectedOption = selected,
            onOptionSelected = onSelect,
            optionToString = { messageTemplateRoleLabel(it) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun messageTemplateRoleLabel(role: MessageRole): String = when (role) {
    MessageRole.SYSTEM -> stringResource(R.string.prompt_page_message_template_mapping_system)
    MessageRole.USER -> stringResource(R.string.prompt_page_role_user)
    MessageRole.ASSISTANT -> stringResource(R.string.prompt_page_role_assistant)
    else -> role.name
}

@Composable
private fun getNodeTypeLabel(node: MessageTemplateNode): String = when (node) {
    is MessageTemplateNode.PromptNode -> stringResource(R.string.prompt_page_message_template_node_prompt)
    is MessageTemplateNode.HistoryNode -> stringResource(R.string.prompt_page_message_template_node_history)
    is MessageTemplateNode.LastUserMessageNode -> stringResource(R.string.prompt_page_message_template_node_last_user)
}

@Composable
private fun getNodeDisplayName(node: MessageTemplateNode): String = when (node) {
    is MessageTemplateNode.PromptNode -> {
        if (node.name.isBlank()) stringResource(R.string.prompt_page_message_template_prompt_default_name) else node.name
    }

    is MessageTemplateNode.HistoryNode -> stringResource(R.string.prompt_page_message_template_history_title)
    is MessageTemplateNode.LastUserMessageNode -> stringResource(R.string.prompt_page_message_template_last_user_title)
}

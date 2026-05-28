package me.rerere.rikkahub.ui.components.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Link01
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.Conversation
import me.rerere.rikkahub.data.model.PromptInjection
import me.rerere.rikkahub.data.model.QuickMessage
import me.rerere.rikkahub.ui.theme.CustomColors
import kotlin.uuid.Uuid

@Composable
fun ExtensionSelector(
    modifier: Modifier = Modifier,
    assistant: Assistant,
    settings: Settings,
    onUpdate: (Assistant) -> Unit,
    conversation: Conversation? = null,
    onUpdateConversation: ((Conversation) -> Unit)? = null,
    onNavigateToQuickMessages: () -> Unit = {},
    onNavigateToPrompts: () -> Unit = {},
) {
    val pagerState = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier) {
        SecondaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxWidth(),
            edgePadding = 4.dp,
        ) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = {
                    scope.launch { pagerState.animateScrollToPage(0) }
                },
                text = { Text(stringResource(R.string.extension_selector_tab_quick_messages)) }
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = {
                    scope.launch { pagerState.animateScrollToPage(1) }
                },
                text = { Text(stringResource(R.string.extension_selector_tab_mode_injections)) }
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            when (page) {
                0 -> QuickMessagesExtensionPage(
                    quickMessages = settings.quickMessages,
                    selectedIds = assistant.quickMessageIds,
                    onToggle = { id, checked ->
                        val newIds = if (checked) {
                            assistant.quickMessageIds + id
                        } else {
                            assistant.quickMessageIds - id
                        }
                        onUpdate(assistant.copy(quickMessageIds = newIds))
                    },
                    onManage = onNavigateToQuickMessages,
                )

                1 -> ModeInjectionsExtensionPage(
                    assistant = assistant,
                    conversation = conversation,
                    modeInjections = settings.modeInjections,
                    onUpdate = onUpdate,
                    onUpdateConversation = onUpdateConversation,
                    onManage = onNavigateToPrompts,
                )
            }
        }
    }
}

@Composable
private fun QuickMessagesExtensionPage(
    quickMessages: List<QuickMessage>,
    selectedIds: Set<Uuid>,
    onToggle: (Uuid, Boolean) -> Unit,
    onManage: () -> Unit,
) {
    if (quickMessages.isEmpty()) {
        ExtensionEmptyState(
            message = stringResource(R.string.extension_selector_quick_messages_empty),
            buttonText = stringResource(R.string.extension_selector_go_to_extensions),
            onAction = onManage,
        )
        return
    }

    Column {
        ManageButton(onClick = onManage)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(quickMessages, key = { it.id }) { quickMessage ->
                ListItem(
                    headlineContent = {
                        Text(
                            text = quickMessage.title.ifBlank {
                                stringResource(R.string.quick_messages_page_untitled)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = quickMessage.content.ifBlank {
                                stringResource(R.string.quick_messages_page_empty_content)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = quickMessage.id in selectedIds,
                            onCheckedChange = { checked ->
                                onToggle(quickMessage.id, checked)
                            }
                        )
                    },
                    colors = CustomColors.listItemColors
                )
            }
        }
    }
}

@Composable
private fun ModeInjectionsExtensionPage(
    assistant: Assistant,
    conversation: Conversation?,
    modeInjections: List<PromptInjection.ModeInjection>,
    onUpdate: (Assistant) -> Unit,
    onUpdateConversation: ((Conversation) -> Unit)?,
    onManage: () -> Unit,
) {
    if (modeInjections.isEmpty()) {
        ExtensionEmptyState(
            message = stringResource(R.string.extension_selector_mode_injections_empty),
            buttonText = stringResource(R.string.extension_selector_go_to_prompts),
            onAction = onManage,
        )
        return
    }

    val useConversationInjections =
        assistant.allowConversationPromptInjection && conversation != null && onUpdateConversation != null
    val selectedIds = if (useConversationInjections) {
        conversation.modeInjectionIds
    } else {
        assistant.modeInjectionIds
    }

    Column {
        ManageButton(onClick = onManage)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(modeInjections, key = { it.id }) { injection ->
                ListItem(
                    headlineContent = {
                        Text(injection.name.ifBlank { stringResource(R.string.injection_selector_unnamed) })
                    },
                    trailingContent = {
                        Switch(
                            checked = injection.id in selectedIds,
                            onCheckedChange = { checked ->
                                val newIds = if (checked) {
                                    selectedIds + injection.id
                                } else {
                                    selectedIds - injection.id
                                }
                                if (useConversationInjections) {
                                    onUpdateConversation(conversation.copy(modeInjectionIds = newIds))
                                } else {
                                    onUpdate(assistant.copy(modeInjectionIds = newIds))
                                }
                            }
                        )
                    },
                    colors = CustomColors.listItemColors
                )
            }
        }
    }
}

@Composable
private fun ManageButton(
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
    ) {
        IconButton(onClick = onClick) {
            Icon(HugeIcons.Link01, contentDescription = null)
        }
    }
}

@Composable
private fun ExtensionEmptyState(
    message: String,
    buttonText: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        TextButton(onClick = onAction) {
            Icon(HugeIcons.Link01, contentDescription = null)
            Text(buttonText)
        }
    }
}

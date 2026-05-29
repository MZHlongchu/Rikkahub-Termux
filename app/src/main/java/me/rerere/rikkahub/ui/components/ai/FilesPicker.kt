package me.rerere.rikkahub.ui.components.ai

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.rerere.ai.provider.ProviderSetting
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Camera01
import me.rerere.hugeicons.stroke.CommandLine
import me.rerere.hugeicons.stroke.Files02
import me.rerere.hugeicons.stroke.FileZip
import me.rerere.hugeicons.stroke.Image02
import me.rerere.hugeicons.stroke.MusicNote03
import me.rerere.hugeicons.stroke.Package
import me.rerere.hugeicons.stroke.Video01
import me.rerere.hugeicons.stroke.WebProgramming
import me.rerere.rikkahub.R
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.findProvider
import me.rerere.rikkahub.data.datastore.getCurrentChatModel
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.Conversation
import me.rerere.rikkahub.ui.components.ui.ExtensionSelector
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.ui.hooks.ChatInputState

@Composable
internal fun FilesPicker(
    conversation: Conversation,
    assistant: Assistant,
    state: ChatInputState,
    onCompressContext: (additionalPrompt: String, targetTokens: Int, keepRecentMessages: Int) -> Job,
    onUpdateAssistant: (Assistant) -> Unit,
    onUpdateConversation: (Conversation) -> Unit,
    showInjectionSheet: Boolean,
    onShowInjectionSheetChange: (Boolean) -> Unit,
    showCompressDialog: Boolean,
    onShowCompressDialogChange: (Boolean) -> Unit,
    termuxCommandModeEnabled: Boolean,
    onToggleTermuxCommandMode: (Boolean) -> Unit,
    codeBlockRichRenderEnabled: Boolean,
    onToggleCodeBlockRichRender: (Boolean) -> Unit,
    onTakePic: () -> Unit,
    onPickImage: () -> Unit,
    onPickVideo: () -> Unit,
    onPickAudio: () -> Unit,
    onPickFile: () -> Unit,
    onDismiss: () -> Unit
) {
    val settings = LocalSettings.current
    val provider = settings.getCurrentChatModel()?.findProvider(providers = settings.providers)
    val attachmentsEnabled = !(termuxCommandModeEnabled && !state.isEditing())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (attachmentsEnabled) {
                TakePicButton(onLaunchCamera = onTakePic)

                ImagePickButton(onClick = onPickImage)

                if (provider != null && provider is ProviderSetting.Google) {
                    VideoPickButton(onClick = onPickVideo)

                    AudioPickButton(onClick = onPickAudio)
                }

                FilePickButton(onClick = onPickFile)
            } else {
                Text(stringResource(R.string.chat_page_termux_command_mode_attachments_disabled))
            }
        }

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth()
        )

        val activeInjectionCount = if (assistant.allowConversationPromptInjection) {
            conversation.modeInjectionIds.size
        } else {
            assistant.modeInjectionIds.size
        }
        val activeExtensionCount = assistant.quickMessageIds.size + activeInjectionCount
        ListItem(
            leadingContent = {
                Icon(
                    imageVector = HugeIcons.Package,
                    contentDescription = stringResource(R.string.assistant_page_tab_extensions),
                )
            },
            headlineContent = {
                Text(stringResource(R.string.assistant_page_tab_extensions))
            },
            trailingContent = {
                if (activeExtensionCount > 0) {
                    Text(
                        text = activeExtensionCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            },
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .clickable {
                    onShowInjectionSheetChange(true)
                },
        )

        ListItem(
            leadingContent = {
                Icon(
                    imageVector = HugeIcons.FileZip,
                    contentDescription = stringResource(R.string.chat_page_compress_context),
                )
            },
            headlineContent = {
                Text(stringResource(R.string.chat_page_compress_context))
            },
            trailingContent = {
                if (conversation.messageNodes.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.chat_page_message_count, conversation.messageNodes.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .clickable {
                    onShowCompressDialogChange(true)
                },
        )
        ListItem(
            leadingContent = {
                Icon(
                    imageVector = HugeIcons.CommandLine,
                    contentDescription = stringResource(R.string.chat_page_termux_command_mode_content_desc),
                )
            },
            headlineContent = {
                Text(stringResource(R.string.chat_page_termux_command_mode_title))
            },
            supportingContent = {
                Text(stringResource(R.string.chat_page_termux_command_mode_supporting))
            },
            trailingContent = {
                Switch(
                    checked = termuxCommandModeEnabled,
                    onCheckedChange = onToggleTermuxCommandMode,
                )
            },
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .clickable {
                    onToggleTermuxCommandMode(!termuxCommandModeEnabled)
                },
        )

        ListItem(
            leadingContent = {
                Icon(
                    imageVector = HugeIcons.WebProgramming,
                    contentDescription = stringResource(R.string.setting_display_page_code_block_rich_render_title),
                )
            },
            headlineContent = {
                Text(stringResource(R.string.setting_display_page_code_block_rich_render_title))
            },
            trailingContent = {
                Switch(
                    checked = codeBlockRichRenderEnabled,
                    onCheckedChange = onToggleCodeBlockRichRender,
                )
            },
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .clickable {
                    onToggleCodeBlockRichRender(!codeBlockRichRenderEnabled)
                },
        )
    }

    if (showInjectionSheet) {
        ExtensionQuickConfigSheet(
            conversation = conversation,
            assistant = assistant,
            settings = settings,
            onUpdateAssistant = onUpdateAssistant,
            onUpdateConversation = onUpdateConversation,
            onDismiss = { onShowInjectionSheetChange(false) }
        )
    }

    if (showCompressDialog) {
        CompressContextDialog(
            onDismiss = {
                onShowCompressDialogChange(false)
                onDismiss()
            },
            onConfirm = { additionalPrompt, targetTokens, keepRecentMessages ->
                onCompressContext(additionalPrompt, targetTokens, keepRecentMessages)
            }
        )
    }
}

@Composable
private fun TakePicButton(onLaunchCamera: () -> Unit = {}) {
    BigIconTextButton(
        icon = {
            Icon(HugeIcons.Camera01, null)
        },
        text = {
            Text(stringResource(R.string.take_picture))
        }
    ) {
        onLaunchCamera()
    }
}

@Composable
private fun ImagePickButton(onClick: () -> Unit = {}) {
    BigIconTextButton(
        icon = {
            Icon(HugeIcons.Image02, null)
        },
        text = {
            Text(stringResource(R.string.photo))
        }
    ) {
        onClick()
    }
}

@Composable
private fun VideoPickButton(onClick: () -> Unit = {}) {
    BigIconTextButton(
        icon = {
            Icon(HugeIcons.Video01, null)
        },
        text = {
            Text(stringResource(R.string.video))
        }
    ) {
        onClick()
    }
}

@Composable
private fun AudioPickButton(onClick: () -> Unit = {}) {
    BigIconTextButton(
        icon = {
            Icon(HugeIcons.MusicNote03, null)
        },
        text = {
            Text(stringResource(R.string.audio))
        }
    ) {
        onClick()
    }
}

@Composable
private fun FilePickButton(onClick: () -> Unit = {}) {
    BigIconTextButton(
        icon = {
            Icon(HugeIcons.Files02, null)
        },
        text = {
            Text(stringResource(R.string.upload_file))
        }
    ) {
        onClick()
    }
}

@Composable
private fun BigIconTextButton(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    text: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .semantics {
                role = Role.Button
            }
            .wrapContentWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Surface(
            tonalElevation = 2.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 32.dp, vertical = 16.dp)
            ) {
                icon()
            }
        }
        ProvideTextStyle(MaterialTheme.typography.bodySmall) {
            text()
        }
    }
}

@Composable
private fun ExtensionQuickConfigSheet(
    conversation: Conversation,
    assistant: Assistant,
    settings: Settings,
    onUpdateAssistant: (Assistant) -> Unit,
    onUpdateConversation: (Conversation) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val navController = LocalNavController.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .padding(horizontal = 16.dp),
        ) {
            ExtensionSelector(
                assistant = assistant,
                settings = settings,
                onUpdate = onUpdateAssistant,
                conversation = conversation,
                onUpdateConversation = onUpdateConversation,
                modifier = Modifier.weight(1f),
                onNavigateToQuickMessages = {
                    scope.launch {
                        sheetState.hide()
                        onDismiss()
                        navController.navigate(Screen.QuickMessages)
                    }
                },
                onNavigateToModeInjections = {
                    scope.launch {
                        sheetState.hide()
                        onDismiss()
                        navController.navigate(Screen.ModeInjections)
                    }
                },
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BigIconTextButtonPreview() {
    Row(
        modifier = Modifier.padding(16.dp)
    ) {
        BigIconTextButton(
            icon = {
                Icon(HugeIcons.Image02, null)
            },
            text = {
                Text(stringResource(R.string.photo))
            }
        ) {}
    }
}

package me.rerere.rikkahub.ui.components.ai

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.dokar.sonner.ToastType
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.rerere.ai.provider.ProviderSetting
import me.rerere.ai.ui.UIMessagePart
import me.rerere.common.android.appTempFolder
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Book03
import me.rerere.hugeicons.stroke.Camera01
import me.rerere.hugeicons.stroke.CommandLine
import me.rerere.hugeicons.stroke.Files02
import me.rerere.hugeicons.stroke.FileZip
import me.rerere.hugeicons.stroke.Image02
import me.rerere.hugeicons.stroke.MusicNote03
import me.rerere.hugeicons.stroke.Video01
import me.rerere.hugeicons.stroke.WebProgramming
import me.rerere.rikkahub.R
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.findProvider
import me.rerere.rikkahub.data.datastore.getCurrentChatModel
import me.rerere.rikkahub.data.files.FilesManager
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.Conversation
import me.rerere.rikkahub.ui.components.ui.InjectionSelector
import me.rerere.rikkahub.ui.components.ui.permission.PermissionCamera
import me.rerere.rikkahub.ui.components.ui.permission.PermissionManager
import me.rerere.rikkahub.ui.components.ui.permission.rememberPermissionState
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.ui.hooks.ChatInputState
import me.rerere.rikkahub.utils.isAllowedFileType
import org.koin.compose.koinInject
import java.io.File
import kotlin.uuid.Uuid

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
                TakePicButton {
                    state.addImages(it)
                    onDismiss()
                }

                ImagePickButton {
                    state.addImages(it)
                    onDismiss()
                }

                if (provider != null && provider is ProviderSetting.Google) {
                    VideoPickButton {
                        state.addVideos(it)
                        onDismiss()
                    }

                    AudioPickButton {
                        state.addAudios(it)
                        onDismiss()
                    }
                }

                FilePickButton {
                    state.addFiles(it)
                    onDismiss()
                }
            } else {
                Text(stringResource(R.string.chat_page_termux_command_mode_attachments_disabled))
            }
        }

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth()
        )

        if (settings.modeInjections.isNotEmpty()) {
            val activeCount = if (assistant.allowConversationPromptInjection) {
                conversation.modeInjectionIds.size
            } else {
                assistant.modeInjectionIds.size
            }
            ListItem(
                leadingContent = {
                    Icon(
                        imageVector = HugeIcons.Book03,
                        contentDescription = stringResource(R.string.chat_page_prompt_injections),
                    )
                },
                headlineContent = {
                    Text(stringResource(R.string.chat_page_prompt_injections))
                },
                trailingContent = {
                    if (activeCount > 0) {
                        Text(
                            text = activeCount.toString(),
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
        }

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
        InjectionQuickConfigSheet(
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
private fun useCropLauncher(
    onCroppedImageReady: (Uri) -> Unit,
    onCleanup: (() -> Unit)? = null
): Pair<ActivityResultLauncher<Intent>, (Uri) -> Unit> {
    val context = LocalContext.current
    var cropOutputUri by remember { mutableStateOf<Uri?>(null) }

    val cropActivityLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            cropOutputUri?.let { croppedUri ->
                onCroppedImageReady(croppedUri)
            }
        }
        cropOutputUri?.toFile()?.delete()
        cropOutputUri = null
        onCleanup?.invoke()
    }

    val launchCrop: (Uri) -> Unit = { sourceUri ->
        val outputFile = File(context.appTempFolder, "crop_output_${System.currentTimeMillis()}.jpg")
        cropOutputUri = Uri.fromFile(outputFile)

        val cropIntent = UCrop.of(sourceUri, cropOutputUri!!)
            .withOptions(UCrop.Options().apply {
                setFreeStyleCropEnabled(true)
                setAllowedGestures(
                    UCropActivity.SCALE,
                    UCropActivity.ROTATE,
                    UCropActivity.NONE
                )
                setCompressionFormat(Bitmap.CompressFormat.PNG)
            })
            .withMaxResultSize(4096, 4096)
            .getIntent(context)

        cropActivityLauncher.launch(cropIntent)
    }

    return Pair(cropActivityLauncher, launchCrop)
}

@Composable
private fun ImagePickButton(onAddImages: (List<Uri>) -> Unit = {}) {
    val context = LocalContext.current
    val settings = LocalSettings.current
    val filesManager: FilesManager = koinInject()
    var preCropTempFile by remember { mutableStateOf<File?>(null) }
    val (_, launchCrop) = useCropLauncher(
        onCroppedImageReady = { croppedUri ->
            onAddImages(filesManager.createChatFilesByContents(listOf(croppedUri)))
        },
        onCleanup = {
            preCropTempFile?.delete()
            preCropTempFile = null
        }
    )
    val imagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { selectedUris ->
            if (selectedUris.isNotEmpty()) {
                if (settings.displaySetting.skipCropImage) {
                    onAddImages(filesManager.createChatFilesByContents(selectedUris))
                } else if (selectedUris.size == 1) {
                    val tempFile = File(context.appTempFolder, "pick_temp_${System.currentTimeMillis()}.jpg")
                    runCatching {
                        context.contentResolver.openInputStream(selectedUris.first())?.use { input ->
                            tempFile.outputStream().use { output -> input.copyTo(output) }
                        }
                        preCropTempFile = tempFile
                        launchCrop(tempFile.toUri())
                    }.onFailure {
                        Log.e("ImagePickButton", "Failed to copy image to temp, falling back", it)
                        launchCrop(selectedUris.first())
                    }
                } else {
                    onAddImages(filesManager.createChatFilesByContents(selectedUris))
                }
            }
        }

    BigIconTextButton(
        icon = {
            Icon(HugeIcons.Image02, null)
        },
        text = {
            Text(stringResource(R.string.photo))
        }
    ) {
        imagePickerLauncher.launch("image/*")
    }
}

@Composable
private fun TakePicButton(onAddImages: (List<Uri>) -> Unit = {}) {
    val cameraPermission = rememberPermissionState(PermissionCamera)

    val context = LocalContext.current
    val settings = LocalSettings.current
    val filesManager: FilesManager = koinInject()
    var cameraOutputUri by remember { mutableStateOf<Uri?>(null) }
    var cameraOutputFile by remember { mutableStateOf<File?>(null) }

    val (_, launchCrop) = useCropLauncher(
        onCroppedImageReady = { croppedUri ->
            onAddImages(filesManager.createChatFilesByContents(listOf(croppedUri)))
        },
        onCleanup = {
            cameraOutputFile?.delete()
            cameraOutputFile = null
            cameraOutputUri = null
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { captureSuccessful ->
        if (captureSuccessful && cameraOutputUri != null) {
            if (settings.displaySetting.skipCropImage) {
                onAddImages(filesManager.createChatFilesByContents(listOf(cameraOutputUri!!)))
                cameraOutputFile?.delete()
                cameraOutputFile = null
                cameraOutputUri = null
            } else {
                launchCrop(cameraOutputUri!!)
            }
        } else {
            cameraOutputFile?.delete()
            cameraOutputFile = null
            cameraOutputUri = null
        }
    }
    PermissionManager(
        permissionState = cameraPermission
    ) {
        BigIconTextButton(
            icon = {
                Icon(HugeIcons.Camera01, null)
            },
            text = {
                Text(stringResource(R.string.take_picture))
            }
        ) {
            if (cameraPermission.allRequiredPermissionsGranted) {
                cameraOutputFile = context.cacheDir.resolve("camera_${Uuid.random()}.jpg")
                cameraOutputUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    cameraOutputFile!!
                )
                cameraLauncher.launch(cameraOutputUri!!)
            } else {
                cameraPermission.requestPermissions()
            }
        }
    }
}

@Composable
private fun VideoPickButton(onAddVideos: (List<Uri>) -> Unit = {}) {
    val filesManager: FilesManager = koinInject()
    val videoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { selectedUris ->
        if (selectedUris.isNotEmpty()) {
            onAddVideos(filesManager.createChatFilesByContents(selectedUris))
        }
    }

    BigIconTextButton(
        icon = {
            Icon(HugeIcons.Video01, null)
        },
        text = {
            Text(stringResource(R.string.video))
        }
    ) {
        videoPickerLauncher.launch("video/*")
    }
}

@Composable
private fun AudioPickButton(onAddAudios: (List<Uri>) -> Unit = {}) {
    val filesManager: FilesManager = koinInject()
    val audioPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { selectedUris ->
        if (selectedUris.isNotEmpty()) {
            onAddAudios(filesManager.createChatFilesByContents(selectedUris))
        }
    }

    BigIconTextButton(
        icon = {
            Icon(HugeIcons.MusicNote03, null)
        },
        text = {
            Text(stringResource(R.string.audio))
        }
    ) {
        audioPickerLauncher.launch("audio/*")
    }
}

@Composable
private fun FilePickButton(onAddFiles: (List<UIMessagePart.Document>) -> Unit = {}) {
    val toaster = LocalToaster.current
    val filesManager: FilesManager = koinInject()
    val pickMedia =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isNotEmpty()) {
                val documents = uris.mapNotNull { uri ->
                    val fileName = filesManager.getFileNameFromUri(uri) ?: "file"
                    val mime = filesManager.resolveMimeType(uri, fileName)

                    if (isAllowedFileType(fileName, mime)) {
                        val localUri = filesManager.createChatFilesByContents(listOf(uri))[0]
                        UIMessagePart.Document(
                            url = localUri.toString(),
                            fileName = fileName,
                            mime = mime
                        )
                    } else {
                        toaster.show("不支持的文件类型: $fileName", type = ToastType.Error)
                        null
                    }
                }

                if (documents.isNotEmpty()) {
                    onAddFiles(documents)
                }
            }
        }
    BigIconTextButton(
        icon = {
            Icon(HugeIcons.Files02, null)
        },
        text = {
            Text(stringResource(R.string.upload_file))
        }
    ) {
        pickMedia.launch(arrayOf("*/*"))
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
private fun InjectionQuickConfigSheet(
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
            InjectionSelector(
                assistant = assistant,
                settings = settings,
                onUpdate = onUpdateAssistant,
                conversation = conversation,
                onUpdateConversation = onUpdateConversation,
                modifier = Modifier.weight(1f),
                onNavigateToPrompts = {
                    scope.launch {
                        sheetState.hide()
                        onDismiss()
                        navController.navigate(Screen.Prompts)
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

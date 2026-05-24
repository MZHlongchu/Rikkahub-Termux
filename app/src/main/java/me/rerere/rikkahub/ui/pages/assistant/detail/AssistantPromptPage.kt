package me.rerere.rikkahub.ui.pages.assistant.detail

import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.ArrowDown01
import me.rerere.hugeicons.stroke.ArrowUp01
import me.rerere.hugeicons.stroke.Add01
import me.rerere.hugeicons.stroke.Delete01
import me.rerere.hugeicons.stroke.Cancel01
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.ai.transformers.DefaultPlaceholderProvider
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.export.SillyTavernCharacterCardExportData
import me.rerere.rikkahub.data.export.SillyTavernCharacterCardPngSerializer
import me.rerere.rikkahub.data.export.SillyTavernCharacterCardSerializer
import me.rerere.rikkahub.data.export.rememberExporter
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.AssistantAffectScope
import me.rerere.rikkahub.data.model.AssistantRegex
import me.rerere.rikkahub.data.model.effectiveUserPersona
import me.rerere.rikkahub.data.model.editableFindRegex
import me.rerere.rikkahub.data.model.sourceLabel
import me.rerere.rikkahub.data.model.selectedUserPersonaProfile
import me.rerere.rikkahub.data.model.withFindRegexInput
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.ExportDialog
import me.rerere.rikkahub.ui.components.ui.FormItem
import me.rerere.rikkahub.ui.components.ui.Select
import me.rerere.rikkahub.ui.components.ui.Tag
import me.rerere.rikkahub.ui.components.ui.TagType
import me.rerere.rikkahub.ui.components.ui.TextArea
import me.rerere.rikkahub.ui.pages.extensions.RegexEditorSection
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.insertAtCursor
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlinx.coroutines.delay
import kotlin.uuid.Uuid

@Composable
fun AssistantPromptPage(id: String) {
    val vm: AssistantDetailVM = koinViewModel(
        parameters = {
            parametersOf(id)
        }
    )
    val assistant by vm.assistant.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(stringResource(R.string.assistant_page_tab_prompt))
                },
                navigationIcon = {
                    BackButton()
                },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { innerPadding ->
        AssistantPromptContent(
            modifier = Modifier.padding(innerPadding),
            assistant = assistant,
            settings = settings,
            onUpdate = { vm.update(it) },
        )
    }
}

@Composable
private fun AssistantPromptContent(
    modifier: Modifier = Modifier,
    assistant: Assistant,
    settings: Settings,
    onUpdate: (Assistant) -> Unit,
) {
    val latestAssistant by rememberUpdatedState(assistant)
    val latestOnUpdate by rememberUpdatedState(onUpdate)
    val selectedPersonaProfile = settings.selectedUserPersonaProfile()
    val effectiveUserPersona = settings.effectiveUserPersona(assistant)
    var showCharacterExportDialog by remember { mutableStateOf(false) }
    var showCharacterPngExportDialog by remember { mutableStateOf(false) }
    val characterCardExportData = remember(assistant) {
        SillyTavernCharacterCardExportData(
            assistant = assistant,
        )
    }
    val characterCardExporter = rememberExporter(
        data = characterCardExportData,
        serializer = SillyTavernCharacterCardSerializer,
    )
    val characterCardPngExporter = rememberExporter(
        data = characterCardExportData,
        serializer = SillyTavernCharacterCardPngSerializer,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CustomColors.cardColorsOnSurfaceContainer
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "SillyTavern 角色卡导入",
                    style = MaterialTheme.typography.titleMedium
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Tag(type = TagType.INFO) {
                        Text("PNG / JSON")
                    }
                    Tag(type = TagType.SUCCESS) {
                        Text("助手资料")
                    }
                    Tag(type = TagType.WARNING) {
                        Text("可合并 Regex")
                    }
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PromptFeatureGuideRow(
                        title = "导入内容",
                        body = "支持 Tavern 角色卡 PNG / JSON，基础信息会写入当前助手。",
                    )
                    PromptFeatureGuideRow(
                        title = "自动合并",
                        body = "角色卡 Regex 可按导入时选择并入当前助手。",
                    )
                    PromptFeatureGuideRow(
                        title = "后续编辑",
                        body = "角色卡基础信息和 Regex 都留在当前助手下维护。",
                    )
                }
                AssistantImporter(
                    allowedKinds = setOf(AssistantImportKind.CHARACTER_CARD),
                    onImport = { payload, includeRegexes ->
                        val updatedAssistant = applyImportedAssistantToExisting(
                            currentAssistant = latestAssistant,
                            payload = payload,
                            includeRegexes = includeRegexes,
                        ).assistant
                        latestOnUpdate(updatedAssistant)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (
                    assistant.stCharacterData != null ||
                    assistant.name.isNotBlank() ||
                    assistant.systemPrompt.isNotBlank() ||
                    assistant.regexes.isNotEmpty()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "当前角色卡信息",
                            style = MaterialTheme.typography.labelLarge
                        )
                        assistant.stCharacterData?.let { character ->
                            Text(
                                text = "角色卡: ${character.sourceName.ifBlank { character.name.ifBlank { "SillyTavern" } }}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (selectedPersonaProfile != null) {
                            Text(
                                text = "全局 Persona: ${selectedPersonaProfile.name.ifBlank { "未命名 Persona" }}${if (effectiveUserPersona.isBlank()) "（内容为空）" else ""}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else if (effectiveUserPersona.isNotBlank()) {
                            Text(
                                text = "全局 Persona: 兼容回退旧助手 Persona",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(
                            text = "助手 Regex ${assistant.regexes.size} 条。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (assistant.stCharacterData != null) {
                                TextButton(
                                    onClick = {
                                        latestOnUpdate(
                                            latestAssistant.copy(
                                                stCharacterData = null
                                            )
                                        )
                                    }
                                ) {
                                    Text("清除角色卡信息")
                                }
                            }
                            TextButton(
                                onClick = { showCharacterExportDialog = true }
                            ) {
                                Text("导出角色卡 JSON")
                            }
                            TextButton(
                                onClick = { showCharacterPngExportDialog = true }
                            ) {
                                Text("导出角色卡 PNG")
                            }
                        }
                    }
                }
            }
        }

        Card(
            colors = CustomColors.cardColorsOnSurfaceContainer
        ) {
            FormItem(
                modifier = Modifier.padding(8.dp),
                label = {
                    Text(stringResource(R.string.assistant_page_allow_conversation_system_prompt))
                },
                description = {
                    Text(stringResource(R.string.assistant_page_allow_conversation_system_prompt_desc))
                },
                tail = {
                    Switch(
                        checked = assistant.allowConversationSystemPrompt,
                        onCheckedChange = {
                            onUpdate(
                                assistant.copy(
                                    allowConversationSystemPrompt = it
                                )
                            )
                        }
                    )
                }
            )
        }

        Card(
            colors = CustomColors.cardColorsOnSurfaceContainer
        ) {
            FormItem(
                modifier = Modifier.padding(8.dp),
                label = {
                    Text(stringResource(R.string.assistant_page_allow_conversation_prompt_injection))
                },
                description = {
                    Text(stringResource(R.string.assistant_page_allow_conversation_prompt_injection_desc))
                },
                tail = {
                    Switch(
                        checked = assistant.allowConversationPromptInjection,
                        onCheckedChange = {
                            onUpdate(
                                assistant.copy(
                                    allowConversationPromptInjection = it
                                )
                            )
                        }
                    )
                }
            )
        }

        Card(
            colors = CustomColors.cardColorsOnSurfaceContainer
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val systemPromptValue = rememberTextFieldState(
                    initialText = assistant.systemPrompt,
                )
                var lastExternalSystemPrompt by remember { mutableStateOf(assistant.systemPrompt) }
                var lastDispatchedSystemPrompt by remember { mutableStateOf(assistant.systemPrompt) }
                val systemPromptText = systemPromptValue.text.toString()

                LaunchedEffect(assistant.systemPrompt) {
                    val currentText = systemPromptValue.text.toString()
                    val shouldSyncText =
                        currentText == lastExternalSystemPrompt ||
                            assistant.systemPrompt != lastDispatchedSystemPrompt ||
                            currentText == assistant.systemPrompt
                    lastExternalSystemPrompt = assistant.systemPrompt

                    if (shouldSyncText && currentText != assistant.systemPrompt) {
                        systemPromptValue.edit {
                            replace(0, length, assistant.systemPrompt)
                        }
                    }
                    if (shouldSyncText) {
                        lastDispatchedSystemPrompt = assistant.systemPrompt
                    }
                }
                LaunchedEffect(systemPromptText) {
                    if (
                        systemPromptText == latestAssistant.systemPrompt ||
                        systemPromptText == lastDispatchedSystemPrompt
                    ) {
                        return@LaunchedEffect
                    }
                    delay(400)
                    val latestText = systemPromptValue.text.toString()
                    if (
                        latestText == systemPromptText &&
                        latestText != latestAssistant.systemPrompt &&
                        latestText != lastDispatchedSystemPrompt
                    ) {
                        lastDispatchedSystemPrompt = latestText
                        latestOnUpdate(
                            latestAssistant.copy(
                                systemPrompt = latestText
                            )
                        )
                    }
                }
                DisposableEffect(Unit) {
                    onDispose {
                        val currentText = systemPromptValue.text.toString()
                        if (
                            currentText != latestAssistant.systemPrompt &&
                            currentText != lastDispatchedSystemPrompt
                        ) {
                            lastDispatchedSystemPrompt = currentText
                            latestOnUpdate(
                                latestAssistant.copy(
                                    systemPrompt = currentText
                                )
                            )
                        }
                    }
                }

                TextArea(
                    state = systemPromptValue,
                    label = stringResource(R.string.assistant_page_system_prompt),
                    minLines = 5,
                    maxLines = 10
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PromptFeatureGuideRow(
                        title = "可用变量",
                        body = "下方变量会在发送前替换为当前环境里的实际值。",
                    )
                }

                Column {
                    Text(
                        text = stringResource(R.string.assistant_page_available_variables),
                        style = MaterialTheme.typography.labelSmall
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        DefaultPlaceholderProvider.placeholders.forEach { (k, info) ->
                            Tag(
                                onClick = {
                                    systemPromptValue.insertAtCursor("{{$k}}")
                                }
                            ) {
                                info.displayName()
                                Text(": {{$k}}")
                            }
                        }
                    }
                }
            }
        }

        Card(
            colors = CustomColors.cardColorsOnSurfaceContainer
        ) {
            FormItem(
                modifier = Modifier.padding(8.dp),
                label = {
                    Text(stringResource(R.string.assistant_page_preset_messages))
                },
                description = {
                    Text(stringResource(R.string.assistant_page_preset_messages_desc))
                }
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                assistant.presetMessages.fastForEachIndexed { index, presetMessage ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Select(
                                options = listOf(MessageRole.USER, MessageRole.ASSISTANT),
                                selectedOption = presetMessage.role,
                                onOptionSelected = { role ->
                                    onUpdate(
                                        assistant.copy(
                                            presetMessages = assistant.presetMessages.mapIndexed { i, msg ->
                                                if (i == index) {
                                                    msg.copy(role = role)
                                                } else {
                                                    msg
                                                }
                                            }
                                        )
                                    )
                                },
                                modifier = Modifier.width(160.dp)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = {
                                    onUpdate(
                                        assistant.copy(
                                            presetMessages = assistant.presetMessages.filterIndexed { i, _ ->
                                                i != index
                                            }
                                        )
                                    )
                                }
                            ) {
                                Icon(HugeIcons.Cancel01, null)
                            }
                        }
                        OutlinedTextField(
                            value = presetMessage.toText(),
                            onValueChange = { text ->
                                onUpdate(
                                    assistant.copy(
                                        presetMessages = assistant.presetMessages.mapIndexed { i, msg ->
                                            if (i == index) {
                                                msg.copy(parts = listOf(UIMessagePart.Text(text)))
                                            } else {
                                                msg
                                            }
                                        }
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 6
                        )
                    }
                }
                Button(
                    onClick = {
                        val lastRole = assistant.presetMessages.lastOrNull()?.role ?: MessageRole.ASSISTANT
                        val nextRole = when (lastRole) {
                            MessageRole.USER -> MessageRole.ASSISTANT
                            MessageRole.ASSISTANT -> MessageRole.USER
                            else -> MessageRole.USER
                        }
                        onUpdate(
                            assistant.copy(
                                presetMessages = assistant.presetMessages + UIMessage(
                                    role = nextRole,
                                    parts = listOf(UIMessagePart.Text(""))
                                )
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(HugeIcons.Add01, null)
                }
            }
        }

        Card(colors = CustomColors.cardColorsOnSurfaceContainer) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "角色 Regex 开关",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = if (assistant.regexEnabled) {
                            "当前助手的 ${assistant.regexes.size} 条 Regex 会参与运行时处理。"
                        } else {
                            "当前助手的 Regex 已整体停用，列表会保留但不会参与匹配。"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = assistant.regexEnabled,
                    onCheckedChange = { enabled ->
                        onUpdate(assistant.copy(regexEnabled = enabled))
                    },
                )
            }
        }

        RegexEditorSection(
            regexes = assistant.regexes,
            onUpdate = { regexes ->
                onUpdate(
                    assistant.copy(regexes = regexes)
                )
            },
            title = "助手级 Regex",
            description = "仅对当前助手生效。适合角色卡自带的格式化、美化和卡片专属规则。",
        )
    }

    if (showCharacterExportDialog) {
        ExportDialog(
            exporter = characterCardExporter,
            title = "导出 ST 角色卡",
            onDismiss = { showCharacterExportDialog = false }
        )
    }

    if (showCharacterPngExportDialog) {
        ExportDialog(
            exporter = characterCardPngExporter,
            title = "导出 ST PNG 角色卡",
            onDismiss = { showCharacterPngExportDialog = false }
        )
    }
}

@Composable
private fun AssistantRegexCard(
    regex: AssistantRegex,
    onUpdate: (Assistant) -> Unit,
    assistant: Assistant,
    index: Int
) {
    var expanded by remember {
        mutableStateOf(false)
    }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = regex.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    regex.sourceLabel()?.let { sourceLabel ->
                        Text(
                            text = sourceLabel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Switch(
                    checked = regex.enabled,
                    onCheckedChange = { enabled ->
                        onUpdate(
                            assistant.copy(
                                regexes = assistant.regexes.mapIndexed { i, reg ->
                                    if (i == index) {
                                        reg.copy(enabled = enabled)
                                    } else {
                                        reg
                                    }
                                }
                            )
                        )
                    },
                    modifier = Modifier.padding(start = 8.dp)
                )
                IconButton(
                    onClick = {
                        expanded = !expanded
                    }
                ) {
                    Icon(
                        imageVector = if (expanded) HugeIcons.ArrowUp01 else HugeIcons.ArrowDown01,
                        contentDescription = null
                    )
                }
            }

            if (expanded) {

                OutlinedTextField(
                    value = regex.name,
                    onValueChange = { name ->
                        onUpdate(
                            assistant.copy(
                                regexes = assistant.regexes.mapIndexed { i, reg ->
                                    if (i == index) {
                                        reg.copy(name = name)
                                    } else {
                                        reg
                                    }
                                }
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.assistant_page_regex_name)) }
                )

                OutlinedTextField(
                    value = regex.editableFindRegex(),
                    onValueChange = { findRegex ->
                        onUpdate(
                            assistant.copy(
                                regexes = assistant.regexes.mapIndexed { i, reg ->
                                    if (i == index) {
                                        reg.withFindRegexInput(findRegex)
                                    } else {
                                        reg
                                    }
                                }
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.assistant_page_regex_find_regex)) },
                    placeholder = { Text("e.g., \\b\\w+@\\w+\\.\\w+\\b") },
                )

                OutlinedTextField(
                    value = regex.replaceString,
                    onValueChange = { replaceString ->
                        onUpdate(
                            assistant.copy(
                                regexes = assistant.regexes.mapIndexed { i, reg ->
                                    if (i == index) {
                                        reg.copy(replaceString = replaceString)
                                    } else {
                                        reg
                                    }
                                }
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.assistant_page_regex_replace_string)) },
                    placeholder = { Text("e.g., [EMAIL]") }
                )

                Column {
                    Text(
                        text = stringResource(R.string.assistant_page_regex_affecting_scopes),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AssistantAffectScope.entries.forEach { scope ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Checkbox(
                                    checked = scope in regex.affectingScope,
                                    onCheckedChange = { checked ->
                                        val newScopes = if (checked) {
                                            regex.affectingScope + scope
                                        } else {
                                            regex.affectingScope - scope
                                        }
                                        onUpdate(
                                            assistant.copy(
                                                regexes = assistant.regexes.mapIndexed { i, reg ->
                                                    if (i == index) {
                                                        reg.copy(affectingScope = newScopes)
                                                    } else {
                                                        reg
                                                    }
                                                }
                                            )
                                        )
                                    }
                                )
                                Text(
                                    text = scope.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = regex.visualOnly,
                        onCheckedChange = { visualOnly ->
                            onUpdate(
                                assistant.copy(
                                    regexes = assistant.regexes.mapIndexed { i, reg ->
                                        if (i == index) {
                                            reg.copy(
                                                visualOnly = visualOnly,
                                                promptOnly = if (visualOnly) false else reg.promptOnly
                                            )
                                        } else {
                                            reg
                                        }
                                    }
                                )
                            )
                        }
                    )
                    Text(
                        text = stringResource(R.string.assistant_page_regex_visual_only),
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = regex.promptOnly,
                        onCheckedChange = { promptOnly ->
                            onUpdate(
                                assistant.copy(
                                    regexes = assistant.regexes.mapIndexed { i, reg ->
                                        if (i == index) {
                                            reg.copy(
                                                promptOnly = promptOnly,
                                                visualOnly = if (promptOnly) false else reg.visualOnly
                                            )
                                        } else {
                                            reg
                                        }
                                    }
                                )
                            )
                        }
                    )
                    Text(
                        text = stringResource(R.string.assistant_page_regex_prompt_only),
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = regex.minDepth?.toString().orEmpty(),
                        onValueChange = { value ->
                            if (value.isNotEmpty() && value.any { !it.isDigit() }) return@OutlinedTextField
                            val minDepth = value.toIntOrNull()?.takeIf { it > 0 }
                            onUpdate(
                                assistant.copy(
                                    regexes = assistant.regexes.mapIndexed { i, reg ->
                                        if (i == index) {
                                            reg.copy(minDepth = minDepth)
                                        } else {
                                            reg
                                        }
                                    }
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.assistant_page_regex_min_depth)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )

                    OutlinedTextField(
                        value = regex.maxDepth?.toString().orEmpty(),
                        onValueChange = { value ->
                            if (value.isNotEmpty() && value.any { !it.isDigit() }) return@OutlinedTextField
                            val maxDepth = value.toIntOrNull()?.takeIf { it > 0 }
                            onUpdate(
                                assistant.copy(
                                    regexes = assistant.regexes.mapIndexed { i, reg ->
                                        if (i == index) {
                                            reg.copy(maxDepth = maxDepth)
                                        } else {
                                            reg
                                        }
                                    }
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.assistant_page_regex_max_depth)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }

                Text(
                    text = stringResource(R.string.assistant_page_regex_depth_desc),
                    style = MaterialTheme.typography.labelSmall,
                )

                TextButton(
                    onClick = {
                        onUpdate(
                            assistant.copy(
                                regexes = assistant.regexes.filterIndexed { i, _ ->
                                    i != index
                                }
                            )
                        )
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(HugeIcons.Delete01, null)
                        Text(stringResource(R.string.delete))
                    }
                }
            }
        }
    }

}

@Composable
private fun PromptFeatureGuideRow(
    title: String,
    body: String,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

package me.rerere.rikkahub.ui.pages.chat

import android.content.ClipData
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dokar.sonner.ToastType
import kotlinx.coroutines.launch
import me.rerere.ai.core.MessageRole
import me.rerere.ai.provider.TextRequestPreview
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.ArrowDown01
import me.rerere.hugeicons.stroke.ArrowUp01
import me.rerere.hugeicons.stroke.Cancel01
import me.rerere.hugeicons.stroke.Copy01
import me.rerere.hugeicons.stroke.Refresh01
import me.rerere.hugeicons.stroke.Search01
import me.rerere.rikkahub.service.ChatPromptPreviewMessage
import me.rerere.rikkahub.service.ChatRuntimeInspection
import me.rerere.rikkahub.ui.components.ui.JsonTree
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.ui.theme.JetbrainsMono
import me.rerere.rikkahub.utils.JsonInstantPretty
import me.rerere.rikkahub.utils.UiState

internal enum class ChatRuntimeInspectorTab {
    PROMPTS,
    VARIABLES,
    PAYLOAD,
}

@Composable
internal fun ChatRuntimeInspectorSheet(
    state: UiState<ChatRuntimeInspection>,
    initialTab: ChatRuntimeInspectorTab = ChatRuntimeInspectorTab.PROMPTS,
    onDismissRequest: () -> Unit,
    onRefresh: () -> Unit,
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    var activeTab by rememberSaveable { mutableStateOf(initialTab) }
    LaunchedEffect(initialTab) { activeTab = initialTab }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f),
        ) {
            InspectorHeader(state = state, onRefresh = onRefresh)
            PrimaryTabRow(selectedTabIndex = activeTab.ordinal) {
                ChatRuntimeInspectorTab.entries.forEach { tab ->
                    Tab(
                        selected = tab == activeTab,
                        onClick = { activeTab = tab },
                        text = {
                            Text(
                                when (tab) {
                                    ChatRuntimeInspectorTab.PROMPTS -> "Prompts"
                                    ChatRuntimeInspectorTab.VARIABLES -> "Variables"
                                    ChatRuntimeInspectorTab.PAYLOAD -> "Payload"
                                }
                            )
                        },
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                when (val current = state) {
                    UiState.Idle, UiState.Loading -> InspectorLoading()
                    is UiState.Error -> InspectorError(current.error, onRefresh)
                    is UiState.Success -> when (activeTab) {
                        ChatRuntimeInspectorTab.PROMPTS -> PromptInspector(current.data)
                        ChatRuntimeInspectorTab.VARIABLES -> VariableInspector(current.data)
                        ChatRuntimeInspectorTab.PAYLOAD -> PayloadInspector(current.data.payloadPreview)
                    }
                }
            }
        }
    }
}

@Composable
private fun InspectorHeader(
    state: UiState<ChatRuntimeInspection>,
    onRefresh: () -> Unit,
) {
    val inspection = (state as? UiState.Success)?.data
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "运行时检查",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            inspection?.let {
                Text(
                    text = "${it.assistantName} / ${it.modelName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onRefresh) {
            Icon(HugeIcons.Refresh01, contentDescription = "刷新运行时检查")
        }
    }
}

@Composable
private fun InspectorLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun InspectorError(error: Throwable, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = error.message ?: error.javaClass.simpleName,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onRefresh) { Text("重试") }
    }
}

@Composable
private fun PromptInspector(inspection: ChatRuntimeInspection) {
    var query by rememberSaveable(inspection.promptMessages) { mutableStateOf("") }
    val messages = remember(inspection.promptMessages, query) {
        inspection.promptMessages.filter {
            query.isBlank() ||
                it.content.contains(query, ignoreCase = true) ||
                it.role.name.contains(query, ignoreCase = true)
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        InspectorSectionHeader(
            title = "Prompt Stack",
            subtitle = "约 ${inspection.promptTokenEstimate} tokens · ${messages.size}/${inspection.promptMessages.size} 条消息",
            copyLabel = "prompt_preview",
            copyText = promptMessagesToText(messages),
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            placeholder = { Text("搜索角色或正文") },
            leadingIcon = { Icon(HugeIcons.Search01, contentDescription = null) },
            trailingIcon = if (query.isBlank()) null else {
                {
                    IconButton(onClick = { query = "" }) {
                        Icon(HugeIcons.Cancel01, contentDescription = "清空搜索")
                    }
                }
            },
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (messages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("没有匹配结果", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(messages) { index, message ->
                    PromptMessageItem(index = index, message = message)
                }
            }
        }
    }
}

@Composable
private fun PromptMessageItem(index: Int, message: ChatPromptPreviewMessage) {
    var expanded by rememberSaveable(message.content, index) { mutableStateOf(index < 2) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("#${index + 1}", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = message.role.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = roleColor(message.role),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${message.tokenEstimate} tokens",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = if (expanded) HugeIcons.ArrowUp01 else HugeIcons.ArrowDown01,
                    contentDescription = if (expanded) "收起" else "展开",
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            SelectionContainer {
                Text(
                    text = message.content,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = JetbrainsMono,
                    maxLines = if (expanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun VariableInspector(inspection: ChatRuntimeInspection) {
    Column(modifier = Modifier.fillMaxSize()) {
        InspectorSectionHeader(
            title = "Context Snapshot",
            subtitle = "${inspection.contextVariables.size} 个顶层字段",
            copyLabel = "runtime_variables",
            copyText = JsonInstantPretty.encodeToString(inspection.contextVariables),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            JsonTree(
                json = inspection.contextVariables,
                modifier = Modifier.fillMaxWidth(),
                initialExpandLevel = 2,
            )
        }
    }
}

@Composable
private fun PayloadInspector(payload: TextRequestPreview) {
    val payloadText = remember(payload) { payloadToText(payload) }
    Column(modifier = Modifier.fillMaxSize()) {
        InspectorSectionHeader(
            title = "${payload.method} · ${payload.apiName}",
            subtitle = payload.providerName,
            copyLabel = "request_payload",
            copyText = payloadText,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PayloadLabel("URL")
            SelectionContainer {
                Text(payload.url, style = MaterialTheme.typography.bodySmall, fontFamily = JetbrainsMono)
            }
            PayloadLabel("Stream")
            Text(payload.stream.toString(), style = MaterialTheme.typography.bodySmall, fontFamily = JetbrainsMono)
            HorizontalDivider()
            PayloadLabel("Headers")
            payload.headers.forEach { header ->
                SelectionContainer {
                    Text(
                        text = "${header.name}: ${header.value}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = JetbrainsMono,
                    )
                }
            }
            HorizontalDivider()
            PayloadLabel("Body")
            JsonTree(json = payload.body, modifier = Modifier.fillMaxWidth(), initialExpandLevel = 2)
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun PayloadLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun InspectorSectionHeader(
    title: String,
    subtitle: String,
    copyLabel: String,
    copyText: String,
) {
    val clipboard = LocalClipboard.current
    val toaster = LocalToaster.current
    val scope = rememberCoroutineScope()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(
            onClick = {
                scope.launch {
                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(copyLabel, copyText)))
                    toaster.show("已复制", type = ToastType.Success)
                }
            },
        ) {
            Icon(HugeIcons.Copy01, contentDescription = "复制")
        }
    }
}

@Composable
private fun roleColor(role: MessageRole) = when (role) {
    MessageRole.SYSTEM -> MaterialTheme.colorScheme.tertiary
    MessageRole.USER -> MaterialTheme.colorScheme.primary
    MessageRole.ASSISTANT -> MaterialTheme.colorScheme.secondary
    MessageRole.TOOL -> MaterialTheme.colorScheme.error
}

private fun promptMessagesToText(messages: List<ChatPromptPreviewMessage>): String {
    return messages.joinToString("\n\n") { message ->
        "[${message.role.name}]\n${message.content}"
    }
}

private fun payloadToText(payload: TextRequestPreview): String = buildString {
    appendLine("${payload.method} ${payload.url}")
    payload.headers.forEach { appendLine("${it.name}: ${it.value}") }
    appendLine()
    append(JsonInstantPretty.encodeToString(payload.body))
}

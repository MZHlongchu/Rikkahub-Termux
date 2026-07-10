package me.rerere.rikkahub.ui.components.ai

import android.Manifest
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dokar.sonner.ToastType
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.BookOpen01
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.ai.tools.local.LocalToolOption
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.components.ui.ToggleSurface
import me.rerere.rikkahub.ui.components.ui.permission.PermissionInfo
import me.rerere.rikkahub.ui.components.ui.permission.PermissionManager
import me.rerere.rikkahub.ui.components.ui.permission.rememberPermissionState
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.utils.hasUsageStatsPermission
import me.rerere.rikkahub.utils.openUsageAccessSettings

private data class LocalToolItem(
    val option: LocalToolOption,
    @StringRes val title: Int,
    @StringRes val description: Int,
)

private val localToolItems = listOf(
    LocalToolItem(
        option = LocalToolOption.JavascriptEngine,
        title = R.string.assistant_page_local_tools_javascript_engine_title,
        description = R.string.assistant_page_local_tools_javascript_engine_desc,
    ),
    LocalToolItem(
        option = LocalToolOption.TimeInfo,
        title = R.string.assistant_page_local_tools_time_info_title,
        description = R.string.assistant_page_local_tools_time_info_desc,
    ),
    LocalToolItem(
        option = LocalToolOption.Clipboard,
        title = R.string.assistant_page_local_tools_clipboard_title,
        description = R.string.assistant_page_local_tools_clipboard_desc,
    ),
    LocalToolItem(
        option = LocalToolOption.Tts,
        title = R.string.assistant_page_local_tools_tts_title,
        description = R.string.assistant_page_local_tools_tts_desc,
    ),
    LocalToolItem(
        option = LocalToolOption.AskUser,
        title = R.string.assistant_page_local_tools_ask_user_title,
        description = R.string.assistant_page_local_tools_ask_user_desc,
    ),
    LocalToolItem(
        option = LocalToolOption.ScreenTime,
        title = R.string.assistant_page_local_tools_screen_time_title,
        description = R.string.assistant_page_local_tools_screen_time_desc,
    ),
    LocalToolItem(
        option = LocalToolOption.Calendar,
        title = R.string.assistant_page_local_tools_calendar_title,
        description = R.string.assistant_page_local_tools_calendar_desc,
    ),
)

@Composable
fun LocalToolPickerButton(
    assistant: Assistant,
    onUpdateAssistant: (Assistant) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val enabledCount = assistant.localTools.distinct().size

    ToggleSurface(
        modifier = modifier,
        checked = enabledCount > 0,
        onClick = { showPicker = true },
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                BadgedBox(
                    badge = {
                        if (enabledCount > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            ) {
                                Text(enabledCount.toString())
                            }
                        }
                    },
                ) {
                    Icon(
                        imageVector = HugeIcons.BookOpen01,
                        contentDescription = stringResource(R.string.assistant_page_tab_local_tools),
                    )
                }
            }
        }
    }

    if (showPicker) {
        ModalBottomSheet(
            onDismissRequest = { showPicker = false },
            sheetState = rememberBottomSheetState(
                initialValue = SheetValue.Hidden,
                enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.assistant_page_tab_local_tools),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
                LocalToolSettings(
                    assistant = assistant,
                    onUpdateAssistant = onUpdateAssistant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }
    }
}

@Composable
fun LocalToolSettings(
    assistant: Assistant,
    onUpdateAssistant: (Assistant) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val toaster = LocalToaster.current
    val permissionRequiredText =
        stringResource(R.string.assistant_page_local_tools_screen_time_permission_required)
    val calendarPermissionState = rememberPermissionState(
        permissions = setOf(
            PermissionInfo(
                permission = Manifest.permission.READ_CALENDAR,
                displayName = { Text(stringResource(R.string.permission_calendar_read)) },
                usage = { Text(stringResource(R.string.permission_calendar_read_desc)) },
                required = true,
            ),
            PermissionInfo(
                permission = Manifest.permission.WRITE_CALENDAR,
                displayName = { Text(stringResource(R.string.permission_calendar_write)) },
                usage = { Text(stringResource(R.string.permission_calendar_write_desc)) },
                required = true,
            ),
        ),
    )
    PermissionManager(permissionState = calendarPermissionState)

    fun toggleLocalTool(option: LocalToolOption, enabled: Boolean) {
        if (enabled && option == LocalToolOption.ScreenTime && !context.hasUsageStatsPermission()) {
            toaster.show(message = permissionRequiredText, type = ToastType.Warning)
            context.openUsageAccessSettings()
        }
        if (enabled && option == LocalToolOption.Calendar && !calendarPermissionState.allPermissionsGranted) {
            calendarPermissionState.requestPermissions()
            return
        }
        val newLocalTools = if (enabled) {
            assistant.localTools + option
        } else {
            assistant.localTools - option
        }
        onUpdateAssistant(assistant.copy(localTools = newLocalTools))
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CardGroup {
            localToolItems.forEach { toolItem ->
                item(
                    headlineContent = { Text(stringResource(toolItem.title)) },
                    supportingContent = { Text(stringResource(toolItem.description)) },
                    trailingContent = {
                        Switch(
                            checked = toolItem.option in assistant.localTools,
                            onCheckedChange = { toggleLocalTool(toolItem.option, it) },
                        )
                    },
                )
            }
        }
    }
}

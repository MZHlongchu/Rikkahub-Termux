package me.rerere.rikkahub.ui.pages.setting

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LargeTopAppBar
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.ai.tools.termux.TermuxWorkdirServerManager
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.FormItem
import me.rerere.rikkahub.utils.plus
import org.koin.compose.koinInject
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

private const val TAG = "SettingTermuxPage"

@Composable
fun SettingTermuxPage() {
    val termuxWorkdirServerManager: TermuxWorkdirServerManager = koinInject()
    val settingsStore: SettingsStore = koinInject()
    val settings by settingsStore.settingsFlow.collectAsStateWithLifecycle()
    val termuxWorkdirServerState by termuxWorkdirServerManager.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var workdirText by remember(settings.termuxWorkdir) {
        mutableStateOf(settings.termuxWorkdir)
    }
    var workdirServerPortText by remember(settings.termuxWorkdirServerPort) {
        mutableStateOf(settings.termuxWorkdirServerPort.toString())
    }
    var timeoutText by remember(settings.termuxTimeoutMs) {
        mutableStateOf(settings.termuxTimeoutMs.toString())
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var allFilesAccessGranted by remember { mutableStateOf(isAllFilesAccessGranted()) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) {
                allFilesAccessGranted = isAllFilesAccessGranted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Termux 权限状态
    var termuxPermissionGranted by remember { mutableStateOf(false) }

    // 使用 Activity Result API 请求权限
    val termuxPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d(TAG, "权限请求结果: isGranted=$isGranted")
        termuxPermissionGranted = isGranted
        if (!isGranted) {
            // 权限被拒绝
            android.widget.Toast.makeText(
                context,
                "权限请求失败，请尝试在系统设置中手动授权",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    // 在页面加载时检查权限状态
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    "com.termux.permission.RUN_COMMAND"
                ) == PackageManager.PERMISSION_GRANTED
                termuxPermissionGranted = granted
                Log.d(TAG, "生命周期事件 $event, termuxPermissionGranted=$granted")
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 初始检查一次权限状态
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            "com.termux.permission.RUN_COMMAND"
        ) == PackageManager.PERMISSION_GRANTED
        termuxPermissionGranted = granted
        Log.d(TAG, "页面初始化, termuxPermissionGranted=$granted")
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.setting_termux_page_title)) },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding + PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item("workdir") {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    FormItem(
                        modifier = Modifier.padding(12.dp),
                        label = { Text(stringResource(R.string.setting_termux_page_workdir_title)) },
                        description = { Text(stringResource(R.string.setting_termux_page_workdir_desc)) },
                        content = {
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = workdirText,
                                onValueChange = { value ->
                                    workdirText = value
                                    scope.launch {
                                        settingsStore.update { it.copy(termuxWorkdir = value) }
                                    }
                                },
                                singleLine = true,
                            )
                        },
                    )
                }
            }

            item("workdirServer") {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    FormItem(
                        modifier = Modifier.padding(12.dp),
                        label = { Text(stringResource(R.string.setting_termux_page_workdir_server_title)) },
                        description = {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(stringResource(R.string.setting_termux_page_workdir_server_desc))
                                Text("http://127.0.0.1:${settings.termuxWorkdirServerPort}/")
                                if (!termuxWorkdirServerState.error.isNullOrBlank()) {
                                    Text(
                                        text = termuxWorkdirServerState.error!!,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        },
                        tail = {
                            Switch(
                                checked = settings.termuxWorkdirServerEnabled,
                                enabled = !termuxWorkdirServerState.isLoading,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        termuxWorkdirServerManager.start(
                                            port = settings.termuxWorkdirServerPort,
                                            workdir = settings.termuxWorkdir,
                                        )
                                    } else {
                                        termuxWorkdirServerManager.stop()
                                    }
                                    scope.launch {
                                        settingsStore.update { it.copy(termuxWorkdirServerEnabled = enabled) }
                                    }
                                },
                            )
                        },
                        content = {
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = workdirServerPortText,
                                onValueChange = { value ->
                                    workdirServerPortText = value.filter { it.isDigit() }
                                    val port = workdirServerPortText.toIntOrNull()
                                    if (port != null && port in 1024..65535) {
                                        scope.launch {
                                            settingsStore.update { it.copy(termuxWorkdirServerPort = port) }
                                        }
                                        if (settings.termuxWorkdirServerEnabled) {
                                            termuxWorkdirServerManager.restart(
                                                port = port,
                                                workdir = settings.termuxWorkdir,
                                            )
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                enabled = !termuxWorkdirServerState.isRunning && !termuxWorkdirServerState.isLoading,
                                isError = workdirServerPortText.toIntOrNull()?.let { it !in 1024..65535 } ?: true,
                                label = { Text(stringResource(R.string.setting_termux_page_workdir_server_port_title)) },
                            )
                        },
                    )
                }
            }

            item("background") {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    FormItem(
                        modifier = Modifier.padding(12.dp),
                        label = { Text(stringResource(R.string.setting_termux_page_background_title)) },
                        description = { Text(stringResource(R.string.setting_termux_page_background_desc)) },
                        tail = {
                            Switch(
                                checked = settings.termuxRunInBackground,
                                onCheckedChange = { enabled ->
                                    scope.launch {
                                        settingsStore.update { it.copy(termuxRunInBackground = enabled) }
                                    }
                                },
                            )
                        },
                    )
                }
            }

            item("approval") {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    FormItem(
                        modifier = Modifier.padding(12.dp),
                        label = { Text(stringResource(R.string.assistant_page_local_tools_termux_needs_approval_title)) },
                        description = { Text(stringResource(R.string.assistant_page_local_tools_termux_needs_approval_desc)) },
                        tail = {
                            Switch(
                                checked = settings.termuxNeedsApproval,
                                onCheckedChange = { enabled ->
                                    scope.launch {
                                        settingsStore.update { it.copy(termuxNeedsApproval = enabled) }
                                    }
                                },
                            )
                        },
                    )
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                item("allFilesAccess") {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ) {
                        FormItem(
                            modifier = Modifier.padding(12.dp),
                            label = { Text(stringResource(R.string.setting_termux_page_all_files_access_title)) },
                            description = {
                                Text(stringResource(R.string.setting_termux_page_all_files_access_desc))
                                Text(
                                    if (allFilesAccessGranted) {
                                        stringResource(R.string.setting_termux_page_all_files_access_granted)
                                    } else {
                                        stringResource(R.string.setting_termux_page_all_files_access_not_granted)
                                    }
                                )
                            },
                            tail = {
                                TextButton(
                                    onClick = {
                                        openAllFilesAccessSettings(context)
                                    }
                                ) {
                                    Text(stringResource(R.string.setting_termux_page_all_files_access_action))
                                }
                            },
                        )
                    }
                }
            }

            item("timeout") {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    FormItem(
                        modifier = Modifier.padding(12.dp),
                        label = { Text(stringResource(R.string.setting_termux_page_timeout_title)) },
                        description = { Text(stringResource(R.string.setting_termux_page_timeout_desc)) },
                        content = {
                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = timeoutText,
                                onValueChange = { value ->
                                    timeoutText = value.filter { it.isDigit() }
                                    val timeoutMs = timeoutText.toLongOrNull()
                                    if (timeoutMs != null && timeoutMs >= 1_000L) {
                                        scope.launch {
                                            settingsStore.update { it.copy(termuxTimeoutMs = timeoutMs) }
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                isError = timeoutText.toLongOrNull()?.let { it < 1_000L } ?: true,
                            )
                        },
                    )
                }
            }

            item("setup") {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.setting_termux_page_setup_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(stringResource(R.string.setting_termux_page_setup_step_1))
                        Text(stringResource(R.string.setting_termux_page_setup_step_2))
                        Text(
                            text = stringResource(R.string.setting_termux_page_setup_step_3),
                            color = if (termuxPermissionGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(stringResource(R.string.setting_termux_page_setup_step_4))
                        TextButton(
                            onClick = {
                                Log.d(TAG, "点击授权按钮，当前termuxPermissionGranted=$termuxPermissionGranted")
                                if (!termuxPermissionGranted) {
                                    termuxPermissionLauncher.launch("com.termux.permission.RUN_COMMAND")
                                }
                            },
                            enabled = !termuxPermissionGranted
                        ) {
                            Text(
                                text = if (termuxPermissionGranted) {
                                    stringResource(R.string.setting_termux_page_permission_already_granted)
                                } else {
                                    stringResource(R.string.setting_termux_page_grant_termux_permission)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun isAllFilesAccessGranted(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()
}

private fun openAllFilesAccessSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
    val packageUri = Uri.fromParts("package", context.packageName, null)
    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
        data = packageUri
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
    }
}

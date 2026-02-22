package me.rerere.rikkahub.data.ai.tools.slash

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import me.rerere.rikkahub.data.ai.tools.termux.TermuxCommandManager
import me.rerere.rikkahub.data.ai.tools.termux.TermuxResult
import me.rerere.rikkahub.data.ai.tools.termux.TermuxRunCommandRequest
import me.rerere.ai.ui.UIMessagePart
import me.rerere.ai.ui.UIMessagePart.Text
import java.util.UUID
import kotlin.coroutines.CoroutineContext

/**
 * 处理斜杠命令的处理器
 *
 * 当用户消息以 "/" 开头时，直接执行 Termux 命令，不调用 LLM
 */
class SlashCommandHandler(
    private val context: Context,
    private val termuxCommandManager: TermuxCommandManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    companion object {
        private const val SLASH_PREFIX = "/"
        private const val DEFAULT_TIMEOUT_MS = 30000L // 30秒

        // 内置命令
        private val BUILTIN_COMMANDS = mapOf(
            "help" to ::handleHelp,
            "version" to ::handleVersion,
        )
    }

    /**
     * 检查消息是否是斜杠命令
     */
    fun isSlashCommand(message: String): Boolean {
        return message.trimStart().startsWith(SLASH_PREFIX)
    }

    /**
     * 提取命令（移除开头的 '/'）
     */
    fun extractCommand(message: String): String {
        return message.trimStart().removePrefix(SLASH_PREFIX).trim()
    }

    /**
     * 处理斜杠命令并返回响应
     *
     * @param command 要执行的命令（完整命令字符串）
     * @return 消息部分列表，包含执行结果
     */
    suspend fun handleSlashCommand(command: String): List<UIMessagePart> = withContext(Dispatchers.IO) {
        val cmd = command.trim()
        
        // 检查是否是内置命令
        val parts = cmd.split(" ").filter { it.isNotBlank() }
        val commandName = parts.firstOrNull() ?: ""
        
        if (commandName in BUILTIN_COMMANDS) {
            return@withContext BUILTIN_COMMANDS[commandName]!!.invoke(parts.drop(1))
        }

        // 执行 Termux 命令
        executeTermuxCommand(cmd)
    }

    /**
     * 处理斜杠命令并返回流式响应（用于实时显示执行过程）
     *
     * @param command 要执行的命令
     * @return 流式响应，包含逐步输出
     */
    fun handleSlashCommandStreaming(command: String): Flow<List<UIMessagePart>> = flow {
        val cmd = command.trim()
        val parts = cmd.split(" ").filter { it.isNotBlank() }
        val commandName = parts.firstOrNull() ?: ""
        
        // 内置命令也通过 flow 发出
        if (commandName in BUILTIN_COMMANDS) {
            emit(BUILTIN_COMMANDS[commandName]!!.invoke(parts.drop(1)))
            return@flow
        }

        // 执行 Termux 命令
        emit(listOf(createTextPart("Executing: $cmd")))
        val result = executeTermuxCommand(cmd)
        emit(result)
    }

    /**
     * 执行 Termux 命令
     */
    private suspend fun executeTermuxCommand(command: String): List<UIMessagePart> {
        if (command.isBlank()) {
            return listOf(createTextPart("Error: Empty command"))
        }

        // 解析命令和参数
        val parts = command.split(" ").filter { it.isNotBlank() }
        val commandPath = parts.firstOrNull() ?: ""
        val arguments = parts.drop(1).toTypedArray()

        if (commandPath.isBlank()) {
            return listOf(createTextPart("Error: No command specified"))
        }

        // 创建请求
        val request = TermuxRunCommandRequest(
            commandPath = commandPath,
            arguments = arguments.toList(),
            workdir = "", // 使用 Termux 默认工作目录
            background = false,
            timeoutMs = DEFAULT_TIMEOUT_MS
        )

        try {
            // 执行命令
            val result = termuxCommandManager.run(request)
            // 构建响应消息部分
            buildResponseParts(result)
        } catch (e: Exception) {
            listOf(createTextPart("Error: ${e.message}"))
        }
    }

    /**
     * 从 Termux 结果构建响应部分列表
     */
    private fun buildResponseParts(result: TermuxResult): List<UIMessagePart> {
        val parts = mutableListOf<UIMessagePart>()

        // 如果超时或出错，显示错误信息
        if (result.timedOut) {
            parts.add(createTextPart("Command timed out after ${DEFAULT_TIMEOUT_MS / 1000} seconds"))
            return parts
        }

        if (result.err != null) {
            parts.add(createTextPart("Error executing command: ${result.errMsg ?: "Unknown error"}"))
            return parts
        }

        // 添加标准输出
        val stdout = result.stdout?.trim()
        if (!stdout.isNullOrEmpty()) {
            parts.add(createTextPart(stdout))
        }

        // 如果有标准错误输出，也显示
        val stderr = result.stderr?.trim()
        if (!stderr.isNullOrEmpty()) {
            // 只有当 stdout 为空或者两者都有时，才显示 stderr 标签
            if (stdout.isNullOrEmpty()) {
                parts.add(createTextPart(stderr))
            } else {
                parts.add(createTextPart("Errors:\n$stderr"))
            }
        }

        // 如果都没有输出，显示成功信息
        if (stdout.isNullOrEmpty() && stderr.isNullOrEmpty()) {
            parts.add(createTextPart("Command executed successfully"))
        }

        return parts
    }

    /**
     * 处理 help 命令
     */
    private fun handleHelp(args: List<String>): List<UIMessagePart> {
        val helpText = """
            Slash Commands:
            /help - Show this help message
            /version - Show app version
            /<command> - Execute any Termux command
            
            Examples:
            /ls - List files
            /pwd - Print working directory
            /echo hello - Print text
            /termux-toast "Hello" - Show toast notification
            
            Note: Termux must be installed and the permission granted.
        """.trimIndent()
        return listOf(createTextPart(helpText))
    }

    /**
     * 处理 version 命令
     */
    private fun handleVersion(args: List<String>): List<UIMessagePart> {
        val version = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        return listOf(createTextPart("RikkaHub Version: $version"))
    }

    /**
     * 创建文本消息部分
     */
    private fun createTextPart(text: String): Text {
        return Text(text = text)
    }
}


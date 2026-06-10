package me.rerere.rikkahub.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.rerere.ai.core.MessageRole
import me.rerere.ai.provider.CustomBody
import me.rerere.ai.provider.CustomHeader
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.core.ReasoningLevel
import me.rerere.rikkahub.data.ai.tools.LocalToolOption
import me.rerere.rikkahub.data.datastore.Settings
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import kotlin.uuid.Uuid

private val DEFAULT_SCHEDULED_TASK_ASSISTANT_ID = Uuid.parse("0950e2dc-9bd5-4801-afa3-aa887aa36b4e")
const val ASSISTANT_TOOL_CALL_KEEP_MESSAGES_SLIDER_MAX = 32
private const val COMPILED_ASSISTANT_REGEX_CACHE_LIMIT = 256
private val compiledAssistantRegexCache = ConcurrentHashMap<AssistantRegexCacheKey, Regex>()

private data class AssistantRegexCacheKey(
    val pattern: String,
    val options: Set<RegexOption>,
)

@Serializable
data class Assistant(
    val id: Uuid = Uuid.random(),
    val chatModelId: Uuid? = null, // 如果为null, 使用全局默认模型
    val name: String = "",
    val avatar: Avatar = Avatar.Dummy,
    val useAssistantAvatar: Boolean = false, // 使用助手头像替代模型头像
    val tags: List<Uuid> = emptyList(),
    val userPersona: String = "",
    val systemPrompt: String = "",
    val temperature: Float? = null,
    val topP: Float? = null,
    val contextMessageSize: Int = 0,
    val toolCallKeepMessages: Int = ASSISTANT_TOOL_CALL_KEEP_MESSAGES_SLIDER_MAX,
    val streamOutput: Boolean = true,
    val enableMemory: Boolean = false,
    val useGlobalMemory: Boolean = false, // 使用全局共享记忆而非助手隔离记忆
    val enableRecentChatsReference: Boolean = false,
    val presetMessages: List<UIMessage> = emptyList(),
    val quickMessageIds: Set<Uuid> = emptySet(),
    val scheduledPromptTasks: List<ScheduledPromptTask> = emptyList(),
    val regexEnabled: Boolean = true,
    val regexes: List<AssistantRegex> = emptyList(),
    val reasoningLevel: ReasoningLevel = ReasoningLevel.AUTO,
    val maxTokens: Int? = null,
    val frequencyPenalty: Float? = null,
    val presencePenalty: Float? = null,
    val minP: Float? = null,
    val topK: Int? = null,
    val topA: Float? = null,
    val repetitionPenalty: Float? = null,
    val seed: Long? = null,
    val stopSequences: List<String> = emptyList(),
    val googleResponseMimeType: String = "",
    val customHeaders: List<CustomHeader> = emptyList(),
    val customBodies: List<CustomBody> = emptyList(),
    val mcpServers: Set<Uuid> = emptySet(),
    val localTools: List<LocalToolOption> = listOf(LocalToolOption.TimeInfo),
    val skillsEnabled: Boolean = false,
    val selectedSkills: Set<String> = emptySet(),
    val termuxNeedsApproval: Boolean = true,
    val background: String? = null,
    val backgroundOpacity: Float = 1.0f,
    val backgroundBlur: Float = 0f,
    val useGradientBackground: Boolean = false, // 开启后聊天页使用动态渐变背景
    val modeInjectionIds: Set<Uuid> = emptySet(),      // 关联的模式注入 ID
    val enableTimeReminder: Boolean = false,            // 时间间隔提醒注入
    val openAIReasoningEffort: String = "",
    val reasoningSummary: String = "",
    val openAIVerbosity: String = "",
    val allowConversationSystemPrompt: Boolean = false, // 允许对话单独重写 system prompt
    val allowConversationPromptInjection: Boolean = false, // 允许对话单独绑定模式注入
    val stCharacterData: SillyTavernCharacterData? = null,
)

fun Assistant.resolveToolCallKeepMessagesLimit(): Int? {
    val normalized = toolCallKeepMessages.coerceIn(1, ASSISTANT_TOOL_CALL_KEEP_MESSAGES_SLIDER_MAX)
    return normalized.takeUnless { it >= ASSISTANT_TOOL_CALL_KEEP_MESSAGES_SLIDER_MAX }
}

fun Assistant.resolveConversationStarterMessages(
    random: Random = Random.Default,
): List<UIMessage> {
    val greetingCandidates = buildList {
        stCharacterData?.firstMessage
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let(::add)
        stCharacterData?.alternateGreetings
            .orEmpty()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach(::add)
    }.distinct()
    if (greetingCandidates.isEmpty()) return presetMessages

    val selectedGreeting = greetingCandidates.random(random)
    if (presetMessages.isEmpty()) {
        return listOf(UIMessage.assistant(selectedGreeting))
    }

    var replacedGreeting = false
    val updatedPresetMessages = presetMessages.map { message ->
        if (!replacedGreeting && message.role == MessageRole.ASSISTANT) {
            replacedGreeting = true
            UIMessage.assistant(selectedGreeting)
        } else {
            message
        }
    }
    return if (replacedGreeting) {
        updatedPresetMessages
    } else {
        updatedPresetMessages + UIMessage.assistant(selectedGreeting)
    }
}

@Serializable
data class ScheduledPromptTask(
    val id: Uuid = Uuid.random(),
    val enabled: Boolean = true,
    val title: String = "",
    val prompt: String = "",
    val scheduleType: ScheduleType = ScheduleType.DAILY,
    val timeMinutesOfDay: Int = 9 * 60,
    val dayOfWeek: Int? = null, // 1..7, Monday..Sunday, only used when scheduleType == WEEKLY
    val assistantId: Uuid = DEFAULT_SCHEDULED_TASK_ASSISTANT_ID,
    val overrideModelId: Uuid? = null,
    val overrideLocalTools: List<LocalToolOption>? = null,
    val overrideMcpServers: Set<Uuid>? = null,
    val overrideEnableWebSearch: Boolean? = null,
    val overrideSearchServiceIndex: Int? = null,
    val overrideTermuxNeedsApproval: Boolean? = null,
    @Deprecated("Scheduled tasks now run in isolated snapshots and no longer use chat conversations")
    val conversationId: Uuid = Uuid.random(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastRunAt: Long = 0L,
    val lastStatus: TaskRunStatus = TaskRunStatus.IDLE,
    val lastError: String = "",
    val lastRunId: Uuid? = null,
)

@Serializable
enum class ScheduleType {
    DAILY,
    WEEKLY,
}

@Serializable
enum class TaskRunStatus {
    IDLE,
    RUNNING,
    SUCCESS,
    FAILED,
}

@Serializable
data class QuickMessage(
    val id: Uuid = Uuid.random(),
    val title: String = "",
    val content: String = "",
)

@Serializable
data class AssistantMemory(
    val id: Int,
    val content: String = "",
)

@Serializable
enum class AssistantAffectScope {
    SYSTEM,
    USER,
    ASSISTANT,
}

@Serializable
enum class AssistantRegexSourceKind {
    MANUAL,
    ST_SCRIPT,
    ST_INLINE_PROMPT,
}

@Serializable
data class AssistantRegex(
    val id: Uuid,
    val name: String = "",
    val enabled: Boolean = true,
    val findRegex: String = "", // 正则表达式
    val rawFindRegex: String = "",
    val replaceString: String = "", // 替换字符串
    val affectingScope: Set<AssistantAffectScope> = setOf(),
    val visualOnly: Boolean = false, // 是否仅在视觉上影响
    val promptOnly: Boolean = false, // 是否仅影响发送给 LLM 的提示词
    val minDepth: Int? = null, // 最小深度：仅在倒数第 x 条及更早消息生效（包含 x）
    val maxDepth: Int? = null, // 最大深度：仅在倒数第 x 条消息内生效（包含 x）
    val trimStrings: List<String> = emptyList(),
    val runOnEdit: Boolean = true,
    val substituteRegex: Int = AssistantRegexSubstituteStrategy.NONE,
    val stPlacements: Set<Int> = emptySet(),
    val sourceKind: AssistantRegexSourceKind = AssistantRegexSourceKind.MANUAL,
    val sourceRef: String = "",
)

fun AssistantRegex.dedupKey(): String {
    return listOf(
        name,
        exportFindRegex(),
        replaceString,
        affectingScope.sortedBy { scope -> scope.name }.joinToString(","),
        visualOnly.toString(),
        promptOnly.toString(),
        minDepth?.toString().orEmpty(),
        maxDepth?.toString().orEmpty(),
        trimStrings.joinToString("\u0000"),
        runOnEdit.toString(),
        substituteRegex.toString(),
        stPlacements.sorted().joinToString(","),
    ).joinToString("|")
}

fun AssistantRegex.sourceLabel(): String? {
    return when (sourceKind) {
        AssistantRegexSourceKind.MANUAL -> null
        AssistantRegexSourceKind.ST_SCRIPT -> "SillyTavern Script"
        AssistantRegexSourceKind.ST_INLINE_PROMPT -> {
            val promptRef = sourceRef.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
            "SillyTavern Inline Prompt$promptRef"
        }
    }
}

enum class AssistantRegexApplyPhase {
    ACTUAL_MESSAGE, // 实际消息（会影响保存与后续上下文）
    VISUAL_ONLY, // 仅视觉渲染
    PROMPT_ONLY, // 仅发送给 LLM 的提示词
}

object AssistantRegexPlacement {
    const val USER_INPUT = 1
    const val AI_OUTPUT = 2
    const val SLASH_COMMAND = 3
    const val REASONING = 6
}

object AssistantRegexSubstituteStrategy {
    const val NONE = 0
    const val RAW = 1
    const val ESCAPED = 2
}

fun List<UIMessage>.chatMessageDepthFromEndMap(): Map<Int, Int> {
    val chatIndices = mapIndexedNotNull { index, message ->
        if (message.role == MessageRole.USER || message.role == MessageRole.ASSISTANT) index else null
    }
    return chatIndices.mapIndexed { chatIndex, messageIndex ->
        messageIndex to (chatIndices.size - chatIndex)
    }.toMap()
}

fun String.replaceRegexes(
    assistant: Assistant?,
    settings: Settings? = null,
    scope: AssistantAffectScope,
    phase: AssistantRegexApplyPhase = AssistantRegexApplyPhase.ACTUAL_MESSAGE,
    messageDepthFromEnd: Int? = null,
    placement: Int? = null,
    isEdit: Boolean = false,
    characterOverride: String? = null,
): String {
    val applicableRegexes = applicableRegexes(
        assistant = assistant,
        settings = settings,
        scope = scope,
        phase = phase,
        messageDepthFromEnd = messageDepthFromEnd,
        placement = placement,
        isEdit = isEdit,
    )
    if (applicableRegexes.isEmpty()) return this

    return applicableRegexes.fold(this) { acc, regex ->
        try {
            val patternSpec = regex.resolveFindRegexPattern(
                assistant = assistant,
                settings = settings,
                characterOverride = characterOverride,
            ) ?: return@fold acc
            val compiledRegex = patternSpec.cachedRegex()
            val result = if (regex.requiresCustomReplacement()) {
                if (patternSpec.replaceAll) {
                    compiledRegex.replace(acc) { matchResult ->
                        regex.buildReplacement(
                            matchResult = matchResult,
                            assistant = assistant,
                            settings = settings,
                            characterOverride = characterOverride,
                        )
                    }
                } else {
                    compiledRegex.replaceFirst(acc) { matchResult ->
                        regex.buildReplacement(
                            matchResult = matchResult,
                            assistant = assistant,
                            settings = settings,
                            characterOverride = characterOverride,
                        )
                    }
                }
            } else {
                if (patternSpec.replaceAll) {
                    acc.replace(
                        regex = compiledRegex,
                        replacement = regex.replaceString,
                    )
                } else {
                    compiledRegex.replaceFirst(acc, regex.replaceString)
                }
            }
            result
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果正则表达式格式错误，返回原字符串
            acc
        }
    }
}

fun Settings.effectiveRegexes(assistant: Assistant?): List<AssistantRegex> {
    val orderedRegexes = assistant
        ?.takeIf { it.regexEnabled }
        ?.regexes
        .orEmpty()

    // Later scopes should be able to override earlier duplicates with the same runtime behavior.
    return orderedRegexes
        .asReversed()
        .distinctBy { it.dedupKey() }
        .asReversed()
}

fun Settings.hasApplicableRegexes(
    assistant: Assistant?,
    scope: AssistantAffectScope,
    phase: AssistantRegexApplyPhase,
    messageDepthFromEnd: Int? = null,
    placement: Int? = null,
    isEdit: Boolean = false,
): Boolean {
    return applicableRegexes(
        assistant = assistant,
        settings = this,
        scope = scope,
        phase = phase,
        messageDepthFromEnd = messageDepthFromEnd,
        placement = placement,
        isEdit = isEdit,
    ).isNotEmpty()
}

private fun applicableRegexes(
    assistant: Assistant?,
    settings: Settings?,
    scope: AssistantAffectScope,
    phase: AssistantRegexApplyPhase,
    messageDepthFromEnd: Int?,
    placement: Int?,
    isEdit: Boolean,
): List<AssistantRegex> {
    val effectiveRegexes = settings?.effectiveRegexes(assistant)
        ?: assistant
            ?.takeIf { it.regexEnabled }
            ?.regexes
            .orEmpty()
    if (effectiveRegexes.isEmpty()) return emptyList()

    return effectiveRegexes.filter { regex ->
        regex.enabled &&
            regex.matchesPlacement(
                scope = scope,
                phase = phase,
                placement = placement,
                isEdit = isEdit,
            ) &&
            regex.matchesDepth(messageDepthFromEnd)
    }
}

private fun AssistantRegexPatternSpec.cachedRegex(): Regex {
    val key = AssistantRegexCacheKey(pattern = pattern, options = options)
    return compiledAssistantRegexCache.getOrPut(key) {
        if (compiledAssistantRegexCache.size >= COMPILED_ASSISTANT_REGEX_CACHE_LIMIT) {
            compiledAssistantRegexCache.clear()
        }
        Regex(pattern, options)
    }
}

private fun AssistantRegex.matchesPlacement(
    scope: AssistantAffectScope,
    phase: AssistantRegexApplyPhase,
    placement: Int?,
    isEdit: Boolean,
): Boolean {
    if (isEdit && !runOnEdit) return false
    if (stPlacements.isNotEmpty()) {
        if (placement == null || placement !in stPlacements) return false
        return when (phase) {
            AssistantRegexApplyPhase.ACTUAL_MESSAGE -> !visualOnly && !promptOnly
            AssistantRegexApplyPhase.VISUAL_ONLY -> visualOnly
            AssistantRegexApplyPhase.PROMPT_ONLY -> promptOnly
        }
    }
    if (!affectingScope.contains(scope)) return false
    return when (phase) {
        AssistantRegexApplyPhase.ACTUAL_MESSAGE -> !visualOnly && !promptOnly
        AssistantRegexApplyPhase.VISUAL_ONLY -> visualOnly
        AssistantRegexApplyPhase.PROMPT_ONLY -> promptOnly
    }
}

private fun AssistantRegex.matchesDepth(messageDepthFromEnd: Int?): Boolean {
    val depth = messageDepthFromEnd ?: return true
    val effectiveMinDepth = minDepth?.takeIf { it > 0 }
    val effectiveMaxDepth = maxDepth?.takeIf { it > 0 }
    if (effectiveMinDepth != null && depth < effectiveMinDepth) return false
    if (effectiveMaxDepth != null && depth > effectiveMaxDepth) return false
    return true
}

private fun AssistantRegex.resolveFindRegexPattern(
    assistant: Assistant?,
    settings: Settings?,
    characterOverride: String?,
): AssistantRegexPatternSpec? {
    val resolvedSource = when (substituteRegex) {
        AssistantRegexSubstituteStrategy.RAW -> {
            substituteRegexMacros(
                text = exportFindRegex(),
                assistant = assistant,
                settings = settings,
                characterOverride = characterOverride,
                escapeReplacement = false,
            )
        }

        AssistantRegexSubstituteStrategy.ESCAPED -> {
            substituteRegexMacros(
                text = exportFindRegex(),
                assistant = assistant,
                settings = settings,
                characterOverride = characterOverride,
                escapeReplacement = true,
            )
        }

        else -> exportFindRegex()
    }
    return resolveAssistantRegexPatternSpec(
        source = resolvedSource,
        stCompatible = shouldUseStRegexCompatibility(),
    )
}

private fun AssistantRegex.requiresCustomReplacement(): Boolean {
    return trimStrings.isNotEmpty() ||
        stPlacements.isNotEmpty() ||
        substituteRegex != AssistantRegexSubstituteStrategy.NONE ||
        replaceString.contains("{{match}}", ignoreCase = true) ||
        replaceString.contains("\$<") ||
        replaceString.contains("\${")
}

private fun AssistantRegex.buildReplacement(
    matchResult: MatchResult,
    assistant: Assistant?,
    settings: Settings?,
    characterOverride: String?,
): String {
    val template = replaceString.replace(Regex("\\{\\{match}}", RegexOption.IGNORE_CASE), "\$0")
    val resolved = StringBuilder()
    var index = 0
    while (index < template.length) {
        val current = template[index]
        if (current != '$') {
            resolved.append(current)
            index++
            continue
        }

        if (index + 1 >= template.length) {
            resolved.append(current)
            index++
            continue
        }

        val next = template[index + 1]
        when {
            next == '$' -> {
                resolved.append('$')
                index += 2
            }

            next == '<' -> {
                val closing = template.indexOf('>', startIndex = index + 2)
                if (closing > index + 2) {
                    val groupName = template.substring(index + 2, closing)
                    val groupValue = matchResult.groups[groupName]?.value.orEmpty()
                    resolved.append(filterTrimStrings(groupValue, assistant, settings, characterOverride))
                    index = closing + 1
                } else {
                    resolved.append('$')
                    index++
                }
            }

            next == '{' -> {
                val closing = template.indexOf('}', startIndex = index + 2)
                if (closing > index + 2) {
                    val groupName = template.substring(index + 2, closing)
                    val groupValue = matchResult.groups[groupName]?.value.orEmpty()
                    resolved.append(filterTrimStrings(groupValue, assistant, settings, characterOverride))
                    index = closing + 1
                } else {
                    resolved.append('$')
                    index++
                }
            }

            next.isDigit() -> {
                var end = index + 2
                while (end < template.length && template[end].isDigit()) {
                    end++
                }
                val groupIndex = template.substring(index + 1, end).toIntOrNull()
                val groupValue = groupIndex
                    ?.let { resolvedGroupIndex -> matchResult.groups[resolvedGroupIndex]?.value }
                    .orEmpty()
                resolved.append(filterTrimStrings(groupValue, assistant, settings, characterOverride))
                index = end
            }

            else -> {
                resolved.append('$')
                index++
            }
        }
    }

    return substituteRegexMacros(
        text = resolved.toString(),
        assistant = assistant,
        settings = settings,
        characterOverride = characterOverride,
        escapeReplacement = false,
    )
}

private fun AssistantRegex.filterTrimStrings(
    value: String,
    assistant: Assistant?,
    settings: Settings?,
    characterOverride: String?,
): String {
    if (value.isEmpty() || trimStrings.isEmpty()) return value
    return trimStrings.fold(value) { acc, trimString ->
        val resolvedTrim = substituteRegexMacros(
            text = trimString,
            assistant = assistant,
            settings = settings,
            characterOverride = characterOverride,
            escapeReplacement = false,
        )
        if (resolvedTrim.isEmpty()) {
            acc
        } else {
            acc.replace(resolvedTrim, "")
        }
    }
}

private fun Regex.replaceFirst(
    input: String,
    transform: (MatchResult) -> String,
): String {
    val match = find(input) ?: return input
    return input.replaceRange(match.range, transform(match))
}

private fun substituteRegexMacros(
    text: String,
    assistant: Assistant?,
    settings: Settings?,
    characterOverride: String?,
    escapeReplacement: Boolean,
): String {
    if (text.isEmpty()) return text
    val characterName = characterOverride
        ?.takeIf { it.isNotBlank() }
        ?: assistant?.stCharacterData?.name
            ?.takeIf { it.isNotBlank() }
        ?: assistant?.name
            ?.takeIf { it.isNotBlank() }
        ?: "char"
    val userName = settings?.effectiveUserName()
        ?.takeIf { it.isNotBlank() }
        ?: "user"
    val persona = settings?.effectiveUserPersona(assistant)
        ?.takeIf { it.isNotBlank() }
        ?: assistant?.userPersona
            ?.takeIf { it.isNotBlank() }
            .orEmpty()
    val replacements = mapOf(
        "char" to characterName,
        "bot" to characterName,
        "name2" to characterName,
        "user" to userName,
        "name1" to userName,
        "persona" to persona,
        "personaDescription" to persona,
    ).mapValues { (_, value) ->
        if (escapeReplacement) Regex.escape(value) else value
    }
    return replacements.entries.fold(text) { acc, (key, value) ->
        acc
            .replace("{{$key}}", value, ignoreCase = true)
            .replace("{${key}}", value, ignoreCase = true)
    }
}

/**
 * 注入位置
 */
@Serializable
enum class InjectionPosition {
    @SerialName("before_system_prompt")
    BEFORE_SYSTEM_PROMPT,

    @SerialName("after_system_prompt")
    AFTER_SYSTEM_PROMPT,

    @SerialName("author_note_top")
    AUTHOR_NOTE_TOP,

    @SerialName("author_note_bottom")
    AUTHOR_NOTE_BOTTOM,

    @SerialName("top_of_chat")
    TOP_OF_CHAT,

    @SerialName("bottom_of_chat")
    BOTTOM_OF_CHAT,

    @SerialName("at_depth")
    AT_DEPTH,

    @SerialName("example_messages_top")
    EXAMPLE_MESSAGES_TOP,

    @SerialName("example_messages_bottom")
    EXAMPLE_MESSAGES_BOTTOM,

    @SerialName("outlet")
    OUTLET,
}

/**
 * 模式注入
 */
@Serializable
sealed class PromptInjection {
    abstract val id: Uuid
    abstract val name: String
    abstract val enabled: Boolean
    abstract val priority: Int
    abstract val position: InjectionPosition
    abstract val content: String
    abstract val injectDepth: Int
    abstract val role: MessageRole

    /**
     * 模式注入 - 基于开关状态触发，作为系统提示词补充
     */
    @Serializable
    @SerialName("mode")
    data class ModeInjection(
        override val id: Uuid = Uuid.random(),
        override val name: String = "",
        override val enabled: Boolean = true,
        override val priority: Int = 0,
        override val position: InjectionPosition = InjectionPosition.AFTER_SYSTEM_PROMPT,
        override val content: String = "",
        override val injectDepth: Int = 4,
        override val role: MessageRole = MessageRole.SYSTEM,
    ) : PromptInjection()
}

fun InjectionPosition.normalizeForModeInjection(): InjectionPosition = when (this) {
    InjectionPosition.BEFORE_SYSTEM_PROMPT -> InjectionPosition.BEFORE_SYSTEM_PROMPT
    InjectionPosition.AFTER_SYSTEM_PROMPT,
    InjectionPosition.AUTHOR_NOTE_TOP,
    InjectionPosition.AUTHOR_NOTE_BOTTOM,
    InjectionPosition.TOP_OF_CHAT,
    InjectionPosition.BOTTOM_OF_CHAT,
    InjectionPosition.AT_DEPTH,
    InjectionPosition.EXAMPLE_MESSAGES_TOP,
    InjectionPosition.EXAMPLE_MESSAGES_BOTTOM,
    InjectionPosition.OUTLET,
    -> InjectionPosition.AFTER_SYSTEM_PROMPT
}

fun PromptInjection.ModeInjection.normalizedForSystemPromptSupplement(): PromptInjection.ModeInjection = copy(
    position = position.normalizeForModeInjection(),
    injectDepth = 4,
    role = MessageRole.SYSTEM,
)

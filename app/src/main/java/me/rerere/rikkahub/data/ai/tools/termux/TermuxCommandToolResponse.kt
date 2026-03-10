package me.rerere.rikkahub.data.ai.tools.termux

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class TermuxCommandToolResponse(
    val output: String = "",
    @SerialName("exit_code")
    val exitCode: Int? = null,
    @SerialName("err_code")
    val errCode: Int? = null,
    @SerialName("stdout_original_length")
    val stdoutOriginalLength: Int? = null,
    @SerialName("stderr_original_length")
    val stderrOriginalLength: Int? = null,
    @SerialName("timed_out")
    val timedOut: Boolean = false,
    val truncated: Boolean = false,
    val error: String? = null,
    val success: Boolean = true,
)

internal fun TermuxResult.toToolResponse(): TermuxCommandToolResponse {
    val error = TermuxOutputFormatter.statusSummary(this).takeIf { it.isNotBlank() }
    return TermuxCommandToolResponse(
        output = TermuxOutputFormatter.merge(stdout = stdout, stderr = stderr),
        exitCode = exitCode,
        errCode = errCode,
        stdoutOriginalLength = stdoutOriginalLength,
        stderrOriginalLength = stderrOriginalLength,
        timedOut = timedOut,
        truncated = isOutputTruncated(),
        error = error,
        success = isSuccessful(),
    )
}

internal fun TermuxResult.isOutputTruncated(): Boolean {
    return listOf(
        stdoutOriginalLength?.let { it > stdout.length },
        stderrOriginalLength?.let { it > stderr.length },
    ).any { it == true }
}

internal fun Throwable.toToolResponse(setupChecklist: String? = null): TermuxCommandToolResponse {
    return TermuxCommandToolResponse(
        error = buildString {
            append(message ?: javaClass.name)
            setupChecklist?.trim()?.takeIf { it.isNotBlank() }?.let {
                append('\n')
                append(it)
            }
        },
        success = false,
    )
}

fun TermuxCommandToolResponse.encode(json: Json): String {
    return json.encodeToString(this)
}

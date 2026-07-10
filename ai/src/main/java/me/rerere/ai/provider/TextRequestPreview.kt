package me.rerere.ai.provider

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import okhttp3.Headers

@Serializable
data class TextRequestHeader(
    val name: String,
    val value: String,
)

@Serializable
data class TextRequestPreview(
    val providerName: String,
    val apiName: String,
    val method: String = "POST",
    val url: String,
    val stream: Boolean,
    val headers: List<TextRequestHeader>,
    val body: JsonObject,
)

fun Headers.toPreviewHeaders(): List<TextRequestHeader> {
    return List(size) { index ->
        TextRequestHeader(
            name = name(index),
            value = value(index),
        )
    }
}

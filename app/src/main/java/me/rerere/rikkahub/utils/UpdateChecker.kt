package me.rerere.rikkahub.utils

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

// DISABLED: This class is modified to disable update checking
// Original API_URL: https://updates.rikka-ai.com/
private const val API_URL = "https://updates.rikka-ai.com/"

class UpdateChecker(private val client: OkHttpClient) {
    private val json = Json { ignoreUnknownKeys = true }

    // DISABLED: Always returns a Flow that immediately emits an error state
    // Update checking is completely disabled to prevent network access
    fun checkUpdate(): Flow<UiState<UpdateInfo>> = flow<UiState<UpdateInfo>> {
        emit(UiState.Loading)
        // Immediately fail - updates are disabled
        throw Exception("Update checking is disabled")
    }.catch { e ->
        emit(UiState.Error(e))
    }.flowOn(Dispatchers.IO)

    // DISABLED: Download functionality is also disabled
    fun downloadUpdate(context: Context, download: UpdateDownload) {
        Toast.makeText(context, "Update feature is disabled", Toast.LENGTH_SHORT).show()
    }
}

@Serializable
data class UpdateDownload(
    val name: String,
    val url: String
)

@Serializable
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val changelog: String,
    val downloads: List<UpdateDownload>
)

package me.rerere.rikkahub.data.api

import me.rerere.rikkahub.data.model.Sponsor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.GET

// DISABLED: This API is modified to prevent access to sponsors.rikka-ai.com
// All methods now return empty data instead of making network requests
interface SponsorAPI {
    @GET("/sponsors")
    suspend fun getSponsors(): List<Sponsor> {
        // Sponsor API is disabled - return empty list
        return emptyList()
    }

    companion object {
        fun create(httpClient: OkHttpClient): SponsorAPI {
            // Return a mock implementation that returns empty data
            return object : SponsorAPI {}
        }
    }
}


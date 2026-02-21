package me.rerere.rikkahub.data.ai

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

// Firebase Remote Config REMOVED
// This interceptor now does nothing, but kept for compatibility
class AIRequestInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        // No Firebase Remote Config functionality
        return chain.proceed(request)
    }
}


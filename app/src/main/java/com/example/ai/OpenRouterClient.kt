package com.example.ai

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class OpenRouterMessage(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class OpenRouterChatRequest(
    @Json(name = "model") val model: String = "google/gemini-3.5-flash-lite",
    @Json(name = "messages") val messages: List<OpenRouterMessage>,
    @Json(name = "temperature") val temperature: Double = 0.1,
    @Json(name = "max_tokens") val maxTokens: Int = 200
)

@JsonClass(generateAdapter = true)
data class OpenRouterChatChoice(
    @Json(name = "message") val message: OpenRouterMessage?
)

@JsonClass(generateAdapter = true)
data class OpenRouterChatResponse(
    @Json(name = "id") val id: String?,
    @Json(name = "choices") val choices: List<OpenRouterChatChoice>?
)

interface OpenRouterApi {
    @POST("api/v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Header("HTTP-Referer") referer: String = "https://ai.studio",
        @Header("X-Title") title: String = "SAVIO Spend Tracker",
        @Body request: OpenRouterChatRequest
    ): OpenRouterChatResponse
}

object OpenRouterClient {
    private const val BASE_URL = "https://openrouter.ai/"

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    val api: OpenRouterApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(OpenRouterApi::class.java)
    }
}

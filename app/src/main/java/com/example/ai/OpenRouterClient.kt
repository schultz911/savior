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
    @param:Json(name = "role") val role: String? = "user",
    @param:Json(name = "content") val content: String? = "",
    @param:Json(name = "reasoning") val reasoning: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterReasoning(
    @param:Json(name = "effort") val effort: String = "minimal"
)

@JsonClass(generateAdapter = true)
data class OpenRouterChatRequest(
    @param:Json(name = "model") val model: String = "google/gemini-3.5-flash-lite",
    @param:Json(name = "messages") val messages: List<OpenRouterMessage>,
    @param:Json(name = "temperature") val temperature: Double = 0.0,
    @param:Json(name = "max_tokens") val maxTokens: Int = 1000,
    @param:Json(name = "reasoning") val reasoning: OpenRouterReasoning? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterChatChoice(
    @param:Json(name = "message") val message: OpenRouterMessage?,
    @param:Json(name = "finish_reason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterError(
    @param:Json(name = "code") val code: Any? = null,
    @param:Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterChatResponse(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "choices") val choices: List<OpenRouterChatChoice>? = null,
    @param:Json(name = "error") val error: OpenRouterError? = null
)

interface OpenRouterApi {
    @POST("api/v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Header("HTTP-Referer") referer: String = "https://ai.studio",
        @Header("X-Title") title: String = "Savio Spend Tracker",
        @Body request: OpenRouterChatRequest
    ): OpenRouterChatResponse
}

object OpenRouterClient {
    private const val BASE_URL = "https://openrouter.ai/"

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (com.example.BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
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

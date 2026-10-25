package com.example.ai

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

object CloudRetryControl {
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    internal val generation = AtomicInteger()

    fun retry() { generation.incrementAndGet(); _message.value = null }
    internal fun report(message: String?) { _message.value = message }
}

internal class CloudRequestSuppressedException(message: String) : Exception(message)

/** One inbox scan (or one live SMS). Never caches transaction results or changes merchant rules. */
internal class CloudRequestSession(
    private val now: () -> Long = android.os.SystemClock::elapsedRealtime
) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<CloudRequestSession>
    private data class Failure(val until: Long, val message: String)
    private val failures = mutableMapOf<String, Failure>()
    private val mutex = Mutex()
    private var generation = CloudRetryControl.generation.get()

    suspend fun <T> execute(key: String, model: String, request: suspend () -> T): T = mutex.withLock {
        val currentGeneration = CloudRetryControl.generation.get()
        if (generation != currentGeneration) { failures.clear(); generation = currentGeneration }
        // Keep only an irreversible fingerprint of credentials in failure state.
        val configuration = MessageDigest.getInstance("SHA-256").digest((key + "\u0000" + model).toByteArray())
            .joinToString("") { "%02x".format(it) }
        failures[configuration]?.let {
            if (now() < it.until) throw CloudRequestSuppressedException(it.message)
            failures.remove(configuration)
        }
        try {
            request().also { CloudRetryControl.report(null) }
        } catch (e: HttpException) {
            val message = when (e.code()) {
                401 -> "Cloud AI rejected the API key. Offline processing is active for this scan. Check the key or retry."
                402 -> "Cloud AI reported an account limit. Offline processing is active for this scan. Check your account or retry."
                429 -> "Cloud AI is rate limited. Offline processing is active while it cools down."
                else -> null
            }
            if (message != null) {
                val until = if (e.code() == 429) now() + retryDelay(e.response()?.headers()?.get("Retry-After")) else Long.MAX_VALUE
                failures[configuration] = Failure(until, message)
                CloudRetryControl.report(message)
            }
            throw e
        }
    }

    private fun retryDelay(header: String?): Long {
        val seconds = header?.trim()?.toLongOrNull()
        val delay = if (seconds != null) seconds.coerceIn(0L, 900L) * 1000L else {
            val date = runCatching {
                java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.US).parse(header ?: "")
            }.getOrNull()
            date?.let { it.time - System.currentTimeMillis() } ?: 30_000L
        }
        return delay.coerceIn(1000L, 900_000L)
    }
}

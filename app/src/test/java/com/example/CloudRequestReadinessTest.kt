package com.example

import com.example.ai.CloudRequestSession
import com.example.ai.CloudRetryControl
import kotlinx.coroutines.runBlocking
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class CloudRequestReadinessTest {
    private fun failure(code: Int, retryAfter: String? = null): HttpException {
        val raw = okhttp3.Response.Builder().request(okhttp3.Request.Builder().url("https://example.invalid").build())
            .protocol(okhttp3.Protocol.HTTP_1_1).code(code).message("test")
            .headers(if (retryAfter == null) Headers.Builder().build() else Headers.headersOf("Retry-After", retryAfter)).build()
        return HttpException(Response.error<Any>("{}".toResponseBody("application/json".toMediaType()), raw))
    }

    @Test fun oneHundredInvalidKeyOrAccountRequestsMakeOnlyOneAttemptPerScan() = runBlocking {
        for (code in listOf(401, 402)) {
            val session = CloudRequestSession { 1000L }
            var attempts = 0
            repeat(100) { runCatching { session.execute("key", "model") { attempts++; throw failure(code) } } }
            assertEquals(1, attempts)
            // A new scan and a changed key are independently retryable.
            CloudRequestSession { 1000L }.execute("key", "model") { attempts++ }
            session.execute("new-key", "model") { attempts++ }
            assertEquals(3, attempts)
            CloudRetryControl.retry()
            session.execute("key", "model") { attempts++ }
            assertEquals(4, attempts)
        }
    }

    @Test fun rateLimitHonorsCooldownThenRetriesAndTransientFailuresAreNotLatched() = runBlocking {
        var time = 0L
        val session = CloudRequestSession { time }
        var attempts = 0
        runCatching { session.execute("key", "model") { attempts++; throw failure(429, "60") } }
        time = 59999L
        runCatching { session.execute("key", "model") { attempts++ } }
        assertEquals(1, attempts)
        time = 60000L
        session.execute("key", "model") { attempts++ }
        assertEquals(2, attempts)
        repeat(2) { runCatching { session.execute("key", "model") { attempts++; throw failure(500) } } }
        assertEquals(4, attempts)
        repeat(2) { runCatching { session.execute("key", "model") { attempts++; throw java.io.IOException("offline") } } }
        assertEquals(6, attempts)
    }
}

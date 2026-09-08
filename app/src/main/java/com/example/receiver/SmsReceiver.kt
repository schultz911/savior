package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val pendingResult = goAsync()
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            pendingResult.finish()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Group multi-part SMS parts by originating address and timestamp
                val combinedBodies = StringBuilder()
                var sender = ""
                var timestamp = System.currentTimeMillis()

                for (msg in messages) {
                    sender = msg.displayOriginatingAddress ?: msg.originatingAddress ?: ""
                    combinedBodies.append(msg.displayMessageBody ?: msg.messageBody ?: "")
                    if (msg.timestampMillis > 0) {
                        timestamp = msg.timestampMillis
                    }
                }

                val fullText = combinedBodies.toString().trim()
                if (fullText.isEmpty()) return@launch

                // Brief delay to allow the telephony provider to commit the SMS row before we query it.
                // Without this, the inbox lookup may return no results and smsId stays 0L.
                delay(1500L)

                // Resolve the canonical telephony smsId for this message so the fast-path
                // existsBySmsId guard fires correctly when syncInbox runs later.
                val smsId = resolveSmsId(context, sender, timestamp)
                Log.d(TAG, "SmsReceiver: resolved smsId=$smsId for sender=$sender ts=$timestamp")

                com.example.service.ExpenseProcessingHelper.processRawSms(
                    context = context,
                    rawText = fullText,
                    sender = sender,
                    timestamp = timestamp,
                    smsId = smsId
                )
            } catch (e: Exception) {
                Log.e(TAG, "SmsReceiver processing error", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Queries the SMS inbox to find the telephony row ID for the just-received message.
     * Searches within a ±10 second window of [timestamp] from [sender].
     * Returns 0L if not yet committed or permissions are missing.
     */
    private fun resolveSmsId(context: Context, sender: String, timestamp: Long): Long {
        return try {
            val windowMs = 10_000L
            val cursor = context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms._ID, Telephony.Sms.DATE),
                "${Telephony.Sms.ADDRESS} = ? AND ${Telephony.Sms.DATE} >= ? AND ${Telephony.Sms.DATE} <= ?",
                arrayOf(sender, (timestamp - windowMs).toString(), (timestamp + windowMs).toString()),
                "${Telephony.Sms.DATE} DESC"
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    it.getLong(it.getColumnIndexOrThrow(Telephony.Sms._ID))
                } else 0L
            } ?: 0L
        } catch (e: Exception) {
            Log.w(TAG, "resolveSmsId failed: ${e.message}")
            0L
        }
    }

    companion object {
        private const val TAG = "SmsReceiver"
    }
}

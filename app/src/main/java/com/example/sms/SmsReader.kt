package com.example.sms

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import androidx.core.content.ContextCompat

data class RawSmsMessage(
    val smsId: Long,
    val sender: String,
    val body: String,
    val timestamp: Long
)

object SmsReader {

    private val INBOX_URI: Uri = Telephony.Sms.Inbox.CONTENT_URI

    fun hasReadSmsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasReceiveSmsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Reads filtered candidate financial SMS messages from inbox for AI validation & enhancement.
     */
    fun readCandidateSmsMessages(context: Context, sinceTimestamp: Long = 0L, limit: Int = 100): List<RawSmsMessage> {
        if (!hasReadSmsPermission(context)) {
            return emptyList()
        }

        val messages = mutableListOf<RawSmsMessage>()
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        val selection = if (sinceTimestamp > 0) {
            "${Telephony.Sms.DATE} > ?"
        } else null

        val selectionArgs = if (sinceTimestamp > 0) {
            arrayOf(sinceTimestamp.toString())
        } else null

        val sortOrder = "${Telephony.Sms.DATE} DESC"

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                INBOX_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.let {
                val idCol = it.getColumnIndexOrThrow(Telephony.Sms._ID)
                val addressCol = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyCol = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateCol = it.getColumnIndexOrThrow(Telephony.Sms.DATE)

                while (it.moveToNext() && messages.size < limit) {
                    val smsId = it.getLong(idCol)
                    val sender = it.getString(addressCol) ?: ""
                    val body = it.getString(bodyCol) ?: ""
                    val timestamp = it.getLong(dateCol)

                    if (SmsParser.isCandidateFinancialSms(body, sender)) {
                        messages.add(RawSmsMessage(smsId, sender, body, timestamp))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }

        return messages
    }
}

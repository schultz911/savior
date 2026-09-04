package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.SpendTrackerApplication
import com.example.data.ExpenseEntity
import com.example.service.LiveExpenditureNotificationService
import com.example.sms.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
                val parsed = SmsParser.parse(fullText, sender)

                if (parsed != null && parsed.isExpense) {
                    com.example.service.ExpenseProcessingHelper.processAndInsertExpense(
                        context = context,
                        parsed = parsed,
                        sender = sender,
                        timestamp = timestamp
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}

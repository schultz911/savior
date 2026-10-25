package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.util.DatabaseBackupHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VaultReadinessTest {
    @Test fun streamingVaultPreservesUnicodeEscapingAndEveryTransactionField() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = db.expenseDao()
            val prefs = ExpensePreferences(context)
            val original = ExpenseEntity(smsId = 733L, amount = 1234.56, currency = "₹",
                merchantOrRecipient = "Café \"店\" & Co", accountInfo = "Card ••4821",
                category = "Food & Dining", rawBody = "line1\nline2\\test\t₹", sender = "BANK",
                timestamp = 1725700000000L, isRecurring = true, refundedAmount = 34.56,
                isReversal = true, isExcluded = true, originalMerchant = "Original café")
            dao.insertExpense(original)
            val out = ByteArrayOutputStream()
            assertTrue(DatabaseBackupHelper.createEncryptedBackup(dao, prefs, "portable", out).isSuccess)
            val bytes = out.toByteArray()
            dao.clearAll()
            assertTrue(DatabaseBackupHelper.restoreEncryptedBackup(ByteArrayInputStream(bytes), "portable", dao, prefs).isSuccess)
            assertEquals(original, dao.getAllExpensesSync().single().copy(id = 0L))
            dao.clearAll()
            // Legacy payloads use the identical salt/IV/ciphertext envelope without SAV1.
            assertTrue(DatabaseBackupHelper.restoreEncryptedBackup(ByteArrayInputStream(bytes.copyOfRange(4, bytes.size)), "portable", dao, prefs).isSuccess)
            assertEquals(original, dao.getAllExpensesSync().single().copy(id = 0L))
        } finally { db.close() }
    }

    @Test fun unauthenticatedVaultNeverChangesPreferencesOrTransactions() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = db.expenseDao()
            val prefs = ExpensePreferences(context)
            prefs.currency = "USD"
            val out = ByteArrayOutputStream()
            DatabaseBackupHelper.createEncryptedBackup(dao, prefs, "correct", out).getOrThrow()
            prefs.currency = "INR"
            val damaged = out.toByteArray().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
            for ((bytes, password) in listOf(damaged to "correct", out.toByteArray() to "wrong")) {
                assertTrue(DatabaseBackupHelper.restoreEncryptedBackup(ByteArrayInputStream(bytes), password, dao, prefs).isFailure)
                assertEquals("INR", prefs.currency)
                assertTrue(dao.getAllExpensesSync().isEmpty())
            }
        } finally { db.close() }
    }
}

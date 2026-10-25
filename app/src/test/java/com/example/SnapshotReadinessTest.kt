package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.security.SnapshotSecretStore
import com.example.util.DatabaseBackupHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SnapshotReadinessTest {
    @Test fun localSecretIsRandomWrappedAndStableWithoutPlaintextStorage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val key = SecretKeySpec(ByteArray(32) { 7 }, "AES")
        val name = "snapshot-test-" + java.util.UUID.randomUUID()
        val store = SnapshotSecretStore(context, { key }, name)
        val secret = store.readOrCreate(true)
        assertEquals(secret, SnapshotSecretStore(context, { key }, name).readOrCreate(false))
        val wrapped = context.getSharedPreferences(name, 0).getString("wrapped", null)!!
        assertFalse(wrapped.contains(secret))
        assertNotEquals(secret, SnapshotSecretStore(context, { key }, name + "-other").readOrCreate(true))
        val lostKey = SnapshotSecretStore(context, { error("key missing") }, name)
        assertTrue(runCatching { lostKey.readOrCreate(false) }.isFailure)
        assertEquals(wrapped, context.getSharedPreferences(name, 0).getString("wrapped", null))
        assertTrue(runCatching { SnapshotSecretStore(context, { key }, name + "-missing").readOrCreate(false) }.isFailure)
    }

    @Test fun migrationAuthenticatesAndVerifiesBeforeDeletingLegacyFile() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val dir = File(context.cacheDir, "snapshot-test-" + java.util.UUID.randomUUID()).apply { mkdirs() }
        try {
            val dao = db.expenseDao()
            val prefs = ExpensePreferences(context)
            dao.insertExpense(ExpenseEntity(amount = 733.0, merchantOrRecipient = "Legacy café", timestamp = 1725700000000L))
            val legacy = File(dir, "savior_snapshot_legacy.savior")
            legacy.outputStream().use { DatabaseBackupHelper.createEncryptedBackup(dao, prefs, "legacy", it).getOrThrow() }
            val original = legacy.readBytes()
            assertTrue(runCatching { DatabaseBackupHelper.migrateLegacySnapshot(legacy, dir, "device-secret", "wrong") }.isFailure)
            assertArrayEquals(original, legacy.readBytes())
            val migrated = DatabaseBackupHelper.migrateLegacySnapshot(legacy, dir, "device-secret", "legacy")
            assertFalse(legacy.exists())
            assertTrue(migrated.name.startsWith("savior_snapshot_device_"))
            dao.clearAll()
            migrated.inputStream().use { assertTrue(DatabaseBackupHelper.restoreEncryptedBackup(it, "device-secret", dao, prefs).isSuccess) }
            assertEquals("Legacy café", dao.getAllExpensesSync().single().merchantOrRecipient)
            migrated.inputStream().use { assertTrue(DatabaseBackupHelper.restoreEncryptedBackup(it, "legacy", dao, prefs).isFailure) }
        } finally { db.close(); dir.listFiles()?.forEach { it.delete() }; dir.delete() }
    }
}

package com.example.util

import android.content.Context
import com.example.data.ExpenseDao
import com.example.data.ExpenseEntity
import com.example.data.ExpensePreferences
import com.example.data.ExpenseType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object DatabaseBackupHelper {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BITS = 128
    private const val IV_LENGTH_BYTES = 12
    private const val SALT_LENGTH_BYTES = 16
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH_BITS = 256
    private const val BACKUP_VERSION = 1

    fun generateDefaultFileName(): String {
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        return "savior_encrypted_backup_$dateStr.savior"
    }

    suspend fun createEncryptedBackup(
        dao: ExpenseDao,
        preferences: ExpensePreferences,
        passphrase: String,
        outputStream: OutputStream
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val allExpenses = dao.getAllExpensesSync()

            val rootJson = JSONObject().apply {
                put("version", BACKUP_VERSION)
                put("exportTimestamp", System.currentTimeMillis())
                put("currency", preferences.currency)
                put("monthlySalary", preferences.monthlySalary)
                put("monthlyBudget", preferences.monthlyBudget)
                put("savingsGoal", preferences.savingsGoal)

                val blacklistedArray = JSONArray()
                preferences.getBlacklistedMerchants().forEach { blacklistedArray.put(it) }
                put("blacklistedMerchants", blacklistedArray)

                val expensesArray = JSONArray()
                allExpenses.forEach { exp ->
                    val obj = JSONObject().apply {
                        put("amount", exp.amount)
                        put("currency", exp.currency)
                        put("type", exp.type.name)
                        put("merchantOrRecipient", exp.merchantOrRecipient)
                        put("accountInfo", exp.accountInfo)
                        put("category", exp.category)
                        put("rawBody", exp.rawBody)
                        put("sender", exp.sender)
                        put("timestamp", exp.timestamp)
                        put("monthKey", exp.monthKey)
                        put("isRecurring", exp.isRecurring)
                    }
                    expensesArray.put(obj)
                }
                put("expenses", expensesArray)
            }

            val plainBytes = rootJson.toString().toByteArray(Charsets.UTF_8)

            // Generate salt & IV
            val random = SecureRandom()
            val salt = ByteArray(SALT_LENGTH_BYTES)
            random.nextBytes(salt)
            val iv = ByteArray(IV_LENGTH_BYTES)
            random.nextBytes(iv)

            // Derive key
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
            val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BITS, iv))
            val cipherBytes = cipher.doFinal(plainBytes)

            // Output format: [Salt: 16 bytes][IV: 12 bytes][Ciphertext]
            outputStream.use { out ->
                out.write(salt)
                out.write(iv)
                out.write(cipherBytes)
                out.flush()
            }

            Result.success(allExpenses.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreEncryptedBackup(
        inputStream: InputStream,
        passphrase: String,
        dao: ExpenseDao,
        preferences: ExpensePreferences
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val allBytes = inputStream.use { it.readBytes() }
            if (allBytes.size < SALT_LENGTH_BYTES + IV_LENGTH_BYTES + 16) {
                return@withContext Result.failure(IllegalArgumentException("File is too small or corrupted."))
            }

            val byteBuffer = ByteBuffer.wrap(allBytes)
            val salt = ByteArray(SALT_LENGTH_BYTES)
            byteBuffer.get(salt)

            val iv = ByteArray(IV_LENGTH_BYTES)
            byteBuffer.get(iv)

            val cipherBytes = ByteArray(byteBuffer.remaining())
            byteBuffer.get(cipherBytes)

            // Derive key
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
            val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BITS, iv))
            val decryptedBytes = cipher.doFinal(cipherBytes)

            val jsonString = String(decryptedBytes, Charsets.UTF_8)
            val rootJson = JSONObject(jsonString)

            if (rootJson.has("currency")) {
                preferences.currency = rootJson.getString("currency")
            }
            if (rootJson.has("monthlySalary")) {
                preferences.monthlySalary = rootJson.getDouble("monthlySalary")
            }
            if (rootJson.has("monthlyBudget")) {
                preferences.monthlyBudget = rootJson.getDouble("monthlyBudget")
            }
            if (rootJson.has("savingsGoal")) {
                preferences.savingsGoal = rootJson.getDouble("savingsGoal")
            }
            if (rootJson.has("blacklistedMerchants")) {
                val blacklistedArr = rootJson.getJSONArray("blacklistedMerchants")
                for (i in 0 until blacklistedArr.length()) {
                    preferences.blacklistMerchant(blacklistedArr.getString(i))
                }
            }

            val expensesArray = rootJson.getJSONArray("expenses")
            val entitiesToInsert = mutableListOf<ExpenseEntity>()

            for (i in 0 until expensesArray.length()) {
                val obj = expensesArray.getJSONObject(i)
                val type = try {
                    ExpenseType.valueOf(obj.getString("type"))
                } catch (e: Exception) {
                    ExpenseType.MERCHANT
                }

                val entity = ExpenseEntity(
                    amount = obj.getDouble("amount"),
                    currency = obj.optString("currency", preferences.currency),
                    type = type,
                    merchantOrRecipient = obj.getString("merchantOrRecipient"),
                    accountInfo = obj.optString("accountInfo", ""),
                    category = obj.optString("category", "General Spend"),
                    rawBody = obj.optString("rawBody", ""),
                    sender = obj.optString("sender", "BackupRestore"),
                    timestamp = obj.getLong("timestamp"),
                    monthKey = obj.optString("monthKey", ExpenseEntity.formatMonthKey(obj.getLong("timestamp"))),
                    isRecurring = obj.optBoolean("isRecurring", false)
                )
                entitiesToInsert.add(entity)
            }

            val insertedIds = dao.insertExpenses(entitiesToInsert)
            val successfulInserts = insertedIds.count { it != -1L }

            Result.success(successfulInserts)
        } catch (e: javax.crypto.AEADBadTagException) {
            Result.failure(IllegalArgumentException("Incorrect passphrase or corrupted backup file."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

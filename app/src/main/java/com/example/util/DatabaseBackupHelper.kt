package com.example.util

import android.content.Context
import com.example.data.ExpenseDao
import com.example.data.ExpenseEntity
import com.example.data.ExpensePreferences
import com.example.data.ExpenseType
import com.example.data.InstrumentRuleDao
import com.example.data.InstrumentRuleEntity
import com.example.data.MerchantRuleDao
import com.example.data.MerchantRuleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
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
    const val BACKUP_VERSION = 2
    const val DEFAULT_SNAPSHOT_PASSPHRASE = "Savio_Vault_Snapshot_Local_Secure_Key"
    val MAGIC_HEADER = byteArrayOf(0x53, 0x41, 0x56, 0x31) // "SAV1"

    fun generateDefaultFileName(): String {
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        return "savior_encrypted_backup_$dateStr.savior"
    }

    suspend fun createEncryptedBackup(
        dao: ExpenseDao,
        preferences: ExpensePreferences,
        passphrase: String,
        outputStream: OutputStream,
        ruleDao: MerchantRuleDao? = null,
        instrumentRuleDao: InstrumentRuleDao? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val allExpenses = dao.getAllExpensesSync()
            val allRules = ruleDao?.getAllRulesSync() ?: emptyList()
            val allInstrumentRules = instrumentRuleDao?.getAllInstrumentsSync() ?: emptyList()
            val allCategoryLimits = preferences.getAllCategoryLimits()

            // Write JSON directly into GZIP: no full document tree, string or UTF-8 buffer.
            val plainBytes = ByteArrayOutputStream().use { baos ->
                GZIPOutputStream(baos).use { gzip ->
                    android.util.JsonWriter(gzip.writer(Charsets.UTF_8).buffered()).use { json ->
                        json.beginObject()
                        json.name("version").value(BACKUP_VERSION.toLong())
                        json.name("exportTimestamp").value(System.currentTimeMillis())
                        json.name("currency").value(preferences.currency)
                        json.name("monthlySalary").value(preferences.monthlySalary)
                        json.name("monthlyBudget").value(preferences.monthlyBudget)
                        json.name("savingsGoal").value(preferences.savingsGoal)
                        json.name("blacklistedMerchants").beginArray()
                        preferences.getBlacklistedMerchants().forEach { json.value(it) }
                        json.endArray()
                        json.name("categoryLimits").beginObject()
                        allCategoryLimits.forEach { (cat, limit) -> json.name(cat).value(limit) }
                        json.endObject()
                        json.name("merchantRules").beginArray()
                        for (rule in allRules) {
                            json.beginObject()
                            json.name("merchantPattern").value(rule.merchantPattern)
                            json.name("assignedCategory").value(rule.assignedCategory)
                            json.name("normalizedAlias").value(rule.normalizedAlias)
                            json.name("isRegex").value(rule.isRegex)
                            json.endObject()
                        }
                        json.endArray()
                        json.name("instrumentRules").beginArray()
                        for (rule in allInstrumentRules) {
                            json.beginObject()
                            json.name("bankName").value(rule.bankName)
                            json.name("cardOrAccountName").value(rule.cardOrAccountName)
                            json.name("last4").value(rule.last4)
                            json.name("markers").value(rule.markers)
                            json.endObject()
                        }
                        json.endArray()
                        json.name("expenses").beginArray()
                        for (exp in allExpenses) {
                            json.beginObject()
                            json.name("smsId").value(exp.smsId)
                            json.name("amount").value(exp.amount)
                            json.name("currency").value(exp.currency)
                            json.name("type").value(exp.type.name)
                            json.name("merchantOrRecipient").value(exp.merchantOrRecipient)
                            json.name("accountInfo").value(exp.accountInfo)
                            json.name("category").value(exp.category)
                            json.name("rawBody").value(exp.rawBody)
                            json.name("sender").value(exp.sender)
                            json.name("timestamp").value(exp.timestamp)
                            json.name("monthKey").value(exp.monthKey)
                            json.name("isRecurring").value(exp.isRecurring)
                            json.name("refundedAmount").value(exp.refundedAmount)
                            json.name("isReversal").value(exp.isReversal)
                            json.name("isExcluded").value(exp.isExcluded)
                            json.name("originalMerchant").value(exp.originalMerchant.ifEmpty { exp.merchantOrRecipient })
                            json.endObject()
                        }
                        json.endArray()
                        json.endObject()
                    }
                }
                baos.toByteArray()
            }

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

            // Output format: [Magic: 4 bytes][Salt: 16 bytes][IV: 12 bytes][Ciphertext]
            outputStream.use { out ->
                out.write(MAGIC_HEADER)
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
        preferences: ExpensePreferences,
        ruleDao: MerchantRuleDao? = null,
        allowLegacyWithoutHeader: Boolean = true,
        instrumentRuleDao: InstrumentRuleDao? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val decryptedBytes = decryptPayload(inputStream, passphrase, allowLegacyWithoutHeader)
            // Transparent GZIP decompression with legacy fallback
            val isGzip = decryptedBytes.size >= 2 &&
                    decryptedBytes[0] == 0x1F.toByte() &&
                    decryptedBytes[1] == 0x8B.toByte()

            val jsonString = if (isGzip) {
                ByteArrayInputStream(decryptedBytes).use { bais ->
                    GZIPInputStream(bais).use { gzipIn ->
                        gzipIn.bufferedReader(Charsets.UTF_8).readText()
                    }
                }
            } else {
                String(decryptedBytes, Charsets.UTF_8)
            }
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

            // Restore Category Limits if present
            if (rootJson.has("categoryLimits")) {
                val catLimitsObj = rootJson.getJSONObject("categoryLimits")
                val keys = catLimitsObj.keys()
                while (keys.hasNext()) {
                    val cat = keys.next()
                    val limit = catLimitsObj.getDouble(cat)
                    preferences.setCategoryLimit(cat, limit)
                }
            }

            // Restore Merchant Rules if present
            if (rootJson.has("merchantRules") && ruleDao != null) {
                val rulesArr = rootJson.getJSONArray("merchantRules")
                val rulesToInsert = mutableListOf<MerchantRuleEntity>()
                for (i in 0 until rulesArr.length()) {
                    val rObj = rulesArr.getJSONObject(i)
                    rulesToInsert.add(
                        MerchantRuleEntity(
                            merchantPattern = rObj.getString("merchantPattern"),
                            assignedCategory = rObj.getString("assignedCategory"),
                            normalizedAlias = rObj.optString("normalizedAlias", ""),
                            isRegex = rObj.optBoolean("isRegex", false)
                        )
                    )
                }
                if (rulesToInsert.isNotEmpty()) {
                    ruleDao.insertRules(rulesToInsert)
                }
            }

            // Restore Instrument Rules if present
            if (rootJson.has("instrumentRules") && instrumentRuleDao != null) {
                val instArr = rootJson.getJSONArray("instrumentRules")
                val instToInsert = mutableListOf<InstrumentRuleEntity>()
                for (i in 0 until instArr.length()) {
                    val iObj = instArr.getJSONObject(i)
                    instToInsert.add(
                        InstrumentRuleEntity(
                            bankName = iObj.optString("bankName", ""),
                            cardOrAccountName = iObj.optString("cardOrAccountName", ""),
                            last4 = iObj.optString("last4", ""),
                            markers = iObj.optString("markers", "")
                        )
                    )
                }
                if (instToInsert.isNotEmpty()) {
                    instrumentRuleDao.insertInstruments(instToInsert)
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
                    smsId = obj.optLong("smsId", 0L),
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
                    isRecurring = obj.optBoolean("isRecurring", false),
                    refundedAmount = obj.optDouble("refundedAmount", 0.0),
                    isReversal = obj.optBoolean("isReversal", false),
                    isExcluded = obj.optBoolean("isExcluded", false),
                    originalMerchant = obj.optString("originalMerchant", obj.optString("merchantOrRecipient", "Unknown"))
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

    private fun decryptPayload(inputStream: InputStream, passphrase: String, allowLegacyWithoutHeader: Boolean = true): ByteArray {
        val allBytes = inputStream.use { it.readBytes() }
        val minLegacySize = SALT_LENGTH_BYTES + IV_LENGTH_BYTES + 16
        val minMagicSize = MAGIC_HEADER.size + minLegacySize

        if (allBytes.size < minLegacySize) {
            throw IllegalArgumentException("File is too small or corrupted.")
        }

        val byteBuffer = ByteBuffer.wrap(allBytes)

        val hasMagic = allBytes.size >= minMagicSize &&
                allBytes[0] == MAGIC_HEADER[0] &&
                allBytes[1] == MAGIC_HEADER[1] &&
                allBytes[2] == MAGIC_HEADER[2] &&
                allBytes[3] == MAGIC_HEADER[3]

        if (hasMagic) {
            byteBuffer.position(MAGIC_HEADER.size)
        } else {
            if (!allowLegacyWithoutHeader || isKnownNonBackupFormat(allBytes)) {
                throw IllegalArgumentException("Invalid or corrupted Savio₹ backup file.")
            }
        }

        val salt = ByteArray(SALT_LENGTH_BYTES)
        byteBuffer.get(salt)

        val iv = ByteArray(IV_LENGTH_BYTES)
        byteBuffer.get(iv)

        val ciphertextOffset = byteBuffer.position()
        val ciphertextLength = byteBuffer.remaining()

        // Derive key
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        return cipher.doFinal(allBytes, ciphertextOffset, ciphertextLength)

    }

    private fun isKnownNonBackupFormat(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return true
        // PDF: "%PDF"
        if (bytes[0] == '%'.code.toByte() && bytes[1] == 'P'.code.toByte() && bytes[2] == 'D'.code.toByte() && bytes[3] == 'F'.code.toByte()) return true
        // PNG: 0x89 'P' 'N' 'G'
        if (bytes[0] == 0x89.toByte() && bytes[1] == 'P'.code.toByte() && bytes[2] == 'N'.code.toByte() && bytes[3] == 'G'.code.toByte()) return true
        // JPEG: 0xFF 0xD8 0xFF
        if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) return true
        // GIF: "GIF8"
        if (bytes[0] == 'G'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte() && bytes[3] == '8'.code.toByte()) return true
        // ZIP / APK / JAR / DOCX / XLSX: "PK\x03\x04"
        if (bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte() && bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()) return true
        // Plain text / JSON / XML / HTML
        val firstChar = bytes[0].toInt().toChar()
        if (firstChar == '{' || firstChar == '[' || firstChar == '<') return true
        return false
    }

    // ==========================================
    // Rolling Local Encrypted Snapshot Engine
    // ==========================================
    fun getSnapshotsDirectory(context: Context): File {
        val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "snapshots")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun createLocalRollingSnapshot(
        context: Context,
        dao: ExpenseDao,
        preferences: ExpensePreferences,
        ruleDao: MerchantRuleDao?,
        passphrase: String = DEFAULT_SNAPSHOT_PASSPHRASE,
        maxSnapshots: Int = 3,
        instrumentRuleDao: InstrumentRuleDao? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val dir = getSnapshotsDirectory(context)
            val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val snapshotFile = File(dir, "savior_snapshot_$timestampStr.savior")

            FileOutputStream(snapshotFile).use { out ->
                val result = createEncryptedBackup(dao, preferences, passphrase, out, ruleDao, instrumentRuleDao)
                if (result.isFailure) {
                    snapshotFile.delete()
                    return@withContext Result.failure(result.exceptionOrNull() ?: Exception("Snapshot creation failed"))
                }
            }

            // Prune older snapshots to keep only the 3 most recent
            val existing = dir.listFiles { _, name -> name.startsWith("savior_snapshot_") && name.endsWith(".savior") }
            if (existing != null && existing.size > maxSnapshots) {
                existing.sortedBy { it.lastModified() }
                    .take(existing.size - maxSnapshots)
                    .forEach { it.delete() }
            }

            Result.success(snapshotFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listLocalSnapshots(context: Context): List<File> {
        val dir = getSnapshotsDirectory(context)
        return dir.listFiles { _, name -> name.startsWith("savior_snapshot_") && name.endsWith(".savior") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    suspend fun restoreLatestLocalSnapshot(
        context: Context,
        dao: ExpenseDao,
        preferences: ExpensePreferences,
        ruleDao: MerchantRuleDao?,
        passphrase: String = DEFAULT_SNAPSHOT_PASSPHRASE,
        instrumentRuleDao: InstrumentRuleDao? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        val snapshots = listLocalSnapshots(context)
        val latest = snapshots.firstOrNull() ?: return@withContext Result.failure(IllegalStateException("No local snapshot available to restore."))
        FileInputStream(latest).use { input ->
            restoreEncryptedBackup(input, passphrase, dao, preferences, ruleDao, instrumentRuleDao = instrumentRuleDao)
        }
    }
}

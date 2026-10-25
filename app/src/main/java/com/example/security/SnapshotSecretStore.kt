package com.example.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** The portable vault password never goes here. This secret belongs only to local snapshots. */
internal class SnapshotSecretStore(
    context: Context,
    private val keyProvider: (Boolean) -> SecretKey = ::androidKey,
    preferencesName: String = "savio_snapshot_secret"
) {
    private val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    fun readOrCreate(allowCreation: Boolean): String = synchronized(lock) {
        val stored = preferences.getString("wrapped", null)
        if (stored == null && !allowCreation) {
            error("The device key for these snapshots is unavailable. Restore a password-protected vault backup instead.")
        }
        val key = keyProvider(stored == null)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        if (stored != null) {
            val bytes = Base64.decode(stored, Base64.NO_WRAP)
            require(bytes.size >= 28) { "Snapshot key storage is damaged; existing snapshots were preserved." }
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes, 0, 12))
            String(cipher.doFinal(bytes, 12, bytes.size - 12), Charsets.UTF_8)
        } else {
            val secret = Base64.encodeToString(ByteArray(32).also { SecureRandom().nextBytes(it) }, Base64.NO_WRAP)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val wrapped = cipher.iv + cipher.doFinal(secret.toByteArray(Charsets.UTF_8))
            check(preferences.edit().putString("wrapped", Base64.encodeToString(wrapped, Base64.NO_WRAP)).commit()) {
                "Could not persist the snapshot key; no snapshot was created."
            }
            secret
        }
    }

    companion object {
        private val lock = Any()
        private const val ALIAS = "savio.local.snapshots.v1"

        private fun androidKey(allowCreation: Boolean): SecretKey {
            val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
            check(allowCreation) { "Snapshot device key is unavailable. Existing files were preserved." }
            return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setKeySize(256)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build())
            }.generateKey()
        }
    }
}

package com.example

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.security.SnapshotSecretStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore

/** Must run on Android: verifies the real non-exportable key, not the JVM crypto substitute. */
@RunWith(AndroidJUnit4::class)
class SnapshotDeviceTest {
    @Test fun deviceKeyPersistsAndCannotBeExported() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "snapshot-device-test-" + java.util.UUID.randomUUID()
        try {
            val secret = SnapshotSecretStore(context, preferencesName = name).readOrCreate(true)
            assertEquals(secret, SnapshotSecretStore(context, preferencesName = name).readOrCreate(false))
            val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            assertNull(store.getKey("savio.local.snapshots.v1", null).encoded)
        } finally {
            context.deleteSharedPreferences(name)
        }
    }
}

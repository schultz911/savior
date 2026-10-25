package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.ExpenseEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QueryReadinessTest {
    @Test fun contentDeduplicationRetainsInclusiveWindowAmountToleranceAndSender() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java).build()
        try {
            val timestamp = 1725700000000L
            val dao = db.expenseDao()
            dao.insertExpense(ExpenseEntity(amount = 500.0, sender = "BANK", timestamp = timestamp))
            for (offset in listOf(-5001L, -5000L, -1L, 0L, 1L, 5000L, 5001L)) {
                assertEquals(kotlin.math.abs(offset) <= 5000, dao.existsByContent("BANK", timestamp + offset, 500.0))
            }
            assertTrue(dao.existsByContent("BANK", timestamp, 500.009))
            assertFalse(dao.existsByContent("BANK", timestamp, 500.011))
            assertFalse(dao.existsByContent("OTHER", timestamp, 500.0))
            db.openHelper.readableDatabase.query("EXPLAIN QUERY PLAN SELECT COUNT(*) > 0 FROM expenses WHERE sender = 'BANK' AND timestamp BETWEEN 1725699995000 AND 1725700005000 AND ABS(amount - 500.0) < 0.01").use {
                assertTrue(it.moveToFirst())
                val plan = it.getString(3)
                assertTrue(plan, plan.contains("sender=?") && plan.contains("timestamp>?") && plan.contains("timestamp<?"))
            }
        } finally { db.close() }
    }
}

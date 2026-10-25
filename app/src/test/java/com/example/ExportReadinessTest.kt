package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ExpenseEntity
import com.example.util.ExcelExportHelper
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.xml.parsers.DocumentBuilderFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExportReadinessTest {
    @Test fun generatedWorkbookPreservesEscapingRowsAndDistinctFiles() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val rows = (1..1000).map {
            ExpenseEntity(id = it.toLong(), amount = it + 0.25, merchantOrRecipient = "Café <Store> & \"Co\"",
                rawBody = "A&B <tag>", timestamp = 1725700000000L + it)
        }
        val first = ExcelExportHelper.createExportFile(context, rows)
        val second = ExcelExportHelper.createExportFile(context, rows.take(1))
        try {
            assertNotEquals(first, second)
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(first)
            assertEquals(1001, document.getElementsByTagName("Row").length)
            assertTrue(document.documentElement.textContent.contains("Café <Store> & \"Co\""))
            assertTrue(document.documentElement.textContent.contains("A&B <tag>"))
        } finally { first.delete(); second.delete() }
    }
}

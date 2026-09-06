package com.example.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.ExpenseEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelExportHelper {

    fun exportExpensesToExcel(context: Context, expenses: List<ExpenseEntity>) {
        try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val fileName = "Savior_Expenses_${dateFormat.format(Date())}.xls"
            val file = File(exportDir, fileName)

            val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val sdfTime = SimpleDateFormat("HH:mm:ss", Locale.US)

            // Stream XML directly to disk — O(1) constant heap usage regardless of transaction count
            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos, Charsets.UTF_8).buffered().use { w ->
                    w.write("""<?xml version="1.0" encoding="UTF-8"?>"""); w.newLine()
                    w.write("""<?mso-application progid="Excel.Sheet"?>"""); w.newLine()
                    w.write("""<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet""""); w.newLine()
                    w.write(""" xmlns:o="urn:schemas-microsoft-com:office:office""""); w.newLine()
                    w.write(""" xmlns:x="urn:schemas-microsoft-com:office:excel""""); w.newLine()
                    w.write(""" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet""""); w.newLine()
                    w.write(""" xmlns:html="http://www.w3.org/TR/REC-html40">"""); w.newLine()
                    w.write(""" <Styles>"""); w.newLine()
                    w.write("""  <Style ss:ID="Header">"""); w.newLine()
                    w.write("""   <Font ss:Bold="1" ss:Color="#FFFFFF"/>"""); w.newLine()
                    w.write("""   <Interior ss:Color="#059669" ss:Pattern="Solid"/>"""); w.newLine()
                    w.write("""   <Alignment ss:Horizontal="Center" ss:Vertical="Center"/>"""); w.newLine()
                    w.write("""  </Style>"""); w.newLine()
                    w.write("""  <Style ss:ID="Amount">"""); w.newLine()
                    w.write("""   <Alignment ss:Horizontal="Right"/>"""); w.newLine()
                    w.write("""   <NumberFormat ss:Format="#,##0.00"/>"""); w.newLine()
                    w.write("""  </Style>"""); w.newLine()
                    w.write("""  <Style ss:ID="Text">"""); w.newLine()
                    w.write("""   <Alignment ss:Horizontal="Left" ss:Vertical="Center"/>"""); w.newLine()
                    w.write("""  </Style>"""); w.newLine()
                    w.write(""" </Styles>"""); w.newLine()
                    w.write(""" <Worksheet ss:Name="Expenses">"""); w.newLine()
                    w.write("""  <Table>"""); w.newLine()

                    // Headers
                    w.write("""   <Row ss:StyleID="Header">"""); w.newLine()
                    w.write("""    <Cell><Data ss:Type="String">Date</Data></Cell>"""); w.newLine()
                    w.write("""    <Cell><Data ss:Type="String">Time</Data></Cell>"""); w.newLine()
                    w.write("""    <Cell><Data ss:Type="String">Merchant / Payee</Data></Cell>"""); w.newLine()
                    w.write("""    <Cell><Data ss:Type="String">Type</Data></Cell>"""); w.newLine()
                    w.write("""    <Cell><Data ss:Type="String">Category</Data></Cell>"""); w.newLine()
                    w.write("""    <Cell><Data ss:Type="String">Amount</Data></Cell>"""); w.newLine()
                    w.write("""    <Cell><Data ss:Type="String">Currency</Data></Cell>"""); w.newLine()
                    w.write("""    <Cell><Data ss:Type="String">Account / Source</Data></Cell>"""); w.newLine()
                    w.write("""    <Cell><Data ss:Type="String">Month Key</Data></Cell>"""); w.newLine()
                    w.write("""    <Cell><Data ss:Type="String">Sender</Data></Cell>"""); w.newLine()
                    w.write("""    <Cell><Data ss:Type="String">Raw SMS</Data></Cell>"""); w.newLine()
                    w.write("""   </Row>"""); w.newLine()

                    // Rows — streamed one at a time, never buffered in memory
                    for (item in expenses) {
                        val dateStr = sdfDate.format(Date(item.timestamp))
                        val timeStr = sdfTime.format(Date(item.timestamp))
                        val cleanMerchant = escapeXml(item.merchantOrRecipient)
                        val cleanCategory = escapeXml(item.category)
                        val cleanAccount = escapeXml(item.accountInfo)
                        val cleanSender = escapeXml(item.sender)
                        val cleanBody = escapeXml(item.rawBody)

                        w.write("""   <Row>"""); w.newLine()
                        w.write("""    <Cell ss:StyleID="Text"><Data ss:Type="String">$dateStr</Data></Cell>"""); w.newLine()
                        w.write("""    <Cell ss:StyleID="Text"><Data ss:Type="String">$timeStr</Data></Cell>"""); w.newLine()
                        w.write("""    <Cell ss:StyleID="Text"><Data ss:Type="String">$cleanMerchant</Data></Cell>"""); w.newLine()
                        w.write("""    <Cell ss:StyleID="Text"><Data ss:Type="String">${item.type.displayName}</Data></Cell>"""); w.newLine()
                        w.write("""    <Cell ss:StyleID="Text"><Data ss:Type="String">$cleanCategory</Data></Cell>"""); w.newLine()
                        w.write("""    <Cell ss:StyleID="Amount"><Data ss:Type="Number">${String.format(Locale.US, "%.2f", item.amount)}</Data></Cell>"""); w.newLine()
                        w.write("""    <Cell ss:StyleID="Text"><Data ss:Type="String">${item.currency}</Data></Cell>"""); w.newLine()
                        w.write("""    <Cell ss:StyleID="Text"><Data ss:Type="String">$cleanAccount</Data></Cell>"""); w.newLine()
                        w.write("""    <Cell ss:StyleID="Text"><Data ss:Type="String">${item.monthKey}</Data></Cell>"""); w.newLine()
                        w.write("""    <Cell ss:StyleID="Text"><Data ss:Type="String">$cleanSender</Data></Cell>"""); w.newLine()
                        w.write("""    <Cell ss:StyleID="Text"><Data ss:Type="String">$cleanBody</Data></Cell>"""); w.newLine()
                        w.write("""   </Row>"""); w.newLine()
                    }

                    w.write("""  </Table>"""); w.newLine()
                    w.write(""" </Worksheet>"""); w.newLine()
                    w.write("""</Workbook>"""); w.newLine()
                }
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.ms-excel"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Savio₹ Expenses Export")
                putExtra(Intent.EXTRA_TEXT, "Exported ${expenses.size} transactions from Savio₹.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Export Expenses to Excel (.XLS)").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)

            Toast.makeText(context, "Exported ${expenses.size} transactions to $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error exporting: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}

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

            val sb = StringBuilder()
            sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append("\n")
            sb.append("""<?mso-application progid="Excel.Sheet"?>""").append("\n")
            sb.append("""<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"""").append("\n")
            sb.append(""" xmlns:o="urn:schemas-microsoft-com:office:office"""").append("\n")
            sb.append(""" xmlns:x="urn:schemas-microsoft-com:office:excel"""").append("\n")
            sb.append(""" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"""").append("\n")
            sb.append(""" xmlns:html="http://www.w3.org/TR/REC-html40">""").append("\n")
            sb.append(""" <Styles>""").append("\n")
            sb.append("""  <Style ss:ID="Header">""").append("\n")
            sb.append("""   <Font ss:Bold="1" ss:Color="#FFFFFF"/>""").append("\n")
            sb.append("""   <Interior ss:Color="#059669" ss:Pattern="Solid"/>""").append("\n")
            sb.append("""   <Alignment ss:Horizontal="Center" ss:Vertical="Center"/>""").append("\n")
            sb.append("""  </Style>""").append("\n")
            sb.append("""  <Style ss:ID="Amount">""").append("\n")
            sb.append("""   <Alignment ss:Horizontal="Right"/>""").append("\n")
            sb.append("""   <NumberFormat ss:Format="#,##0.00"/>""").append("\n")
            sb.append("""  </Style>""").append("\n")
            sb.append("""  <Style ss:ID="Text">""").append("\n")
            sb.append("""   <Alignment ss:Horizontal="Left" ss:Vertical="Center"/>""").append("\n")
            sb.append("""  </Style>""").append("\n")
            sb.append(""" </Styles>""").append("\n")
            sb.append(""" <Worksheet ss:Name="Expenses">""").append("\n")
            sb.append("""  <Table>""").append("\n")

            // Headers
            sb.append("""   <Row ss:StyleID="Header">""").append("\n")
            sb.append("""    <Cell><Data ss:Type="String">Date</Data></Cell>""").append("\n")
            sb.append("""    <Cell><Data ss:Type="String">Time</Data></Cell>""").append("\n")
            sb.append("""    <Cell><Data ss:Type="String">Merchant / Payee</Data></Cell>""").append("\n")
            sb.append("""    <Cell><Data ss:Type="String">Type</Data></Cell>""").append("\n")
            sb.append("""    <Cell><Data ss:Type="String">Category</Data></Cell>""").append("\n")
            sb.append("""    <Cell><Data ss:Type="String">Amount</Data></Cell>""").append("\n")
            sb.append("""    <Cell><Data ss:Type="String">Currency</Data></Cell>""").append("\n")
            sb.append("""    <Cell><Data ss:Type="String">Account / Source</Data></Cell>""").append("\n")
            sb.append("""    <Cell><Data ss:Type="String">Month Key</Data></Cell>""").append("\n")
            sb.append("""    <Cell><Data ss:Type="String">Sender</Data></Cell>""").append("\n")
            sb.append("""    <Cell><Data ss:Type="String">Raw SMS</Data></Cell>""").append("\n")
            sb.append("""   </Row>""").append("\n")

            // Rows
            for (item in expenses) {
                val dateStr = sdfDate.format(Date(item.timestamp))
                val timeStr = sdfTime.format(Date(item.timestamp))
                val cleanMerchant = escapeXml(item.merchantOrRecipient)
                val cleanCategory = escapeXml(item.category)
                val cleanAccount = escapeXml(item.accountInfo)
                val cleanSender = escapeXml(item.sender)
                val cleanBody = escapeXml(item.rawBody)

                sb.append("""   <Row>""").append("\n")
                sb.append("""    <Cell ss:StyleID="Text"><Data ss:Type="String">$dateStr</Data></Cell>""").append("\n")
                sb.append("""    <Cell ss:StyleID="Text"><Data ss:Type="String">$timeStr</Data></Cell>""").append("\n")
                sb.append("""    <Cell ss:StyleID="Text"><Data ss:Type="String">$cleanMerchant</Data></Cell>""").append("\n")
                sb.append("""    <Cell ss:StyleID="Text"><Data ss:Type="String">${item.type.displayName}</Data></Cell>""").append("\n")
                sb.append("""    <Cell ss:StyleID="Text"><Data ss:Type="String">$cleanCategory</Data></Cell>""").append("\n")
                sb.append("""    <Cell ss:StyleID="Amount"><Data ss:Type="Number">${String.format(Locale.US, "%.2f", item.amount)}</Data></Cell>""").append("\n")
                sb.append("""    <Cell ss:StyleID="Text"><Data ss:Type="String">${item.currency}</Data></Cell>""").append("\n")
                sb.append("""    <Cell ss:StyleID="Text"><Data ss:Type="String">$cleanAccount</Data></Cell>""").append("\n")
                sb.append("""    <Cell ss:StyleID="Text"><Data ss:Type="String">${item.monthKey}</Data></Cell>""").append("\n")
                sb.append("""    <Cell ss:StyleID="Text"><Data ss:Type="String">$cleanSender</Data></Cell>""").append("\n")
                sb.append("""    <Cell ss:StyleID="Text"><Data ss:Type="String">$cleanBody</Data></Cell>""").append("\n")
                sb.append("""   </Row>""").append("\n")
            }

            sb.append("""  </Table>""").append("\n")
            sb.append(""" </Worksheet>""").append("\n")
            sb.append("""</Workbook>""").append("\n")

            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos, Charsets.UTF_8).use { writer ->
                    writer.write(sb.toString())
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

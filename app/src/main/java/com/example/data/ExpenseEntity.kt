package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["monthKey"]),
        Index(value = ["timestamp"]),
        Index(value = ["sender", "timestamp", "amount"], unique = true)
    ]
)
@TypeConverters(ExpenseTypeConverter::class)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val smsId: Long = 0L,
    val amount: Double,
    val currency: String = "₹",
    val type: ExpenseType = ExpenseType.MERCHANT,
    val merchantOrRecipient: String = "Unknown",
    val accountInfo: String = "",
    val category: String = "General",
    val rawBody: String = "",
    val sender: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val monthKey: String = formatMonthKey(timestamp),
    @ColumnInfo(name = "isRecurring", defaultValue = "0")
    val isRecurring: Boolean = false,
    @ColumnInfo(name = "refundedAmount", defaultValue = "0.0")
    val refundedAmount: Double = 0.0,
    @ColumnInfo(name = "isReversal", defaultValue = "0")
    val isReversal: Boolean = false
) {
    val netAmount: Double
        get() = (amount - refundedAmount).coerceAtLeast(0.0)

    val isFullyRefunded: Boolean
        get() = refundedAmount >= amount && amount > 0.0

    val isPartiallyRefunded: Boolean
        get() = refundedAmount > 0.0 && refundedAmount < amount

    companion object {
        fun formatMonthKey(epochMillis: Long): String {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
            return sdf.format(Date(epochMillis))
        }

        fun formatMonthDisplay(monthKey: String): String {
            return try {
                val parser = SimpleDateFormat("yyyy-MM", Locale.US)
                val formatter = SimpleDateFormat("MMMM yyyy", Locale.US)
                val date = parser.parse(monthKey)
                if (date != null) formatter.format(date) else monthKey
            } catch (e: Exception) {
                monthKey
            }
        }
    }
}

class ExpenseTypeConverter {
    @TypeConverter
    fun fromExpenseType(type: ExpenseType): String = type.name

    @TypeConverter
    fun toExpenseType(name: String): ExpenseType = ExpenseType.fromString(name)
}

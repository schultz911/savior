package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    suspend fun getAllExpensesSync(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE monthKey = :monthKey ORDER BY timestamp DESC")
    fun getExpensesForMonth(monthKey: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE monthKey = :monthKey ORDER BY timestamp DESC")
    suspend fun getExpensesForMonthSync(monthKey: String): List<ExpenseEntity>

    @Query("SELECT DISTINCT monthKey FROM expenses ORDER BY monthKey DESC")
    fun getAllMonthKeys(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>): List<Long>

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseById(id: Long): ExpenseEntity?

    @Query("UPDATE expenses SET category = :newCategory, type = :newType WHERE id = :id")
    suspend fun updateCategoryAndType(id: Long, newCategory: String, newType: ExpenseType)

    @Query("UPDATE expenses SET category = :newCategory, type = :newType WHERE LOWER(TRIM(merchantOrRecipient)) = LOWER(TRIM(:merchant))")
    suspend fun updateCategoryAndTypeForMerchant(merchant: String, newCategory: String, newType: ExpenseType)

    @Query("UPDATE expenses SET merchantOrRecipient = :newMerchant WHERE id = :id")
    suspend fun updateMerchantName(id: Long, newMerchant: String)

    @Query("UPDATE expenses SET merchantOrRecipient = :newMerchant WHERE LOWER(TRIM(merchantOrRecipient)) = LOWER(TRIM(:oldMerchant))")
    suspend fun updateMerchantNameForMatching(oldMerchant: String, newMerchant: String)

    @Query("SELECT SUM(CASE WHEN amount > refundedAmount THEN amount - refundedAmount ELSE 0.0 END) FROM expenses WHERE monthKey = :monthKey AND category = :category")
    suspend fun getTotalForCategoryInMonthSync(monthKey: String, category: String): Double?

    @Query("""
        SELECT * FROM expenses 
        WHERE timestamp >= :minTimestamp 
        AND timestamp <= :maxTimestamp 
        AND isReversal = 0 
        AND LOWER(category) != 'refund'
        AND (LENGTH(:merchantKeyword) >= 2 AND (
            LOWER(merchantOrRecipient) LIKE '%' || LOWER(:merchantKeyword) || '%'
            OR LOWER(:merchantKeyword) LIKE '%' || LOWER(merchantOrRecipient) || '%'
        ))
        ORDER BY 
        CASE WHEN ABS(amount - :amount) < 0.01 THEN 0 ELSE 1 END,
        timestamp DESC 
        LIMIT 1
    """)
    suspend fun findMatchingDebitByMerchant(
        merchantKeyword: String,
        amount: Double,
        minTimestamp: Long,
        maxTimestamp: Long
    ): ExpenseEntity?

    @Query("""
        SELECT * FROM expenses 
        WHERE timestamp >= :minTimestamp 
          AND timestamp <= :maxTimestamp 
          AND isReversal = 0 
          AND LOWER(category) != 'refund'
          AND type != 'SELF'
          AND type != 'CREDIT_CARD'
          AND ABS(amount - :amount) < 0.01
        ORDER BY timestamp DESC 
        LIMIT 1
    """)
    suspend fun findMatchingDebitByAmount(
        amount: Double,
        minTimestamp: Long,
        maxTimestamp: Long
    ): ExpenseEntity?

    @Query("""
        SELECT COUNT(*) > 0 FROM expenses 
        WHERE isReversal = 1 
          AND ABS(amount - :amount) < 0.01 
          AND (
            (rawBody = :rawBody AND LENGTH(:rawBody) > 5)
            OR (timestamp >= :minTimestamp AND timestamp <= :maxTimestamp AND (sender = :sender OR LOWER(TRIM(merchantOrRecipient)) = LOWER(TRIM(:merchant))))
          )
    """)
    suspend fun existsRefundDuplicate(
        amount: Double,
        rawBody: String,
        sender: String,
        merchant: String,
        minTimestamp: Long,
        maxTimestamp: Long
    ): Boolean

    @Query("UPDATE expenses SET refundedAmount = MIN(amount, refundedAmount + :refundAmount) WHERE id = :id")
    suspend fun applyRefund(id: Long, refundAmount: Double)

    @Query("UPDATE expenses SET refundedAmount = :refundAmount WHERE id = :id")
    suspend fun setRefundedAmount(id: Long, refundAmount: Double)

    @Query("SELECT * FROM expenses WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    suspend fun getExpensesSinceSync(sinceTimestamp: Long): List<ExpenseEntity>

    @Query("SELECT COUNT(*) > 0 FROM expenses WHERE smsId = :smsId AND smsId > 0")
    suspend fun existsBySmsId(smsId: Long): Boolean

    @Query("SELECT COUNT(*) > 0 FROM expenses WHERE rawBody = :rawBody AND sender = :sender AND timestamp >= :minTimestamp AND timestamp <= :maxTimestamp")
    suspend fun existsByRawBody(rawBody: String, sender: String, minTimestamp: Long, maxTimestamp: Long): Boolean

    @Query("SELECT COUNT(*) > 0 FROM expenses WHERE sender = :sender AND ABS(timestamp - :timestamp) <= 5000 AND ABS(amount - :amount) < 0.01")
    suspend fun existsByContent(sender: String, timestamp: Long, amount: Double): Boolean

    @Query("SELECT COUNT(*) FROM expenses WHERE monthKey = :monthKey AND (type = 'CREDIT_CARD' OR LOWER(category) = 'credit card bill') AND ABS(amount - :amount) < 0.01")
    suspend fun countCreditCardPaymentsInMonth(monthKey: String, amount: Double): Int

    @Query("UPDATE expenses SET isRecurring = :isRecurring WHERE id = :id")
    suspend fun updateIsRecurring(id: Long, isRecurring: Boolean)

    @Query("UPDATE expenses SET isExcluded = :isExcluded WHERE id = :id")
    suspend fun updateIsExcluded(id: Long, isExcluded: Boolean)

    @Query("UPDATE expenses SET isRecurring = :isRecurring WHERE LOWER(TRIM(merchantOrRecipient)) = LOWER(TRIM(:merchant))")
    suspend fun updateIsRecurringForMerchant(merchant: String, isRecurring: Boolean)

    @Query("SELECT * FROM expenses WHERE isRecurring = 1 ORDER BY timestamp DESC")
    suspend fun getRecurringExpensesSync(): List<ExpenseEntity>

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)

    @Query("DELETE FROM expenses WHERE monthKey = :monthKey")
    suspend fun deleteExpensesForMonth(monthKey: String): Int

    @Query("DELETE FROM expenses")
    suspend fun clearAll()

    @Query("""
        SELECT amount FROM expenses 
        WHERE timestamp >= :sinceTimestamp 
          AND isReversal = 0 
          AND isExcluded = 0 
          AND type != 'SELF' 
          AND category NOT IN ('Self', 'Credit Card Bill')
        ORDER BY amount ASC
    """)
    suspend fun getRecentDebitAmounts(sinceTimestamp: Long): List<Double>
}

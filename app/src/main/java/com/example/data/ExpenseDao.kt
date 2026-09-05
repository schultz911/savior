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

    @Query("SELECT SUM(amount) FROM expenses WHERE monthKey = :monthKey")
    fun getTotalExpenditureForMonth(monthKey: String): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expenses WHERE monthKey = :monthKey AND type = :type")
    fun getTotalByTypeForMonth(monthKey: String, type: ExpenseType): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expenses WHERE monthKey = :monthKey")
    suspend fun getTotalExpenditureForMonthSync(monthKey: String): Double?

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

    @Query("UPDATE expenses SET category = :newCategory WHERE id = :id")
    suspend fun updateCategory(id: Long, newCategory: String)

    @Query("UPDATE expenses SET category = :newCategory, type = :newType WHERE id = :id")
    suspend fun updateCategoryAndType(id: Long, newCategory: String, newType: ExpenseType)

    @Query("UPDATE expenses SET category = :newCategory WHERE LOWER(TRIM(merchantOrRecipient)) = LOWER(TRIM(:merchant))")
    suspend fun updateCategoryForMerchant(merchant: String, newCategory: String)

    @Query("UPDATE expenses SET category = :newCategory, type = :newType WHERE LOWER(TRIM(merchantOrRecipient)) = LOWER(TRIM(:merchant))")
    suspend fun updateCategoryAndTypeForMerchant(merchant: String, newCategory: String, newType: ExpenseType)

    @Query("SELECT SUM(amount) FROM expenses WHERE monthKey = :monthKey AND category = :category")
    suspend fun getTotalForCategoryInMonthSync(monthKey: String, category: String): Double?

    @Query("SELECT COUNT(*) > 0 FROM expenses WHERE sender = :sender AND timestamp = :timestamp AND amount = :amount")
    suspend fun existsByContent(sender: String, timestamp: Long, amount: Double): Boolean

    @Query("SELECT COUNT(*) FROM expenses WHERE monthKey = :monthKey AND (type = 'CREDIT_CARD' OR LOWER(category) = 'credit card bill') AND ABS(amount - :amount) < 0.01")
    suspend fun countCreditCardPaymentsInMonth(monthKey: String, amount: Double): Int

    @Query("UPDATE expenses SET isRecurring = :isRecurring WHERE id = :id")
    suspend fun updateIsRecurring(id: Long, isRecurring: Boolean)

    @Query("UPDATE expenses SET isRecurring = :isRecurring WHERE LOWER(TRIM(merchantOrRecipient)) = LOWER(TRIM(:merchant))")
    suspend fun updateIsRecurringForMerchant(merchant: String, isRecurring: Boolean)

    @Query("SELECT * FROM expenses WHERE isRecurring = 1 ORDER BY timestamp DESC")
    fun getRecurringExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE isRecurring = 1 ORDER BY timestamp DESC")
    suspend fun getRecurringExpensesSync(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE LOWER(TRIM(merchantOrRecipient)) = LOWER(TRIM(:merchant)) ORDER BY timestamp DESC")
    fun getExpensesForMerchant(merchant: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE LOWER(TRIM(merchantOrRecipient)) = LOWER(TRIM(:merchant)) ORDER BY timestamp DESC")
    suspend fun getExpensesForMerchantSync(merchant: String): List<ExpenseEntity>

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)

    @Query("DELETE FROM expenses")
    suspend fun clearAll()
}

package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantRuleDao {

    @Query("SELECT * FROM merchant_rules ORDER BY createdAt DESC")
    fun getAllRules(): Flow<List<MerchantRuleEntity>>

    @Query("SELECT * FROM merchant_rules ORDER BY createdAt DESC")
    suspend fun getAllRulesSync(): List<MerchantRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: MerchantRuleEntity): Long

    @Query("DELETE FROM merchant_rules WHERE id = :id")
    suspend fun deleteRule(id: Long)

    @Query("DELETE FROM merchant_rules WHERE LOWER(TRIM(merchantPattern)) = LOWER(TRIM(:pattern))")
    suspend fun deleteRuleByPattern(pattern: String)

    @Query("SELECT * FROM merchant_rules WHERE LOWER(TRIM(merchantPattern)) = LOWER(TRIM(:merchant)) LIMIT 1")
    suspend fun findExactRule(merchant: String): MerchantRuleEntity?

    @Query("DELETE FROM merchant_rules")
    suspend fun clearAllRules()
}

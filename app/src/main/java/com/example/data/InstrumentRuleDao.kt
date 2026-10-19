package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface InstrumentRuleDao {

    @Query("SELECT * FROM instrument_rules ORDER BY createdAt DESC")
    fun getAllInstruments(): Flow<List<InstrumentRuleEntity>>

    @Query("SELECT * FROM instrument_rules ORDER BY createdAt DESC")
    fun getAllInstrumentsSync(): List<InstrumentRuleEntity>

    @Query("SELECT * FROM instrument_rules WHERE last4 = :last4 LIMIT 1")
    suspend fun getInstrumentByLast4(last4: String): InstrumentRuleEntity?

    @Query("SELECT * FROM instrument_rules WHERE UPPER(TRIM(last4)) = UPPER(TRIM(:last4))")
    suspend fun getInstrumentsByLast4(last4: String): List<InstrumentRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstrument(rule: InstrumentRuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstruments(rules: List<InstrumentRuleEntity>): List<Long>

    @Update
    suspend fun updateInstrument(rule: InstrumentRuleEntity)

    @Delete
    suspend fun deleteInstrument(rule: InstrumentRuleEntity)

    @Query("DELETE FROM instrument_rules WHERE id = :id")
    suspend fun deleteInstrumentById(id: Long)

    @Query("DELETE FROM instrument_rules")
    suspend fun clearAll()
}

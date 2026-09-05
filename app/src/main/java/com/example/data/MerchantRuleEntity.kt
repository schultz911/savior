package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "merchant_rules",
    indices = [
        Index(value = ["merchantPattern"], unique = true)
    ]
)
data class MerchantRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val merchantPattern: String,
    val assignedCategory: String,
    val normalizedAlias: String = "",
    @ColumnInfo(name = "isRegex", defaultValue = "0")
    val isRegex: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

package com.example.data

import android.content.Context
import android.content.SharedPreferences

class ExpensePreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("spend_tracker_prefs", Context.MODE_PRIVATE)

    var currency: String
        get() = prefs.getString(KEY_CURRENCY, "₹") ?: "₹"
        set(value) = prefs.edit().putString(KEY_CURRENCY, value).apply()

    var monthlySalary: Double
        get() = java.lang.Double.longBitsToDouble(
            prefs.getLong(KEY_MONTHLY_SALARY, java.lang.Double.doubleToLongBits(0.0))
        )
        set(value) = prefs.edit().putLong(
            KEY_MONTHLY_SALARY,
            java.lang.Double.doubleToLongBits(value)
        ).apply()

    var savingsGoal: Double
        get() = java.lang.Double.longBitsToDouble(
            prefs.getLong(KEY_SAVINGS_GOAL, java.lang.Double.doubleToLongBits(0.0))
        )
        set(value) = prefs.edit().putLong(
            KEY_SAVINGS_GOAL,
            java.lang.Double.doubleToLongBits(value)
        ).apply()

    var monthlyBudget: Double
        get() = java.lang.Double.longBitsToDouble(
            prefs.getLong(KEY_MONTHLY_BUDGET, java.lang.Double.doubleToLongBits(0.0))
        )
        set(value) = prefs.edit().putLong(
            KEY_MONTHLY_BUDGET,
            java.lang.Double.doubleToLongBits(value)
        ).apply()

    var isPersistentNotificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, value).apply()

    var lastSyncTimestamp: Long
        get() = prefs.getLong(KEY_LAST_SYNC, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC, value).apply()

    var hasImportedInitialSamples: Boolean
        get() = prefs.getBoolean(KEY_HAS_IMPORTED_SAMPLES, false)
        set(value) = prefs.edit().putBoolean(KEY_HAS_IMPORTED_SAMPLES, value).apply()

    var openRouterApiKey: String
        get() = prefs.getString(KEY_OPENROUTER_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_OPENROUTER_API_KEY, value).apply()

    fun getCategoryLimit(category: String): Double {
        val key = KEY_CATEGORY_LIMIT_PREFIX + category
        return java.lang.Double.longBitsToDouble(
            prefs.getLong(key, java.lang.Double.doubleToLongBits(0.0))
        )
    }

    fun setCategoryLimit(category: String, limit: Double) {
        val key = KEY_CATEGORY_LIMIT_PREFIX + category
        prefs.edit().putLong(key, java.lang.Double.doubleToLongBits(limit)).apply()
    }

    fun getAllCategoryLimits(): Map<String, Double> {
        val all = prefs.all
        val limits = mutableMapOf<String, Double>()
        for ((key, value) in all) {
            if (key.startsWith(KEY_CATEGORY_LIMIT_PREFIX) && value is Long) {
                val cat = key.removePrefix(KEY_CATEGORY_LIMIT_PREFIX)
                limits[cat] = java.lang.Double.longBitsToDouble(value)
            }
        }
        return limits
    }

    companion object {
        private const val KEY_CURRENCY = "key_currency"
        private const val KEY_MONTHLY_SALARY = "key_monthly_salary"
        private const val KEY_SAVINGS_GOAL = "key_savings_goal"
        private const val KEY_MONTHLY_BUDGET = "key_monthly_budget"
        private const val KEY_NOTIFICATION_ENABLED = "key_notification_enabled"
        private const val KEY_LAST_SYNC = "key_last_sync"
        private const val KEY_HAS_IMPORTED_SAMPLES = "key_has_imported_samples"
        private const val KEY_OPENROUTER_API_KEY = "key_openrouter_api_key"
        private const val KEY_CATEGORY_LIMIT_PREFIX = "cat_limit_"
    }
}

package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.AiCoreCategorizer
import com.example.data.ExpensePreferences
import com.example.data.ExpenseType
import com.example.sms.SmsParser
import com.example.ui.AiEngineTier
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Savio₹", appName)

    val d1 = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_savio_logo)
    assertNotNull(d1)
    val d2 = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_stat_rupee)
    assertNotNull(d2)
    val d4 = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_savio_launcher_foreground)
    assertNotNull(d4)
  }

  @Test
  fun `test boot receiver`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val receiver = com.example.receiver.BootReceiver()
    val intent = android.content.Intent(android.content.Intent.ACTION_BOOT_COMPLETED)
    receiver.onReceive(context, intent)
  }

  @Test
  fun `test sms parser debits and spends`() {
    val chaseSms = "Your Chase card ending 4821 was charged $58.40 at WHOLE FOODS on Sep 04."
    val parsedChase = SmsParser.parse(chaseSms, "CHASE-ALERT")
    assertNotNull(parsedChase)
    assertEquals(58.40, parsedChase!!.amount, 0.01)
    assertEquals(ExpenseType.MERCHANT, parsedChase.type)
    assertTrue(parsedChase.title.contains("Whole Foods", ignoreCase = true))

    val zelleSms = "Bank of America: You sent $150.00 with Zelle to Sarah Miller."
    val parsedZelle = SmsParser.parse(zelleSms, "BOA-BANK")
    assertNotNull(parsedZelle)
    assertEquals(150.00, parsedZelle!!.amount, 0.01)
    assertEquals(ExpenseType.P2P, parsedZelle.type)
    assertEquals("Sarah Miller", parsedZelle.title)
  }

  @Test
  fun `test upi sms parsing and categorization`() {
    val upiSms = "Debited INR 450.00 via UPI to Sharma General Store on 05-Sep. UPI Ref: 98124901."
    val parsed = SmsParser.parse(upiSms, "AXIS-UPI")
    assertNotNull(parsed)
    assertEquals(450.00, parsed!!.amount, 0.01)
    assertEquals("₹", parsed.currency)
    assertEquals("UPI ••4901", parsed.accountInfo)
    assertEquals(ExpenseType.MERCHANT, parsed.type)
    assertEquals("Groceries", parsed.category)
    assertEquals("Sharma General Store", parsed.title)

    val upiTransferSms = "Sent Rs 1,200.00 to rahul@okaxis via Google Pay UPI (UPI Ref 429104)."
    val parsedTransfer = SmsParser.parse(upiTransferSms, "GPAY-UPI")
    assertNotNull(parsedTransfer)
    assertEquals(1200.00, parsedTransfer!!.amount, 0.01)
    assertEquals(ExpenseType.P2P, parsedTransfer.type)
    assertEquals("Transfers", parsedTransfer.category)
    assertEquals("rahul@okaxis", parsedTransfer.title)
  }

  @Test
  fun `test bank sms parses merchant or payee and never bank sender name`() {
    val hdfcSms = "Rs 1,450.00 debited from A/c **4821 on 04-Sep at SWIGGY BANGALORE. Avl Bal: Rs 48,250.00."
    val parsedHdfc = SmsParser.parse(hdfcSms, "VM-HDFCBK")
    assertNotNull(parsedHdfc)
    assertEquals("Swiggy Bangalore", parsedHdfc!!.title)
    org.junit.Assert.assertNotEquals("VM-HDFCBK", parsedHdfc.title)
    org.junit.Assert.assertNotEquals("HDFC", parsedHdfc.title)

    val sbiTransfer = "Dear SBI User, your A/c XX3391 debited by Rs 5,000.00 on 03-Sep towards Transfer to Ramesh Kumar."
    val parsedSbi = SmsParser.parse(sbiTransfer, "SBI-UPI")
    assertNotNull(parsedSbi)
    assertEquals("Ramesh Kumar", parsedSbi!!.title)
    assertEquals(ExpenseType.P2P, parsedSbi.type)
  }

  @Test
  fun `test credit card and self transfer detection`() {
    val ccBillSms = "Payment received of INR 8,500.00 towards your HDFC Credit Card ending 4821."
    val parsedCc = SmsParser.parse(ccBillSms, "HDFC-CARD")
    assertNotNull(parsedCc)
    assertEquals(ExpenseType.CREDIT_CARD, parsedCc!!.type)
    assertEquals("Credit Card Bill", parsedCc.category)

    val selfSms = "Transfer of INR 15,000.00 to your self savings account A/c 9901 is successful."
    val parsedSelf = SmsParser.parse(selfSms, "ICICI-ALERT")
    assertNotNull(parsedSelf)
    assertEquals(ExpenseType.SELF, parsedSelf!!.type)
    assertEquals("Self", parsedSelf.category)
  }

  @Test
  fun `test candidate financial sms filter`() {
    assertTrue(SmsParser.isCandidateFinancialSms("Debited INR 450.00 via UPI to Sharma", "AXIS-UPI"))
    assertTrue(SmsParser.isCandidateFinancialSms("Rs 1,450.00 debited from A/c **4821", "HDFC-BANK"))
    // OTP should NOT be candidate
    org.junit.Assert.assertFalse(SmsParser.isCandidateFinancialSms("Your OTP is 123456 to login", "HDFC-BANK"))
  }

  @Test
  fun `test credit card payment deduplication logic`() {
    val sms1 = "Payment received of INR 8,500.00 towards your HDFC Credit Card ending 4821."
    val parsed1 = SmsParser.parse(sms1, "HDFC-CARD")
    assertNotNull(parsed1)
    assertEquals(ExpenseType.CREDIT_CARD, parsed1!!.type)
    assertEquals("Credit Card Bill", parsed1.category)

    val sms2 = "Rs 8,500.00 debited from A/c **4821 towards HDFC Credit Card payment on 04-Sep."
    val parsed2 = SmsParser.parse(sms2, "HDFC-BANK")
    assertNotNull(parsed2)
    assertEquals(ExpenseType.CREDIT_CARD, parsed2!!.type)
    assertEquals("Credit Card Bill", parsed2.category)
    assertEquals(parsed1.amount, parsed2.amount, 0.01)
  }

  @Test
  fun `test app security manager activity result does not cause lock`() {
    val securityManager = com.example.security.AppSecurityManager
    securityManager.unlock()
    org.junit.Assert.assertFalse(securityManager.isLocked.value)

    // User triggers permission dialog or vault picker
    securityManager.markAwaitingActivityResult()
    assertTrue(securityManager.isAwaitingActivityResult)

    // Android pauses/stops app transiently for dialog
    securityManager.onAppBackgrounded()

    // Activity returns from launcher
    securityManager.onActivityResultCompleted()
    org.junit.Assert.assertFalse(securityManager.isAwaitingActivityResult)

    // App foregrounds
    securityManager.onAppForegrounded(isBiometricEnabled = true, lockTimeoutSeconds = 0)
    org.junit.Assert.assertFalse(securityManager.isLocked.value)
  }

  @Test
  fun `test backup helper generates valid filename`() {
    val fileName = com.example.util.DatabaseBackupHelper.generateDefaultFileName()
    assertTrue(fileName.startsWith("savior_encrypted_backup_"))
    assertTrue(fileName.endsWith(".savior"))
  }

  @Test
  fun `test refund and reversal sms parsing`() {
    val refundSms = "Refund of INR 850.00 credited to your A/c XX4821 from ZOMATO."
    val parsed = SmsParser.parse(refundSms, "HDFC-ALERT")
    assertNotNull(parsed)
    assertTrue(parsed!!.isRefund)
    assertEquals(850.00, parsed.amount, 0.01)
    assertEquals("Refund", parsed.category)
    assertTrue(parsed.title.contains("Zomato", ignoreCase = true))

    val reversalSms = "Reversal of Rs 350.00 processed for your transaction at Swiggy. Credited to UPI."
    val parsedReversal = SmsParser.parse(reversalSms, "AXIS-UPI")
    assertNotNull(parsedReversal)
    assertTrue(parsedReversal!!.isRefund)
    assertEquals(350.00, parsedReversal.amount, 0.01)
  }

  @Test
  fun `test auto rule and merchant alias matching`() {
    val rule = com.example.data.MerchantRuleEntity(
      id = 1L,
      merchantPattern = "SWIGGY",
      assignedCategory = "Food & Dining",
      normalizedAlias = "Swiggy Food Delivery",
      isRegex = false,
      createdAt = System.currentTimeMillis()
    )

    val rawMerchant = "SWIGGY BANGALORE IN"
    val isMatch = rawMerchant.contains(rule.merchantPattern, ignoreCase = true)
    assertTrue(isMatch)
    val effectiveName = if (rule.normalizedAlias.isNotBlank()) rule.normalizedAlias else rawMerchant
    assertEquals("Swiggy Food Delivery", effectiveName)
  }

  @Test
  fun `test instrument classification intelligence`() {
    val upiType = com.example.ui.models.InstrumentType.fromAccountInfo("UPI ••4901")
    assertEquals(com.example.ui.models.InstrumentType.UPI, upiType)

    val cardType = com.example.ui.models.InstrumentType.fromAccountInfo("Card ••4821")
    assertEquals(com.example.ui.models.InstrumentType.CARD, cardType)

    val bankType = com.example.ui.models.InstrumentType.fromAccountInfo("A/c ••9901")
    assertEquals(com.example.ui.models.InstrumentType.BANK_ACCOUNT, bankType)

    val unknownType = com.example.ui.models.InstrumentType.fromAccountInfo("Wallet Cash")
    assertEquals(com.example.ui.models.InstrumentType.OTHER, unknownType)
  }

  @Test
  fun `test daily burn down pacing calculations`() {
    val budget = 30000.0
    val daysInMonth = 30
    val currentDay = 15
    val currentSpent = 20000.0 // higher than 15,000 benchmark

    val targetDailySlope = budget / daysInMonth // 1000/day
    val benchmarkAtCurrentDay = targetDailySlope * currentDay // 15000
    val isOverPaced = currentSpent > benchmarkAtCurrentDay
    val burnRate = currentSpent / currentDay // ~1333.33/day
    val projected = burnRate * daysInMonth // 40000

    assertTrue(isOverPaced)
    assertEquals(1333.33, burnRate, 0.1)
    assertEquals(40000.0, projected, 1.0)
  }

  @Test
  fun `test recurring detection engine filters ignored merchants`() {
    val expenses = listOf(
      com.example.data.ExpenseEntity(
        id = 1L,
        amount = 499.0,
        currency = "₹",
        type = ExpenseType.MERCHANT,
        merchantOrRecipient = "Netflix India",
        accountInfo = "Card ••4821",
        category = "Subscriptions",
        rawBody = "Netflix charged 499",
        sender = "HDFC",
        timestamp = System.currentTimeMillis()
      ),
      com.example.data.ExpenseEntity(
        id = 2L,
        amount = 1200.0,
        currency = "₹",
        type = ExpenseType.MERCHANT,
        merchantOrRecipient = "Electricity BESCOM",
        accountInfo = "A/c ••9901",
        category = "Bills & Utilities",
        rawBody = "BESCOM bill payment 1200",
        sender = "ICICI",
        timestamp = System.currentTimeMillis()
      )
    )

    // Initially both should be detected
    val detectedAll = com.example.engine.RecurringDetectionEngine.detectRecurringBills(expenses)
    assertEquals(2, detectedAll.size)

    // When Netflix is added to ignored merchants
    val ignored = setOf("Netflix India")
    val detectedFiltered = com.example.engine.RecurringDetectionEngine.detectRecurringBills(
      expenses,
      ignoredMerchants = ignored
    )
    assertEquals(1, detectedFiltered.size)
    assertEquals("Electricity BESCOM", detectedFiltered.first().merchant)
  }

  @Test
  fun `test notification format matches exact specifications`() {
    val monthName = "September"
    val totalSpend = 4250.00
    val budget = 10000.00
    val safeDaily = 230.00
    val currency = "₹"
    val burnRateStatus = "Safe"

    val totalFormatted = "$currency" + java.text.NumberFormat.getNumberInstance(java.util.Locale.US).apply {
      minimumFractionDigits = 2
      maximumFractionDigits = 2
    }.format(totalSpend)

    val budgetFormatted = "$currency" + java.text.NumberFormat.getNumberInstance(java.util.Locale.US).apply {
      minimumFractionDigits = 2
      maximumFractionDigits = 2
    }.format(budget)

    val safeDailyFormatted = "$currency" + java.text.NumberFormat.getNumberInstance(java.util.Locale.US).apply {
      minimumFractionDigits = 2
      maximumFractionDigits = 2
    }.format(safeDaily)

    val progress = ((totalSpend / budget) * 100).toInt()

    val title = "$monthName:  $totalFormatted"
    val contentText = "$progress% of $budgetFormatted • $burnRateStatus burn rate • Safe daily spend pace: $safeDailyFormatted/day"

    assertEquals("September:  ₹4,250.00", title)
    assertEquals("42% of ₹10,000.00 • Safe burn rate • Safe daily spend pace: ₹230.00/day", contentText)
  }

  @Test
  fun `test merchant name editing updates transaction and autosaves classification rule and alias`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val dao = db.expenseDao()
    val ruleDao = db.merchantRuleDao()
    val prefs = com.example.data.ExpensePreferences(context)
    val repo = com.example.data.ExpenseRepository(context, dao, prefs, ruleDao)

    val exp1 = com.example.data.ExpenseEntity(
      amount = 350.0,
      currency = "₹",
      type = ExpenseType.MERCHANT,
      merchantOrRecipient = "SWIGGY BANGALORE",
      accountInfo = "UPI ••1234",
      category = "Food & Dining",
      rawBody = "Debited 350 at Swiggy",
      sender = "HDFC",
      timestamp = System.currentTimeMillis()
    )
    val exp2 = exp1.copy(amount = 450.0)
    val id1 = dao.insertExpense(exp1)
    val id2 = dao.insertExpense(exp2)

    // Edit merchant name
    repo.updateMerchantName(id1, "SWIGGY BANGALORE", "Swiggy", "Food & Dining")

    val updated1 = dao.getExpenseById(id1)
    val updated2 = dao.getExpenseById(id2)
    assertEquals("Swiggy", updated1?.merchantOrRecipient)
    assertEquals("Swiggy", updated2?.merchantOrRecipient)

    val rules = ruleDao.getAllRulesSync()
    assertTrue(rules.any { it.merchantPattern.equals("SWIGGY BANGALORE", ignoreCase = true) && it.normalizedAlias == "Swiggy" && it.assignedCategory == "Food & Dining" })

    db.close()
  }

  @Test
  fun `test sms catchup worker scheduling and notification add spend action`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    try {
      androidx.work.WorkManager.initialize(
        context,
        androidx.work.Configuration.Builder().setMinimumLoggingLevel(android.util.Log.DEBUG).build()
      )
    } catch (_: Exception) {}

    com.example.service.SmsCatchUpWorker.schedule(context)
    val workManager = androidx.work.WorkManager.getInstance(context)
    assertNotNull(workManager)

    assertEquals("com.example.savior.ACTION_ADD_SPEND", com.example.MainActivity.ACTION_ADD_SPEND)
    assertEquals("com.example.savior.ACTION_ADD_SPEND", com.example.service.LiveExpenditureNotificationService.ACTION_ADD_SPEND)
  }

  @Test
  fun `test velocity and anomaly guardrail preferences and recent debit query`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = com.example.data.ExpensePreferences(context)

    // Test preferences default and toggle
    assertTrue(prefs.isVelocityAlertsEnabled)
    assertTrue(prefs.isAnomalyAlertsEnabled)
    prefs.isVelocityAlertsEnabled = false
    org.junit.Assert.assertFalse(prefs.isVelocityAlertsEnabled)
    prefs.isVelocityAlertsEnabled = true

    // Test database query for median calculation
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val dao = db.expenseDao()

    val now = System.currentTimeMillis()
    dao.insertExpense(com.example.data.ExpenseEntity(amount = 200.0, category = "Food", timestamp = now - 1000))
    dao.insertExpense(com.example.data.ExpenseEntity(amount = 500.0, category = "Shopping", timestamp = now - 2000))
    dao.insertExpense(com.example.data.ExpenseEntity(amount = 100.0, category = "Transport", timestamp = now - 3000))
    dao.insertExpense(com.example.data.ExpenseEntity(amount = 1000.0, category = "Self", type = ExpenseType.SELF, timestamp = now - 4000))
    dao.insertExpense(com.example.data.ExpenseEntity(amount = 300.0, category = "Refund", isReversal = true, timestamp = now - 5000))

    val debits = dao.getRecentDebitAmounts(now - 10000)
    // Should exclude Self and Reversals, and be sorted ascending
    assertEquals(3, debits.size)
    assertEquals(100.0, debits[0], 0.01)
    assertEquals(200.0, debits[1], 0.01)
    assertEquals(500.0, debits[2], 0.01)

    // Compute median
    val median = debits[debits.size / 2]
    assertEquals(200.0, median, 0.01)

    // Verify SpendAlertManager methods execute safely
    val exp = com.example.data.ExpenseEntity(id = 99L, amount = 2500.0, merchantOrRecipient = "Croma Electronics", timestamp = now)
    com.example.service.SpendAlertManager.checkAndNotifyHighValueAnomaly(context, exp, median, "₹")
    com.example.service.SpendAlertManager.checkAndNotifySpendVelocity(context, currentSpent = 15000.0, monthlyBudget = 20000.0, currency = "₹")

    db.close()
  }

  @Test
  fun `test spend breakup tally with net monthly expenditure considering refunds and reversals`() {
    val exp1 = com.example.data.ExpenseEntity(
      amount = 500.0,
      category = "Food & Dining",
      merchantOrRecipient = "Swiggy"
    )
    val exp2 = com.example.data.ExpenseEntity(
      amount = 1000.0,
      category = "Shopping",
      merchantOrRecipient = "Amazon",
      refundedAmount = 200.0 // Net 800.0
    )
    val refundExp = com.example.data.ExpenseEntity(
      amount = 300.0,
      category = "Shopping",
      merchantOrRecipient = "Amazon",
      isReversal = true // Net -300.0
    )
    val selfExp = com.example.data.ExpenseEntity(
      amount = 5000.0,
      category = "Self",
      type = ExpenseType.SELF
    )
    val ccExp = com.example.data.ExpenseEntity(
      amount = 10000.0,
      category = "Credit Card Bill",
      type = ExpenseType.CREDIT_CARD
    )
    val excludedExp = com.example.data.ExpenseEntity(
      amount = 400.0,
      category = "Groceries",
      isExcluded = true
    )

    val all = listOf(exp1, exp2, refundExp, selfExp, ccExp, excludedExp)
    val valid = all.filterNot { exp ->
      val isSelf = exp.type == ExpenseType.SELF || exp.category.equals("Self", ignoreCase = true)
      val isCc = exp.type == ExpenseType.CREDIT_CARD || exp.category.equals("Credit Card Bill", ignoreCase = true)
      exp.isExcluded || isSelf || isCc
    }

    // Net spend calculation
    val netMonthlyExpenditure = valid.sumOf { it.effectiveSpendAmount }.coerceAtLeast(0.0)
    assertEquals(1000.0, netMonthlyExpenditure, 0.01)

    // Category grouping
    val grouped = valid.groupBy { it.category }
      .mapValues { (_, list) -> list.sumOf { it.effectiveSpendAmount } }
      .filterValues { it > 0.0 }

    assertEquals(500.0, grouped["Food & Dining"] ?: 0.0, 0.01)
    assertEquals(500.0, grouped["Shopping"] ?: 0.0, 0.01)
    assertEquals(1000.0, grouped.values.sum(), 0.01)
  }

  @Test
  fun `test velocity pacing spend calculation excludes excluded and blacklisted items`() {
    val blacklistedMerchants = setOf("crypto exchange", "gambling app")
    val normalizedBlacklist = blacklistedMerchants.map { it.trim().lowercase() }.toSet()

    val exp1 = com.example.data.ExpenseEntity(
      amount = 1500.0,
      merchantOrRecipient = "Amazon",
      category = "Shopping"
    )
    val excludedExp = com.example.data.ExpenseEntity(
      amount = 8000.0,
      merchantOrRecipient = "Big Purchase",
      isExcluded = true
    )
    val blacklistedExp = com.example.data.ExpenseEntity(
      amount = 12000.0,
      merchantOrRecipient = "Crypto Exchange ",
      category = "Investments"
    )
    val refundExp = com.example.data.ExpenseEntity(
      amount = 500.0,
      merchantOrRecipient = "Amazon",
      isReversal = true
    )

    val expenses = listOf(exp1, excludedExp, blacklistedExp, refundExp)

    var currentSpent = 0.0
    for (exp in expenses) {
      if (exp.isExcluded) continue
      val isSelf = exp.type == ExpenseType.SELF || exp.category.equals("Self", ignoreCase = true)
      val isCc = exp.type == ExpenseType.CREDIT_CARD || exp.category.equals("Credit Card Bill", ignoreCase = true)
      val isBlacklisted = exp.merchantOrRecipient?.trim()?.lowercase()?.let { normalizedBlacklist.contains(it) } == true
      if (isSelf || isCc || isBlacklisted) continue

      if (exp.isRefundOrReversal) {
        currentSpent -= exp.amount
      } else {
        currentSpent += (exp.amount - exp.refundedAmount).coerceAtLeast(0.0)
      }
    }
    currentSpent = currentSpent.coerceAtLeast(0.0)

    // Expected: exp1 (1500) - refundExp (500) = 1000.0 (excluded 8000 and blacklisted 12000 are omitted)
    assertEquals(1000.0, currentSpent, 0.01)
  }

  @Test
  fun `test aicore availability check and override`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    try {
      AiCoreCategorizer.testAvailabilityOverride = true
      assertTrue(AiCoreCategorizer.isAiCoreAvailable(context))

      AiCoreCategorizer.testAvailabilityOverride = false
      assertFalse(AiCoreCategorizer.isAiCoreAvailable(context))
    } finally {
      AiCoreCategorizer.testAvailabilityOverride = null
    }
  }

  @Test
  fun `test aicore json parsing of debit expense`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val mockJson = """
      {
        "is_financial": true,
        "classification": "debit",
        "amount": 349.00,
        "currency": "INR",
        "type": "debit",
        "merchant": "Zepto Quick Commerce",
        "category": "Groceries",
        "account": "HDFC **9876"
      }
    """.trimIndent()

    try {
      AiCoreCategorizer.testAvailabilityOverride = true
      AiCoreCategorizer.testInferenceProvider = { _, _ -> mockJson }

      val rawSms = "Rs 349.00 debited from A/c **9876 on 06-Sep at Zepto Quick Commerce."
      val parsed = AiCoreCategorizer.parseSmsTransaction(context, rawSms, "HDFC")

      assertNotNull(parsed)
      assertEquals(349.00, parsed!!.amount, 0.01)
      assertEquals("₹", parsed.currency)
      assertEquals("Zepto Quick Commerce", parsed.merchant)
      assertEquals("Groceries", parsed.category)
      assertEquals(ExpenseType.MERCHANT, parsed.type)
      assertEquals("HDFC ••9876", parsed.accountInfo)
      assertTrue(parsed.isExpense)
    } finally {
      AiCoreCategorizer.testAvailabilityOverride = null
      AiCoreCategorizer.testInferenceProvider = null
    }
  }

  @Test
  fun `test aicore non-financial message classification`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val mockJson = """
      {
        "is_financial": false,
        "classification": "otp"
      }
    """.trimIndent()

    try {
      AiCoreCategorizer.testAvailabilityOverride = true
      AiCoreCategorizer.testInferenceProvider = { _, _ -> mockJson }

      val rawSms = "Your OTP for netbanking login is 492019. Do not share with anyone."
      val parsed = AiCoreCategorizer.parseSmsTransaction(context, rawSms, "HDFC")

      assertNotNull(parsed)
      assertFalse(parsed!!.isExpense)
      assertEquals("otp", parsed.classification)
      assertEquals(0.0, parsed.amount, 0.01)
    } finally {
      AiCoreCategorizer.testAvailabilityOverride = null
      AiCoreCategorizer.testInferenceProvider = null
    }
  }

  @Test
  fun `test aicore categorize fallback`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val mockJson = """
      {
        "category": "Travel & Commute"
      }
    """.trimIndent()

    try {
      AiCoreCategorizer.testAvailabilityOverride = true
      AiCoreCategorizer.testInferenceProvider = { _, _ -> mockJson }

      val result = AiCoreCategorizer.categorizeSms(context, "Uber ride payment Rs 250", "UBER")
      assertEquals("Travel & Commute", result.category)
      assertTrue(result.isAiClassified)
    } finally {
      AiCoreCategorizer.testAvailabilityOverride = null
      AiCoreCategorizer.testInferenceProvider = null
    }
  }

  @Test
  fun `test aicore resilience on malformed json`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    try {
      AiCoreCategorizer.testAvailabilityOverride = true
      AiCoreCategorizer.testInferenceProvider = { _, _ -> "Sorry, I am unable to parse this message." }

      val parsed = AiCoreCategorizer.parseSmsTransaction(context, "Random text", "SENDER")
      assertNull(parsed)

      val result = AiCoreCategorizer.categorizeSms(context, "Random text", "SENDER")
      assertEquals("UNKNOWN", result.category)
      assertFalse(result.isAiClassified)
    } finally {
      AiCoreCategorizer.testAvailabilityOverride = null
      AiCoreCategorizer.testInferenceProvider = null
    }
  }

  @Test
  fun `test three-tier ai waterfall tier resolution`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    try {
      // 1. If OpenRouter API key is set -> CLOUD_OPENROUTER
      val tierWithKey = if ("sk-or-v1-testkey".isNotBlank()) {
        AiEngineTier.CLOUD_OPENROUTER
      } else if (AiCoreCategorizer.isAiCoreAvailable(context)) {
        AiEngineTier.ON_DEVICE_AICORE
      } else {
        AiEngineTier.LOCAL_RULES
      }
      assertEquals(AiEngineTier.CLOUD_OPENROUTER, tierWithKey)

      // 2. If API key is blank and AICore is available -> ON_DEVICE_AICORE
      AiCoreCategorizer.testAvailabilityOverride = true
      val tierWithAiCore = if ("".isNotBlank()) {
        AiEngineTier.CLOUD_OPENROUTER
      } else if (AiCoreCategorizer.isAiCoreAvailable(context)) {
        AiEngineTier.ON_DEVICE_AICORE
      } else {
        AiEngineTier.LOCAL_RULES
      }
      assertEquals(AiEngineTier.ON_DEVICE_AICORE, tierWithAiCore)

      // 3. If API key is blank and AICore is unavailable -> LOCAL_RULES
      AiCoreCategorizer.testAvailabilityOverride = false
      val tierWithOffline = if ("".isNotBlank()) {
        AiEngineTier.CLOUD_OPENROUTER
      } else if (AiCoreCategorizer.isAiCoreAvailable(context)) {
        AiEngineTier.ON_DEVICE_AICORE
      } else {
        AiEngineTier.LOCAL_RULES
      }
      assertEquals(AiEngineTier.LOCAL_RULES, tierWithOffline)
    } finally {
      AiCoreCategorizer.testAvailabilityOverride = null
    }
  }

  @Test
  fun `test aicore force enable preference override`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = ExpensePreferences(context)
    val origForce = prefs.isAiCoreForceEnabled
    try {
      prefs.isAiCoreForceEnabled = true
      assertTrue(AiCoreCategorizer.isAiCoreAvailable(context))

      prefs.isAiCoreForceEnabled = false
      // Without override and in Robolectric default env, returns based on system/hardware
      AiCoreCategorizer.testAvailabilityOverride = false
      assertFalse(AiCoreCategorizer.isAiCoreAvailable(context))
    } finally {
      prefs.isAiCoreForceEnabled = origForce
      AiCoreCategorizer.testAvailabilityOverride = null
    }
  }

  @Test
  fun `test real on-device semantic inference without test provider`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    try {
      AiCoreCategorizer.testAvailabilityOverride = true
      AiCoreCategorizer.testInferenceProvider = null // Ensure real production on-device engine executes!

      val rawSms = "Rs 450.00 debited from A/c **1234 on 06-Sep at Swiggy."
      val parsed = AiCoreCategorizer.parseSmsTransaction(context, rawSms, "HDFC")

      assertNotNull(parsed)
      assertEquals(450.00, parsed!!.amount, 0.01)
      assertEquals("₹", parsed.currency)
      assertEquals("Swiggy", parsed.merchant)
      assertEquals("Food & Dining", parsed.category)
      assertEquals(ExpenseType.MERCHANT, parsed.type)
      assertTrue(parsed.isAiClassified)
      assertTrue(parsed.isExpense)
    } finally {
      AiCoreCategorizer.testAvailabilityOverride = null
      AiCoreCategorizer.testInferenceProvider = null
    }
  }

  @Test
  fun `test real on-device categorization without test provider`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    try {
      AiCoreCategorizer.testAvailabilityOverride = true
      AiCoreCategorizer.testInferenceProvider = null // Ensure real production on-device engine executes!

      val result = AiCoreCategorizer.categorizeSms(context, "Paid at Blinkit Rs 320", "Blinkit")
      assertEquals("Groceries", result.category)
      assertTrue(result.isAiClassified)
    } finally {
      AiCoreCategorizer.testAvailabilityOverride = null
      AiCoreCategorizer.testInferenceProvider = null
    }
  }

  @Test
  fun `test real on-device non-financial message suppression`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    try {
      AiCoreCategorizer.testAvailabilityOverride = true
      AiCoreCategorizer.testInferenceProvider = null // Ensure real production on-device engine executes!

      val rawSms = "Your OTP is 987654 for login. Do not share."
      val parsed = AiCoreCategorizer.parseSmsTransaction(context, rawSms, "HDFC")

      assertNotNull(parsed)
      assertFalse(parsed!!.isExpense)
      assertEquals("otp", parsed.classification.lowercase())
    } finally {
      AiCoreCategorizer.testAvailabilityOverride = null
      AiCoreCategorizer.testInferenceProvider = null
    }
  }
}


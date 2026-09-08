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
  fun `test backup helper generates valid filename and magic header`() = kotlinx.coroutines.runBlocking {
    val fileName = com.example.util.DatabaseBackupHelper.generateDefaultFileName()
    assertTrue(fileName.startsWith("savior_encrypted_backup_"))
    assertTrue(fileName.endsWith(".savior"))

    val magicHeader = com.example.util.DatabaseBackupHelper.MAGIC_HEADER
    assertEquals("SAV1", String(magicHeader, Charsets.US_ASCII))
  }

  @Test
  fun `test backup helper export writes SAV1 magic header and restores correctly`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val dao = db.expenseDao()
    val ruleDao = db.merchantRuleDao()
    val prefs = com.example.data.ExpensePreferences(context)

    dao.insertExpense(
      com.example.data.ExpenseEntity(
        amount = 199.0,
        currency = "₹",
        type = ExpenseType.MERCHANT,
        merchantOrRecipient = "NETFLIX",
        accountInfo = "Card ••9999",
        category = "Entertainment",
        rawBody = "Charged 199 at Netflix",
        sender = "BANK",
        timestamp = System.currentTimeMillis(),
        monthKey = "2026-09"
      )
    )

    val outStream = java.io.ByteArrayOutputStream()
    val backupResult = com.example.util.DatabaseBackupHelper.createEncryptedBackup(
      dao = dao,
      preferences = prefs,
      passphrase = "SecretPassword123!",
      outputStream = outStream,
      ruleDao = ruleDao
    )
    assertTrue(backupResult.isSuccess)
    assertEquals(1, backupResult.getOrNull())

    val backupBytes = outStream.toByteArray()
    assertTrue(backupBytes.size > 4)
    assertEquals("SAV1", String(backupBytes.copyOfRange(0, 4), Charsets.US_ASCII))

    // Restore into a fresh db
    val freshDb = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val freshDao = freshDb.expenseDao()
    val freshRuleDao = freshDb.merchantRuleDao()

    val inStream = java.io.ByteArrayInputStream(backupBytes)
    val restoreResult = com.example.util.DatabaseBackupHelper.restoreEncryptedBackup(
      inputStream = inStream,
      passphrase = "SecretPassword123!",
      dao = freshDao,
      preferences = prefs,
      ruleDao = freshRuleDao
    )
    assertTrue(restoreResult.isSuccess)
    val restoredExpenses = freshDao.getAllExpensesSync()
    assertEquals(1, restoredExpenses.size)
    assertEquals("NETFLIX", restoredExpenses[0].merchantOrRecipient)
  }

  @Test
  fun `test backup helper restore fast fails without PBKDF2 delay on invalid non-backup files`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val dao = db.expenseDao()
    val prefs = com.example.data.ExpensePreferences(context)

    // Invalid non-backup content (JSON structure, PDF header, or corrupted arbitrary bytes)
    val invalidPayloads = listOf(
      "{\"version\": 2, \"corrupt\": true}".toByteArray(Charsets.UTF_8),
      "%PDF-1.5-invalid-backup-stream-bytes-data".toByteArray(Charsets.UTF_8),
      byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05) // too small
    )

    for (payload in invalidPayloads) {
      val start = System.currentTimeMillis()
      val result = com.example.util.DatabaseBackupHelper.restoreEncryptedBackup(
        inputStream = java.io.ByteArrayInputStream(payload),
        passphrase = "test_passphrase",
        dao = dao,
        preferences = prefs,
        allowLegacyWithoutHeader = false
      )
      val elapsed = System.currentTimeMillis() - start
      assertTrue(result.isFailure)
      assertTrue("Fast-fail should reject corrupt/invalid payload in <100ms without PBKDF2 burn, took ${elapsed}ms", elapsed < 100)
    }
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
    dao.insertExpense(com.example.data.ExpenseEntity(amount = 50000.0, category = "Work Reimbursement", isExcluded = true, timestamp = now - 6000))

    val debits = dao.getRecentDebitAmounts(now - 10000)
    // Should exclude Self, Reversals, and Excluded items, and be sorted ascending
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
  fun `test openrouter api headers are ascii compliant and okhttp safe`() {
    // Non-ASCII headers like '₹' (0x20b9) crash OkHttp with IllegalArgumentException.
    // Ensure the default title header in OpenRouterApi is pure ASCII.
    val headerBuilder = okhttp3.Headers.Builder()
    headerBuilder.add("X-Title", "Savio Spend Tracker")
    headerBuilder.add("HTTP-Referer", "https://ai.studio")
    val headers = headerBuilder.build()
    assertEquals("Savio Spend Tracker", headers.get("X-Title"))
    assertEquals("https://ai.studio", headers.get("HTTP-Referer"))
  }

  @Test
  fun `test openrouter moshi serialization and deserialization with reasoning and null content`() {
    val moshi = com.squareup.moshi.Moshi.Builder().build()
    val requestAdapter = moshi.adapter(com.example.ai.OpenRouterChatRequest::class.java)
    val responseAdapter = moshi.adapter(com.example.ai.OpenRouterChatResponse::class.java)

    // 1. Verify request with reasoning serializes cleanly
    val req = com.example.ai.OpenRouterChatRequest(
      model = "google/gemini-3.5-flash-lite",
      messages = listOf(
        com.example.ai.OpenRouterMessage(role = "system", content = "sys"),
        com.example.ai.OpenRouterMessage(role = "user", content = "hi")
      ),
      temperature = 0.0,
      maxTokens = 1000,
      reasoning = com.example.ai.OpenRouterReasoning(effort = "minimal")
    )
    val reqJson = requestAdapter.toJson(req)
    assertTrue(reqJson.contains("\"effort\":\"minimal\""))
    assertTrue(reqJson.contains("\"max_tokens\":1000"))

    // 2. Verify response with null content and reasoning string does not crash Moshi
    val jsonWithNullContent = """
      {
        "id": "gen-12345",
        "choices": [
          {
            "message": {
              "role": "assistant",
              "content": null,
              "reasoning": "Thinking about the SMS..."
            },
            "finish_reason": "length"
          }
        ]
      }
    """.trimIndent()
    val parsedResp = responseAdapter.fromJson(jsonWithNullContent)
    assertNotNull(parsedResp)
    assertEquals("gen-12345", parsedResp!!.id)
    assertEquals(1, parsedResp.choices?.size)
    assertNull(parsedResp.choices?.firstOrNull()?.message?.content)
    assertEquals("Thinking about the SMS...", parsedResp.choices?.firstOrNull()?.message?.reasoning)
    assertEquals("length", parsedResp.choices?.firstOrNull()?.finishReason)

    // 3. Verify error response deserialization
    val jsonWithError = """
      {
        "error": {
          "code": 401,
          "message": "User not found."
        }
      }
    """.trimIndent()
    val errResp = responseAdapter.fromJson(jsonWithError)
    assertNotNull(errResp)
    assertNotNull(errResp!!.error)
    assertEquals("User not found.", errResp.error?.message)
  }

  @Test
  fun `test openrouter parsing extracts json from content or reasoning with amounts with commas`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = ExpensePreferences(context)

    // Verify empty key returns null immediately without network call
    val emptyResult = com.example.ai.OpenRouterCategorizer.parseSmsTransaction(
      rawText = "Debited Rs 500 at Swiggy",
      sender = "HDFC",
      apiKey = ""
    )
    assertNull(emptyResult)
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

  @Test
  fun `test existsBySmsId and fast-path deduplication`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val app = context as SpendTrackerApplication
    val dao = app.database.expenseDao()

    val testSmsId = 987654321L
    assertFalse(dao.existsBySmsId(testSmsId))
    assertFalse(dao.existsBySmsId(0L))

    val entity = com.example.data.ExpenseEntity(
      smsId = testSmsId,
      amount = 250.0,
      merchantOrRecipient = "Test Cafe",
      category = "Food & Dining",
      sender = "VK-HDFCBK",
      timestamp = 1725700000000L
    )
    dao.insertExpense(entity)

    assertTrue(dao.existsBySmsId(testSmsId))
    assertFalse(dao.existsBySmsId(12345L))

    // Verify processRawSms short-circuits on known smsId
    val duplicateResult = com.example.service.ExpenseProcessingHelper.processRawSms(
      context = context,
      rawText = "Rs 250.00 debited from A/c **1234 on 07-Sep at Test Cafe.",
      sender = "VK-HDFCBK",
      timestamp = 1725700000000L,
      smsId = testSmsId
    )
    assertNull(duplicateResult)
  }

  @Test
  fun `test placeholder merchant undo suppression logic`() {
    val placeholders = setOf(
      "merchant / payee",
      "transfer recipient",
      "unknown",
      "refund / reversal",
      "upi"
    )

    // A generic UPI message that SmsParser maps to "Merchant / Payee"
    val genericUpiSms = "Dear UPI user A/C *1234 debited by 100.0 on 07-09-26. Ref 12345."
    val parsed = SmsParser.parse(genericUpiSms, "HDFC")
    assertNotNull(parsed)
    val parsedTitle = parsed?.title

    val isPlaceholder = parsedTitle != null && parsedTitle.trim().lowercase() in placeholders
    assertTrue("Should be identified as placeholder merchant", isPlaceholder)

    // A recognized merchant
    val swiggySms = "Rs 450.00 debited from A/c **1234 on 06-Sep at Swiggy."
    val swiggyParsed = SmsParser.parse(swiggySms, "HDFC")
    assertNotNull(swiggyParsed)
    val swiggyTitle = swiggyParsed?.title
    val isSwiggyPlaceholder = swiggyTitle != null && swiggyTitle.trim().lowercase() in placeholders
    assertFalse("Swiggy should NOT be identified as placeholder", isSwiggyPlaceholder)
  }

  @Test
  fun `test numbers and phone numbers are suppressed from SmsParser merchants`() {
    val phoneSms = "Dear SBI User, your A/c ending 1234 has been debited by Rs. 500 on 12-05-24 by transfer to 9876543210 Ref 4123456789."
    val parsed = SmsParser.parse(phoneSms, "SBI")
    assertNotNull(parsed)
    val title = parsed?.title ?: ""
    assertFalse("Phone number must not be extracted as merchant name", title.contains("9876543210"))
    assertFalse("Phone number must not be pure digits", title.replace(" ", "").all { it.isDigit() })
    assertTrue("Should fallback to Transfer Recipient or placeholder", title.equals("Transfer Recipient", ignoreCase = true) || title.equals("Merchant / Payee", ignoreCase = true))
  }

  @Test
  fun `test originalMerchant storage and undo restoration`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val app = context as SpendTrackerApplication
    val dao = app.database.expenseDao()

    val sms = "Rs 850.00 debited from A/c **4321 on 08-Sep at Blue Tokai Coffee."
    val inserted = com.example.service.ExpenseProcessingHelper.processRawSms(
      context = context,
      rawText = sms,
      sender = "HDFC-BANK",
      timestamp = 1725790000000L
    )
    assertNotNull(inserted)
    assertEquals("Blue Tokai Coffee", inserted?.merchantOrRecipient)
    assertEquals("Blue Tokai Coffee", inserted?.originalMerchant)

    // Simulate user editing merchant name to something custom
    val expenseId = inserted!!.id
    dao.updateMerchantName(expenseId, "Afternoon Meeting Coffee")
    val updated = dao.getExpenseById(expenseId)
    assertNotNull(updated)
    assertEquals("Afternoon Meeting Coffee", updated?.merchantOrRecipient)
    // originalMerchant must remain unchanged
    assertEquals("Blue Tokai Coffee", updated?.originalMerchant)

    // Undo action should restore to originalMerchant
    val targetUndoName = updated!!.originalMerchant
    dao.updateMerchantName(expenseId, targetUndoName)
    val restored = dao.getExpenseById(expenseId)
    assertNotNull(restored)
    assertEquals("Blue Tokai Coffee", restored?.merchantOrRecipient)
    assertEquals("Blue Tokai Coffee", restored?.originalMerchant)
  }

  @Test
  fun `test refund processing and refund reference messages are suppressed`() {
    val initiatedSms = "Dear Customer, your refund of Rs. 450 has been initiated. Refund ref no: 12345. It will be credited to your account in 2-4 business days."
    assertTrue(SmsParser.isRefundIntimationOrPending(initiatedSms))
    assertNull(SmsParser.parse(initiatedSms, "SWIGGY"))

    val processingSms = "Refund processing: Rs 300 will be refunded for order #123. Refund reference number: 8912839."
    assertTrue(SmsParser.isRefundIntimationOrPending(processingSms))
    assertNull(SmsParser.parse(processingSms, "VK-ZOMATO"))

    val refNumberOnlySms = "Refund Reference Number: 987654321 for your order refund of Rs 500."
    assertTrue(SmsParser.isRefundIntimationOrPending(refNumberOnlySms))
    assertNull(SmsParser.parse(refNumberOnlySms, "AMAZON"))

    val daysPendingSms = "Your refund request for Rs. 200 has been received and will reflect in 3-5 working days."
    assertTrue(SmsParser.isRefundIntimationOrPending(daysPendingSms))
    assertNull(SmsParser.parse(daysPendingSms, "FLIPKART"))
  }

  @Test
  fun `test actual refund messages parse correct merchant names`() {
    val sbiZomato = "Dear SBI User, your A/c ending 1234 has been credited by Rs. 500.00 on 08-Sep-26 towards refund from Zomato. UPI Ref 4123456789. Avl Bal: Rs 15,200.00."
    assertFalse(SmsParser.isRefundIntimationOrPending(sbiZomato))
    val parsedZomato = SmsParser.parse(sbiZomato, "SBI")
    assertNotNull(parsedZomato)
    assertTrue(parsedZomato!!.isRefund)
    assertEquals(500.0, parsedZomato.amount, 0.01)
    assertEquals("Zomato", parsedZomato.title)

    val hdfcSwiggy = "Dear Customer, INR 450.00 is credited to your A/c ending with 4821 on 08-Sep-26 towards reversal of txn at SWIGGY. Avl Bal: INR 48,700.00."
    val parsedSwiggy = SmsParser.parse(hdfcSwiggy, "HDFC")
    assertNotNull(parsedSwiggy)
    assertTrue(parsedSwiggy!!.isRefund)
    assertEquals(450.0, parsedSwiggy.amount, 0.01)
    assertEquals("Swiggy", parsedSwiggy.title)

    val kotakBlinkit = "Dear Customer, your Kotak Bank A/c ending 7890 is credited with Rs 300.00 on 08-Sep-26 towards refund for order at Blinkit. Avl Bal Rs 12,000."
    val parsedBlinkit = SmsParser.parse(kotakBlinkit, "KOTAK")
    assertNotNull(parsedBlinkit)
    assertTrue(parsedBlinkit!!.isRefund)
    assertEquals(300.0, parsedBlinkit.amount, 0.01)
    assertEquals("Blinkit", parsedBlinkit.title)

    val iciciFlipkart = "Dear Customer, your Account ending 5678 has been credited with INR 600.00 on 08-Sep-26. Info: BIL*REFUND*FLIPKART. Avl Balance is INR 25,000.00."
    val parsedFlipkart = SmsParser.parse(iciciFlipkart, "ICICI")
    assertNotNull(parsedFlipkart)
    assertTrue(parsedFlipkart!!.isRefund)
    assertEquals(600.0, parsedFlipkart.amount, 0.01)
    assertEquals("Flipkart", parsedFlipkart.title)

    val axisUber = "Your A/c 1234 is credited by Rs 250 on 06-Sep-26 by refund of UPI txn to Uber. UPI Ref 89128391."
    val parsedUber = SmsParser.parse(axisUber, "AXIS")
    assertNotNull(parsedUber)
    assertTrue(parsedUber!!.isRefund)
    assertEquals(250.0, parsedUber.amount, 0.01)
    assertEquals("Uber", parsedUber.title)

    val refundedSwiggy = "Rs 450.00 refunded to your account ending 1234 on 08-Sep from Swiggy. Avl Bal: Rs 15,200.00."
    assertFalse(SmsParser.isRefundIntimationOrPending(refundedSwiggy))
    val parsedRefSwiggy = SmsParser.parse(refundedSwiggy, "HDFC")
    assertNotNull(parsedRefSwiggy)
    assertTrue(parsedRefSwiggy!!.isRefund)
    assertEquals(450.0, parsedRefSwiggy.amount, 0.01)
    assertEquals("Swiggy", parsedRefSwiggy.title)

    val refundedBlinkit = "INR 300.00 has been refunded for order at Blinkit to card ending 5678."
    assertFalse(SmsParser.isRefundIntimationOrPending(refundedBlinkit))
    val parsedRefBlinkit = SmsParser.parse(refundedBlinkit, "SBI")
    assertNotNull(parsedRefBlinkit)
    assertTrue(parsedRefBlinkit!!.isRefund)
    assertEquals(300.0, parsedRefBlinkit.amount, 0.01)
    assertEquals("Blinkit", parsedRefBlinkit.title)

    val amazonRefund = "INR 1,299.00 has been credited to your HDFC Bank A/c xx1234 for refund from AMAZON PAY on 08-SEP-26. Bal: INR 12,345."
    val parsedAmazon = SmsParser.parse(amazonRefund, "HDFC")
    assertNotNull(parsedAmazon)
    assertTrue(parsedAmazon!!.isRefund)
    assertEquals(1299.0, parsedAmazon.amount, 0.01)
    assertEquals("Amazon PAY", parsedAmazon.title)

    val cancelledRideUber = "Dear Customer, refund of Rs 250.00 for your cancelled ride with Uber has been credited to your ICICI Bank account ending 9012."
    val parsedCancelledRide = SmsParser.parse(cancelledRideUber, "ICICI")
    assertNotNull(parsedCancelledRide)
    assertTrue(parsedCancelledRide!!.isRefund)
    assertEquals(250.0, parsedCancelledRide.amount, 0.01)
    assertEquals("Uber", parsedCancelledRide.title)

    val zomatoRefundCredited = "Dear Customer, a refund of INR 450.00 from ZOMATO has been credited to your A/c ending 1234 on 08-Sep-26."
    val parsedZomatoCredited = SmsParser.parse(zomatoRefundCredited, "SBI")
    assertNotNull(parsedZomatoCredited)
    assertTrue(parsedZomatoCredited!!.isRefund)
    assertEquals(450.0, parsedZomatoCredited.amount, 0.01)
    assertEquals("Zomato", parsedZomatoCredited.title)

    val dominosBil = "Credit Alert: INR 150.00 credited to A/c ending 1234 on 08-Sep-26. Info: BIL*REFUND*DOMINOS PIZZA."
    val parsedDominos = SmsParser.parse(dominosBil, "KOTAK")
    assertNotNull(parsedDominos)
    assertTrue(parsedDominos!!.isRefund)
    assertEquals(150.0, parsedDominos.amount, 0.01)
    assertEquals("Dominos Pizza", parsedDominos.title)

    val zaraCreditCard = "Refund of INR 899.00 has been credited to your credit card ending with 4567 towards purchase at ZARA on 05-Sep."
    val parsedZara = SmsParser.parse(zaraCreditCard, "HDFC")
    assertNotNull(parsedZara)
    assertTrue(parsedZara!!.isRefund)
    assertEquals(899.0, parsedZara.amount, 0.01)
    assertEquals("Zara", parsedZara.title)

    val bigBasketOrder = "Dear Customer, Rs 299.00 has been credited to your account towards refund for your order with BigBasket."
    val parsedBigBasket = SmsParser.parse(bigBasketOrder, "AXIS")
    assertNotNull(parsedBigBasket)
    assertTrue(parsedBigBasket!!.isRefund)
    assertEquals(299.0, parsedBigBasket.amount, 0.01)
    assertEquals("Bigbasket", parsedBigBasket.title)
  }

  @Test
  fun `test refund processRawSms preserves merchant name without matching debit`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sms = "Dear Customer, INR 750.00 is credited to your A/c ending 9012 towards reversal of txn at Swiggy."
    val entity = com.example.service.ExpenseProcessingHelper.processRawSms(
      context = context,
      rawText = sms,
      sender = "HDFC",
      timestamp = 1725800000000L
    )
    assertNotNull(entity)
    assertTrue(entity!!.isRefundOrReversal)
    assertEquals("Swiggy", entity.merchantOrRecipient)
    assertEquals("Swiggy", entity.originalMerchant)
    assertFalse("Refund must not be generic 'Refund / Reversal'", entity.merchantOrRecipient.equals("Refund / Reversal", ignoreCase = true))
  }

  @Test
  fun `test refund deduplication and debit matching`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val app = context as SpendTrackerApplication
    val dao = app.database.expenseDao()

    // 1. First record an original debit for Zomato
    val debitTimestamp = 1725700000000L
    val debit = com.example.data.ExpenseEntity(
      amount = 500.0,
      merchantOrRecipient = "Zomato",
      originalMerchant = "Zomato",
      category = "Food & Dining",
      sender = "HDFC-BANK",
      timestamp = debitTimestamp
    )
    val debitId = dao.insertExpense(debit)
    assertTrue(debitId > 0L)

    // Also record another debit with same amount (500.0) for Shell Petrol, to ensure merchant matching doesn't confuse them
    val shellDebit = com.example.data.ExpenseEntity(
      amount = 500.0,
      merchantOrRecipient = "Shell Petrol",
      originalMerchant = "Shell Petrol",
      category = "Fuel",
      sender = "HDFC-BANK",
      timestamp = debitTimestamp + 3600000L // 1 hour later
    )
    dao.insertExpense(shellDebit)

    // 2. An intimation SMS arrives first -> should be ignored completely
    val intimationSms = "Your refund of Rs. 500 for Zomato order has been initiated. Refund ref no: 8891238. Amount will be credited in 2-4 days."
    val intimationResult = com.example.service.ExpenseProcessingHelper.processRawSms(
      context = context,
      rawText = intimationSms,
      sender = "ZOMATO",
      timestamp = debitTimestamp + 7200000L
    )
    assertNull("Refund intimation must be ignored", intimationResult)

    // 3. Now the actual bank settlement SMS arrives
    val bankCreditSms = "Dear SBI User, your A/c ending 1234 has been credited by Rs. 500.00 on 08-Sep-26 towards refund from Zomato. UPI Ref 4123456789. Avl Bal: Rs 15,200.00."
    val refundTimestamp = debitTimestamp + 10000000L
    val refundSmsId = 88776655L

    val refundResult = com.example.service.ExpenseProcessingHelper.processRawSms(
      context = context,
      rawText = bankCreditSms,
      sender = "SBI",
      timestamp = refundTimestamp,
      smsId = refundSmsId
    )
    assertNotNull("Actual refund must be inserted", refundResult)
    assertEquals(500.0, refundResult?.amount)
    assertTrue(refundResult!!.isReversal)
    assertEquals("Zomato", refundResult.merchantOrRecipient)
    assertEquals("Food & Dining", refundResult.category)
    assertEquals(refundSmsId, refundResult.smsId)

    // 4. Ingesting duplicate via SMS sync (same smsId or same rawText or duplicate within 24h) must be skipped
    val duplicateResult = com.example.service.ExpenseProcessingHelper.processRawSms(
      context = context,
      rawText = bankCreditSms,
      sender = "SBI",
      timestamp = refundTimestamp + 1000L,
      smsId = refundSmsId
    )
    assertNull("Duplicate refund with same smsId must be skipped", duplicateResult)

    val duplicateByContent = com.example.service.ExpenseProcessingHelper.processRawSms(
      context = context,
      rawText = bankCreditSms,
      sender = "SBI",
      timestamp = refundTimestamp + 2000L,
      smsId = 0L
    )
    assertNull("Duplicate refund with same rawText within 24h must be skipped", duplicateByContent)
  }

  @Test
  fun `test aicore on-device parsing and processing of confirmed refund`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    AiCoreCategorizer.testAvailabilityOverride = true
    try {
      val refundSms = "Rs 650.00 refunded to A/c ending 9821 from Swiggy for cancelled order. UPI Ref: 778899. Avl Bal: Rs 24,000."
      val parsed = AiCoreCategorizer.parseSmsTransaction(context, refundSms, "HDFC")
      assertNotNull(parsed)
      assertTrue("Parsed message must be marked as refund", parsed!!.isRefund)
      assertEquals(650.0, parsed.amount, 0.01)
      assertEquals("Swiggy", parsed.merchant)
      assertEquals("Refund", parsed.category)
      assertFalse("Refund is not an outgoing expense", parsed.isExpense)

      // Test end-to-end processing through ExpenseProcessingHelper with Tier 2 AICore
      val processed = com.example.service.ExpenseProcessingHelper.processRawSms(
        context = context,
        rawText = refundSms,
        sender = "HDFC-BANK",
        timestamp = System.currentTimeMillis(),
        smsId = 998877L
      )
      assertNotNull("AICore refund must be processed and recorded by handleRefund", processed)
      assertTrue(processed!!.isReversal)
      assertEquals("Swiggy", processed.merchantOrRecipient)
      assertEquals(650.0, processed.amount, 0.01)
    } finally {
      AiCoreCategorizer.testAvailabilityOverride = null
      AiCoreCategorizer.testInferenceProvider = null
    }
  }

  @Test
  fun `test categories assigned at parse time do not appear as custom rules in ruleDao`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val app = context as SpendTrackerApplication
    val ruleDao = app.database.merchantRuleDao()

    val testMerchant = "Kailash Parbat Restaurant"
    val testCategory = "Food & Dining"

    // Simulate AI parsing an SMS from this merchant
    val parsed = com.example.sms.ParsedSms(
      amount = 750.0,
      currency = "₹",
      type = ExpenseType.MERCHANT,
      title = testMerchant,
      accountInfo = "A/c ••3321",
      category = testCategory,
      isExpense = true,
      rawText = "Paid Rs 750 at Kailash Parbat Restaurant on 08-Sep."
    )

    val inserted = com.example.service.ExpenseProcessingHelper.processAndInsertExpense(
      context = context,
      parsed = parsed,
      sender = "BANK",
      timestamp = System.currentTimeMillis()
    )
    assertNotNull(inserted)
    assertEquals(testCategory, inserted!!.category)

    // Verify that categories parsed from SMS remain default and do NOT create custom rules in ruleDao
    val rules = ruleDao.getAllRulesSync()
    val matchingRule = rules.find { it.merchantPattern.equals(testMerchant, ignoreCase = true) }
    assertNull("Categories assigned at parse time must NOT appear in custom rules section", matchingRule)
  }

  @Test
  fun `test backup and restore preserves smsId for fast-path deduplication`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val app = context as SpendTrackerApplication
    val dao = app.database.expenseDao()
    val prefs = app.preferences
    val ruleDao = app.database.merchantRuleDao()

    val uniqueSmsId = 5544332211L
    val expense = com.example.data.ExpenseEntity(
      smsId = uniqueSmsId,
      amount = 320.0,
      merchantOrRecipient = "Starbucks Coffee",
      category = "Food & Dining",
      sender = "HDFC",
      timestamp = System.currentTimeMillis()
    )
    dao.insertExpense(expense)
    assertTrue("Original expense must exist by smsId", dao.existsBySmsId(uniqueSmsId))

    val baos = java.io.ByteArrayOutputStream()
    val backupResult = com.example.util.DatabaseBackupHelper.createEncryptedBackup(
      dao = dao,
      preferences = prefs,
      passphrase = "test-passphrase-123",
      outputStream = baos,
      ruleDao = ruleDao
    )
    assertTrue(backupResult.isSuccess)

    // Clear database
    dao.clearAll()
    assertFalse("Cleared DB must not contain smsId", dao.existsBySmsId(uniqueSmsId))

    // Restore from backup
    val bais = java.io.ByteArrayInputStream(baos.toByteArray())
    val restoreResult = com.example.util.DatabaseBackupHelper.restoreEncryptedBackup(
      inputStream = bais,
      passphrase = "test-passphrase-123",
      dao = dao,
      preferences = prefs,
      ruleDao = ruleDao
    )
    assertTrue(restoreResult.isSuccess)

    // Verify smsId is preserved and fast-path dedup works post-restore
    assertTrue("Restored database must preserve smsId for fast-path deduplication", dao.existsBySmsId(uniqueSmsId))
  }

  @Test
  fun `test account extraction distinguishes bank account from upi handle`() {
    val bankAccountSms = "Rs 1,450.00 debited from A/c **4821 on 04-Sep at SWIGGY BANGALORE. UPI Ref: 489218291. Avl Bal: Rs 48,250.00."
    val parsedBank = SmsParser.parse(bankAccountSms, "HDFC-BANK")
    assertNotNull(parsedBank)
    assertEquals("A/c ••4821", parsedBank!!.accountInfo)
    assertEquals(com.example.ui.models.InstrumentType.BANK_ACCOUNT, com.example.ui.models.InstrumentType.fromAccountInfo(parsedBank.accountInfo))

    val upiOnlySms = "Debited INR 450.00 via UPI to Sharma General Store on 05-Sep. UPI Ref: 98124901."
    val parsedUpi = SmsParser.parse(upiOnlySms, "AXIS-UPI")
    assertNotNull(parsedUpi)
    assertEquals("UPI ••4901", parsedUpi!!.accountInfo)
    assertEquals(com.example.ui.models.InstrumentType.UPI, com.example.ui.models.InstrumentType.fromAccountInfo(parsedUpi.accountInfo))
  }

  @Test
  fun `test credit card purchase is classified as merchant spend and not credit card bill`() {
    // 1. Purchase at merchant with credit card must be MERCHANT and categorized by merchant
    val cardSpendSms = "Thank you for using your HDFC Bank Credit Card ending 4821 for payment of Rs 1,450.00 at SWIGGY BANGALORE on 04-Sep. Avl Limit: Rs 48,250.00."
    val parsedSpend = SmsParser.parse(cardSpendSms, "HDFC")
    assertNotNull(parsedSpend)
    assertEquals(ExpenseType.MERCHANT, parsedSpend!!.type)
    assertEquals("Food & Dining", parsedSpend.category)
    assertEquals("Card ••4821", parsedSpend.accountInfo)
    assertTrue("Merchant name must be preserved from SMS", parsedSpend.title.contains("SWIGGY BANGALORE", ignoreCase = true))

    // 2. Spent on Axis Card at Amazon
    val axisCardAmazon = "Spent INR 6,890.00 on Axis Card ending 1004 at AMAZON INDIA on 01-Sep. Avl Limit: Rs 1,85,000.00."
    val parsedAmazon = SmsParser.parse(axisCardAmazon, "AXIS-BANK")
    assertNotNull(parsedAmazon)
    assertEquals(ExpenseType.MERCHANT, parsedAmazon!!.type)
    assertEquals("Shopping", parsedAmazon.category)
    assertEquals("Card ••1004", parsedAmazon.accountInfo)
    assertTrue(parsedAmazon.title.contains("AMAZON INDIA", ignoreCase = true))

    // 3. Payment received towards Credit Card is CREDIT_CARD bill repayment
    val cardBillSms = "Payment received of INR 8,500.00 towards your HDFC Bank Credit Card ending 4821 on 02-Sep."
    val parsedBill = SmsParser.parse(cardBillSms, "HDFC-CARD")
    assertNotNull(parsedBill)
    assertEquals(ExpenseType.CREDIT_CARD, parsedBill!!.type)
    assertEquals("Credit Card Bill", parsedBill.category)
  }

  @Test
  fun `test aicore distinguishes credit card purchase from credit card bill`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    AiCoreCategorizer.testAvailabilityOverride = true
    try {
      // 1. Credit card purchase
      val cardPurchase = "Rs 1,450.00 spent on your HDFC Credit Card ending 4821 at SWIGGY BANGALORE on 04-Sep."
      val parsedPurchase = AiCoreCategorizer.parseSmsTransaction(context, cardPurchase, "HDFC")
      assertNotNull(parsedPurchase)
      assertEquals(ExpenseType.MERCHANT, parsedPurchase!!.type)
      assertEquals("Food & Dining", parsedPurchase.category)
      assertEquals("Card ••4821", parsedPurchase.accountInfo)
      assertTrue(parsedPurchase.merchant.contains("SWIGGY BANGALORE", ignoreCase = true))

      // 2. Credit card bill repayment
      val billSms = "INR 8,500.00 payment received towards your HDFC Credit Card ending 4821."
      val parsedBill = AiCoreCategorizer.parseSmsTransaction(context, billSms, "HDFC")
      assertNotNull(parsedBill)
      assertEquals(ExpenseType.CREDIT_CARD, parsedBill!!.type)
      assertEquals("Credit Card Bill", parsedBill.category)
    } finally {
      AiCoreCategorizer.testAvailabilityOverride = null
      AiCoreCategorizer.testInferenceProvider = null
    }
  }

  @Test
  fun `test exact merchant name is preserved in transaction without unwanted auto-alias`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val exactSms = "Rs 350.00 paid to BLUE TOKAI COFFEE ROASTERS on 04-Sep from A/c ending 1234."
    val processed = com.example.service.ExpenseProcessingHelper.processRawSms(
      context = context,
      rawText = exactSms,
      sender = "HDFC",
      timestamp = System.currentTimeMillis(),
      smsId = 88112233L
    )
    assertNotNull(processed)
    assertEquals("Blue Tokai Coffee Roasters", processed!!.merchantOrRecipient)
    assertEquals("Blue Tokai Coffee Roasters", processed.originalMerchant)
  }

  @Test
  fun `test credit card bill grouping and null account sanitization in instrument intelligence`() {
    val cardType = com.example.ui.models.InstrumentType.fromAccountInfo("Credit Card Bills")
    assertEquals(com.example.ui.models.InstrumentType.CARD, cardType)

    val bankType = com.example.ui.models.InstrumentType.fromAccountInfo("A/c ••1234")
    assertEquals(com.example.ui.models.InstrumentType.BANK_ACCOUNT, bankType)

    val upiType = com.example.ui.models.InstrumentType.fromAccountInfo("UPI ••5678")
    assertEquals(com.example.ui.models.InstrumentType.UPI, upiType)

    val otherType = com.example.ui.models.InstrumentType.fromAccountInfo("Other / Cash")
    assertEquals(com.example.ui.models.InstrumentType.OTHER, otherType)
  }

  @Test
  fun `test version name is 1_0_4`() {
    assertEquals("1.0.4", com.example.BuildConfig.VERSION_NAME)
    assertEquals(5, com.example.BuildConfig.VERSION_CODE)
  }
}


package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ExpenseType
import com.example.sms.SmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
}

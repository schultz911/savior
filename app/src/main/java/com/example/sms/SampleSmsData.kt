package com.example.sms

import com.example.data.ExpenseEntity
import com.example.data.ExpenseType

object SampleSmsData {

    data class SampleSms(
        val sender: String,
        val body: String,
        val description: String,
        val expectedType: ExpenseType
    )

    val SAMPLE_LIST = listOf(
        SampleSms(
            sender = "HDFC-BANK",
            body = "Rs 1,450.00 debited from A/c **4821 on 04-Sep at SWIGGY BANGALORE. UPI Ref: 489218291. Avl Bal: Rs 48,250.00.",
            description = "HDFC Swiggy Spend (₹1,450)",
            expectedType = ExpenseType.SPEND
        ),
        SampleSms(
            sender = "SBI-UPI",
            body = "Dear SBI User, your A/c XX3391 debited by Rs 5,000.00 on 03-Sep towards Transfer to Ramesh Kumar. UPI Ref 382910.",
            description = "SBI UPI Transfer (₹5,000)",
            expectedType = ExpenseType.TRANSFER
        ),
        SampleSms(
            sender = "ICICI-ALERT",
            body = "Debit alert: Your A/c XX8921 was debited by INR 3,250.00 towards Electricity Bill BESCOM on 02-Sep.",
            description = "ICICI Electricity Bill (₹3,250)",
            expectedType = ExpenseType.SPEND
        ),
        SampleSms(
            sender = "AXIS-BANK",
            body = "Spent INR 6,890.00 on Axis Card ending 1004 at AMAZON INDIA on 01-Sep. Avl Limit: Rs 1,85,000.00.",
            description = "Axis Card Amazon Spend (₹6,890)",
            expectedType = ExpenseType.SPEND
        ),
        SampleSms(
            sender = "KOTAK-BANK",
            body = "Kotak Bank: Rs 10,000.00 withdrawn from ATM #4012 on 01-Sep from A/c ending 6620.",
            description = "Kotak ATM Withdrawal (₹10,000)",
            expectedType = ExpenseType.SPEND
        ),
        SampleSms(
            sender = "PAYTM-UPI",
            body = "Paid Rs 350.00 to Blue Tokai Coffee using Paytm UPI on 04-Sep. Ref #99218.",
            description = "Paytm Coffee Spend (₹350)",
            expectedType = ExpenseType.SPEND
        ),
        SampleSms(
            sender = "AXIS-UPI",
            body = "Debited INR 450.00 via UPI to Sharma General Store on 05-Sep. UPI Ref: 98124901.",
            description = "Axis UPI Store Spend (₹450)",
            expectedType = ExpenseType.SPEND
        ),
        SampleSms(
            sender = "GPAY-UPI",
            body = "Sent Rs 1,200.00 to rahul@okaxis via Google Pay UPI (UPI Ref 429104).",
            description = "GPay UPI Transfer (₹1,200)",
            expectedType = ExpenseType.TRANSFER
        )
    )

    fun createInitialSampleExpenses(currency: String = "₹"): List<ExpenseEntity> {
        val now = System.currentTimeMillis()
        val oneDay = 24 * 60 * 60 * 1000L
        val oneMonth = 30L * oneDay

        val list = mutableListOf<ExpenseEntity>()

        // Current Month (Sep 2026)
        list.add(
            ExpenseEntity(
                amount = 1450.00,
                currency = currency,
                type = ExpenseType.SPEND,
                merchantOrRecipient = "Swiggy",
                accountInfo = "A/c ••4821",
                category = "Food & Dining",
                rawBody = "Rs 1,450.00 debited from A/c **4821 on 04-Sep at SWIGGY. Avl Bal: Rs 48,250.00.",
                sender = "HDFC-BANK",
                timestamp = now - (2 * 60 * 60 * 1000L)
            )
        )
        list.add(
            ExpenseEntity(
                amount = 450.00,
                currency = currency,
                type = ExpenseType.SPEND,
                merchantOrRecipient = "Sharma Store",
                accountInfo = "UPI ••4901",
                category = "UPI",
                rawBody = "Debited INR 450.00 via UPI to Sharma General Store on 05-Sep. UPI Ref: 98124901.",
                sender = "AXIS-UPI",
                timestamp = now - (5 * 60 * 60 * 1000L)
            )
        )
        list.add(
            ExpenseEntity(
                amount = 5000.00,
                currency = currency,
                type = ExpenseType.TRANSFER,
                merchantOrRecipient = "Ramesh Kumar",
                accountInfo = "A/c ••3391",
                category = "Transfers",
                rawBody = "A/c XX3391 debited by Rs 5,000.00 towards Transfer to Ramesh Kumar.",
                sender = "SBI-UPI",
                timestamp = now - (1 * oneDay)
            )
        )
        list.add(
            ExpenseEntity(
                amount = 3250.00,
                currency = currency,
                type = ExpenseType.SPEND,
                merchantOrRecipient = "Electricity Bill",
                accountInfo = "A/c ••8921",
                category = "Bills & Utilities",
                rawBody = "Debit alert: Your A/c XX8921 was debited by INR 3,250.00 towards BESCOM.",
                sender = "ICICI-ALERT",
                timestamp = now - (2 * oneDay)
            )
        )
        list.add(
            ExpenseEntity(
                amount = 6890.00,
                currency = currency,
                type = ExpenseType.SPEND,
                merchantOrRecipient = "Amazon India",
                accountInfo = "Card ••1004",
                category = "Shopping",
                rawBody = "Spent INR 6,890.00 on Axis Card ending 1004 at AMAZON INDIA.",
                sender = "AXIS-BANK",
                timestamp = now - (3 * oneDay)
            )
        )
        list.add(
            ExpenseEntity(
                amount = 10000.00,
                currency = currency,
                type = ExpenseType.SPEND,
                merchantOrRecipient = "ATM Cash Withdrawal",
                accountInfo = "A/c ••6620",
                category = "General",
                rawBody = "Kotak Bank: Rs 10,000.00 withdrawn from ATM #4012.",
                sender = "KOTAK-BANK",
                timestamp = now - (4 * oneDay)
            )
        )

        // Generate realistic historical expenses for past months across 12 months for the analytics graph
        val pastMonthsData = listOf(
            Pair(1, listOf(Pair(12000.0, "House Rent"), Pair(4500.0, "Groceries & Supermarket"), Pair(3200.0, "Electricity & Internet"))),
            Pair(2, listOf(Pair(12000.0, "House Rent"), Pair(5100.0, "Travel & Flights"), Pair(2900.0, "Dining Out"))),
            Pair(3, listOf(Pair(12000.0, "House Rent"), Pair(6800.0, "Electronics Sale"), Pair(3500.0, "Groceries"))),
            Pair(4, listOf(Pair(12000.0, "House Rent"), Pair(4200.0, "Bills & Utilities"), Pair(2100.0, "Medicines"))),
            Pair(5, listOf(Pair(12000.0, "House Rent"), Pair(8500.0, "Home Maintenance"), Pair(3900.0, "Groceries"))),
            Pair(6, listOf(Pair(12000.0, "House Rent"), Pair(4800.0, "Shopping Festival"), Pair(3100.0, "Utilities"))),
            Pair(7, listOf(Pair(12000.0, "House Rent"), Pair(3900.0, "Groceries"), Pair(1800.0, "Subscriptions"))),
            Pair(8, listOf(Pair(12000.0, "House Rent"), Pair(7200.0, "Vehicle Service"), Pair(4100.0, "Groceries"))),
            Pair(9, listOf(Pair(12000.0, "House Rent"), Pair(4400.0, "Utilities"), Pair(2800.0, "Dining"))),
            Pair(10, listOf(Pair(12000.0, "House Rent"), Pair(5600.0, "Gift & Celebration"), Pair(3600.0, "Groceries"))),
            Pair(11, listOf(Pair(12000.0, "House Rent"), Pair(4300.0, "Groceries"), Pair(2500.0, "Utilities")))
        )

        for ((monthOffset, items) in pastMonthsData) {
            val pastTime = now - (monthOffset * oneMonth)
            for ((amt, title) in items) {
                list.add(
                    ExpenseEntity(
                        amount = amt,
                        currency = currency,
                        type = if (title.contains("Rent")) ExpenseType.TRANSFER else ExpenseType.SPEND,
                        merchantOrRecipient = title,
                        accountInfo = "A/c ••4821",
                        category = if (title.contains("Rent") || title.contains("Transfer")) "Transfers" else "General",
                        rawBody = "Auto-tracked SMS record for $title ($currency$amt).",
                        sender = "BANK-ALERT",
                        timestamp = pastTime
                    )
                )
            }
        }

        return list
    }
}

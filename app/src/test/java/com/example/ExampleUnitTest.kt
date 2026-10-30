package com.example

import com.example.notifications.BankSmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun parsesHdfcUpiDebitWithBankAccountLast4AndMerchant() {
        val sms = "Sent Rs.450.00 From HDFC Bank A/C *4821 To Swiggy On 27/10/26 Ref 630012349876"
        val parsed = BankSmsParser.parse("AD-HDFCBK", sms, 1761574000000L)

        assertNotNull(parsed)
        assertEquals(450.0, parsed!!.amount, 0.001)
        assertTrue(parsed.isDebit)
        assertEquals("Swiggy", parsed.counterpartyName)
        assertEquals("HDFC Bank", parsed.bankName)
        assertEquals("4821", parsed.accountLast4)
        assertEquals("630012349876", parsed.upiRef)
        assertTrue(parsed.isUpi)
    }

    @Test
    fun parsesSbiUpiTransferWithoutCurrencySymbolPrefix() {
        val sms = "Dear UPI user A/C X9104 debited by 1280.0 on date 27Oct26 trf to RAHUL SHARMA Refno 630098765432 -SBI"
        val parsed = BankSmsParser.parse("VM-SBIUPI", sms, 1761574100000L)

        assertNotNull(parsed)
        assertEquals(1280.0, parsed!!.amount, 0.001)
        assertTrue(parsed.isDebit)
        assertEquals("Rahul Sharma", parsed.counterpartyName)
        assertEquals("SBI", parsed.bankName)
        assertEquals("9104", parsed.accountLast4)
        assertEquals("630098765432", parsed.upiRef)
    }

    @Test
    fun parsesIciciSemicolonDebitCorrectlyEvenWhenMerchantCreditedAppears() {
        val sms = "ICICI Bank Acct XX7732 debited for Rs 899.00 on 27-Oct-26; AMAZON PAY INDIA credited. UPI:630055443322."
        val parsed = BankSmsParser.parse("JX-ICICIB", sms, 1761574200000L)

        assertNotNull(parsed)
        assertEquals(899.0, parsed!!.amount, 0.001)
        assertTrue(parsed.isDebit)
        assertEquals("Amazon Pay India", parsed.counterpartyName)
        assertEquals("ICICI Bank", parsed.bankName)
        assertEquals("7732", parsed.accountLast4)
        assertEquals("630055443322", parsed.upiRef)
    }

    @Test
    fun parsesSalaryCreditAndIgnoresTrailingAvailableBalance() {
        val sms = "Your HDFC Bank A/c XX4821 is credited with INR 55,000.00 on 01-10-26 by ACME TECH SALARY. Avl bal INR 68,400.00"
        val parsed = BankSmsParser.parse("AD-HDFCBK", sms, 1761574300000L)

        assertNotNull(parsed)
        assertEquals(55000.0, parsed!!.amount, 0.001)
        assertFalse(parsed.isDebit)
        assertEquals("Acme Tech Salary", parsed.counterpartyName)
        assertEquals("HDFC Bank", parsed.bankName)
        assertEquals("4821", parsed.accountLast4)
    }

    @Test
    fun ignoresOtpAndCollectRequestMessages() {
        val otpSms = "839201 is your OTP for Rs.1,500.00 transaction on HDFC Bank Card XX4821. Do not share."
        val collectSms = "Zomato has requested Rs.420.00 from your UPI app. Approve collect request to pay."

        assertNull(BankSmsParser.parse("AD-HDFCBK", otpSms))
        assertNull(BankSmsParser.parse("AX-AXISBK", collectSms))
    }
}

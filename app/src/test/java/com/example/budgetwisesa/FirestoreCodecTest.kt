package com.example.budgetwisesa

import com.example.budgetwisesa.data.model.*
import com.example.budgetwisesa.data.remote.*
import org.junit.Assert.*
import org.junit.Test

class FirestoreCodecTest {
    @Test fun survivingPrototypeDocumentsReceiveStableMetadata() {
        val item = Transaction(id = "0", type = TransactionType.EXPENSE, category = "Food", amount = 12.0, date = 1000, description = "Legacy")
        val fields = FirestoreCodec.encode(item).fields.filterKeys { it !in listOf("revision", "deleted") } + ("id" to CloudValue(integerValue = "0"))
        val restored = FirestoreCodec.decode(CloudDocument(name = "transactions/0", fields = fields), Transaction::class.java)
        assertEquals("0", restored.id)
        assertEquals("legacy-0", restored.revision)
        assertFalse(restored.deleted)
    }
    @Test fun restRoundTripPreservesFinancialDataAndDeletionState() {
        val item = Transaction(type = TransactionType.EXPENSE, category = "Food", amount = 12.50,
            date = 1793872800000, description = "Lunch", deleted = true)
        val document = FirestoreCodec.encode(item).copy(name = "projects/demo/databases/(default)/documents/users/a/transactions/${item.id}")
        val result = FirestoreCodec.decode(document, Transaction::class.java)
        assertEquals(item.id, result.id)
        assertEquals(item.amount, result.amount, 0.00001)
        assertEquals(item.date, result.date)
        assertEquals(item.type, result.type)
        assertTrue(result.deleted)
        assertTrue(result.isSynced)
        assertFalse(document.fields.containsKey("isSynced"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun mismatchedCloudIdIsRejected() {
        FirestoreCodec.decode(CloudDocument(name = "transactions/wrong", fields = mapOf(
            "id" to CloudValue(stringValue = "other"), "revision" to CloudValue(stringValue = "v1"),
            "deleted" to CloudValue(booleanValue = false))), Transaction::class.java)
    }

    @Test fun absentOptionalReceiptSurvivesRoundTrip() {
        val item = Transaction(type = TransactionType.INCOME, category = "Salary", amount = 200.0, date = 1000, description = "")
        val result = FirestoreCodec.decode(FirestoreCodec.encode(item).copy(name = "transactions/${item.id}"), Transaction::class.java)
        assertNull(result.receiptPath)
    }
}

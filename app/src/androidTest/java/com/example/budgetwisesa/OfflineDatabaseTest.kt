package com.example.budgetwisesa

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.budgetwisesa.data.local.AppDatabase
import com.example.budgetwisesa.data.model.*
import com.example.budgetwisesa.data.repository.BudgetRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class OfflineDatabaseTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun item() = Transaction(type = TransactionType.EXPENSE, category = "Food", amount = 12.0,
        date = System.currentTimeMillis(), description = "Offline")

    @Test fun staleUploadAcknowledgementDoesNotLoseANewerEdit() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val original = item()
            val edited = original.copy(amount = 24.0, revision = UUID.randomUUID().toString())
            db.transactionDao().insertTransaction(edited)
            db.transactionDao().acknowledge(original.id, original.revision, null)
            assertEquals(edited.revision, db.transactionDao().pending().single().revision)
            db.transactionDao().acknowledge(edited.id, edited.revision, null)
            assertTrue(db.transactionDao().pending().isEmpty())
        } finally { db.close() }
    }

    @Test fun offlineChangesPersistAndDeletedRowsStayPendingButHidden() = runBlocking {
        val name = "offline-test-${UUID.randomUUID()}"
        var db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        val original = item()
        db.transactionDao().insertTransaction(original)
        db.close()
        db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            assertEquals(original.id, db.transactionDao().getAllTransactions().first().single().id)
            db.transactionDao().insertTransaction(original.copy(deleted = true))
            assertTrue(db.transactionDao().getAllTransactions().first().isEmpty())
            assertTrue(db.transactionDao().pending().single().deleted)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun accountsAndGuestHaveSeparateLocalDatabases() = runBlocking {
        val suffix = UUID.randomUUID().toString()
        val first = BudgetRepository(context, "account-a-$suffix")
        val second = BudgetRepository(context, "account-b-$suffix")
        val guest = BudgetRepository(context, "guest-$suffix")
        first.save(item())
        assertEquals(1, first.allTransactions.first().size)
        assertTrue(second.allTransactions.first().isEmpty())
        assertTrue(guest.allTransactions.first().isEmpty())
    }
}

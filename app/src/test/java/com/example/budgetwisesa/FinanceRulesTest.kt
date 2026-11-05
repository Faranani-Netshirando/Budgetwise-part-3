package com.example.budgetwisesa

import com.example.budgetwisesa.data.model.*
import com.example.budgetwisesa.domain.FinanceRules
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class FinanceRulesTest {
    private val zone = ZoneId.of("Africa/Johannesburg")
    private fun at(value: String) = Instant.parse(value).toEpochMilli()
    private fun expense(amount: Double, date: String, category: String = "Food") =
        Transaction(type = TransactionType.EXPENSE, category = category, amount = amount, date = at(date), description = "")
    private val now = at("2026-11-05T10:00:00Z")

    @Test fun amountsRejectNonFiniteNegativeZeroAndOverflow() {
        for (input in listOf("", "abc", "NaN", "Infinity", "1e999", "0", "-1", "1000000001")) {
            assertNull(input, FinanceRules.positiveAmount(input))
        }
        assertEquals(12.50, FinanceRules.positiveAmount("12,50")!!, 0.0001)
    }

    @Test fun monthlySpendingUsesLocalCalendarBoundariesAndCategory() {
        val budget = Budget(category = "food", amount = 500.0, period = "Monthly")
        val items = listOf(expense(20.0, "2026-10-31T21:59:59Z"), expense(30.0, "2026-10-31T22:00:00Z"),
            expense(50.0, "2026-11-30T22:00:00Z"), expense(80.0, "2026-11-05T10:00:00Z", "Travel"),
            expense(40.0, "2026-11-05T10:00:00Z").copy(type = TransactionType.INCOME),
            expense(100.0, "2026-11-05T10:00:00Z").copy(deleted = true))
        assertEquals(30.0, FinanceRules.spending(budget, items, now, zone), 0.0001)
    }

    @Test fun weeklyAndYearlyBudgetsUseTheirOwnPeriods() {
        val items = listOf(expense(10.0, "2026-11-01T10:00:00Z"), expense(20.0, "2026-11-02T10:00:00Z"),
            expense(40.0, "2026-01-01T10:00:00Z"), expense(90.0, "2025-12-31T21:00:00Z"))
        assertEquals(20.0, FinanceRules.spending(Budget(category = "Food", amount = 100.0, period = "Weekly"), items, now, zone), 0.001)
        assertEquals(70.0, FinanceRules.spending(Budget(category = "Food", amount = 100.0, period = "Yearly"), items, now, zone), 0.001)
    }

    @Test fun editCategoryAmountAndDeletionRecomputeWithoutDrift() {
        val budget = Budget(category = "Food", amount = 100.0, period = "Monthly")
        val original = expense(50.0, "2026-11-05T10:00:00Z")
        assertEquals(50.0, FinanceRules.spending(budget, listOf(original), now, zone), 0.001)
        assertEquals(25.0, FinanceRules.spending(budget, listOf(original.copy(amount = 25.0)), now, zone), 0.001)
        assertEquals(0.0, FinanceRules.spending(budget, listOf(original.copy(category = "Travel")), now, zone), 0.001)
        assertEquals(0.0, FinanceRules.spending(budget, emptyList(), now, zone), 0.001)
    }

    @Test fun invalidEntitiesAreRejected() {
        assertFalse(FinanceRules.valid(expense(Double.NaN, "2026-11-05T10:00:00Z")))
        assertFalse(FinanceRules.valid(Budget(category = "", amount = 10.0, period = "Monthly")))
        assertFalse(FinanceRules.valid(Budget(category = "Food", amount = 10.0, period = "Unknown")))
        assertFalse(FinanceRules.valid(SavingsGoal(name = "Save", targetAmount = 100.0, currentAmount = -1.0, targetDate = now, description = "")))
    }

    @Test fun newRecordsHaveDistinctStableIds() {
        val first = expense(1.0, "2026-11-05T10:00:00Z")
        val second = expense(1.0, "2026-11-05T10:00:00Z")
        assertNotEquals(first.id, second.id)
        assertEquals(first.id, first.copy(amount = 2.0).id)
    }
}

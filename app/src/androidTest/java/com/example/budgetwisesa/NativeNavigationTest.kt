package com.example.budgetwisesa

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import org.hamcrest.Matchers.not
import org.junit.Test

class NativeNavigationTest {
    @Test fun guestCanReachEveryNativeScreen() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.guest)).perform(click())
            onView(withId(R.id.dashboard_screen)).check(matches(isDisplayed()))
            onView(withId(R.id.nav_transactions)).perform(click())
            onView(withId(R.id.add_record)).check(matches(withText(R.string.add_transaction)))
            onView(withId(R.id.nav_budgets)).perform(click())
            onView(withId(R.id.add_record)).check(matches(withText(R.string.add_budget)))
            onView(withId(R.id.nav_goals)).perform(click())
            onView(withId(R.id.add_record)).check(matches(withText(R.string.add_goal)))
            onView(withId(R.id.nav_reports)).perform(click())
            onView(withId(R.id.reports_screen)).check(matches(isDisplayed()))
            onView(withId(R.id.action_settings)).perform(click())
            onView(withId(R.id.settings_screen)).check(matches(isDisplayed()))
        }
    }
    @Test fun transactionDialogRejectsZeroBeforeEnablingSave() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.guest)).perform(click())
            onView(withId(R.id.nav_transactions)).perform(click())
            onView(withId(R.id.add_record)).perform(click())
            onView(withId(android.R.id.button1)).check(matches(not(isEnabled())))
            onView(withId(R.id.category)).perform(replaceText("Food"))
            onView(withId(R.id.amount)).perform(replaceText("0"))
            onView(withId(android.R.id.button1)).check(matches(not(isEnabled())))
            onView(withId(R.id.amount)).perform(replaceText("12.50"))
            onView(withId(android.R.id.button1)).check(matches(isEnabled()))
        }
    }
}

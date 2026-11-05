package com.example.budgetwisesa

import android.view.LayoutInflater
import android.view.ContextThemeWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.example.budgetwisesa.ui.views.SpendingChartView

/** Inflate the actual screens, dialogs and custom chart under both application themes.
 * This catches missing Material theme attributes and View constructors that compilation cannot.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ViewsInflationTest {
    private fun inflateAll() {
        val context = ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.Theme_BudgetWiseSA)
        val inflater = LayoutInflater.from(context)
        listOf(R.layout.activity_main, R.layout.screen_login, R.layout.screen_register,
            R.layout.screen_dashboard, R.layout.screen_records, R.layout.screen_reports,
            R.layout.screen_settings, R.layout.dialog_transaction, R.layout.dialog_budget,
            R.layout.dialog_goal, R.layout.item_record).forEach { layout ->
            val root = inflater.inflate(layout, null, false)
            assertNotNull(root)
            root.findViewById<SpendingChartView>(R.id.chart)?.let { chart ->
                chart.update(listOf("Food" to 25.0, "Rent" to 75.0))
                chart.layout(0, 0, 320, 320)
                chart.draw(Canvas(Bitmap.createBitmap(320, 320, Bitmap.Config.ARGB_8888)))
            }
        }
    }
    @Test fun allNativeLayoutsInflateInLightTheme() = inflateAll()
    @Test @Config(qualifiers = "night") fun allNativeLayoutsInflateInDarkTheme() = inflateAll()
}

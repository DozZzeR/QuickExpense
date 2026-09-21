package dev.keslorod.quickexpense

import dev.keslorod.quickexpense.ui.quickinput.localDayToUtcMillis
import dev.keslorod.quickexpense.ui.quickinput.utcMillisToLocalDay
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class DatePickerDayConversionTest {
    private lateinit var originalZone: TimeZone

    @Before
    fun saveZone() {
        originalZone = TimeZone.getDefault()
    }

    @After
    fun restoreZone() {
        TimeZone.setDefault(originalZone)
    }

    private fun localCal(year: Int, month: Int, day: Int, hour: Int, minute: Int) =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, hour, minute)
        }

    @Test
    fun pickedDayStaysTheSameDayWestOfUtc() {
        // Reading DatePicker's UTC midnight as a local instant used to land on the previous
        // day here (UTC-5): 2026-03-10T00:00Z is 2026-03-09 19:00 in New York.
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
        val picked = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(2026, Calendar.MARCH, 10)
        }.timeInMillis

        val result = utcMillisToLocalDay(picked, timeOfDayFrom = localCal(2026, Calendar.MARCH, 20, 14, 30))

        assertEquals(2026, result.get(Calendar.YEAR))
        assertEquals(Calendar.MARCH, result.get(Calendar.MONTH))
        assertEquals(10, result.get(Calendar.DAY_OF_MONTH))
        // Time of day is kept from the previously selected date, not reset to midnight.
        assertEquals(14, result.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, result.get(Calendar.MINUTE))
    }

    @Test
    fun roundTripsLocalDayEastOfUtc() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Belgrade"))
        // 00:30 local on the 1st is still the previous day in UTC — the picker must still
        // be handed the 1st.
        val local = localCal(2026, Calendar.JANUARY, 1, 0, 30)

        val utc = localDayToUtcMillis(local)
        val back = utcMillisToLocalDay(utc, timeOfDayFrom = local)

        assertEquals(1, back.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.JANUARY, back.get(Calendar.MONTH))
        assertEquals(local.timeInMillis, back.timeInMillis)
    }
}

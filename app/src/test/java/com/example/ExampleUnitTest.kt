package com.example

import com.example.notifications.MatchScheduleParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun resolveStartTimeMillis_parsesIstAndComputesTenMinuteTrigger() {
    val ist = TimeZone.getTimeZone("Asia/Kolkata")
    val nowCal = Calendar.getInstance(ist).apply {
      set(2026, Calendar.NOVEMBER, 23, 19, 0, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val parsed = MatchScheduleParser.resolveStartTimeMillis(
      scheduleLabel = "Today • 8:30 PM IST",
      nowMillis = nowCal.timeInMillis
    )
    assertNotNull(parsed)
    val startCal = Calendar.getInstance(ist).apply { timeInMillis = parsed!! }
    assertEquals(20, startCal.get(Calendar.HOUR_OF_DAY))
    assertEquals(30, startCal.get(Calendar.MINUTE))

    val triggerAt = MatchScheduleParser.reminderTriggerMillis(parsed!!)
    assertEquals(MatchScheduleParser.TEN_MINUTES_MILLIS, parsed - triggerAt)

    val eightTwenty = parsed - MatchScheduleParser.TEN_MINUTES_MILLIS
    val eightNineteen = eightTwenty - 60_000L
    assertFalse(MatchScheduleParser.isInTenMinutePreStartWindow(parsed, eightNineteen))
    assertTrue(MatchScheduleParser.isInTenMinutePreStartWindow(parsed, eightTwenty))
  }

  @Test
  fun resolveStartTimeMillis_parsesTomorrowAndRelativeMinutes() {
    val ist = TimeZone.getTimeZone("Asia/Kolkata")
    val nowCal = Calendar.getInstance(ist).apply {
      set(2026, Calendar.NOVEMBER, 23, 18, 0, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val tomorrow = MatchScheduleParser.resolveStartTimeMillis(
      scheduleLabel = "Tomorrow • 6:00 PM IST",
      nowMillis = nowCal.timeInMillis
    )
    assertNotNull(tomorrow)
    val tomorrowCal = Calendar.getInstance(ist).apply { timeInMillis = tomorrow!! }
    assertEquals(24, tomorrowCal.get(Calendar.DAY_OF_MONTH))
    assertEquals(18, tomorrowCal.get(Calendar.HOUR_OF_DAY))

    val relative = MatchScheduleParser.resolveStartTimeMillis(
      scheduleLabel = "In 15 mins",
      nowMillis = nowCal.timeInMillis
    )
    assertEquals(nowCal.timeInMillis + 15 * 60_000L, relative)
    assertTrue(MatchScheduleParser.isRelativeMinutesSchedule("In 15 mins"))
  }
}

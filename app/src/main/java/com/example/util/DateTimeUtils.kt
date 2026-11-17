package com.example.util

import com.example.data.model.SalonProfile
import com.example.data.model.SlotLock
import com.example.data.model.SlotStatus
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

data class GeneratedSlot(
    val startMinutes: Int,
    val endMinutes: Int,
    val status: SlotStatus,
    val lockId: String? = null,
    val holderUid: String? = null,
    val expiresAtEpochMs: Long = 0L
)

data class CalendarDayCell(
    val dateString: String,
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val isPast: Boolean,
    val isToday: Boolean,
    val isWorkingDay: Boolean,
    val isFullyBooked: Boolean
)

object DateTimeUtils {
    const val DEFAULT_TIMEZONE = "Asia/Karachi" // Pakistan Standard Time (PKT)

    val supportedTimezones = listOf(
        "Asia/Karachi" to "Pakistan Standard Time (PKT, UTC+5)",
        "Asia/Dubai" to "Gulf Standard Time (GST, UTC+4)",
        "Europe/London" to "British Time (GMT/BST)",
        "America/New_York" to "Eastern Time (ET)",
        "UTC" to "Coordinated Universal Time (UTC)"
    )

    fun tz(timezoneId: String): TimeZone =
        TimeZone.getTimeZone(timezoneId.ifBlank { DEFAULT_TIMEZONE })

    fun todayDateString(timezoneId: String = DEFAULT_TIMEZONE): String {
        val cal = Calendar.getInstance(tz(timezoneId))
        return formatCalendarDate(cal)
    }

    fun currentYearMonth(timezoneId: String = DEFAULT_TIMEZONE): Pair<Int, Int> {
        val cal = Calendar.getInstance(tz(timezoneId))
        return cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
    }

    fun currentMinutesOfDay(timezoneId: String = DEFAULT_TIMEZONE): Int {
        val cal = Calendar.getInstance(tz(timezoneId))
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    fun formatCalendarDate(cal: Calendar): String {
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "%04d-%02d-%02d", y, m, d)
    }

    fun formatMonthYear(year: Int, monthZeroBased: Int, timezoneId: String = DEFAULT_TIMEZONE): String {
        val cal = Calendar.getInstance(tz(timezoneId)).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.US).apply {
            timeZone = tz(timezoneId)
        }
        return sdf.format(cal.time)
    }

    fun formatDisplayDate(dateStr: String): String {
        return try {
            val parts = dateStr.split("-")
            if (parts.size != 3) return dateStr
            val cal = Calendar.getInstance().apply {
                set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
            }
            SimpleDateFormat("EEE, dd MMM yyyy", Locale.US).format(cal.time)
        } catch (_: Exception) {
            dateStr
        }
    }

    fun formatMinutesAmPm(minutes: Int): String {
        val clamped = minutes.coerceIn(0, 1439)
        val hour24 = clamped / 60
        val min = clamped % 60
        val amPm = if (hour24 < 12) "AM" else "PM"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return String.format(Locale.US, "%02d:%02d %s", hour12, min, amPm)
    }

    fun formatPricePkr(amount: Double): String {
        val nf = NumberFormat.getNumberInstance(Locale.US).apply {
            maximumFractionDigits = 0
        }
        return "Rs. ${nf.format(amount)}"
    }

    fun dayShortName(dateStr: String, timezoneId: String = DEFAULT_TIMEZONE): String {
        return try {
            val parts = dateStr.split("-")
            val cal = Calendar.getInstance(tz(timezoneId)).apply {
                set(Calendar.YEAR, parts[0].toInt())
                set(Calendar.MONTH, parts[1].toInt() - 1)
                set(Calendar.DAY_OF_MONTH, parts[2].toInt())
            }
            when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "Sun"
                Calendar.MONDAY -> "Mon"
                Calendar.TUESDAY -> "Tue"
                Calendar.WEDNESDAY -> "Wed"
                Calendar.THURSDAY -> "Thu"
                Calendar.FRIDAY -> "Fri"
                Calendar.SATURDAY -> "Sat"
                else -> "Mon"
            }
        } catch (_: Exception) {
            "Mon"
        }
    }

    fun appointmentStartEpochMs(dateStr: String, startMinutes: Int, timezoneId: String = DEFAULT_TIMEZONE): Long {
        return try {
            val parts = dateStr.split("-")
            val cal = Calendar.getInstance(tz(timezoneId)).apply {
                set(Calendar.YEAR, parts[0].toInt())
                set(Calendar.MONTH, parts[1].toInt() - 1)
                set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                set(Calendar.HOUR_OF_DAY, startMinutes / 60)
                set(Calendar.MINUTE, startMinutes % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        } catch (_: Exception) {
            0L
        }
    }

    fun buildMonthCalendar(
        year: Int,
        monthZeroBased: Int,
        salon: SalonProfile?,
        slotLocks: List<SlotLock>,
        serviceDurationMinutes: Int,
        timezoneId: String = DEFAULT_TIMEZONE
    ): List<CalendarDayCell> {
        val todayStr = todayDateString(timezoneId)
        val cal = Calendar.getInstance(tz(timezoneId)).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // Sunday = 1
        val leadingEmpty = firstDayOfWeek - 1
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val cells = mutableListOf<CalendarDayCell>()
        repeat(leadingEmpty) {
            cells.add(
                CalendarDayCell(
                    dateString = "",
                    dayOfMonth = 0,
                    isCurrentMonth = false,
                    isPast = true,
                    isToday = false,
                    isWorkingDay = false,
                    isFullyBooked = false
                )
            )
        }

        val nowMs = System.currentTimeMillis()
        for (day in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            val dateStr = formatCalendarDate(cal)
            val isPast = dateStr < todayStr
            val isToday = dateStr == todayStr
            val dayName = dayShortName(dateStr, timezoneId)
            val isWorkingDay = salon == null || salon.workingDays.isEmpty() || salon.workingDays.contains(dayName)
            val isFullyBooked = if (salon != null && !isPast && isWorkingDay) {
                val slots = generateSlotsForDate(
                    salon = salon,
                    dateStr = dateStr,
                    durationMinutes = max(15, serviceDurationMinutes),
                    slotLocks = slotLocks,
                    currentUid = "",
                    nowEpochMs = nowMs,
                    timezoneId = timezoneId
                )
                slots.none { it.status == SlotStatus.AVAILABLE }
            } else {
                false
            }
            cells.add(
                CalendarDayCell(
                    dateString = dateStr,
                    dayOfMonth = day,
                    isCurrentMonth = true,
                    isPast = isPast,
                    isToday = isToday,
                    isWorkingDay = isWorkingDay,
                    isFullyBooked = isFullyBooked
                )
            )
        }
        return cells
    }

    fun generateSlotsForDate(
        salon: SalonProfile,
        dateStr: String,
        durationMinutes: Int,
        slotLocks: List<SlotLock>,
        currentUid: String,
        nowEpochMs: Long = System.currentTimeMillis(),
        timezoneId: String = DEFAULT_TIMEZONE
    ): List<GeneratedSlot> {
        val dayName = dayShortName(dateStr, timezoneId)
        if (salon.workingDays.isNotEmpty() && !salon.workingDays.contains(dayName)) {
            return emptyList()
        }
        val todayStr = todayDateString(timezoneId)
        if (dateStr < todayStr) return emptyList()

        val minStartToday = if (dateStr == todayStr) currentMinutesOfDay(timezoneId) + 15 else 0
        val effectiveDuration = durationMinutes.coerceAtLeast(15)
        val stepMinutes = 30
        val dateLocks = slotLocks.filter { it.date == dateStr }
        val result = mutableListOf<GeneratedSlot>()

        var start = salon.openingTimeMinutes
        while (start + effectiveDuration <= salon.closingTimeMinutes) {
            val end = start + effectiveDuration

            // Skip past times on today
            if (start < minStartToday) {
                start += stepMinutes
                continue
            }

            // Skip break window overlap if salon has configured a break
            val overlapsBreak = salon.breakStartMinutes >= 0 &&
                salon.breakEndMinutes > salon.breakStartMinutes &&
                start < salon.breakEndMinutes &&
                end > salon.breakStartMinutes

            if (!overlapsBreak) {
                // Check overlapping active slot locks
                val overlapping = dateLocks.filter { lock ->
                    val isActiveHold = lock.status == SlotStatus.SELECTED.value && lock.expiresAtEpochMs > nowEpochMs
                    val isBooked = lock.status == SlotStatus.BOOKED.value
                    (isActiveHold || isBooked) && start < lock.endMinutes && end > lock.startMinutes
                }

                val bookedLock = overlapping.firstOrNull { it.status == SlotStatus.BOOKED.value }
                val selectedLock = overlapping.firstOrNull { it.status == SlotStatus.SELECTED.value }

                val slot = when {
                    bookedLock != null -> GeneratedSlot(
                        startMinutes = start,
                        endMinutes = end,
                        status = SlotStatus.BOOKED,
                        lockId = bookedLock.id,
                        holderUid = bookedLock.holderUid
                    )
                    selectedLock != null -> GeneratedSlot(
                        startMinutes = start,
                        endMinutes = end,
                        status = SlotStatus.SELECTED,
                        lockId = selectedLock.id,
                        holderUid = selectedLock.holderUid,
                        expiresAtEpochMs = selectedLock.expiresAtEpochMs
                    )
                    else -> GeneratedSlot(
                        startMinutes = start,
                        endMinutes = end,
                        status = SlotStatus.AVAILABLE
                    )
                }
                result.add(slot)
            }
            start += stepMinutes
        }
        return result
    }
}

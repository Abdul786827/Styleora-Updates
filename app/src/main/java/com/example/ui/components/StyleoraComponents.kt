package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppNotification
import com.example.data.model.Appointment
import com.example.data.model.AppointmentStatus
import com.example.data.model.SalonProfile
import com.example.data.model.ServicePresets
import com.example.data.model.SlotLock
import com.example.data.model.SlotStatus
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneGoldLight
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.StatusAvailableContainer
import com.example.ui.theme.StatusAvailableGreen
import com.example.ui.theme.StatusBookedContainer
import com.example.ui.theme.StatusBookedRed
import com.example.ui.theme.StatusCancelledBlack
import com.example.ui.theme.StatusCancelledContainer
import com.example.ui.theme.StatusCompletedGold
import com.example.ui.theme.StatusSelectedContainer
import com.example.ui.theme.StatusSelectedOrange
import com.example.util.DateTimeUtils
import com.example.util.GeneratedSlot
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun StyleoraCrest(size: Dp = 44.dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.styleora_app_icon),
        contentDescription = "STYLEORA Logo",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(1.5.dp, ChampagneGold, CircleShape)
    )
}

@Composable
fun StyleoraTopBar(
    title: String,
    subtitle: String? = null,
    unreadNotificationCount: Int = 0,
    onNotificationsClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StyleoraCrest(size = 40.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        trailingContent?.invoke()
        if (onNotificationsClick != null) {
            IconButton(
                onClick = onNotificationsClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("notifications_button")
            ) {
                BadgedBox(
                    badge = {
                        if (unreadNotificationCount > 0) {
                            Badge(containerColor = ChampagneGold, contentColor = MatteBlack) {
                                Text(unreadNotificationCount.coerceAtMost(99).toString())
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

@Composable
fun StatusPill(statusText: String, modifier: Modifier = Modifier) {
    val (bg, fg, label) = when (statusText.uppercase()) {
        SlotStatus.AVAILABLE.value -> Triple(StatusAvailableContainer, StatusAvailableGreen, "Available")
        SlotStatus.SELECTED.value -> Triple(StatusSelectedContainer, StatusSelectedOrange, "Selected")
        AppointmentStatus.BOOKED.value -> Triple(StatusBookedContainer, StatusBookedRed, "Booked")
        AppointmentStatus.CANCELLED.value -> Triple(StatusCancelledContainer, StatusCancelledBlack, "Cancelled")
        AppointmentStatus.COMPLETED.value -> Triple(Color(0xFFF6EED9), StatusCompletedGold, "Completed")
        AppointmentStatus.PENDING.value -> Triple(StatusSelectedContainer, StatusSelectedOrange, "Pending")
        AppointmentStatus.REJECTED.value -> Triple(StatusBookedContainer, StatusBookedRed, "Rejected")
        else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, statusText)
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(fg)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = fg
            )
        }
    }
}

@Composable
fun StatusLegendRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendDot(color = StatusAvailableGreen, label = "Available", modifier = Modifier.weight(1f))
        LegendDot(color = StatusSelectedOrange, label = "Selected", modifier = Modifier.weight(1f))
        LegendDot(color = StatusBookedRed, label = "Booked", modifier = Modifier.weight(1f))
        LegendDot(color = StatusCancelledBlack, label = "Cancelled", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun LegendDot(color: Color, label: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun InteractiveCalendarPicker(
    selectedDate: String,
    salon: SalonProfile?,
    slotLocks: List<SlotLock>,
    serviceDurationMinutes: Int,
    timezoneId: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialYm = remember(timezoneId) { DateTimeUtils.currentYearMonth(timezoneId) }
    var year by remember { mutableIntStateOf(initialYm.first) }
    var month by remember { mutableIntStateOf(initialYm.second) }

    val cells = remember(year, month, salon, slotLocks, serviceDurationMinutes, timezoneId) {
        DateTimeUtils.buildMonthCalendar(
            year = year,
            monthZeroBased = month,
            salon = salon,
            slotLocks = slotLocks,
            serviceDurationMinutes = serviceDurationMinutes,
            timezoneId = timezoneId
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        if (month == 0) {
                            month = 11
                            year -= 1
                        } else {
                            month -= 1
                        }
                    },
                    modifier = Modifier.testTag("calendar_prev_month")
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month")
                }
                Text(
                    text = DateTimeUtils.formatMonthYear(year, month, timezoneId),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = {
                        if (month == 11) {
                            month = 0
                            year += 1
                        } else {
                            month += 1
                        }
                    },
                    modifier = Modifier.testTag("calendar_next_month")
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            val rows = cells.chunked(7)
            rows.forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (idx in 0 until 7) {
                        val cell = week.getOrNull(idx)
                        if (cell == null || !cell.isCurrentMonth) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val isSelected = cell.dateString == selectedDate
                            val selectable = !cell.isPast && cell.isWorkingDay && !cell.isFullyBooked
                            val bg by animateColorAsState(
                                targetValue = when {
                                    isSelected -> ChampagneGold
                                    cell.isToday -> MaterialTheme.colorScheme.primaryContainer
                                    cell.isFullyBooked -> StatusBookedContainer
                                    !cell.isPast && cell.isWorkingDay -> StatusAvailableContainer.copy(alpha = 0.55f)
                                    else -> Color.Transparent
                                },
                                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                label = "dayBg"
                            )
                            val textColor = when {
                                isSelected -> MatteBlack
                                cell.isPast || !cell.isWorkingDay -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                cell.isFullyBooked -> StatusBookedRed
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(bg)
                                    .then(
                                        if (cell.isToday && !isSelected) {
                                            Modifier.border(1.5.dp, ChampagneGold, CircleShape)
                                        } else Modifier
                                    )
                                    .clickable(enabled = selectable) { onDateSelected(cell.dateString) }
                                    .testTag("calendar_day_${cell.dateString}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = cell.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected || cell.isToday) FontWeight.Bold else FontWeight.Medium,
                                        color = textColor
                                    )
                                    if (!cell.isPast && cell.isWorkingDay && !isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(if (cell.isFullyBooked) StatusBookedRed else StatusAvailableGreen)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendDot(color = StatusAvailableGreen, label = "Open")
                LegendDot(color = ChampagneGold, label = "Today / Picked")
                LegendDot(color = StatusBookedRed, label = "Full")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveClockTimePicker(
    slots: List<GeneratedSlot>,
    selectedStartMinutes: Int?,
    currentUserId: String,
    holdExpiresAtEpochMs: Long,
    onSlotSelected: (GeneratedSlot) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPeriod by remember { mutableStateOf("ALL") } // ALL, AM, PM
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(holdExpiresAtEpochMs) {
        while (holdExpiresAtEpochMs > System.currentTimeMillis()) {
            nowMs = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val filteredSlots = remember(slots, selectedPeriod) {
        when (selectedPeriod) {
            "AM" -> slots.filter { it.startMinutes < 720 }
            "PM" -> slots.filter { it.startMinutes >= 720 }
            else -> slots
        }
    }

    val dialMinutes = selectedStartMinutes ?: filteredSlots.firstOrNull { it.status == SlotStatus.AVAILABLE }?.startMinutes ?: 540

    Column(modifier = modifier.fillMaxWidth()) {
        // Luxury Analog Clock Dial + Period Selector
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LuxuryClockDial(
                    minutesOfDay = dialMinutes,
                    modifier = Modifier.size(104.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (selectedStartMinutes != null) {
                            DateTimeUtils.formatMinutesAmPm(selectedStartMinutes)
                        } else {
                            "Choose Time"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = ChampagneGold
                    )
                    if (holdExpiresAtEpochMs > nowMs && selectedStartMinutes != null) {
                        val remSec = ((holdExpiresAtEpochMs - nowMs) / 1000L).coerceAtLeast(0L)
                        val min = remSec / 60
                        val sec = remSec % 60
                        Text(
                            text = "Held for %02d:%02d".format(min, sec),
                            style = MaterialTheme.typography.labelMedium,
                            color = StatusSelectedOrange
                        )
                    } else {
                        Text(
                            text = "Tap any green slot below",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("ALL" to "All", "AM" to "AM", "PM" to "PM").forEach { (key, label) ->
                            FilterChip(
                                selected = selectedPeriod == key,
                                onClick = { selectedPeriod = key },
                                label = { Text(label) },
                                modifier = Modifier.testTag("period_filter_$key")
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        StatusLegendRow()
        Spacer(modifier = Modifier.height(12.dp))

        if (filteredSlots.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "No time slots open for this period or day.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filteredSlots.forEach { slot ->
                    val isMineSelected = selectedStartMinutes == slot.startMinutes ||
                        (slot.status == SlotStatus.SELECTED && slot.holderUid == currentUserId)
                    val (bg, border, textColor) = when {
                        isMineSelected -> Triple(StatusSelectedContainer, StatusSelectedOrange, StatusSelectedOrange)
                        slot.status == SlotStatus.AVAILABLE -> Triple(StatusAvailableContainer, StatusAvailableGreen, StatusAvailableGreen)
                        slot.status == SlotStatus.SELECTED -> Triple(StatusSelectedContainer, StatusSelectedOrange, StatusSelectedOrange)
                        slot.status == SlotStatus.BOOKED -> Triple(StatusBookedContainer, StatusBookedRed, StatusBookedRed)
                        else -> Triple(StatusCancelledContainer, StatusCancelledBlack, StatusCancelledBlack)
                    }
                    val enabled = slot.status == SlotStatus.AVAILABLE || isMineSelected
                    Surface(
                        color = bg,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(if (isMineSelected) 2.dp else 1.dp, border),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = enabled) { onSlotSelected(slot) }
                            .testTag("time_slot_${slot.startMinutes}")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = DateTimeUtils.formatMinutesAmPm(slot.startMinutes),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Text(
                                text = when {
                                    isMineSelected -> "Selected"
                                    slot.status == SlotStatus.AVAILABLE -> "Open"
                                    slot.status == SlotStatus.SELECTED -> "Held"
                                    else -> "Booked"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LuxuryClockDial(minutesOfDay: Int, modifier: Modifier = Modifier) {
    val hour = (minutesOfDay / 60) % 12
    val minute = minutesOfDay % 60
    val bgColor = MaterialTheme.colorScheme.surfaceVariant
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val handColor = ChampagneGold
    val hubColor = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(color = bgColor, radius = radius, center = center)
        drawCircle(
            color = handColor,
            radius = radius - 2f,
            center = center,
            style = Stroke(width = 3f)
        )

        for (i in 0 until 12) {
            val angle = (i * 30.0 - 90.0) * PI / 180.0
            val outer = Offset(
                center.x + (radius * 0.85f * cos(angle)).toFloat(),
                center.y + (radius * 0.85f * sin(angle)).toFloat()
            )
            val inner = Offset(
                center.x + (radius * 0.72f * cos(angle)).toFloat(),
                center.y + (radius * 0.72f * sin(angle)).toFloat()
            )
            drawLine(color = tickColor, start = inner, end = outer, strokeWidth = 2.5f, cap = StrokeCap.Round)
        }

        val hourAngle = ((hour + minute / 60.0) * 30.0 - 90.0) * PI / 180.0
        val hourEnd = Offset(
            center.x + (radius * 0.48f * cos(hourAngle)).toFloat(),
            center.y + (radius * 0.48f * sin(hourAngle)).toFloat()
        )
        drawLine(color = hubColor, start = center, end = hourEnd, strokeWidth = 5f, cap = StrokeCap.Round)

        val minAngle = (minute * 6.0 - 90.0) * PI / 180.0
        val minEnd = Offset(
            center.x + (radius * 0.72f * cos(minAngle)).toFloat(),
            center.y + (radius * 0.72f * sin(minAngle)).toFloat()
        )
        drawLine(color = handColor, start = center, end = minEnd, strokeWidth = 3.5f, cap = StrokeCap.Round)

        drawCircle(color = handColor, radius = 5.5f, center = center)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CancelAppointmentDialog(
    appointment: Appointment,
    onDismiss: () -> Unit,
    onConfirmCancel: (String) -> Unit
) {
    var selectedReason by remember { mutableStateOf("") }
    var customReason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Cancel Appointment", style = MaterialTheme.typography.headlineMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Are you sure you want to cancel this appointment?",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${appointment.salonName} • ${DateTimeUtils.formatDisplayDate(appointment.date)} at ${DateTimeUtils.formatMinutesAmPm(appointment.startMinutes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Reason (Optional):",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ServicePresets.cancellationReasons.forEach { reason ->
                        FilterChip(
                            selected = selectedReason == reason,
                            onClick = {
                                selectedReason = if (selectedReason == reason) "" else reason
                                if (selectedReason.isNotBlank()) customReason = ""
                            },
                            label = { Text(reason, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                OutlinedTextField(
                    value = customReason,
                    onValueChange = {
                        customReason = it
                        if (it.isNotBlank()) selectedReason = ""
                    },
                    label = { Text("Other reason") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalReason = customReason.ifBlank { selectedReason }
                    onConfirmCancel(finalReason)
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusBookedRed, contentColor = Color.White),
                modifier = Modifier.testTag("confirm_cancel_appointment_button")
            ) {
                Text("Confirm Cancellation")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Keep Appointment") }
        }
    )
}

@Composable
fun ReviewAppointmentDialog(
    appointment: Appointment,
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit
) {
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate Your Visit", style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "How was your experience at ${appointment.salonName}?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    for (star in 1..5) {
                        IconButton(
                            onClick = { rating = star },
                            modifier = Modifier.testTag("review_star_$star")
                        ) {
                            Icon(
                                imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "$star stars",
                                tint = ChampagneGold,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Write a review (optional)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(rating, comment) },
                modifier = Modifier.testTag("submit_review_button")
            ) {
                Text("Submit Review")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun RescheduleAppointmentDialog(
    appointment: Appointment,
    salon: SalonProfile?,
    slotLocks: List<SlotLock>,
    timezoneId: String,
    onDismiss: () -> Unit,
    onConfirmReschedule: (String, Int, Int) -> Unit
) {
    var newDate by remember { mutableStateOf(appointment.date) }
    var selectedStart by remember { mutableStateOf<Int?>(null) }
    var selectedEnd by remember { mutableStateOf<Int?>(null) }

    val slots = remember(salon, newDate, slotLocks, appointment.totalDurationMinutes) {
        if (salon == null) emptyList()
        else DateTimeUtils.generateSlotsForDate(
            salon = salon,
            dateStr = newDate,
            durationMinutes = appointment.totalDurationMinutes,
            slotLocks = slotLocks,
            currentUid = appointment.customerId,
            timezoneId = timezoneId
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reschedule Appointment", style = MaterialTheme.typography.headlineMedium) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().height(420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    InteractiveCalendarPicker(
                        selectedDate = newDate,
                        salon = salon,
                        slotLocks = slotLocks,
                        serviceDurationMinutes = appointment.totalDurationMinutes,
                        timezoneId = timezoneId,
                        onDateSelected = {
                            newDate = it
                            selectedStart = null
                            selectedEnd = null
                        }
                    )
                }
                item {
                    InteractiveClockTimePicker(
                        slots = slots,
                        selectedStartMinutes = selectedStart,
                        currentUserId = appointment.customerId,
                        holdExpiresAtEpochMs = 0L,
                        onSlotSelected = { slot ->
                            selectedStart = slot.startMinutes
                            selectedEnd = slot.endMinutes
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedStart != null && selectedEnd != null,
                onClick = {
                    if (selectedStart != null && selectedEnd != null) {
                        onConfirmReschedule(newDate, selectedStart!!, selectedEnd!!)
                    }
                },
                modifier = Modifier.testTag("confirm_reschedule_button")
            ) {
                Text("Reschedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSheet(
    notifications: List<AppNotification>,
    onMarkRead: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Notifications", style = MaterialTheme.typography.headlineMedium)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close notifications")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "You're all caught up. Booking updates appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(380.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notifications, key = { it.id }) { notif ->
                        Card(
                            onClick = { if (!notif.isRead) onMarkRead(notif.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (notif.isRead) MaterialTheme.colorScheme.surface
                                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                            ),
                            border = BorderStroke(1.dp, if (notif.isRead) MaterialTheme.colorScheme.outlineVariant else ChampagneGold)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = if (notif.isRead) MaterialTheme.colorScheme.onSurfaceVariant else ChampagneGold,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = notif.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = notif.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

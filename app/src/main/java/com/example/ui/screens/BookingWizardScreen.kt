package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Man
import androidx.compose.material.icons.filled.Woman
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.PaymentMethod
import com.example.data.model.SalonProfile
import com.example.data.model.SalonService
import com.example.data.model.ServiceCategory
import com.example.data.model.SlotLock
import com.example.data.model.SlotStatus
import com.example.ui.components.InteractiveCalendarPicker
import com.example.ui.components.InteractiveClockTimePicker
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.MatteBlack
import com.example.ui.viewmodel.BookingWizardState
import com.example.util.DateTimeUtils

@Composable
fun BookingWizardScreen(
    state: BookingWizardState,
    salons: List<SalonProfile>,
    services: List<SalonService>,
    slotLocks: List<SlotLock>,
    currentUserId: String,
    isBusy: Boolean,
    onStepChange: (Int) -> Unit,
    onSelectCategory: (String) -> Unit,
    onSelectSalon: (SalonProfile) -> Unit,
    onToggleService: (SalonService) -> Unit,
    onUpdateCustomerInfo: (String, String, String, String, String, String) -> Unit,
    onSelectDate: (String) -> Unit,
    onHoldSlot: (Int, Int) -> Unit,
    onConfirmBooking: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler {
        if (state.step > 1) onStepChange(state.step - 1) else onClose()
    }

    val salonServices = remember(services, state.selectedSalon, state.category) {
        val salonId = state.selectedSalon?.id ?: return@remember emptyList()
        services.filter { svc ->
            svc.salonId == salonId && svc.isAvailable &&
                (svc.category == state.category || svc.category == ServiceCategory.BOTH.value)
        }
    }

    val stepTitle = when (state.step) {
        1 -> "Step 1: Select Category"
        2 -> "Step 2: Choose Salon / Barber"
        3 -> "Step 3: Select Services"
        4 -> "Step 4: Your Information"
        5 -> "Step 5: Appointment Date"
        else -> "Step 6: Time & Confirm"
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (state.step > 1) onStepChange(state.step - 1) else onClose() },
                        modifier = Modifier.testTag("wizard_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stepTitle, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "${state.step} of 6",
                            style = MaterialTheme.typography.labelSmall,
                            color = ChampagneGold
                        )
                    }
                    if (state.selectedServices.isNotEmpty()) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = DateTimeUtils.formatPricePkr(state.totalPrice),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ChampagneGold
                            )
                            Text(
                                text = "${state.totalDurationMinutes} min",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { state.step / 6f },
                    modifier = Modifier.fillMaxWidth(),
                    color = ChampagneGold
                )
            }
        },
        bottomBar = {
            WizardBottomBar(
                state = state,
                isBusy = isBusy,
                onBack = { if (state.step > 1) onStepChange(state.step - 1) else onClose() },
                onNext = { onStepChange(state.step + 1) },
                onConfirm = onConfirmBooking
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (state.step) {
                1 -> {
                    item {
                        Text(
                            text = "Select the service category you are booking for:",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    item {
                        CategoryChoiceCard(
                            title = "Male Grooming",
                            subtitle = "Haircuts, Beard Styling, Shaves, Facials & Grooming Packages",
                            icon = Icons.Filled.Man,
                            selected = state.category == ServiceCategory.MALE.value,
                            onClick = { onSelectCategory(ServiceCategory.MALE.value) },
                            modifier = Modifier.testTag("wizard_cat_male")
                        )
                    }
                    item {
                        CategoryChoiceCard(
                            title = "Female Salon & Beauty",
                            subtitle = "Haircuts, Highlights, Keratin, Bridal Makeup, Nails & Spa",
                            icon = Icons.Filled.Woman,
                            selected = state.category == ServiceCategory.FEMALE.value,
                            onClick = { onSelectCategory(ServiceCategory.FEMALE.value) },
                            modifier = Modifier.testTag("wizard_cat_female")
                        )
                    }
                }

                2 -> {
                    if (salons.isEmpty()) {
                        item {
                            Text(
                                text = "No salons are currently registered. Please ask a barber/salon owner to create their salon profile first.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(salons, key = { it.id }) { salon ->
                            val isSelected = state.selectedSalon?.id == salon.id
                            Card(
                                onClick = { onSelectSalon(salon) },
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) ChampagneGold else MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth().testTag("wizard_salon_${salon.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(salon.salonName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("Lead Barber: ${salon.ownerName}", style = MaterialTheme.typography.bodySmall, color = ChampagneGold)
                                        Text(salon.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = "Selected", tint = ChampagneGold)
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            border = BorderStroke(1.dp, ChampagneGold)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Selected: ${state.selectedServices.size} service(s)", fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "${DateTimeUtils.formatPricePkr(state.totalPrice)} • ${state.totalDurationMinutes} min",
                                    fontWeight = FontWeight.Bold,
                                    color = ChampagneGold
                                )
                            }
                        }
                    }
                    if (salonServices.isEmpty()) {
                        item {
                            Text(
                                text = "This salon has not added ${state.category.lowercase()} services yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(salonServices, key = { it.id }) { svc ->
                            val checked = state.selectedServices.any { it.id == svc.id }
                            Card(
                                onClick = { onToggleService(svc) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(if (checked) 2.dp else 1.dp, if (checked) ChampagneGold else MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth().testTag("wizard_service_${svc.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(checked = checked, onCheckedChange = { onToggleService(svc) })
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(svc.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        if (!svc.description.isNullOrBlank()) {
                                            Text(svc.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text("${svc.durationMinutes} min", style = MaterialTheme.typography.labelSmall, color = ChampagneGold)
                                    }
                                    Text(
                                        text = DateTimeUtils.formatPricePkr(svc.effectivePrice),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                4 -> {
                    item {
                        CustomerInfoStep(
                            state = state,
                            acceptedPayments = state.selectedSalon?.acceptedPaymentMethods ?: listOf(PaymentMethod.PAY_AT_SALON.value),
                            onUpdate = onUpdateCustomerInfo
                        )
                    }
                }

                5 -> {
                    item {
                        val salon = state.selectedSalon
                        InteractiveCalendarPicker(
                            selectedDate = state.selectedDate,
                            salon = salon,
                            slotLocks = slotLocks,
                            serviceDurationMinutes = state.totalDurationMinutes,
                            timezoneId = salon?.timezone ?: DateTimeUtils.DEFAULT_TIMEZONE,
                            onDateSelected = onSelectDate
                        )
                    }
                }

                6 -> {
                    val salon = state.selectedSalon
                    if (salon != null) {
                        item {
                            Text(
                                text = "Date: ${DateTimeUtils.formatDisplayDate(state.selectedDate)} (${salon.timezone})",
                                style = MaterialTheme.typography.titleSmall,
                                color = ChampagneGold
                            )
                        }
                        item {
                            val slots = remember(salon, state.selectedDate, state.totalDurationMinutes, slotLocks) {
                                DateTimeUtils.generateSlotsForDate(
                                    salon = salon,
                                    dateStr = state.selectedDate,
                                    durationMinutes = state.totalDurationMinutes,
                                    slotLocks = slotLocks,
                                    currentUid = currentUserId,
                                    timezoneId = salon.timezone
                                )
                            }
                            InteractiveClockTimePicker(
                                slots = slots,
                                selectedStartMinutes = state.selectedStartMinutes,
                                currentUserId = currentUserId,
                                holdExpiresAtEpochMs = state.holdExpiresAtEpochMs,
                                onSlotSelected = { slot ->
                                    if (slot.status == SlotStatus.AVAILABLE) {
                                        onHoldSlot(slot.startMinutes, slot.endMinutes)
                                    }
                                }
                            )
                        }
                        item {
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(6.dp))
                            BookingSummaryCard(state)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChoiceCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) ChampagneGold else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) {
                Icon(Icons.Filled.CheckCircle, contentDescription = "Selected", tint = ChampagneGold)
            }
        }
    }
}

@Composable
private fun CustomerInfoStep(
    state: BookingWizardState,
    acceptedPayments: List<String>,
    onUpdate: (String, String, String, String, String, String) -> Unit
) {
    var name by remember(state.customerName) { mutableStateOf(state.customerName) }
    var phone by remember(state.customerPhone) { mutableStateOf(state.customerPhone) }
    var email by remember(state.customerEmail) { mutableStateOf(state.customerEmail) }
    var gender by remember(state.customerGender) { mutableStateOf(state.customerGender) }
    var notes by remember(state.specialInstructions) { mutableStateOf(state.specialInstructions) }
    var payment by remember(state.paymentMethod) { mutableStateOf(state.paymentMethod) }

    fun sync() = onUpdate(name, phone, email, gender, notes, payment)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; sync() },
            label = { Text("Customer Name *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("wizard_name_input")
        )
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it; sync() },
            label = { Text("Mobile Number *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("wizard_phone_input")
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; sync() },
            label = { Text("Email Address *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("wizard_email_input")
        )
        Text("Gender", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("MALE", "FEMALE", "OTHER").forEach { g ->
                FilterChip(
                    selected = gender == g,
                    onClick = { gender = g; sync() },
                    label = { Text(g) }
                )
            }
        }
        Text("Payment Method", style = MaterialTheme.typography.titleSmall)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PaymentMethod.entries.filter { acceptedPayments.contains(it.value) }.forEach { pm ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (payment == pm.value) ChampagneGold else MaterialTheme.colorScheme.outlineVariant),
                    color = if (payment == pm.value) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().clickable { payment = pm.value; sync() }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(pm.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        if (payment == pm.value) Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it; sync() },
            label = { Text("Special Instructions (Optional)") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BookingSummaryCard(state: BookingWizardState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, ChampagneGold)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Booking Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Salon: ${state.selectedSalon?.salonName ?: "-"}")
            Text("Services: ${state.selectedServices.joinToString(", ") { it.name }}")
            Text("Date: ${DateTimeUtils.formatDisplayDate(state.selectedDate)}")
            Text(
                text = if (state.selectedStartMinutes != null) {
                    "Time: ${DateTimeUtils.formatMinutesAmPm(state.selectedStartMinutes)} - ${DateTimeUtils.formatMinutesAmPm(state.selectedEndMinutes ?: state.selectedStartMinutes)}"
                } else {
                    "Time: Select an available slot above"
                }
            )
            Text(
                text = "Total: ${DateTimeUtils.formatPricePkr(state.totalPrice)} • ${state.totalDurationMinutes} min",
                fontWeight = FontWeight.Bold,
                color = ChampagneGold
            )
        }
    }
}

@Composable
private fun WizardBottomBar(
    state: BookingWizardState,
    isBusy: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onConfirm: () -> Unit
) {
    val canAdvance = when (state.step) {
        1 -> true
        2 -> state.selectedSalon != null
        3 -> state.selectedServices.isNotEmpty()
        4 -> state.customerName.trim().length >= 2 && state.customerPhone.trim().length >= 7 && state.customerEmail.contains("@")
        5 -> state.selectedDate.isNotBlank()
        6 -> state.selectedStartMinutes != null && !state.isSubmitting && !isBusy
        else -> false
    }

    Surface(tonalElevation = 4.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Text(if (state.step == 1) "Cancel" else "Back")
            }
            if (state.step < 6) {
                Button(
                    onClick = onNext,
                    enabled = canAdvance,
                    colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = MatteBlack),
                    modifier = Modifier.weight(1.5f).height(50.dp).testTag("wizard_next_button")
                ) {
                    Text("Continue", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onConfirm,
                    enabled = canAdvance,
                    colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = MatteBlack),
                    modifier = Modifier.weight(1.5f).height(50.dp).testTag("wizard_confirm_booking_button")
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MatteBlack, strokeWidth = 2.dp)
                    } else {
                        Text("Confirm Booking", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

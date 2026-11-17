package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.Appointment
import com.example.data.model.AppointmentStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.SalonProfile
import com.example.data.model.SalonService
import com.example.data.model.ServiceCategory
import com.example.data.model.ServicePreset
import com.example.data.model.ServicePresets
import com.example.data.model.SlotLock
import com.example.data.model.SlotStatus
import com.example.data.model.UserProfile
import com.example.ui.components.CancelAppointmentDialog
import com.example.ui.components.InteractiveCalendarPicker
import com.example.ui.components.RescheduleAppointmentDialog
import com.example.ui.components.StatusLegendRow
import com.example.ui.components.StatusPill
import com.example.ui.components.StyleoraTopBar
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.StatusAvailableGreen
import com.example.ui.theme.StatusBookedRed
import com.example.util.DateTimeUtils

@Composable
fun BarberDashboardScreen(
    profile: UserProfile,
    salon: SalonProfile?,
    appointments: List<Appointment>,
    slotLocks: List<SlotLock>,
    unreadNotifications: Int,
    onOpenNotifications: () -> Unit,
    onSaveSalon: (SalonProfile) -> Unit
) {
    var showEditSalon by remember { mutableStateOf(false) }

    val tzId = salon?.timezone ?: DateTimeUtils.DEFAULT_TIMEZONE
    val today = remember(tzId) { DateTimeUtils.todayDateString(tzId) }

    val totalCount = appointments.size
    val todayCount = appointments.count { it.date == today && it.status != AppointmentStatus.CANCELLED.value }
    val upcomingCount = appointments.count { it.date >= today && (it.status == AppointmentStatus.BOOKED.value || it.status == AppointmentStatus.PENDING.value) }
    val completedCount = appointments.count { it.status == AppointmentStatus.COMPLETED.value }
    val cancelledCount = appointments.count { it.status == AppointmentStatus.CANCELLED.value }
    val totalCustomers = appointments.map { it.customerId }.distinct().size
    val totalEarnings = appointments
        .filter { it.status == AppointmentStatus.COMPLETED.value || it.paymentStatus == PaymentStatus.PAID.value }
        .sumOf { it.totalPrice }
    val availableSlotsToday = remember(salon, slotLocks, today) {
        if (salon == null) 0
        else DateTimeUtils.generateSlotsForDate(
            salon = salon,
            dateStr = today,
            durationMinutes = 30,
            slotLocks = slotLocks,
            currentUid = "",
            timezoneId = tzId
        ).count { it.status == SlotStatus.AVAILABLE }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            StyleoraTopBar(
                title = salon?.salonName ?: "Barber Studio",
                subtitle = "Owner: ${profile.fullName}",
                unreadNotificationCount = unreadNotifications,
                onNotificationsClick = onOpenNotifications
            )
        }

        if (salon == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    border = BorderStroke(1.5.dp, ChampagneGold)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.Storefront, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(34.dp))
                        Text("Create Your Salon Profile", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = "Publish your salon hours, location, payment methods, and cancellation policy so customers can discover and book your services.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = { showEditSalon = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = MatteBlack),
                            modifier = Modifier.fillMaxWidth().testTag("create_salon_profile_button")
                        ) {
                            Text("Set Up Salon Profile", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(salon.salonName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(salon.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            OutlinedButton(
                                onClick = { showEditSalon = true },
                                modifier = Modifier.testTag("edit_salon_profile_button")
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Manage Salon")
                            }
                        }
                        Text(
                            text = "Hours: ${DateTimeUtils.formatMinutesAmPm(salon.openingTimeMinutes)} – ${DateTimeUtils.formatMinutesAmPm(salon.closingTimeMinutes)} • ${salon.timezone}",
                            style = MaterialTheme.typography.labelMedium,
                            color = ChampagneGold
                        )
                    }
                }
            }
        }

        // KPI Grid (2 columns)
        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiCard("Total Earnings", DateTimeUtils.formatPricePkr(totalEarnings), ChampagneGold, Modifier.weight(1f))
                    KpiCard("Today's Slots Open", "$availableSlotsToday slots", StatusAvailableGreen, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiCard("Today's Visits", todayCount.toString(), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                    KpiCard("Upcoming", upcomingCount.toString(), ChampagneGold, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiCard("Completed", completedCount.toString(), StatusAvailableGreen, Modifier.weight(1f))
                    KpiCard("Cancelled", cancelledCount.toString(), StatusBookedRed, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiCard("Total Appointments", totalCount.toString(), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                    KpiCard("Total Customers", totalCustomers.toString(), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
                }
            }
        }
    }

    if (showEditSalon) {
        EditSalonProfileSheet(
            initial = salon ?: SalonProfile(
                ownerId = profile.uid,
                ownerName = profile.fullName,
                contactNumber = profile.mobileNumber,
                whatsappNumber = profile.mobileNumber
            ),
            onDismiss = { showEditSalon = false },
            onSave = { updated ->
                showEditSalon = false
                onSaveSalon(updated)
            }
        )
    }
}

@Composable
private fun KpiCard(title: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = accent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditSalonProfileSheet(
    initial: SalonProfile,
    onDismiss: () -> Unit,
    onSave: (SalonProfile) -> Unit
) {
    var salonName by remember { mutableStateOf(initial.salonName) }
    var ownerName by remember { mutableStateOf(initial.ownerName) }
    var contact by remember { mutableStateOf(initial.contactNumber) }
    var whatsapp by remember { mutableStateOf(initial.whatsappNumber ?: "") }
    var address by remember { mutableStateOf(initial.address) }
    var mapsLocation by remember { mutableStateOf(initial.mapsLocation ?: "") }
    var description by remember { mutableStateOf(initial.description ?: "") }
    var logoUri by remember { mutableStateOf(initial.logoUri) }
    var coverUri by remember { mutableStateOf(initial.coverImageUri) }
    var workingDays by remember { mutableStateOf(initial.workingDays) }
    var openHour by remember { mutableIntStateOf(initial.openingTimeMinutes / 60) }
    var closeHour by remember { mutableIntStateOf(initial.closingTimeMinutes / 60) }
    var breakStartHour by remember { mutableIntStateOf(if (initial.breakStartMinutes >= 0) initial.breakStartMinutes / 60 else -1) }
    var payments by remember { mutableStateOf(initial.acceptedPaymentMethods) }
    var allowCancel by remember { mutableStateOf(initial.allowCustomerCancellation) }
    var minCancelHours by remember { mutableStateOf(initial.minCancellationHours.toString()) }
    var cancelApproval by remember { mutableStateOf(initial.cancellationRequiresApproval) }
    var cancelPolicyText by remember { mutableStateOf(initial.cancellationPolicyText ?: "") }
    var holdMinutes by remember { mutableStateOf(initial.holdTimeoutMinutes.toString()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) logoUri = uri.toString()
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) coverUri = uri.toString()
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Manage Salon Profile", style = MaterialTheme.typography.headlineMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.weight(1f)
                ) { Text(if (logoUri != null) "Logo Selected" else "Pick Salon Logo") }
                OutlinedButton(
                    onClick = { coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.weight(1f)
                ) { Text(if (coverUri != null) "Cover Selected" else "Pick Cover Image") }
            }

            OutlinedTextField(value = salonName, onValueChange = { salonName = it }, label = { Text("Salon Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("salon_name_input"))
            OutlinedTextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text("Owner Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Contact Number *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = whatsapp, onValueChange = { whatsapp = it }, label = { Text("WhatsApp Number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Salon Address *") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("salon_address_input"))
            OutlinedTextField(value = mapsLocation, onValueChange = { mapsLocation = it }, label = { Text("Google Maps URL or Coordinates") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Salon Description") }, minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())

            Text("Working Days", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                    val selected = workingDays.contains(day)
                    FilterChip(
                        selected = selected,
                        onClick = {
                            workingDays = if (selected) workingDays - day else workingDays + day
                        },
                        label = { Text(day) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { openHour = if (openHour >= 12) 7 else openHour + 1 },
                    modifier = Modifier.weight(1f)
                ) { Text("Opens: ${DateTimeUtils.formatMinutesAmPm(openHour * 60)}") }
                OutlinedButton(
                    onClick = { closeHour = if (closeHour >= 23) 17 else closeHour + 1 },
                    modifier = Modifier.weight(1f)
                ) { Text("Closes: ${DateTimeUtils.formatMinutesAmPm(closeHour * 60)}") }
            }
            OutlinedButton(
                onClick = {
                    breakStartHour = when (breakStartHour) {
                        -1 -> 13
                        13 -> 14
                        else -> -1
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (breakStartHour < 0) "Break Time: None (Tap to add 1h break)"
                    else "Break Time: ${DateTimeUtils.formatMinutesAmPm(breakStartHour * 60)} - ${DateTimeUtils.formatMinutesAmPm((breakStartHour + 1) * 60)}"
                )
            }

            Text("Accepted Payment Methods", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PaymentMethod.entries.forEach { pm ->
                    val active = payments.contains(pm.value)
                    FilterChip(
                        selected = active,
                        onClick = {
                            payments = if (active && payments.size > 1) payments - pm.value else if (!active) payments + pm.value else payments
                        },
                        label = { Text(pm.label) }
                    )
                }
            }

            Text("Slot Hold & Cancellation Policy", style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Allow Customers to Cancel")
                Switch(checked = allowCancel, onCheckedChange = { allowCancel = it })
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Cancellation Requires Approval")
                Switch(checked = cancelApproval, onCheckedChange = { cancelApproval = it })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = minCancelHours,
                    onValueChange = { minCancelHours = it.filter { c -> c.isDigit() } },
                    label = { Text("Min Cancel Notice (hrs)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = holdMinutes,
                    onValueChange = { holdMinutes = it.filter { c -> c.isDigit() } },
                    label = { Text("Slot Hold (min)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = cancelPolicyText,
                onValueChange = { cancelPolicyText = it },
                label = { Text("Cancellation Policy Notes") },
                modifier = Modifier.fillMaxWidth()
            )

            if (!errorMsg.isNullOrBlank()) {
                Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    when {
                        salonName.trim().length < 2 -> errorMsg = "Enter a valid salon name."
                        address.trim().length < 2 -> errorMsg = "Enter your salon address."
                        closeHour <= openHour -> errorMsg = "Closing time must be after opening time."
                        else -> {
                            onSave(
                                initial.copy(
                                    salonName = salonName.trim(),
                                    ownerName = ownerName.trim().ifBlank { "Owner" },
                                    logoUri = logoUri,
                                    coverImageUri = coverUri,
                                    contactNumber = contact.trim(),
                                    whatsappNumber = whatsapp.trim().takeIf { it.isNotBlank() },
                                    address = address.trim(),
                                    mapsLocation = mapsLocation.trim().takeIf { it.isNotBlank() },
                                    description = description.trim().takeIf { it.isNotBlank() },
                                    workingDays = workingDays,
                                    openingTimeMinutes = openHour * 60,
                                    closingTimeMinutes = closeHour * 60,
                                    breakStartMinutes = if (breakStartHour >= 0) breakStartHour * 60 else -1,
                                    breakEndMinutes = if (breakStartHour >= 0) (breakStartHour + 1) * 60 else -1,
                                    acceptedPaymentMethods = payments,
                                    allowCustomerCancellation = allowCancel,
                                    minCancellationHours = minCancelHours.toIntOrNull()?.coerceIn(0, 168) ?: 2,
                                    cancellationRequiresApproval = cancelApproval,
                                    cancellationPolicyText = cancelPolicyText.trim().takeIf { it.isNotBlank() },
                                    holdTimeoutMinutes = holdMinutes.toIntOrNull()?.coerceIn(1, 60) ?: 10
                                )
                            )
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = MatteBlack),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_salon_profile_button")
            ) {
                Text("Save Salon Profile", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun BarberServicesScreen(
    salon: SalonProfile?,
    services: List<SalonService>,
    unreadNotifications: Int,
    onOpenNotifications: () -> Unit,
    onSaveService: (SalonService) -> Unit,
    onAddPresets: (SalonProfile, List<ServicePreset>) -> Unit,
    onToggleAvailable: (SalonService) -> Unit,
    onDeleteService: (String) -> Unit
) {
    var categoryTab by remember { mutableStateOf("MALE") } // MALE, FEMALE, BOTH
    var editingService by remember { mutableStateOf<SalonService?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showPresetsDialog by remember { mutableStateOf(false) }

    val filtered = remember(services, categoryTab) {
        services.filter { it.category == categoryTab || (categoryTab != "BOTH" && it.category == ServiceCategory.BOTH.value) }
    }

    Scaffold(
        floatingActionButton = {
            if (salon != null) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = ChampagneGold,
                    contentColor = MatteBlack,
                    modifier = Modifier.testTag("add_custom_service_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Custom Service")
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            StyleoraTopBar(
                title = "Manage Services",
                subtitle = "Male & Female service catalogs",
                unreadNotificationCount = unreadNotifications,
                onNotificationsClick = onOpenNotifications
            )

            if (salon == null) {
                Box(modifier = Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Please create your Salon Profile in the Dashboard tab first before adding services.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("MALE" to "Male", "FEMALE" to "Female", "BOTH" to "Unisex").forEach { (key, label) ->
                        FilterChip(
                            selected = categoryTab == key,
                            onClick = { categoryTab = key },
                            label = { Text(label) },
                            modifier = Modifier.testTag("barber_svc_tab_$key")
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    OutlinedButton(
                        onClick = { showPresetsDialog = true },
                        modifier = Modifier.testTag("quick_add_presets_button")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Presets")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (filtered.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("No ${categoryTab.lowercase()} services added yet.", style = MaterialTheme.typography.titleMedium)
                                    Text("Tap 'Presets' to import standard Male/Female salon services or '+' to add a custom service.")
                                }
                            }
                        }
                    } else {
                        items(filtered, key = { it.id }) { svc ->
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!svc.imageUri.isNullOrBlank()) {
                                        AsyncImage(
                                            model = svc.imageUri,
                                            contentDescription = svc.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(svc.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "${svc.category} • ${svc.durationMinutes} min • ${DateTimeUtils.formatPricePkr(svc.effectivePrice)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ChampagneGold
                                        )
                                        Text(
                                            text = if (svc.isAvailable) "Available for booking" else "Paused",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (svc.isAvailable) StatusAvailableGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = svc.isAvailable,
                                        onCheckedChange = { onToggleAvailable(svc) }
                                    )
                                    IconButton(onClick = { editingService = svc }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit service")
                                    }
                                    IconButton(onClick = { onDeleteService(svc.id) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete service", tint = StatusBookedRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if ((showAddDialog || editingService != null) && salon != null) {
        EditServiceDialog(
            salon = salon,
            initial = editingService ?: SalonService(
                salonId = salon.id,
                barberId = salon.ownerId,
                salonName = salon.salonName,
                category = categoryTab
            ),
            onDismiss = {
                showAddDialog = false
                editingService = null
            },
            onSave = { svc ->
                showAddDialog = false
                editingService = null
                onSaveService(svc)
            }
        )
    }

    if (showPresetsDialog && salon != null) {
        ServicePresetsDialog(
            category = categoryTab,
            onDismiss = { showPresetsDialog = false },
            onImport = { chosen ->
                showPresetsDialog = false
                onAddPresets(salon, chosen)
            }
        )
    }
}

@Composable
private fun EditServiceDialog(
    salon: SalonProfile,
    initial: SalonService,
    onDismiss: () -> Unit,
    onSave: (SalonService) -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    var category by remember { mutableStateOf(initial.category) }
    var description by remember { mutableStateOf(initial.description ?: "") }
    var priceStr by remember { mutableStateOf(if (initial.price > 0) initial.price.toInt().toString() else "") }
    var discountStr by remember { mutableStateOf(initial.discountPrice?.toInt()?.toString() ?: "") }
    var durationStr by remember { mutableStateOf(initial.durationMinutes.toString()) }
    var imageUri by remember { mutableStateOf(initial.imageUri) }
    var isAvailable by remember { mutableStateOf(initial.isAvailable) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) imageUri = uri.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id.isBlank()) "Add Custom Service" else "Edit Service", style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Service Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("service_name_input"))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ServiceCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat.value,
                            onClick = { category = cat.value },
                            label = { Text(cat.label) }
                        )
                    }
                }
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Price (PKR) *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("service_price_input")
                    )
                    OutlinedTextField(
                        value = discountStr,
                        onValueChange = { discountStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Discount (Opt)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = durationStr,
                    onValueChange = { durationStr = it.filter { c -> c.isDigit() } },
                    label = { Text("Duration (Minutes) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("service_duration_input")
                )
                OutlinedButton(
                    onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (imageUri != null) "Image Attached" else "Attach Service Image (Optional)")
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Available for Booking")
                    Switch(checked = isAvailable, onCheckedChange = { isAvailable = it })
                }
                if (!errorMsg.isNullOrBlank()) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceStr.toDoubleOrNull()
                    val discount = discountStr.toDoubleOrNull()
                    val duration = durationStr.toIntOrNull()
                    when {
                        name.trim().length < 2 -> errorMsg = "Enter a valid service name."
                        price == null || price <= 0 -> errorMsg = "Enter a valid price."
                        duration == null || duration < 5 -> errorMsg = "Duration must be at least 5 minutes."
                        discount != null && discount > price -> errorMsg = "Discount price cannot exceed regular price."
                        else -> {
                            onSave(
                                initial.copy(
                                    salonId = salon.id,
                                    barberId = salon.ownerId,
                                    salonName = salon.salonName,
                                    name = name.trim(),
                                    category = category,
                                    description = description.trim().takeIf { it.isNotBlank() },
                                    imageUri = imageUri,
                                    price = price,
                                    discountPrice = discount,
                                    durationMinutes = duration,
                                    isAvailable = isAvailable,
                                    isActive = true
                                )
                            )
                        }
                    }
                },
                modifier = Modifier.testTag("save_service_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ServicePresetsDialog(
    category: String,
    onDismiss: () -> Unit,
    onImport: (List<ServicePreset>) -> Unit
) {
    val list = if (category == "FEMALE") ServicePresets.femaleServices else ServicePresets.maleServices
    var selectedNames by remember { mutableStateOf(list.take(4).map { it.name }.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick-Add ${if (category == "FEMALE") "Female" else "Male"} Services", style = MaterialTheme.typography.headlineMedium) },
        text = {
            LazyColumn(modifier = Modifier.height(340.dp)) {
                items(list) { preset ->
                    val checked = selectedNames.contains(preset.name)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                selectedNames = if (checked) selectedNames - preset.name else selectedNames + preset.name
                            }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(preset.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${DateTimeUtils.formatPricePkr(preset.defaultPrice)} • ${preset.defaultDurationMinutes} min",
                                style = MaterialTheme.typography.bodySmall,
                                color = ChampagneGold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedNames.isNotEmpty(),
                onClick = { onImport(list.filter { selectedNames.contains(it.name) }) },
                modifier = Modifier.testTag("confirm_import_presets_button")
            ) {
                Text("Add Selected (${selectedNames.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun BarberAppointmentsScreen(
    salon: SalonProfile?,
    appointments: List<Appointment>,
    slotLocks: List<SlotLock>,
    unreadNotifications: Int,
    onOpenNotifications: () -> Unit,
    onUpdateStatus: (Appointment, AppointmentStatus, Boolean) -> Unit,
    onCancelAppointment: (Appointment, String) -> Unit,
    onRescheduleAppointment: (Appointment, String, Int, Int) -> Unit
) {
    var filterTab by remember { mutableStateOf("ALL") } // ALL, TODAY, UPCOMING, COMPLETED, CANCELLED
    var cancellingAppt by remember { mutableStateOf<Appointment?>(null) }
    var reschedulingAppt by remember { mutableStateOf<Appointment?>(null) }
    val today = remember(salon) { DateTimeUtils.todayDateString(salon?.timezone ?: DateTimeUtils.DEFAULT_TIMEZONE) }

    val filtered = remember(appointments, filterTab, today) {
        when (filterTab) {
            "TODAY" -> appointments.filter { it.date == today }
            "UPCOMING" -> appointments.filter { it.date >= today && (it.status == AppointmentStatus.BOOKED.value || it.status == AppointmentStatus.PENDING.value) }
            "COMPLETED" -> appointments.filter { it.status == AppointmentStatus.COMPLETED.value }
            "CANCELLED" -> appointments.filter { it.status == AppointmentStatus.CANCELLED.value || it.status == AppointmentStatus.REJECTED.value }
            else -> appointments
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        StyleoraTopBar(
            title = "Appointments",
            subtitle = "${appointments.size} total bookings",
            unreadNotificationCount = unreadNotifications,
            onNotificationsClick = onOpenNotifications
        )

        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("ALL" to "All", "TODAY" to "Today", "UPCOMING" to "Upcoming", "COMPLETED" to "Done", "CANCELLED" to "Cancelled").forEach { (key, label) ->
                FilterChip(
                    selected = filterTab == key,
                    onClick = { filterTab = key },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No appointments match this filter.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { it.id }) { appt ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(appt.customerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                StatusPill(appt.status)
                            }
                            Text(
                                text = "${appt.customerPhone} • ${appt.customerEmail}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${DateTimeUtils.formatDisplayDate(appt.date)} • ${DateTimeUtils.formatMinutesAmPm(appt.startMinutes)} - ${DateTimeUtils.formatMinutesAmPm(appt.endMinutes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ChampagneGold
                            )
                            Text(appt.serviceNames.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${DateTimeUtils.formatPricePkr(appt.totalPrice)} • ${appt.paymentMethod} (${appt.paymentStatus})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (!appt.specialInstructions.isNullOrBlank()) {
                                Text("Notes: ${appt.specialInstructions}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (!appt.cancellationReason.isNullOrBlank()) {
                                Text("Reason: ${appt.cancellationReason}", style = MaterialTheme.typography.bodySmall, color = StatusBookedRed)
                            }

                            if (appt.status == AppointmentStatus.PENDING.value || appt.status == AppointmentStatus.BOOKED.value) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (appt.status == AppointmentStatus.PENDING.value) {
                                        Button(
                                            onClick = { onUpdateStatus(appt, AppointmentStatus.BOOKED, false) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Confirm")
                                        }
                                    } else {
                                        Button(
                                            onClick = { onUpdateStatus(appt, AppointmentStatus.COMPLETED, true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = StatusAvailableGreen, contentColor = Color.White),
                                            modifier = Modifier.weight(1f).testTag("barber_complete_${appt.id}")
                                        ) {
                                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Complete")
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { reschedulingAppt = appt },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Reschedule")
                                    }
                                    OutlinedButton(
                                        onClick = { cancellingAppt = appt },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusBookedRed),
                                        border = BorderStroke(1.dp, StatusBookedRed),
                                        modifier = Modifier.weight(1f).testTag("barber_cancel_${appt.id}")
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Cancel")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    cancellingAppt?.let { appt ->
        CancelAppointmentDialog(
            appointment = appt,
            onDismiss = { cancellingAppt = null },
            onConfirmCancel = { reason ->
                cancellingAppt = null
                onCancelAppointment(appt, reason)
            }
        )
    }

    reschedulingAppt?.let { appt ->
        RescheduleAppointmentDialog(
            appointment = appt,
            salon = salon,
            slotLocks = slotLocks,
            timezoneId = salon?.timezone ?: DateTimeUtils.DEFAULT_TIMEZONE,
            onDismiss = { reschedulingAppt = null },
            onConfirmReschedule = { date, start, end ->
                reschedulingAppt = null
                onRescheduleAppointment(appt, date, start, end)
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BarberCalendarScreen(
    salon: SalonProfile?,
    appointments: List<Appointment>,
    slotLocks: List<SlotLock>,
    unreadNotifications: Int,
    onOpenNotifications: () -> Unit
) {
    val tzId = salon?.timezone ?: DateTimeUtils.DEFAULT_TIMEZONE
    var selectedDate by remember(tzId) { mutableStateOf(DateTimeUtils.todayDateString(tzId)) }
    val dayAppointments = remember(appointments, selectedDate) {
        appointments.filter { it.date == selectedDate }
    }
    val slots = remember(salon, selectedDate, slotLocks) {
        if (salon == null) emptyList()
        else DateTimeUtils.generateSlotsForDate(
            salon = salon,
            dateStr = selectedDate,
            durationMinutes = 30,
            slotLocks = slotLocks,
            currentUid = "",
            timezoneId = tzId
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StyleoraTopBar(
                title = "Salon Calendar",
                subtitle = "Real-time slot & booking matrix",
                unreadNotificationCount = unreadNotifications,
                onNotificationsClick = onOpenNotifications
            )
        }
        item {
            InteractiveCalendarPicker(
                selectedDate = selectedDate,
                salon = salon,
                slotLocks = slotLocks,
                serviceDurationMinutes = 30,
                timezoneId = tzId,
                onDateSelected = { selectedDate = it },
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Slots for ${DateTimeUtils.formatDisplayDate(selectedDate)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                StatusLegendRow()
                if (salon == null) {
                    Text("Set up your salon profile to view daily generated slots.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        slots.forEach { slot ->
                            StatusPill(
                                statusText = slot.status.value,
                                modifier = Modifier
                            )
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        slots.forEach { slot ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                    Text(DateTimeUtils.formatMinutesAmPm(slot.startMinutes), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text(slot.status.label, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Text(
                text = "Appointments on $selectedDate (${dayAppointments.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
        items(dayAppointments, key = { it.id }) { appt ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(appt.customerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("${DateTimeUtils.formatMinutesAmPm(appt.startMinutes)} • ${appt.serviceNames.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                    }
                    StatusPill(appt.status)
                }
            }
        }
    }
}

package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.AppSettings
import com.example.data.model.Appointment
import com.example.data.model.SalonProfile
import com.example.data.model.SalonReview
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.ui.components.StatusPill
import com.example.ui.components.StyleoraTopBar
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.StatusAvailableGreen
import com.example.ui.theme.StatusBookedRed
import com.example.util.DateTimeUtils

@Composable
fun UserProfileScreen(
    profile: UserProfile,
    isDarkMode: Boolean,
    isAdmin: Boolean,
    unreadNotifications: Int,
    onOpenNotifications: () -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onUpdateProfile: (String, String, String, String, String?, String) -> Unit,
    onOpenAdminPanel: () -> Unit,
    onSignOut: () -> Unit
) {
    var fullName by remember(profile.fullName) { mutableStateOf(profile.fullName) }
    var mobile by remember(profile.mobileNumber) { mutableStateOf(profile.mobileNumber) }
    var role by remember(profile.userType) { mutableStateOf(profile.userType) }
    var photoUri by remember(profile.profilePhotoUri) { mutableStateOf(profile.profilePhotoUri) }
    var timezone by remember(profile.preferredTimezone) { mutableStateOf(profile.preferredTimezone.ifBlank { DateTimeUtils.DEFAULT_TIMEZONE }) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) photoUri = uri.toString()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            StyleoraTopBar(
                title = "My Profile",
                subtitle = profile.email,
                unreadNotificationCount = unreadNotifications,
                onNotificationsClick = onOpenNotifications
            )
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(2.dp, ChampagneGold, CircleShape)
                        .clickable {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                        .testTag("profile_photo_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (!photoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "Profile photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(38.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Tap photo to update", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        label = { Text("Mobile Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Dashboard Mode", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        UserRole.entries.forEach { r ->
                            FilterChip(
                                selected = role == r.value,
                                onClick = { role = r.value },
                                label = { Text(r.label) },
                                modifier = Modifier.testTag("profile_role_${r.value}")
                            )
                        }
                    }
                    Text("Timezone", style = MaterialTheme.typography.titleSmall)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DateTimeUtils.supportedTimezones.take(3).forEach { (id, label) ->
                            FilterChip(
                                selected = timezone == id,
                                onClick = { timezone = id },
                                label = { Text(label) }
                            )
                        }
                    }
                    Button(
                        onClick = {
                            onUpdateProfile(
                                fullName.trim().ifBlank { profile.fullName },
                                profile.email,
                                mobile.trim(),
                                role,
                                photoUri,
                                timezone
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = MatteBlack),
                        modifier = Modifier.fillMaxWidth().testTag("save_profile_button")
                    ) {
                        Text("Save Profile Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.DarkMode, contentDescription = null, tint = ChampagneGold)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Luxury Dark Mode", style = MaterialTheme.typography.titleSmall)
                            Text("Matte Black & Champagne Gold", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = onToggleDarkMode,
                        modifier = Modifier.testTag("dark_mode_switch")
                    )
                }
            }
        }

        if (isAdmin) {
            item {
                Button(
                    onClick = onOpenAdminPanel,
                    colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = MatteBlack),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(52.dp).testTag("open_admin_panel_button")
                ) {
                    Icon(Icons.Filled.AdminPanelSettings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Super Admin Panel", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onSignOut,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusBookedRed),
                border = BorderStroke(1.dp, StatusBookedRed),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(50.dp).testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out")
            }
        }
    }
}

@Composable
fun AdminPanelScreen(
    users: List<UserProfile>,
    salons: List<SalonProfile>,
    appointments: List<Appointment>,
    reviews: List<SalonReview>,
    appSettings: AppSettings,
    onSetUserStatus: (String, String) -> Unit,
    onToggleSalonActive: (String, Boolean) -> Unit,
    onSaveAppSettings: (String, Int) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var tab by remember { mutableStateOf("OVERVIEW") } // OVERVIEW, USERS, SALONS, APPOINTMENTS, SETTINGS
    var timezoneInput by remember(appSettings.defaultTimezone) { mutableStateOf(appSettings.defaultTimezone) }
    var holdMinInput by remember(appSettings.defaultHoldMinutes) { mutableStateOf(appSettings.defaultHoldMinutes.toString()) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("STYLEORA Super Admin", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("OVERVIEW" to "Reports", "USERS" to "Users", "SALONS" to "Salons", "APPTS" to "Bookings", "SETTINGS" to "Settings").forEach { (k, label) ->
                    FilterChip(selected = tab == k, onClick = { tab = k }, label = { Text(label, style = MaterialTheme.typography.labelSmall) })
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (tab) {
                    "OVERVIEW" -> {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Platform Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Total Registered Users: ${users.size} (${users.count { it.userType == "BARBER" }} Barbers, ${users.count { it.userType == "CUSTOMER" }} Customers)")
                                    Text("Total Salons: ${salons.size} (${salons.count { it.isActive }} Active)")
                                    Text("Total Appointments: ${appointments.size}")
                                    Text("Total Reviews: ${reviews.size}")
                                    Text("Gross Volume: ${DateTimeUtils.formatPricePkr(appointments.sumOf { it.totalPrice })}", color = ChampagneGold, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    "USERS" -> {
                        items(users, key = { it.uid }) { user ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(user.fullName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text("${user.email} • ${user.userType}", style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            text = "Status: ${user.accountStatus}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (user.accountStatus == "ACTIVE") StatusAvailableGreen else StatusBookedRed
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val next = if (user.accountStatus == "ACTIVE") "SUSPENDED" else "ACTIVE"
                                            onSetUserStatus(user.uid, next)
                                        }
                                    ) {
                                        Text(if (user.accountStatus == "ACTIVE") "Suspend" else "Activate")
                                    }
                                }
                            }
                        }
                    }

                    "SALONS" -> {
                        items(salons, key = { it.id }) { salon ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(salon.salonName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text("${salon.ownerName} • ${salon.address}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    OutlinedButton(onClick = { onToggleSalonActive(salon.id, !salon.isActive) }) {
                                        Text(if (salon.isActive) "Suspend" else "Activate")
                                    }
                                }
                            }
                        }
                    }

                    "APPTS" -> {
                        items(appointments, key = { it.id }) { appt ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${appt.customerName} @ ${appt.salonName}", style = MaterialTheme.typography.titleSmall)
                                        Text("${appt.date} • ${DateTimeUtils.formatPricePkr(appt.totalPrice)}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    StatusPill(appt.status)
                                }
                            }
                        }
                    }

                    "SETTINGS" -> {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("Global System Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    DateTimeUtils.supportedTimezones.forEach { (id, label) ->
                                        FilterChip(
                                            selected = timezoneInput == id,
                                            onClick = { timezoneInput = id },
                                            label = { Text(label) }
                                        )
                                    }
                                    OutlinedTextField(
                                        value = holdMinInput,
                                        onValueChange = { holdMinInput = it.filter { c -> c.isDigit() } },
                                        label = { Text("Default Slot Hold Timeout (Minutes)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(
                                        onClick = {
                                            onSaveAppSettings(timezoneInput, holdMinInput.toIntOrNull()?.coerceIn(1, 60) ?: 10)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Save Global Settings")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

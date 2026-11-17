package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Appointment
import com.example.data.model.AppointmentStatus
import com.example.data.model.FavouriteSalon
import com.example.data.model.SalonProfile
import com.example.data.model.SalonReview
import com.example.data.model.SalonService
import com.example.data.model.ServiceCategory
import com.example.data.model.SlotLock
import com.example.data.model.UserProfile
import com.example.ui.components.CancelAppointmentDialog
import com.example.ui.components.RescheduleAppointmentDialog
import com.example.ui.components.ReviewAppointmentDialog
import com.example.ui.components.StatusPill
import com.example.ui.components.StyleoraTopBar
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneGoldLight
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.StatusBookedRed
import com.example.ui.theme.WarmWhite
import com.example.util.DateTimeUtils

@Composable
fun CustomerHomeScreen(
    profile: UserProfile,
    salons: List<SalonProfile>,
    services: List<SalonService>,
    appointments: List<Appointment>,
    favourites: List<FavouriteSalon>,
    unreadNotifications: Int,
    onOpenNotifications: () -> Unit,
    onBookNow: (SalonProfile?, SalonService?) -> Unit,
    onExploreCategory: (String) -> Unit,
    onToggleFavourite: (SalonProfile, Boolean) -> Unit,
    onInspectSalon: (SalonProfile) -> Unit,
    onViewAppointments: () -> Unit
) {
    val upcoming = remember(appointments) {
        appointments.filter { it.status == AppointmentStatus.BOOKED.value || it.status == AppointmentStatus.PENDING.value }
    }
    val favIds = remember(favourites) { favourites.map { it.salonId }.toSet() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            StyleoraTopBar(
                title = "STYLEORA",
                subtitle = "Welcome, ${profile.fullName.ifBlank { "Guest" }}",
                unreadNotificationCount = unreadNotifications,
                onNotificationsClick = onOpenNotifications
            )
        }

        // Luxury Hero Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(210.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_salon_hero),
                        contentDescription = "STYLEORA Luxury Salon",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(MatteBlack.copy(alpha = 0.88f), MatteBlack.copy(alpha = 0.35f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "YOUR STYLE, YOUR TIME.",
                                style = MaterialTheme.typography.labelSmall,
                                color = ChampagneGoldLight
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Bespoke Grooming &\nLuxury Hair Artistry",
                                style = MaterialTheme.typography.headlineLarge,
                                color = WarmWhite
                            )
                        }
                        Button(
                            onClick = { onBookNow(null, null) },
                            colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = MatteBlack),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("home_book_appointment_button")
                        ) {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Book Appointment", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Upcoming Appointment Highlight
        if (upcoming.isNotEmpty()) {
            val next = upcoming.first()
            item {
                Card(
                    onClick = onViewAppointments,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    border = BorderStroke(1.dp, ChampagneGold)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "UPCOMING APPOINTMENT",
                                style = MaterialTheme.typography.labelSmall,
                                color = ChampagneGold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(next.salonName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${DateTimeUtils.formatDisplayDate(next.date)} • ${DateTimeUtils.formatMinutesAmPm(next.startMinutes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = next.serviceNames.joinToString(", "),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        StatusPill(next.status)
                    }
                }
            }
        }

        // Male & Female Category Cards
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text("Curated Categories", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CategoryBannerCard(
                        title = "Male Grooming",
                        subtitle = "Cuts, Beards & Shaves",
                        imageRes = R.drawable.img_category_male,
                        onClick = { onExploreCategory(ServiceCategory.MALE.value) },
                        modifier = Modifier.weight(1f).testTag("category_male_card")
                    )
                    CategoryBannerCard(
                        title = "Female Salon",
                        subtitle = "Hair, Bridal & Spa",
                        imageRes = R.drawable.img_category_female,
                        onClick = { onExploreCategory(ServiceCategory.FEMALE.value) },
                        modifier = Modifier.weight(1f).testTag("category_female_card")
                    )
                }
            }
        }

        // Registered Salons Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Registered Salons & Barbers", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "${salons.size} active",
                    style = MaterialTheme.typography.labelMedium,
                    color = ChampagneGold
                )
            }
        }

        if (salons.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Storefront, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No Salons Registered Yet", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Registered barbers and salons will appear here in real time as soon as they publish their shop profile and services.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(salons, key = { it.id }) { salon ->
                SalonDirectoryCard(
                    salon = salon,
                    serviceCount = services.count { it.salonId == salon.id },
                    isFavourite = favIds.contains(salon.id),
                    onToggleFavourite = { onToggleFavourite(salon, favIds.contains(salon.id)) },
                    onInspect = { onInspectSalon(salon) },
                    onBook = { onBookNow(salon, null) },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }
}

@Composable
private fun CategoryBannerCard(
    title: String,
    subtitle: String,
    imageRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(148.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, MatteBlack.copy(alpha = 0.88f))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = WarmWhite, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = ChampagneGoldLight)
            }
        }
    }
}

@Composable
fun SalonDirectoryCard(
    salon: SalonProfile,
    serviceCount: Int,
    isFavourite: Boolean,
    onToggleFavourite: () -> Unit,
    onInspect: () -> Unit,
    onBook: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onInspect,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(130.dp)) {
                if (!salon.coverImageUri.isNullOrBlank()) {
                    AsyncImage(
                        model = salon.coverImageUri,
                        contentDescription = salon.salonName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.img_salon_hero),
                        contentDescription = salon.salonName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                IconButton(
                    onClick = onToggleFavourite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(MatteBlack.copy(alpha = 0.6f))
                        .testTag("fav_button_${salon.id}")
                ) {
                    Icon(
                        imageVector = if (isFavourite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favourite Salon",
                        tint = if (isFavourite) ChampagneGold else WarmWhite
                    )
                }
                Surface(
                    color = MatteBlack.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (salon.reviewCount > 0) String.format("%.1f (%d)", salon.averageRating, salon.reviewCount) else "New",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmWhite
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(salon.salonName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Master Stylist: ${salon.ownerName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onBook,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("book_salon_${salon.id}")
                    ) {
                        Text("Book")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = salon.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$serviceCount services",
                        style = MaterialTheme.typography.labelSmall,
                        color = ChampagneGold
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerExploreScreen(
    salons: List<SalonProfile>,
    services: List<SalonService>,
    favourites: List<FavouriteSalon>,
    initialCategoryFilter: String,
    unreadNotifications: Int,
    onOpenNotifications: () -> Unit,
    onToggleFavourite: (SalonProfile, Boolean) -> Unit,
    onInspectSalon: (SalonProfile) -> Unit,
    onBookService: (SalonProfile, SalonService) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var categoryFilter by remember(initialCategoryFilter) { mutableStateOf(initialCategoryFilter) } // ALL, MALE, FEMALE
    val favIds = remember(favourites) { favourites.map { it.salonId }.toSet() }
    val salonMap = remember(salons) { salons.associateBy { it.id } }

    val filteredSalons = remember(salons, query) {
        if (query.isBlank()) salons
        else salons.filter {
            it.salonName.contains(query, true) ||
                it.ownerName.contains(query, true) ||
                it.address.contains(query, true)
        }
    }

    val filteredServices = remember(services, query, categoryFilter) {
        services.filter { svc ->
            val matchesCat = categoryFilter == "ALL" || svc.category == categoryFilter || svc.category == ServiceCategory.BOTH.value
            val matchesQuery = query.isBlank() ||
                svc.name.contains(query, true) ||
                svc.salonName.contains(query, true) ||
                (svc.description?.contains(query, true) == true)
            matchesCat && matchesQuery && svc.isAvailable
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        StyleoraTopBar(
            title = "Explore Salons & Services",
            subtitle = "Real-time registered studios",
            unreadNotificationCount = unreadNotifications,
            onNotificationsClick = onOpenNotifications
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text("Search haircuts, beard, facial, salons...") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("explore_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL" to "All", "MALE" to "Male Services", "FEMALE" to "Female Services").forEach { (key, label) ->
                FilterChip(
                    selected = categoryFilter == key,
                    onClick = { categoryFilter = key },
                    label = { Text(label) },
                    modifier = Modifier.testTag("explore_filter_$key")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filteredSalons.isNotEmpty()) {
                item {
                    Text("Salons & Barbers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(filteredSalons, key = { it.id }) { salon ->
                            Card(
                                onClick = { onInspectSalon(salon) },
                                modifier = Modifier.width(240.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = salon.salonName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { onToggleFavourite(salon, favIds.contains(salon.id)) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (favIds.contains(salon.id)) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                                contentDescription = "Favourite",
                                                tint = ChampagneGold,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Text(salon.ownerName, style = MaterialTheme.typography.bodySmall, color = ChampagneGold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = salon.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Available Services (${filteredServices.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (filteredServices.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = "No matching services found from registered salons.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                items(filteredServices, key = { it.id }) { svc ->
                    val parentSalon = salonMap[svc.salonId]
                    ServiceCatalogCard(
                        service = svc,
                        onBookClick = {
                            if (parentSalon != null) onBookService(parentSalon, svc)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ServiceCatalogCard(
    service: SalonService,
    onBookClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!service.imageUri.isNullOrBlank()) {
                AsyncImage(
                    model = service.imageUri,
                    contentDescription = service.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(14.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(service.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = service.category,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = service.salonName,
                    style = MaterialTheme.typography.bodySmall,
                    color = ChampagneGold
                )
                if (!service.description.isNullOrBlank()) {
                    Text(
                        text = service.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = DateTimeUtils.formatPricePkr(service.effectivePrice),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (service.discountPrice != null && service.discountPrice > 0 && service.discountPrice < service.price) {
                        Text(
                            text = DateTimeUtils.formatPricePkr(service.price),
                            style = MaterialTheme.typography.bodySmall,
                            textDecoration = TextDecoration.LineThrough,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("${service.durationMinutes} min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onBookClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("book_service_${service.id}")
            ) {
                Text("Book")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalonDetailBottomSheet(
    salon: SalonProfile,
    services: List<SalonService>,
    reviews: List<SalonReview>,
    isFavourite: Boolean,
    onToggleFavourite: () -> Unit,
    onBookSalon: () -> Unit,
    onBookService: (SalonService) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(salon.salonName, style = MaterialTheme.typography.headlineLarge)
                        Text("Owner / Lead Barber: ${salon.ownerName}", style = MaterialTheme.typography.bodyMedium, color = ChampagneGold)
                    }
                    IconButton(onClick = onToggleFavourite) {
                        Icon(
                            imageVector = if (isFavourite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Toggle favourite",
                            tint = ChampagneGold
                        )
                    }
                }
            }
            item {
                Text(salon.address, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Hours: ${DateTimeUtils.formatMinutesAmPm(salon.openingTimeMinutes)} – ${DateTimeUtils.formatMinutesAmPm(salon.closingTimeMinutes)} (${salon.workingDays.joinToString(", ")})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!salon.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(salon.description, style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (salon.contactNumber.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${salon.contactNumber}"))
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Call")
                        }
                    }
                    if (!salon.mapsLocation.isNullOrBlank() || salon.address.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                val target = salon.mapsLocation?.takeIf { it.startsWith("http") }
                                    ?: "geo:0,0?q=${Uri.encode(salon.address)}"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)))
                            }
                        ) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Map")
                        }
                    }
                    Button(
                        onClick = onBookSalon,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Book Appointment")
                    }
                }
            }
            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(6.dp))
                Text("Services (${services.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(services, key = { it.id }) { svc ->
                ServiceCatalogCard(service = svc, onBookClick = { onBookService(svc) })
            }
            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Reviews (${reviews.size}) • Avg ${String.format("%.1f", salon.averageRating)}★",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (reviews.isEmpty()) {
                item {
                    Text(
                        "No verified reviews yet. Complete an appointment to leave the first review!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(reviews, key = { it.id }) { rev ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(rev.customerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("${rev.rating} ★", color = ChampagneGold, fontWeight = FontWeight.Bold)
                            }
                            if (!rev.comment.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(rev.comment, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerAppointmentsScreen(
    appointments: List<Appointment>,
    salons: List<SalonProfile>,
    observedSlotLocks: List<SlotLock>,
    unreadNotifications: Int,
    onOpenNotifications: () -> Unit,
    onWatchSalon: (String) -> Unit,
    onCancelAppointment: (Appointment, String, SalonProfile?) -> Unit,
    onRescheduleAppointment: (Appointment, String, Int, Int) -> Unit,
    onSubmitReview: (Appointment, Int, String) -> Unit
) {
    var filterTab by remember { mutableStateOf("UPCOMING") } // UPCOMING, HISTORY, CANCELLED
    var cancellingAppt by remember { mutableStateOf<Appointment?>(null) }
    var reschedulingAppt by remember { mutableStateOf<Appointment?>(null) }
    var reviewingAppt by remember { mutableStateOf<Appointment?>(null) }
    var detailAppt by remember { mutableStateOf<Appointment?>(null) }

    val salonMap = remember(salons) { salons.associateBy { it.id } }

    val filtered = remember(appointments, filterTab) {
        when (filterTab) {
            "UPCOMING" -> appointments.filter { it.status == AppointmentStatus.BOOKED.value || it.status == AppointmentStatus.PENDING.value }
            "CANCELLED" -> appointments.filter { it.status == AppointmentStatus.CANCELLED.value || it.status == AppointmentStatus.REJECTED.value }
            else -> appointments
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        StyleoraTopBar(
            title = "My Appointments",
            subtitle = "Upcoming, history & cancellations",
            unreadNotificationCount = unreadNotifications,
            onNotificationsClick = onOpenNotifications
        )

        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("UPCOMING" to "Upcoming", "HISTORY" to "All History", "CANCELLED" to "Cancelled").forEach { (key, label) ->
                FilterChip(
                    selected = filterTab == key,
                    onClick = { filterTab = key },
                    label = { Text(label) },
                    modifier = Modifier.testTag("appt_tab_$key")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "No appointments in this view yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { it.id }) { appt ->
                    Card(
                        onClick = { detailAppt = appt },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(appt.salonName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                StatusPill(appt.status)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${DateTimeUtils.formatDisplayDate(appt.date)} • ${DateTimeUtils.formatMinutesAmPm(appt.startMinutes)} - ${DateTimeUtils.formatMinutesAmPm(appt.endMinutes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ChampagneGold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = appt.serviceNames.joinToString(", "),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${DateTimeUtils.formatPricePkr(appt.totalPrice)} • ${appt.totalDurationMinutes} min",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${appt.paymentMethod} (${appt.paymentStatus})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (!appt.cancellationReason.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Cancellation reason (${appt.cancelledByRole ?: ""}): ${appt.cancellationReason}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StatusBookedRed
                                )
                            }

                            if (appt.status == AppointmentStatus.BOOKED.value || appt.status == AppointmentStatus.PENDING.value) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            onWatchSalon(appt.salonId)
                                            reschedulingAppt = appt
                                        },
                                        modifier = Modifier.weight(1f).testTag("reschedule_btn_${appt.id}")
                                    ) {
                                        Icon(Icons.Filled.Update, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Reschedule")
                                    }
                                    OutlinedButton(
                                        onClick = { cancellingAppt = appt },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusBookedRed),
                                        border = BorderStroke(1.dp, StatusBookedRed),
                                        modifier = Modifier.weight(1f).testTag("cancel_btn_${appt.id}")
                                    ) {
                                        Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Cancel Appointment")
                                    }
                                }
                            } else if (appt.status == AppointmentStatus.COMPLETED.value && !appt.hasReview) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { reviewingAppt = appt },
                                    modifier = Modifier.fillMaxWidth().testTag("review_btn_${appt.id}")
                                ) {
                                    Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Rate & Review Salon")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    detailAppt?.let { appt ->
        AlertDialog(
            onDismissRequest = { detailAppt = null },
            title = { Text(appt.salonName, style = MaterialTheme.typography.headlineMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusPill(appt.status)
                    Text("Date: ${DateTimeUtils.formatDisplayDate(appt.date)}")
                    Text("Time: ${DateTimeUtils.formatMinutesAmPm(appt.startMinutes)} - ${DateTimeUtils.formatMinutesAmPm(appt.endMinutes)}")
                    Text("Services: ${appt.serviceNames.joinToString(", ")}")
                    Text("Total: ${DateTimeUtils.formatPricePkr(appt.totalPrice)} (${appt.totalDurationMinutes} min)")
                    Text("Payment: ${appt.paymentMethod} • ${appt.paymentStatus}")
                    if (!appt.specialInstructions.isNullOrBlank()) {
                        Text("Notes: ${appt.specialInstructions}")
                    }
                    if (!appt.cancellationReason.isNullOrBlank()) {
                        Text("Cancelled by ${appt.cancelledByRole}: ${appt.cancellationReason}", color = StatusBookedRed)
                    }
                }
            },
            confirmButton = {
                if (appt.status == AppointmentStatus.BOOKED.value || appt.status == AppointmentStatus.PENDING.value) {
                    TextButton(
                        onClick = {
                            detailAppt = null
                            cancellingAppt = appt
                        }
                    ) {
                        Text("Cancel Appointment", color = StatusBookedRed)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { detailAppt = null }) { Text("Close") }
            }
        )
    }

    cancellingAppt?.let { appt ->
        CancelAppointmentDialog(
            appointment = appt,
            onDismiss = { cancellingAppt = null },
            onConfirmCancel = { reason ->
                cancellingAppt = null
                onCancelAppointment(appt, reason, salonMap[appt.salonId])
            }
        )
    }

    reschedulingAppt?.let { appt ->
        val salon = salonMap[appt.salonId]
        RescheduleAppointmentDialog(
            appointment = appt,
            salon = salon,
            slotLocks = observedSlotLocks,
            timezoneId = salon?.timezone ?: DateTimeUtils.DEFAULT_TIMEZONE,
            onDismiss = { reschedulingAppt = null },
            onConfirmReschedule = { date, start, end ->
                reschedulingAppt = null
                onRescheduleAppointment(appt, date, start, end)
            }
        )
    }

    reviewingAppt?.let { appt ->
        ReviewAppointmentDialog(
            appointment = appt,
            onDismiss = { reviewingAppt = null },
            onSubmit = { rating, comment ->
                reviewingAppt = null
                onSubmitReview(appt, rating, comment)
            }
        )
    }
}

@Composable
fun CustomerFavouritesScreen(
    salons: List<SalonProfile>,
    services: List<SalonService>,
    favourites: List<FavouriteSalon>,
    unreadNotifications: Int,
    onOpenNotifications: () -> Unit,
    onToggleFavourite: (SalonProfile, Boolean) -> Unit,
    onInspectSalon: (SalonProfile) -> Unit,
    onBookSalon: (SalonProfile) -> Unit
) {
    val favIds = remember(favourites) { favourites.map { it.salonId }.toSet() }
    val favSalons = remember(salons, favIds) { salons.filter { favIds.contains(it.id) } }

    Column(modifier = Modifier.fillMaxSize()) {
        StyleoraTopBar(
            title = "Favourite Salons",
            subtitle = "${favSalons.size} saved studios",
            unreadNotificationCount = unreadNotifications,
            onNotificationsClick = onOpenNotifications
        )
        if (favSalons.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "Tap the heart icon on any salon to save it to your favourites.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(favSalons, key = { it.id }) { salon ->
                    SalonDirectoryCard(
                        salon = salon,
                        serviceCount = services.count { it.salonId == salon.id },
                        isFavourite = true,
                        onToggleFavourite = { onToggleFavourite(salon, true) },
                        onInspect = { onInspectSalon(salon) },
                        onBook = { onBookSalon(salon) }
                    )
                }
            }
        }
    }
}

package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.SalonProfile
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.repository.StyleoraRepository
import com.example.data.repository.ThemePreferencesRepository
import com.example.ui.auth.EmailAuthHelper
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.signOutUser
import com.example.ui.components.NotificationsSheet
import com.example.ui.components.StyleoraCrest
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.EmailVerificationScreen
import com.example.ui.screens.BarberAppointmentsScreen
import com.example.ui.screens.BarberCalendarScreen
import com.example.ui.screens.BarberDashboardScreen
import com.example.ui.screens.BarberServicesScreen
import com.example.ui.screens.BookingWizardScreen
import com.example.ui.screens.CustomerAppointmentsScreen
import com.example.ui.screens.CustomerExploreScreen
import com.example.ui.screens.CustomerFavouritesScreen
import com.example.ui.screens.CustomerHomeScreen
import com.example.ui.screens.RoleOnboardingScreen
import com.example.ui.screens.SalonDetailBottomSheet
import com.example.ui.screens.UserProfileScreen
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.StyleoraTheme
import com.example.ui.viewmodel.StyleoraViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val themePrefs = ThemePreferencesRepository(applicationContext)

        setContent {
            val systemDark = isSystemInDarkTheme()
            val storedDark by themePrefs.isDarkMode.collectAsStateWithLifecycle(initialValue = null)
            val isDarkMode = storedDark ?: systemDark
            val scope = rememberCoroutineScope()

            StyleoraTheme(darkTheme = isDarkMode) {
                StyleoraRoot(
                    themePrefs = themePrefs,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { enabled ->
                        scope.launch { themePrefs.setDarkMode(enabled) }
                    }
                )
            }
        }
    }
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

private enum class CustomerTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    EXPLORE("Explore", Icons.Filled.Explore),
    APPOINTMENTS("Bookings", Icons.Filled.Schedule),
    FAVOURITES("Favourites", Icons.Filled.Favorite),
    PROFILE("Profile", Icons.Filled.Person)
}

private enum class BarberTab(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Filled.Dashboard),
    SERVICES("Services", Icons.Filled.ContentCut),
    APPOINTMENTS("Bookings", Icons.Filled.Schedule),
    CALENDAR("Calendar", Icons.Filled.CalendarMonth),
    PROFILE("Profile", Icons.Filled.Person)
}

@Composable
private fun StyleoraRoot(
    themePrefs: ThemePreferencesRepository,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val authRepository = remember(context) {
        val dbId = context.applicationContext.getString(R.string.firestore_database_id)
        StyleoraRepository(FirebaseFirestore.getInstance(dbId), Firebase.auth)
    }
    val rememberedEmail by themePrefs.rememberedEmail.collectAsStateWithLifecycle(initialValue = "")
    val rememberMeEnabled by themePrefs.rememberMeEnabled.collectAsStateWithLifecycle(initialValue = false)
    val currentUser by Firebase.auth.authStateFlow().collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    var verificationNotice by remember { mutableStateOf<String?>(null) }
    var emailVerifiedState by remember(currentUser?.uid) {
        mutableStateOf(currentUser?.isEmailVerified == true)
    }

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    emailVerifiedState = Firebase.auth.currentUser?.isEmailVerified == true
                },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    LaunchedEffect(currentUser?.uid) {
        val u = currentUser
        if (u != null && !u.isEmailVerified) {
            EmailAuthHelper.reloadAndCheckVerified(Firebase.auth).onSuccess { verified ->
                emailVerifiedState = verified
            }
        } else if (u != null && u.isEmailVerified) {
            emailVerifiedState = true
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            repository = authRepository,
            rememberedEmail = rememberedEmail,
            rememberMeEnabled = rememberMeEnabled,
            onRememberMePersist = { rememberMe, email ->
                themePrefs.setRememberedLogin(rememberMe, email)
            },
            onAuthSuccess = {
                verificationNotice = null
                emailVerifiedState = Firebase.auth.currentUser?.isEmailVerified == true
            },
            onEmailSignUpNotice = { notice ->
                verificationNotice = notice
                emailVerifiedState = Firebase.auth.currentUser?.isEmailVerified == true
            },
            onEmailLoginSuccess = { verified ->
                verificationNotice = null
                emailVerifiedState = verified
            }
        )
    } else if (!emailVerifiedState) {
        EmailVerificationScreen(
            userEmail = user.email ?: "",
            initialNotice = verificationNotice,
            onVerified = {
                verificationNotice = null
                emailVerifiedState = true
            },
            onSignOut = {
                verificationNotice = null
                signOutUser(credentialManager, scope)
            }
        )
    } else {
        key(user.uid) {
            val viewModel: StyleoraViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        val repo = StyleoraRepository(db, Firebase.auth)
                        StyleoraViewModel(
                            repository = repo,
                            currentUserId = user.uid,
                            currentUserEmail = user.email ?: "",
                            currentUserDisplayName = user.displayName ?: "",
                            currentUserPhotoUrl = user.photoUrl?.toString()
                        )
                    }
                }
            )
            AuthenticatedApp(
                viewModel = viewModel,
                isDarkMode = isDarkMode,
                onToggleDarkMode = onToggleDarkMode,
                onSignOut = {
                    verificationNotice = null
                    signOutUser(credentialManager, scope)
                }
            )
        }
    }
}

@Composable
private fun AuthenticatedApp(
    viewModel: StyleoraViewModel,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onSignOut: () -> Unit
) {
    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val feedback by viewModel.feedbackMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedback) {
        if (!feedback.isNullOrBlank()) {
            snackbarHostState.showSnackbar(feedback!!)
            viewModel.clearFeedback()
        }
    }

    when (val state = userProfileState) {
        UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    StyleoraCrest(size = 72.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(color = ChampagneGold)
                }
            }
        }

        is UiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Unable to load profile", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(state.message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onSignOut) { Text("Sign Out") }
                }
            }
        }

        is UiState.Success -> {
            val profile = state.data
            if (profile == null) {
                RoleOnboardingScreen(
                    defaultName = viewModel.currentUserDisplayName,
                    defaultEmail = viewModel.currentUserEmail,
                    defaultPhotoUrl = viewModel.currentUserPhotoUrl,
                    isBusy = isBusy,
                    onSubmitProfile = { name, email, phone, role, photo, tz ->
                        viewModel.saveUserProfile(name, email, phone, role, photo, tz)
                    },
                    onSignOutClick = onSignOut
                )
            } else if (profile.accountStatus == "SUSPENDED") {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Account Suspended", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Please contact STYLEORA support to reactivate your account.", textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(onClick = onSignOut) { Text("Sign Out") }
                    }
                }
            } else {
                MainRoleShell(
                    viewModel = viewModel,
                    profile = profile,
                    isDarkMode = isDarkMode,
                    snackbarHostState = snackbarHostState,
                    onToggleDarkMode = onToggleDarkMode,
                    onSignOut = onSignOut
                )
            }
        }
    }
}

@Composable
private fun MainRoleShell(
    viewModel: StyleoraViewModel,
    profile: UserProfile,
    isDarkMode: Boolean,
    snackbarHostState: SnackbarHostState,
    onToggleDarkMode: (Boolean) -> Unit,
    onSignOut: () -> Unit
) {
    val activeSalonsState by viewModel.activeSalonsState.collectAsStateWithLifecycle()
    val barberSalonsState by viewModel.barberSalonsState.collectAsStateWithLifecycle()
    val activeServicesState by viewModel.activeServicesState.collectAsStateWithLifecycle()
    val barberServicesState by viewModel.barberServicesState.collectAsStateWithLifecycle()
    val customerApptsState by viewModel.customerAppointmentsState.collectAsStateWithLifecycle()
    val barberApptsState by viewModel.barberAppointmentsState.collectAsStateWithLifecycle()
    val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
    val favouritesState by viewModel.favouritesState.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettingsState.collectAsStateWithLifecycle()
    val slotLocks by viewModel.observedSalonSlotLocks.collectAsStateWithLifecycle()
    val salonReviews by viewModel.observedSalonReviews.collectAsStateWithLifecycle()
    val bookingWizard by viewModel.bookingWizard.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    val adminUsersState by viewModel.allUsersAdminState.collectAsStateWithLifecycle()
    val adminSalonsState by viewModel.allSalonsAdminState.collectAsStateWithLifecycle()
    val adminApptsState by viewModel.allAppointmentsAdminState.collectAsStateWithLifecycle()
    val adminReviewsState by viewModel.allReviewsAdminState.collectAsStateWithLifecycle()

    val salons = (activeSalonsState as? UiState.Success)?.data ?: emptyList()
    val barberSalon = ((barberSalonsState as? UiState.Success)?.data ?: emptyList()).firstOrNull()
    val services = (activeServicesState as? UiState.Success)?.data ?: emptyList()
    val barberServices = (barberServicesState as? UiState.Success)?.data ?: emptyList()
    val customerAppts = (customerApptsState as? UiState.Success)?.data ?: emptyList()
    val barberAppts = (barberApptsState as? UiState.Success)?.data ?: emptyList()
    val notifications = (notificationsState as? UiState.Success)?.data ?: emptyList()
    val favourites = (favouritesState as? UiState.Success)?.data ?: emptyList()

    val unreadCount = notifications.count { !it.isRead }

    var customerTab by remember { mutableStateOf(CustomerTab.HOME) }
    var barberTab by remember { mutableStateOf(BarberTab.DASHBOARD) }
    var exploreFilter by remember { mutableStateOf("ALL") }
    var inspectedSalon by remember { mutableStateOf<SalonProfile?>(null) }
    var showNotifications by remember { mutableStateOf(false) }
    var showAdminPanel by remember { mutableStateOf(false) }

    LaunchedEffect(profile.userType, barberSalon?.id) {
        if (profile.userType == UserRole.BARBER.value && barberSalon != null) {
            viewModel.watchSalon(barberSalon.id)
        }
    }

    if (bookingWizard.isActive) {
        BookingWizardScreen(
            state = bookingWizard,
            salons = salons,
            services = services,
            slotLocks = slotLocks,
            currentUserId = viewModel.currentUserId,
            isBusy = isBusy,
            onStepChange = viewModel::setBookingStep,
            onSelectCategory = viewModel::selectBookingCategory,
            onSelectSalon = viewModel::selectBookingSalon,
            onToggleService = viewModel::toggleBookingService,
            onUpdateCustomerInfo = viewModel::updateCustomerBookingInfo,
            onSelectDate = viewModel::selectBookingDate,
            onHoldSlot = viewModel::holdBookingSlot,
            onConfirmBooking = {
                viewModel.confirmBooking {
                    customerTab = CustomerTab.APPOINTMENTS
                }
            },
            onClose = viewModel::closeBookingWizard
        )
        return
    }

    if (showAdminPanel && viewModel.isAdmin) {
        AdminPanelScreen(
            users = (adminUsersState as? UiState.Success)?.data ?: emptyList(),
            salons = (adminSalonsState as? UiState.Success)?.data ?: emptyList(),
            appointments = (adminApptsState as? UiState.Success)?.data ?: emptyList(),
            reviews = (adminReviewsState as? UiState.Success)?.data ?: emptyList(),
            appSettings = appSettings,
            onSetUserStatus = viewModel::setUserAccountStatusAdmin,
            onToggleSalonActive = viewModel::toggleSalonActiveAdmin,
            onSaveAppSettings = viewModel::saveAppSettingsAdmin,
            onBack = { showAdminPanel = false }
        )
        return
    }

    val isBarber = profile.userType == UserRole.BARBER.value
    BackHandler(enabled = (isBarber && barberTab != BarberTab.DASHBOARD) || (!isBarber && customerTab != CustomerTab.HOME)) {
        if (isBarber) barberTab = BarberTab.DASHBOARD else customerTab = CustomerTab.HOME
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        if (isBarber) {
                            BarberTab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = barberTab == tab,
                                    onClick = { barberTab = tab },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label, maxLines = 1) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MatteBlack,
                                        indicatorColor = ChampagneGold
                                    ),
                                    modifier = Modifier.testTag("nav_barber_${tab.name.lowercase()}")
                                )
                            }
                        } else {
                            CustomerTab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = customerTab == tab,
                                    onClick = { customerTab = tab },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label, maxLines = 1) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MatteBlack,
                                        indicatorColor = ChampagneGold
                                    ),
                                    modifier = Modifier.testTag("nav_customer_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                if (useRail) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        if (isBarber) {
                            BarberTab.entries.forEach { tab ->
                                NavigationRailItem(
                                    selected = barberTab == tab,
                                    onClick = { barberTab = tab },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label) }
                                )
                            }
                        } else {
                            CustomerTab.entries.forEach { tab ->
                                NavigationRailItem(
                                    selected = customerTab == tab,
                                    onClick = { customerTab = tab },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label) }
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(modifier = Modifier.widthIn(max = 880.dp).fillMaxSize()) {
                        if (isBarber) {
                            when (barberTab) {
                                BarberTab.DASHBOARD -> BarberDashboardScreen(
                                    profile = profile,
                                    salon = barberSalon,
                                    appointments = barberAppts,
                                    slotLocks = slotLocks,
                                    unreadNotifications = unreadCount,
                                    onOpenNotifications = { showNotifications = true },
                                    onSaveSalon = { updated -> viewModel.saveSalonProfile(updated) }
                                )

                                BarberTab.SERVICES -> BarberServicesScreen(
                                    salon = barberSalon,
                                    services = barberServices,
                                    unreadNotifications = unreadCount,
                                    onOpenNotifications = { showNotifications = true },
                                    onSaveService = { svc -> viewModel.saveService(svc) },
                                    onAddPresets = { s, presets -> viewModel.addPresetServices(s, presets) },
                                    onToggleAvailable = viewModel::toggleServiceAvailability,
                                    onDeleteService = viewModel::deleteService
                                )

                                BarberTab.APPOINTMENTS -> BarberAppointmentsScreen(
                                    salon = barberSalon,
                                    appointments = barberAppts,
                                    slotLocks = slotLocks,
                                    unreadNotifications = unreadCount,
                                    onOpenNotifications = { showNotifications = true },
                                    onUpdateStatus = viewModel::updateAppointmentStatusByBarber,
                                    onCancelAppointment = { appt, reason ->
                                        viewModel.cancelAppointment(appt, reason, "BARBER")
                                    },
                                    onRescheduleAppointment = { appt, date, start, end ->
                                        viewModel.rescheduleAppointment(appt, date, start, end, "BARBER")
                                    }
                                )

                                BarberTab.CALENDAR -> BarberCalendarScreen(
                                    salon = barberSalon,
                                    appointments = barberAppts,
                                    slotLocks = slotLocks,
                                    unreadNotifications = unreadCount,
                                    onOpenNotifications = { showNotifications = true }
                                )

                                BarberTab.PROFILE -> UserProfileScreen(
                                    profile = profile,
                                    isDarkMode = isDarkMode,
                                    isAdmin = viewModel.isAdmin,
                                    unreadNotifications = unreadCount,
                                    onOpenNotifications = { showNotifications = true },
                                    onToggleDarkMode = onToggleDarkMode,
                                    onUpdateProfile = viewModel::saveUserProfile,
                                    onOpenAdminPanel = { showAdminPanel = true },
                                    onSignOut = onSignOut
                                )
                            }
                        } else {
                            when (customerTab) {
                                CustomerTab.HOME -> CustomerHomeScreen(
                                    profile = profile,
                                    salons = salons,
                                    services = services,
                                    appointments = customerAppts,
                                    favourites = favourites,
                                    unreadNotifications = unreadCount,
                                    onOpenNotifications = { showNotifications = true },
                                    onBookNow = { salon, svc ->
                                        viewModel.startBookingWizard(profile, salon, svc, appSettings.defaultTimezone)
                                    },
                                    onExploreCategory = { cat ->
                                        exploreFilter = cat
                                        customerTab = CustomerTab.EXPLORE
                                    },
                                    onToggleFavourite = viewModel::toggleFavourite,
                                    onInspectSalon = { salon ->
                                        viewModel.watchSalon(salon.id)
                                        inspectedSalon = salon
                                    },
                                    onViewAppointments = { customerTab = CustomerTab.APPOINTMENTS }
                                )

                                CustomerTab.EXPLORE -> CustomerExploreScreen(
                                    salons = salons,
                                    services = services,
                                    favourites = favourites,
                                    initialCategoryFilter = exploreFilter,
                                    unreadNotifications = unreadCount,
                                    onOpenNotifications = { showNotifications = true },
                                    onToggleFavourite = viewModel::toggleFavourite,
                                    onInspectSalon = { salon ->
                                        viewModel.watchSalon(salon.id)
                                        inspectedSalon = salon
                                    },
                                    onBookService = { salon, svc ->
                                        viewModel.startBookingWizard(profile, salon, svc, appSettings.defaultTimezone)
                                    }
                                )

                                CustomerTab.APPOINTMENTS -> CustomerAppointmentsScreen(
                                    appointments = customerAppts,
                                    salons = salons,
                                    observedSlotLocks = slotLocks,
                                    unreadNotifications = unreadCount,
                                    onOpenNotifications = { showNotifications = true },
                                    onWatchSalon = { salonId -> viewModel.watchSalon(salonId) },
                                    onCancelAppointment = { appt, reason, policy ->
                                        viewModel.cancelAppointment(appt, reason, "CUSTOMER", policy)
                                    },
                                    onRescheduleAppointment = { appt, date, start, end ->
                                        viewModel.rescheduleAppointment(appt, date, start, end, "CUSTOMER")
                                    },
                                    onSubmitReview = { appt, rating, comment ->
                                        viewModel.submitReview(appt, rating, comment)
                                    }
                                )

                                CustomerTab.FAVOURITES -> CustomerFavouritesScreen(
                                    salons = salons,
                                    services = services,
                                    favourites = favourites,
                                    unreadNotifications = unreadCount,
                                    onOpenNotifications = { showNotifications = true },
                                    onToggleFavourite = viewModel::toggleFavourite,
                                    onInspectSalon = { salon ->
                                        viewModel.watchSalon(salon.id)
                                        inspectedSalon = salon
                                    },
                                    onBookSalon = { salon ->
                                        viewModel.startBookingWizard(profile, salon, null, appSettings.defaultTimezone)
                                    }
                                )

                                CustomerTab.PROFILE -> UserProfileScreen(
                                    profile = profile,
                                    isDarkMode = isDarkMode,
                                    isAdmin = viewModel.isAdmin,
                                    unreadNotifications = unreadCount,
                                    onOpenNotifications = { showNotifications = true },
                                    onToggleDarkMode = onToggleDarkMode,
                                    onUpdateProfile = viewModel::saveUserProfile,
                                    onOpenAdminPanel = { showAdminPanel = true },
                                    onSignOut = onSignOut
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    inspectedSalon?.let { salon ->
        val favIds = favourites.map { it.salonId }.toSet()
        SalonDetailBottomSheet(
            salon = salon,
            services = services.filter { it.salonId == salon.id && it.isAvailable },
            reviews = salonReviews,
            isFavourite = favIds.contains(salon.id),
            onToggleFavourite = { viewModel.toggleFavourite(salon, favIds.contains(salon.id)) },
            onBookSalon = {
                inspectedSalon = null
                viewModel.startBookingWizard(profile, salon, null, appSettings.defaultTimezone)
            },
            onBookService = { svc ->
                inspectedSalon = null
                viewModel.startBookingWizard(profile, salon, svc, appSettings.defaultTimezone)
            },
            onDismiss = { inspectedSalon = null }
        )
    }

    if (showNotifications) {
        NotificationsSheet(
            notifications = notifications,
            onMarkRead = viewModel::markNotificationRead,
            onDismiss = { showNotifications = false }
        )
    }
}

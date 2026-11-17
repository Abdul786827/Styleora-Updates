package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppNotification
import com.example.data.model.AppSettings
import com.example.data.model.Appointment
import com.example.data.model.AppointmentStatus
import com.example.data.model.FavouriteSalon
import com.example.data.model.PaymentMethod
import com.example.data.model.SalonProfile
import com.example.data.model.SalonReview
import com.example.data.model.SalonService
import com.example.data.model.ServiceCategory
import com.example.data.model.ServicePreset
import com.example.data.model.SlotLock
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.repository.StyleoraRepository
import com.example.util.DateTimeUtils
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class BookingWizardState(
    val isActive: Boolean = false,
    val step: Int = 1,
    val category: String = ServiceCategory.MALE.value,
    val selectedSalon: SalonProfile? = null,
    val selectedServices: List<SalonService> = emptyList(),
    val customerName: String = "",
    val customerPhone: String = "",
    val customerEmail: String = "",
    val customerGender: String = "MALE",
    val specialInstructions: String = "",
    val paymentMethod: String = PaymentMethod.PAY_AT_SALON.value,
    val selectedDate: String = "",
    val selectedStartMinutes: Int? = null,
    val selectedEndMinutes: Int? = null,
    val heldLockId: String? = null,
    val holdExpiresAtEpochMs: Long = 0L,
    val isSubmitting: Boolean = false
) {
    val totalPrice: Double
        get() = selectedServices.sumOf { it.effectivePrice }

    val totalDurationMinutes: Int
        get() = selectedServices.sumOf { it.durationMinutes }.coerceAtLeast(if (selectedServices.isEmpty()) 30 else 0)
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class StyleoraViewModel(
    private val repository: StyleoraRepository,
    val currentUserId: String,
    val currentUserEmail: String,
    val currentUserDisplayName: String,
    val currentUserPhotoUrl: String? = null
) : ViewModel() {

    val isAdmin: Boolean = currentUserEmail.equals(ADMIN_EMAIL, ignoreCase = true)

    private fun <T> Flow<T>.toUiState(tag: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Stream error ($tag)", root)
                emit(UiState.Error(root.message ?: "Unable to sync $tag"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId).toUiState("userProfile")

    val activeSalonsState: StateFlow<UiState<List<SalonProfile>>> =
        repository.observeActiveSalons().toUiState("activeSalons")

    val barberSalonsState: StateFlow<UiState<List<SalonProfile>>> =
        repository.observeBarberSalons(currentUserId).toUiState("barberSalons")

    val activeServicesState: StateFlow<UiState<List<SalonService>>> =
        repository.observeActiveServices().toUiState("activeServices")

    val barberServicesState: StateFlow<UiState<List<SalonService>>> =
        repository.observeBarberServices(currentUserId).toUiState("barberServices")

    val customerAppointmentsState: StateFlow<UiState<List<Appointment>>> =
        repository.observeCustomerAppointments(currentUserId).toUiState("customerAppointments")

    val barberAppointmentsState: StateFlow<UiState<List<Appointment>>> =
        repository.observeBarberAppointments(currentUserId).toUiState("barberAppointments")

    val notificationsState: StateFlow<UiState<List<AppNotification>>> =
        repository.observeNotifications(currentUserId).toUiState("notifications")

    val favouritesState: StateFlow<UiState<List<FavouriteSalon>>> =
        repository.observeFavourites(currentUserId).toUiState("favourites")

    val appSettingsState: StateFlow<AppSettings> =
        repository.observeAppSettings()
            .catch { emit(AppSettings()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppSettings())

    // Admin streams (active only when isAdmin == true)
    val allUsersAdminState: StateFlow<UiState<List<UserProfile>>> =
        if (isAdmin) repository.observeAllUsersAdmin().toUiState("adminUsers")
        else MutableStateFlow(UiState.Success(emptyList()))

    val allSalonsAdminState: StateFlow<UiState<List<SalonProfile>>> =
        if (isAdmin) repository.observeAllSalonsAdmin().toUiState("adminSalons")
        else MutableStateFlow(UiState.Success(emptyList()))

    val allAppointmentsAdminState: StateFlow<UiState<List<Appointment>>> =
        if (isAdmin) repository.observeAllAppointmentsAdmin().toUiState("adminAppointments")
        else MutableStateFlow(UiState.Success(emptyList()))

    val allReviewsAdminState: StateFlow<UiState<List<SalonReview>>> =
        if (isAdmin) repository.observeAllReviewsAdmin().toUiState("adminReviews")
        else MutableStateFlow(UiState.Success(emptyList()))

    // Observed salon for booking / detail view
    private val _observedSalonId = MutableStateFlow<String?>(null)
    val observedSalonSlotLocks: StateFlow<List<SlotLock>> = _observedSalonId
        .flatMapLatest { salonId ->
            if (salonId.isNullOrBlank()) flowOf(emptyList())
            else repository.observeSalonSlotLocks(salonId).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val observedSalonReviews: StateFlow<List<SalonReview>> = _observedSalonId
        .flatMapLatest { salonId ->
            if (salonId.isNullOrBlank()) flowOf(emptyList())
            else repository.observeSalonReviews(salonId).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _bookingWizard = MutableStateFlow(BookingWizardState())
    val bookingWizard: StateFlow<BookingWizardState> = _bookingWizard.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private var holdExpiryJob: Job? = null

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun showFeedback(msg: String) {
        _feedbackMessage.value = msg
    }

    fun watchSalon(salonId: String?) {
        _observedSalonId.value = salonId
    }

    // --- USER PROFILE ---

    fun saveUserProfile(
        fullName: String,
        email: String,
        mobileNumber: String,
        userType: String,
        profilePhotoUri: String?,
        preferredTimezone: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.saveUserProfile(
                UserProfile(
                    uid = currentUserId,
                    fullName = fullName,
                    email = email,
                    mobileNumber = mobileNumber,
                    userType = userType,
                    profilePhotoUri = profilePhotoUri,
                    preferredTimezone = preferredTimezone
                )
            )
            _isBusy.value = false
            _feedbackMessage.value = result.fold(
                onSuccess = { "Profile saved successfully." },
                onFailure = { it.localizedMessage ?: "Failed to save profile." }
            )
        }
    }

    // --- SALON & SERVICE MANAGEMENT (BARBER) ---

    fun saveSalonProfile(salon: SalonProfile, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.saveSalonProfile(salon)
            _isBusy.value = false
            result.fold(
                onSuccess = {
                    _feedbackMessage.value = "Salon profile saved."
                    onSuccess()
                },
                onFailure = {
                    _feedbackMessage.value = it.localizedMessage ?: "Could not save salon profile."
                }
            )
        }
    }

    fun saveService(service: SalonService, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.saveService(service)
            _isBusy.value = false
            result.fold(
                onSuccess = {
                    _feedbackMessage.value = "Service '${service.name}' saved."
                    onSuccess()
                },
                onFailure = {
                    _feedbackMessage.value = it.localizedMessage ?: "Failed to save service."
                }
            )
        }
    }

    fun addPresetServices(salon: SalonProfile, presets: List<ServicePreset>, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            var count = 0
            presets.forEach { preset ->
                val res = repository.saveService(
                    SalonService(
                        salonId = salon.id,
                        barberId = currentUserId,
                        salonName = salon.salonName,
                        name = preset.name,
                        category = preset.category,
                        description = preset.description,
                        price = preset.defaultPrice,
                        durationMinutes = preset.defaultDurationMinutes,
                        isAvailable = true,
                        isActive = true
                    )
                )
                if (res.isSuccess) count++
            }
            _isBusy.value = false
            _feedbackMessage.value = "Added $count services to ${salon.salonName}."
            onComplete()
        }
    }

    fun toggleServiceAvailability(service: SalonService) {
        viewModelScope.launch {
            val res = repository.saveService(service.copy(isAvailable = !service.isAvailable))
            if (res.isFailure) {
                _feedbackMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to update availability."
            }
        }
    }

    fun toggleServiceActive(service: SalonService) {
        viewModelScope.launch {
            val res = repository.saveService(service.copy(isActive = !service.isActive))
            if (res.isFailure) {
                _feedbackMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to update service status."
            }
        }
    }

    fun deleteService(serviceId: String) {
        viewModelScope.launch {
            val res = repository.deleteService(serviceId)
            _feedbackMessage.value = res.fold(
                onSuccess = { "Service deleted." },
                onFailure = { it.localizedMessage ?: "Failed to delete service." }
            )
        }
    }

    // --- BOOKING WIZARD ---

    fun startBookingWizard(
        profile: UserProfile?,
        preselectedSalon: SalonProfile? = null,
        preselectedService: SalonService? = null,
        timezoneId: String = DateTimeUtils.DEFAULT_TIMEZONE
    ) {
        holdExpiryJob?.cancel()
        val initialCategory = when (preselectedService?.category) {
            ServiceCategory.FEMALE.value -> ServiceCategory.FEMALE.value
            else -> ServiceCategory.MALE.value
        }
        val initialStep = when {
            preselectedSalon != null && preselectedService != null -> 4
            preselectedSalon != null -> 3
            else -> 1
        }
        if (preselectedSalon != null) {
            watchSalon(preselectedSalon.id)
        }
        _bookingWizard.value = BookingWizardState(
            isActive = true,
            step = initialStep,
            category = initialCategory,
            selectedSalon = preselectedSalon,
            selectedServices = listOfNotNull(preselectedService),
            customerName = profile?.fullName?.ifBlank { currentUserDisplayName } ?: currentUserDisplayName,
            customerPhone = profile?.mobileNumber ?: "",
            customerEmail = profile?.email?.ifBlank { currentUserEmail } ?: currentUserEmail,
            customerGender = initialCategory,
            paymentMethod = preselectedSalon?.acceptedPaymentMethods?.firstOrNull() ?: PaymentMethod.PAY_AT_SALON.value,
            selectedDate = DateTimeUtils.todayDateString(preselectedSalon?.timezone ?: timezoneId)
        )
    }

    fun closeBookingWizard() {
        val lockId = _bookingWizard.value.heldLockId
        holdExpiryJob?.cancel()
        if (!lockId.isNullOrBlank()) {
            viewModelScope.launch { repository.releaseHeldSlot(lockId) }
        }
        _bookingWizard.value = BookingWizardState()
    }

    fun setBookingStep(step: Int) {
        _bookingWizard.update { it.copy(step = step.coerceIn(1, 6)) }
    }

    fun selectBookingCategory(category: String) {
        _bookingWizard.update {
            it.copy(
                category = category,
                customerGender = category,
                selectedServices = emptyList(),
                selectedStartMinutes = null,
                selectedEndMinutes = null
            )
        }
    }

    fun selectBookingSalon(salon: SalonProfile) {
        val oldLock = _bookingWizard.value.heldLockId
        if (!oldLock.isNullOrBlank()) {
            viewModelScope.launch { repository.releaseHeldSlot(oldLock) }
        }
        watchSalon(salon.id)
        _bookingWizard.update {
            it.copy(
                selectedSalon = salon,
                selectedServices = emptyList(),
                paymentMethod = salon.acceptedPaymentMethods.firstOrNull() ?: PaymentMethod.PAY_AT_SALON.value,
                selectedDate = DateTimeUtils.todayDateString(salon.timezone),
                selectedStartMinutes = null,
                selectedEndMinutes = null,
                heldLockId = null,
                holdExpiresAtEpochMs = 0L
            )
        }
    }

    fun toggleBookingService(service: SalonService) {
        _bookingWizard.update { state ->
            val exists = state.selectedServices.any { it.id == service.id }
            val updated = if (exists) {
                state.selectedServices.filterNot { it.id == service.id }
            } else {
                state.selectedServices + service
            }
            state.copy(
                selectedServices = updated,
                selectedStartMinutes = null,
                selectedEndMinutes = null
            )
        }
    }

    fun updateCustomerBookingInfo(
        name: String,
        phone: String,
        email: String,
        gender: String,
        instructions: String,
        paymentMethod: String
    ) {
        _bookingWizard.update {
            it.copy(
                customerName = name,
                customerPhone = phone,
                customerEmail = email,
                customerGender = gender,
                specialInstructions = instructions,
                paymentMethod = paymentMethod
            )
        }
    }

    fun selectBookingDate(dateStr: String) {
        val oldLock = _bookingWizard.value.heldLockId
        if (!oldLock.isNullOrBlank()) {
            viewModelScope.launch { repository.releaseHeldSlot(oldLock) }
        }
        holdExpiryJob?.cancel()
        _bookingWizard.update {
            it.copy(
                selectedDate = dateStr,
                selectedStartMinutes = null,
                selectedEndMinutes = null,
                heldLockId = null,
                holdExpiresAtEpochMs = 0L
            )
        }
    }

    fun holdBookingSlot(startMinutes: Int, endMinutes: Int) {
        val state = _bookingWizard.value
        val salon = state.selectedSalon ?: return
        viewModelScope.launch {
            _isBusy.value = true
            state.heldLockId?.takeIf { it.isNotBlank() }?.let { repository.releaseHeldSlot(it) }
            val holdMinutes = salon.holdTimeoutMinutes.coerceIn(1, 60)
            val result = repository.holdTimeSlot(
                salonId = salon.id,
                barberId = salon.ownerId,
                date = state.selectedDate,
                startMinutes = startMinutes,
                endMinutes = endMinutes,
                holdTimeoutMinutes = holdMinutes
            )
            _isBusy.value = false
            result.fold(
                onSuccess = { lockId ->
                    val expiresAt = System.currentTimeMillis() + (holdMinutes * 60_000L)
                    _bookingWizard.update {
                        it.copy(
                            selectedStartMinutes = startMinutes,
                            selectedEndMinutes = endMinutes,
                            heldLockId = lockId,
                            holdExpiresAtEpochMs = expiresAt
                        )
                    }
                    startHoldCountdown(lockId, holdMinutes * 60_000L)
                },
                onFailure = {
                    _feedbackMessage.value = it.localizedMessage ?: "Slot could not be held."
                }
            )
        }
    }

    private fun startHoldCountdown(lockId: String, durationMs: Long) {
        holdExpiryJob?.cancel()
        holdExpiryJob = viewModelScope.launch {
            delay(durationMs)
            val current = _bookingWizard.value
            if (current.isActive && current.heldLockId == lockId) {
                repository.releaseHeldSlot(lockId)
                _bookingWizard.update {
                    it.copy(
                        selectedStartMinutes = null,
                        selectedEndMinutes = null,
                        heldLockId = null,
                        holdExpiresAtEpochMs = 0L
                    )
                }
                _feedbackMessage.value = "Slot reservation timed out and was released automatically."
            }
        }
    }

    fun confirmBooking(onBooked: () -> Unit) {
        val state = _bookingWizard.value
        val salon = state.selectedSalon ?: return
        val start = state.selectedStartMinutes ?: return
        val end = state.selectedEndMinutes ?: return
        if (state.selectedServices.isEmpty()) {
            _feedbackMessage.value = "Please select at least one service."
            return
        }
        viewModelScope.launch {
            _bookingWizard.update { it.copy(isSubmitting = true) }
            val appt = Appointment(
                salonId = salon.id,
                salonName = salon.salonName,
                barberId = salon.ownerId,
                customerId = currentUserId,
                customerName = state.customerName.trim(),
                customerPhone = state.customerPhone.trim(),
                customerEmail = state.customerEmail.trim(),
                customerGender = state.customerGender,
                category = state.category,
                serviceIds = state.selectedServices.map { it.id },
                serviceNames = state.selectedServices.map { it.name },
                totalPrice = state.totalPrice,
                totalDurationMinutes = state.totalDurationMinutes,
                date = state.selectedDate,
                startMinutes = start,
                endMinutes = end,
                specialInstructions = state.specialInstructions.trim(),
                paymentMethod = state.paymentMethod
            )
            val res = repository.confirmBooking(appt, state.heldLockId)
            holdExpiryJob?.cancel()
            res.fold(
                onSuccess = {
                    _bookingWizard.value = BookingWizardState()
                    _feedbackMessage.value = "Appointment confirmed! Your STYLEORA slot is booked."
                    onBooked()
                },
                onFailure = { err ->
                    _bookingWizard.update { it.copy(isSubmitting = false) }
                    _feedbackMessage.value = err.localizedMessage ?: "Booking confirmation failed."
                }
            )
        }
    }

    // --- APPOINTMENT ACTIONS (CANCELLATION, RESCHEDULE, STATUS, REVIEW) ---

    fun cancelAppointment(appointment: Appointment, reason: String, role: String, salonPolicy: SalonProfile? = null) {
        if (role == "CUSTOMER" && salonPolicy != null) {
            if (!salonPolicy.allowCustomerCancellation) {
                _feedbackMessage.value = "This salon does not allow self-service cancellations. Please contact ${salonPolicy.contactNumber}."
                return
            }
            if (salonPolicy.minCancellationHours > 0) {
                val apptMs = DateTimeUtils.appointmentStartEpochMs(appointment.date, appointment.startMinutes, salonPolicy.timezone)
                val diffHours = (apptMs - System.currentTimeMillis()) / 3_600_000.0
                if (diffHours < salonPolicy.minCancellationHours) {
                    _feedbackMessage.value = "Cancellations require at least ${salonPolicy.minCancellationHours}h notice per salon policy."
                    return
                }
            }
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.cancelAppointment(appointment, reason, role)
            _isBusy.value = false
            _feedbackMessage.value = res.fold(
                onSuccess = { "Appointment cancelled and time slot released to Available." },
                onFailure = { it.localizedMessage ?: "Unable to cancel appointment." }
            )
        }
    }

    fun rescheduleAppointment(
        appointment: Appointment,
        newDate: String,
        newStartMinutes: Int,
        newEndMinutes: Int,
        actorRole: String,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.rescheduleAppointment(appointment, newDate, newStartMinutes, newEndMinutes, actorRole)
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    _feedbackMessage.value = "Appointment rescheduled to $newDate."
                    onDone()
                },
                onFailure = {
                    _feedbackMessage.value = it.localizedMessage ?: "Could not reschedule appointment."
                }
            )
        }
    }

    fun updateAppointmentStatusByBarber(appointment: Appointment, status: AppointmentStatus, markPaid: Boolean = false) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateAppointmentStatusByBarber(appointment, status, markPaid)
            _isBusy.value = false
            _feedbackMessage.value = res.fold(
                onSuccess = { "Appointment updated to ${status.label}." },
                onFailure = { it.localizedMessage ?: "Failed to update status." }
            )
        }
    }

    fun submitReview(appointment: Appointment, rating: Int, comment: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.submitReview(appointment, rating, comment)
            _isBusy.value = false
            res.fold(
                onSuccess = {
                    _feedbackMessage.value = "Thank you for your $rating-star review!"
                    onDone()
                },
                onFailure = {
                    _feedbackMessage.value = it.localizedMessage ?: "Could not submit review."
                }
            )
        }
    }

    fun toggleFavourite(salon: SalonProfile, isCurrentlyFav: Boolean) {
        viewModelScope.launch {
            val res = repository.toggleFavourite(salon, isCurrentlyFav)
            if (res.isFailure) {
                _feedbackMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to update favourites."
            }
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(notificationId)
        }
    }

    // --- ADMIN ACTIONS ---

    fun setUserAccountStatusAdmin(uid: String, status: String) {
        viewModelScope.launch {
            val res = repository.setUserAccountStatusAdmin(uid, status)
            _feedbackMessage.value = res.fold(
                onSuccess = { "Account updated to $status." },
                onFailure = { it.localizedMessage ?: "Failed to update user account." }
            )
        }
    }

    fun toggleSalonActiveAdmin(salonId: String, isActive: Boolean) {
        viewModelScope.launch {
            val res = repository.toggleSalonActiveAdmin(salonId, isActive)
            _feedbackMessage.value = res.fold(
                onSuccess = { if (isActive) "Salon activated." else "Salon suspended." },
                onFailure = { it.localizedMessage ?: "Failed to update salon." }
            )
        }
    }

    fun saveAppSettingsAdmin(timezone: String, holdMinutes: Int) {
        viewModelScope.launch {
            val res = repository.saveAppSettingsAdmin(timezone, holdMinutes)
            _feedbackMessage.value = res.fold(
                onSuccess = { "Global app settings updated." },
                onFailure = { it.localizedMessage ?: "Failed to update app settings." }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        holdExpiryJob?.cancel()
    }

    companion object {
        private const val TAG = "StyleoraVM"
        const val ADMIN_EMAIL = "abdulquddus7745@gmail.com"
    }
}

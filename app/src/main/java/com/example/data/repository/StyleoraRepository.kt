package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AppNotification
import com.example.data.model.AppSettings
import com.example.data.model.Appointment
import com.example.data.model.AppointmentStatus
import com.example.data.model.FavouriteSalon
import com.example.data.model.PaymentStatus
import com.example.data.model.SalonProfile
import com.example.data.model.SalonReview
import com.example.data.model.SalonService
import com.example.data.model.SlotLock
import com.example.data.model.SlotStatus
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StyleoraRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> safeSnapshotFlow(
        path: String,
        expectedUid: String? = null,
        flowProvider: () -> Flow<T>
    ): Flow<T> = flow {
        emitAll(
            flowProvider().catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val toLog = firestoreError ?: (error as? Exception)
                if (toLog != null && (expectedUid == null || auth.currentUser?.uid == expectedUid)) {
                    handleFirestoreError(toLog, OperationType.LIST, path)
                }
                throw error
            }
        )
    }

    // --- USER PROFILE ---

    fun observeUserProfile(uid: String): Flow<UserProfile?> =
        safeSnapshotFlow("users/$uid", uid) {
            db.collection("users").document(uid)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
        }

    fun observeAllUsersAdmin(): Flow<List<UserProfile>> =
        safeSnapshotFlow("users") {
            db.collection("users")
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
        }

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val payload = profile.copy(uid = uid, accountStatus = "ACTIVE").toCreateMap()
                docRef.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "fullName" to profile.fullName.trim(),
                    "email" to profile.email.trim(),
                    "mobileNumber" to profile.mobileNumber.trim(),
                    "userType" to profile.userType,
                    "preferredTimezone" to profile.preferredTimezone,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                profile.profilePhotoUri?.let { updates["profilePhotoUri"] = it }
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun setUserAccountStatusAdmin(targetUid: String, status: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("users").document(targetUid)
        return try {
            docRef.update(
                mapOf(
                    "accountStatus" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- SALONS ---

    fun observeActiveSalons(): Flow<List<SalonProfile>> =
        safeSnapshotFlow("salons") {
            db.collection("salons")
                .whereEqualTo("isActive", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(SalonProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.averageRating }
                }
        }

    fun observeBarberSalons(ownerId: String): Flow<List<SalonProfile>> =
        safeSnapshotFlow("salons", ownerId) {
            db.collection("salons")
                .whereEqualTo("ownerId", ownerId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(SalonProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
        }

    fun observeAllSalonsAdmin(): Flow<List<SalonProfile>> =
        safeSnapshotFlow("salons") {
            db.collection("salons")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SalonProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
        }

    suspend fun saveSalonProfile(salon: SalonProfile): Result<String> {
        val uid = requireUserId()
        val isCreate = salon.id.isBlank()
        val docRef = if (isCreate) {
            db.collection("salons").document("salon_${UUID.randomUUID().toString().take(12)}")
        } else {
            db.collection("salons").document(salon.id)
        }
        return try {
            if (isCreate) {
                val payload = mapOf(
                    "ownerId" to uid,
                    "salonName" to salon.salonName.trim(),
                    "ownerName" to salon.ownerName.trim(),
                    "logoUri" to salon.logoUri,
                    "coverImageUri" to salon.coverImageUri,
                    "contactNumber" to salon.contactNumber.trim(),
                    "whatsappNumber" to salon.whatsappNumber?.trim(),
                    "address" to salon.address.trim(),
                    "mapsLocation" to salon.mapsLocation?.trim(),
                    "workingDays" to salon.workingDays,
                    "openingTimeMinutes" to salon.openingTimeMinutes,
                    "closingTimeMinutes" to salon.closingTimeMinutes,
                    "breakStartMinutes" to salon.breakStartMinutes,
                    "breakEndMinutes" to salon.breakEndMinutes,
                    "description" to salon.description?.trim(),
                    "acceptedPaymentMethods" to salon.acceptedPaymentMethods.ifEmpty { listOf("PAY_AT_SALON") },
                    "depositPercent" to salon.depositPercent,
                    "allowCustomerCancellation" to salon.allowCustomerCancellation,
                    "minCancellationHours" to salon.minCancellationHours,
                    "cancellationRequiresApproval" to salon.cancellationRequiresApproval,
                    "cancellationPolicyText" to salon.cancellationPolicyText?.trim(),
                    "holdTimeoutMinutes" to salon.holdTimeoutMinutes.coerceIn(1, 60),
                    "timezone" to salon.timezone.ifBlank { "Asia/Karachi" },
                    "averageRating" to 0.0,
                    "reviewCount" to 0,
                    "isActive" to salon.isActive,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }.mapValues { it.value!! }
                docRef.set(payload).await()
            } else {
                val payload = mapOf(
                    "salonName" to salon.salonName.trim(),
                    "ownerName" to salon.ownerName.trim(),
                    "logoUri" to salon.logoUri,
                    "coverImageUri" to salon.coverImageUri,
                    "contactNumber" to salon.contactNumber.trim(),
                    "whatsappNumber" to salon.whatsappNumber?.trim(),
                    "address" to salon.address.trim(),
                    "mapsLocation" to salon.mapsLocation?.trim(),
                    "workingDays" to salon.workingDays,
                    "openingTimeMinutes" to salon.openingTimeMinutes,
                    "closingTimeMinutes" to salon.closingTimeMinutes,
                    "breakStartMinutes" to salon.breakStartMinutes,
                    "breakEndMinutes" to salon.breakEndMinutes,
                    "description" to salon.description?.trim(),
                    "acceptedPaymentMethods" to salon.acceptedPaymentMethods.ifEmpty { listOf("PAY_AT_SALON") },
                    "depositPercent" to salon.depositPercent,
                    "allowCustomerCancellation" to salon.allowCustomerCancellation,
                    "minCancellationHours" to salon.minCancellationHours,
                    "cancellationRequiresApproval" to salon.cancellationRequiresApproval,
                    "cancellationPolicyText" to salon.cancellationPolicyText?.trim(),
                    "holdTimeoutMinutes" to salon.holdTimeoutMinutes.coerceIn(1, 60),
                    "timezone" to salon.timezone.ifBlank { "Asia/Karachi" },
                    "isActive" to salon.isActive,
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }.mapValues { it.value!! }
                docRef.update(payload).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleSalonActiveAdmin(salonId: String, isActive: Boolean): Result<Unit> {
        requireUserId()
        val docRef = db.collection("salons").document(salonId)
        return try {
            docRef.update(
                mapOf(
                    "isActive" to isActive,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- SERVICES ---

    fun observeActiveServices(): Flow<List<SalonService>> =
        safeSnapshotFlow("services") {
            db.collection("services")
                .whereEqualTo("isActive", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(SalonService::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.name }
                }
        }

    fun observeBarberServices(barberId: String): Flow<List<SalonService>> =
        safeSnapshotFlow("services", barberId) {
            db.collection("services")
                .whereEqualTo("barberId", barberId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(SalonService::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
        }

    suspend fun saveService(service: SalonService): Result<String> {
        val uid = requireUserId()
        val isCreate = service.id.isBlank()
        val docRef = if (isCreate) {
            db.collection("services").document("svc_${UUID.randomUUID().toString().take(12)}")
        } else {
            db.collection("services").document(service.id)
        }
        return try {
            if (isCreate) {
                val payload = mapOf(
                    "salonId" to service.salonId,
                    "barberId" to uid,
                    "salonName" to service.salonName.trim(),
                    "name" to service.name.trim(),
                    "category" to service.category,
                    "description" to service.description?.trim(),
                    "imageUri" to service.imageUri,
                    "price" to service.price,
                    "discountPrice" to service.discountPrice,
                    "durationMinutes" to service.durationMinutes.coerceIn(5, 480),
                    "isAvailable" to service.isAvailable,
                    "isActive" to service.isActive,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }.mapValues { it.value!! }
                docRef.set(payload).await()
            } else {
                val payload = mapOf(
                    "salonName" to service.salonName.trim(),
                    "name" to service.name.trim(),
                    "category" to service.category,
                    "description" to service.description?.trim(),
                    "imageUri" to service.imageUri,
                    "price" to service.price,
                    "discountPrice" to service.discountPrice,
                    "durationMinutes" to service.durationMinutes.coerceIn(5, 480),
                    "isAvailable" to service.isAvailable,
                    "isActive" to service.isActive,
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }.mapValues { it.value!! }
                docRef.update(payload).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteService(serviceId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("services").document(serviceId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- SLOT LOCKS & REAL-TIME AVAILABILITY ---

    fun observeSalonSlotLocks(salonId: String): Flow<List<SlotLock>> =
        safeSnapshotFlow("slot_locks") {
            db.collection("slot_locks")
                .whereEqualTo("salonId", salonId)
                .whereIn("status", listOf(SlotStatus.SELECTED.value, SlotStatus.BOOKED.value, SlotStatus.CANCELLED.value))
                .snapshots()
                .map { snap ->
                    snap.toObjects(SlotLock::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
        }

    suspend fun holdTimeSlot(
        salonId: String,
        barberId: String,
        date: String,
        startMinutes: Int,
        endMinutes: Int,
        holdTimeoutMinutes: Int
    ): Result<String> {
        val uid = requireUserId()
        val nowMs = System.currentTimeMillis()
        val expiresAt = nowMs + (holdTimeoutMinutes.coerceIn(1, 60) * 60_000L)
        val lockId = "lock_${salonId}_${date}_${startMinutes}_${UUID.randomUUID().toString().take(6)}"
        val docRef = db.collection("slot_locks").document(lockId)

        return try {
            // Verify no overlapping BOOKED or active SELECTED lock exists for this salon+date
            val existingLocks = db.collection("slot_locks")
                .whereEqualTo("salonId", salonId)
                .whereIn("status", listOf(SlotStatus.SELECTED.value, SlotStatus.BOOKED.value))
                .get()
                .await()
                .toObjects(SlotLock::class.java)
                .filter { it.date == date }

            val conflict = existingLocks.any { lock ->
                val isActive = lock.status == SlotStatus.BOOKED.value ||
                    (lock.status == SlotStatus.SELECTED.value && lock.expiresAtEpochMs > nowMs && lock.holderUid != uid)
                isActive && startMinutes < lock.endMinutes && endMinutes > lock.startMinutes
            }

            if (conflict) {
                return Result.failure(IllegalStateException("This time slot was just reserved by another guest. Please pick another slot."))
            }

            val payload = mapOf(
                "salonId" to salonId,
                "barberId" to barberId,
                "date" to date,
                "startMinutes" to startMinutes,
                "endMinutes" to endMinutes,
                "status" to SlotStatus.SELECTED.value,
                "holderUid" to uid,
                "expiresAtEpochMs" to expiresAt,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(lockId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun releaseHeldSlot(lockId: String): Result<Unit> {
        if (lockId.isBlank()) return Result.success(Unit)
        requireUserId()
        val docRef = db.collection("slot_locks").document(lockId)
        return try {
            val snap = docRef.get().await()
            if (snap.exists() && snap.getString("status") == SlotStatus.SELECTED.value) {
                docRef.delete().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- APPOINTMENTS ---

    fun observeCustomerAppointments(customerId: String): Flow<List<Appointment>> =
        safeSnapshotFlow("appointments", customerId) {
            db.collection("appointments")
                .whereEqualTo("customerId", customerId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Appointment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { "${it.date}_${it.startMinutes.toString().padStart(4, '0')}" }
                }
        }

    fun observeBarberAppointments(barberId: String): Flow<List<Appointment>> =
        safeSnapshotFlow("appointments", barberId) {
            db.collection("appointments")
                .whereEqualTo("barberId", barberId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Appointment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { "${it.date}_${it.startMinutes.toString().padStart(4, '0')}" }
                }
        }

    fun observeAllAppointmentsAdmin(): Flow<List<Appointment>> =
        safeSnapshotFlow("appointments") {
            db.collection("appointments")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Appointment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
        }

    suspend fun confirmBooking(
        appointment: Appointment,
        existingHoldLockId: String?
    ): Result<String> {
        val uid = requireUserId()
        val nowMs = System.currentTimeMillis()
        val appointmentId = "appt_${UUID.randomUUID().toString().take(12)}"
        val lockId = existingHoldLockId?.takeIf { it.isNotBlank() }
            ?: "lock_${appointment.salonId}_${appointment.date}_${appointment.startMinutes}_${UUID.randomUUID().toString().take(6)}"

        val apptRef = db.collection("appointments").document(appointmentId)
        val lockRef = db.collection("slot_locks").document(lockId)
        val barberNotifRef = db.collection("notifications").document("notif_${UUID.randomUUID().toString().take(12)}")
        val customerNotifRef = db.collection("notifications").document("notif_${UUID.randomUUID().toString().take(12)}")

        return try {
            // Double-booking prevention check
            val activeLocks = db.collection("slot_locks")
                .whereEqualTo("salonId", appointment.salonId)
                .whereIn("status", listOf(SlotStatus.SELECTED.value, SlotStatus.BOOKED.value))
                .get()
                .await()
                .toObjects(SlotLock::class.java)
                .filter { it.date == appointment.date && it.id != lockId }

            val hasConflict = activeLocks.any { lock ->
                val isBooked = lock.status == SlotStatus.BOOKED.value
                val isOtherHold = lock.status == SlotStatus.SELECTED.value && lock.expiresAtEpochMs > nowMs && lock.holderUid != uid
                (isBooked || isOtherHold) && appointment.startMinutes < lock.endMinutes && appointment.endMinutes > lock.startMinutes
            }

            if (hasConflict) {
                return Result.failure(IllegalStateException("Selected slot is no longer available. Please choose another time."))
            }

            val paymentStatus = if (appointment.paymentMethod == "ONLINE" || appointment.paymentMethod == "DEPOSIT") {
                PaymentStatus.PAID.value
            } else {
                PaymentStatus.PENDING.value
            }

            val batch = db.batch()
            val lockSnap = lockRef.get().await()
            if (lockSnap.exists()) {
                batch.update(
                    lockRef,
                    mapOf(
                        "status" to SlotStatus.BOOKED.value,
                        "appointmentId" to appointmentId,
                        "expiresAtEpochMs" to 0L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    lockRef,
                    mapOf(
                        "salonId" to appointment.salonId,
                        "barberId" to appointment.barberId,
                        "date" to appointment.date,
                        "startMinutes" to appointment.startMinutes,
                        "endMinutes" to appointment.endMinutes,
                        "status" to SlotStatus.BOOKED.value,
                        "holderUid" to uid,
                        "appointmentId" to appointmentId,
                        "expiresAtEpochMs" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            val apptPayload = mapOf(
                "salonId" to appointment.salonId,
                "salonName" to appointment.salonName,
                "barberId" to appointment.barberId,
                "customerId" to uid,
                "customerName" to appointment.customerName.trim(),
                "customerPhone" to appointment.customerPhone.trim(),
                "customerEmail" to appointment.customerEmail.trim(),
                "customerGender" to appointment.customerGender,
                "category" to appointment.category,
                "serviceIds" to appointment.serviceIds,
                "serviceNames" to appointment.serviceNames,
                "totalPrice" to appointment.totalPrice,
                "totalDurationMinutes" to appointment.totalDurationMinutes,
                "date" to appointment.date,
                "startMinutes" to appointment.startMinutes,
                "endMinutes" to appointment.endMinutes,
                "specialInstructions" to appointment.specialInstructions?.takeIf { it.isNotBlank() },
                "status" to AppointmentStatus.BOOKED.value,
                "paymentMethod" to appointment.paymentMethod,
                "paymentStatus" to paymentStatus,
                "slotLockId" to lockId,
                "hasReview" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }.mapValues { it.value!! }

            batch.set(apptRef, apptPayload)

            batch.set(
                barberNotifRef,
                mapOf(
                    "recipientId" to appointment.barberId,
                    "senderId" to uid,
                    "title" to "New Appointment Request",
                    "message" to "${appointment.customerName} booked ${appointment.serviceNames.joinToString(", ")} on ${appointment.date}.",
                    "type" to "BOOKING_NEW",
                    "appointmentId" to appointmentId,
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                customerNotifRef,
                mapOf(
                    "recipientId" to uid,
                    "senderId" to uid,
                    "title" to "Booking Confirmed",
                    "message" to "Your STYLEORA visit at ${appointment.salonName} on ${appointment.date} is confirmed.",
                    "type" to "BOOKING_CONFIRMED",
                    "appointmentId" to appointmentId,
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            batch.commit().await()
            Result.success(appointmentId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, apptRef.path)
            Result.failure(e)
        }
    }

    suspend fun cancelAppointment(
        appointment: Appointment,
        reason: String,
        role: String // "CUSTOMER" or "BARBER"
    ): Result<Unit> {
        val uid = requireUserId()
        if (appointment.status == AppointmentStatus.COMPLETED.value ||
            appointment.status == AppointmentStatus.CANCELLED.value
        ) {
            return Result.failure(IllegalStateException("This appointment can no longer be cancelled."))
        }
        val apptRef = db.collection("appointments").document(appointment.id)
        val batch = db.batch()
        return try {
            val nextPaymentStatus = if (appointment.paymentStatus == PaymentStatus.PAID.value) {
                PaymentStatus.REFUNDED.value
            } else {
                appointment.paymentStatus
            }
            batch.update(
                apptRef,
                mapOf(
                    "status" to AppointmentStatus.CANCELLED.value,
                    "cancellationReason" to reason.ifBlank { "No reason provided" },
                    "cancelledByUid" to uid,
                    "cancelledByRole" to role,
                    "cancelledAtEpochMs" to System.currentTimeMillis(),
                    "paymentStatus" to nextPaymentStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            val lockId = appointment.slotLockId
            if (!lockId.isNullOrBlank()) {
                val lockRef = db.collection("slot_locks").document(lockId)
                batch.update(
                    lockRef,
                    mapOf(
                        "status" to SlotStatus.CANCELLED.value,
                        "expiresAtEpochMs" to 0L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            // Notify both Barber and Customer
            val otherPartyId = if (role == "CUSTOMER") appointment.barberId else appointment.customerId
            val actorLabel = if (role == "CUSTOMER") appointment.customerName else appointment.salonName

            val notifOtherRef = db.collection("notifications").document("notif_${UUID.randomUUID().toString().take(12)}")
            batch.set(
                notifOtherRef,
                mapOf(
                    "recipientId" to otherPartyId,
                    "senderId" to uid,
                    "title" to "Appointment Cancelled",
                    "message" to "$actorLabel cancelled the appointment on ${appointment.date}. Reason: ${reason.ifBlank { "Not specified" }}",
                    "type" to "BOOKING_CANCELLED",
                    "appointmentId" to appointment.id,
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            val notifSelfRef = db.collection("notifications").document("notif_${UUID.randomUUID().toString().take(12)}")
            batch.set(
                notifSelfRef,
                mapOf(
                    "recipientId" to uid,
                    "senderId" to uid,
                    "title" to "Cancellation Recorded",
                    "message" to "Appointment at ${appointment.salonName} on ${appointment.date} was cancelled and the time slot was released.",
                    "type" to "BOOKING_CANCELLED",
                    "appointmentId" to appointment.id,
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, apptRef.path)
            Result.failure(e)
        }
    }

    suspend fun rescheduleAppointment(
        appointment: Appointment,
        newDate: String,
        newStartMinutes: Int,
        newEndMinutes: Int,
        actorRole: String
    ): Result<Unit> {
        val uid = requireUserId()
        val nowMs = System.currentTimeMillis()
        val apptRef = db.collection("appointments").document(appointment.id)
        val newLockId = "lock_${appointment.salonId}_${newDate}_${newStartMinutes}_${UUID.randomUUID().toString().take(6)}"
        val newLockRef = db.collection("slot_locks").document(newLockId)

        return try {
            val activeLocks = db.collection("slot_locks")
                .whereEqualTo("salonId", appointment.salonId)
                .whereIn("status", listOf(SlotStatus.SELECTED.value, SlotStatus.BOOKED.value))
                .get()
                .await()
                .toObjects(SlotLock::class.java)
                .filter { it.date == newDate && it.id != appointment.slotLockId }

            val conflict = activeLocks.any { lock ->
                val active = lock.status == SlotStatus.BOOKED.value ||
                    (lock.status == SlotStatus.SELECTED.value && lock.expiresAtEpochMs > nowMs && lock.holderUid != uid)
                active && newStartMinutes < lock.endMinutes && newEndMinutes > lock.startMinutes
            }
            if (conflict) {
                return Result.failure(IllegalStateException("Selected reschedule slot is unavailable."))
            }

            val batch = db.batch()
            if (!appointment.slotLockId.isNullOrBlank()) {
                val oldLockRef = db.collection("slot_locks").document(appointment.slotLockId)
                batch.update(
                    oldLockRef,
                    mapOf(
                        "status" to SlotStatus.CANCELLED.value,
                        "expiresAtEpochMs" to 0L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.set(
                newLockRef,
                mapOf(
                    "salonId" to appointment.salonId,
                    "barberId" to appointment.barberId,
                    "date" to newDate,
                    "startMinutes" to newStartMinutes,
                    "endMinutes" to newEndMinutes,
                    "status" to SlotStatus.BOOKED.value,
                    "holderUid" to uid,
                    "appointmentId" to appointment.id,
                    "expiresAtEpochMs" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            batch.update(
                apptRef,
                mapOf(
                    "date" to newDate,
                    "startMinutes" to newStartMinutes,
                    "endMinutes" to newEndMinutes,
                    "slotLockId" to newLockId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            val otherPartyId = if (actorRole == "CUSTOMER") appointment.barberId else appointment.customerId
            val notifRef = db.collection("notifications").document("notif_${UUID.randomUUID().toString().take(12)}")
            batch.set(
                notifRef,
                mapOf(
                    "recipientId" to otherPartyId,
                    "senderId" to uid,
                    "title" to "Appointment Rescheduled",
                    "message" to "Appointment at ${appointment.salonName} moved to $newDate.",
                    "type" to "BOOKING_RESCHEDULED",
                    "appointmentId" to appointment.id,
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, apptRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateAppointmentStatusByBarber(
        appointment: Appointment,
        newStatus: AppointmentStatus,
        markPaid: Boolean = false
    ): Result<Unit> {
        val uid = requireUserId()
        val apptRef = db.collection("appointments").document(appointment.id)
        val batch = db.batch()
        return try {
            val nextPayStatus = when {
                markPaid || newStatus == AppointmentStatus.COMPLETED -> PaymentStatus.PAID.value
                newStatus == AppointmentStatus.REJECTED && appointment.paymentStatus == PaymentStatus.PAID.value -> PaymentStatus.REFUNDED.value
                else -> appointment.paymentStatus
            }
            batch.update(
                apptRef,
                mapOf(
                    "status" to newStatus.value,
                    "paymentStatus" to nextPayStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (newStatus == AppointmentStatus.REJECTED && !appointment.slotLockId.isNullOrBlank()) {
                val lockRef = db.collection("slot_locks").document(appointment.slotLockId)
                batch.update(
                    lockRef,
                    mapOf(
                        "status" to SlotStatus.CANCELLED.value,
                        "expiresAtEpochMs" to 0L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            val notifRef = db.collection("notifications").document("notif_${UUID.randomUUID().toString().take(12)}")
            val notifType = when (newStatus) {
                AppointmentStatus.BOOKED -> "BOOKING_CONFIRMED"
                AppointmentStatus.COMPLETED -> "BOOKING_COMPLETED"
                AppointmentStatus.REJECTED -> "BOOKING_REJECTED"
                else -> "BOOKING_CANCELLED"
            }
            batch.set(
                notifRef,
                mapOf(
                    "recipientId" to appointment.customerId,
                    "senderId" to uid,
                    "title" to "Appointment ${newStatus.label}",
                    "message" to "Your appointment at ${appointment.salonName} on ${appointment.date} is now ${newStatus.label}.",
                    "type" to notifType,
                    "appointmentId" to appointment.id,
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, apptRef.path)
            Result.failure(e)
        }
    }

    // --- REVIEWS & RATINGS ---

    fun observeSalonReviews(salonId: String): Flow<List<SalonReview>> =
        safeSnapshotFlow("reviews") {
            db.collection("reviews")
                .whereEqualTo("salonId", salonId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(SalonReview::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
        }

    fun observeAllReviewsAdmin(): Flow<List<SalonReview>> =
        safeSnapshotFlow("reviews") {
            db.collection("reviews")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SalonReview::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
        }

    suspend fun submitReview(
        appointment: Appointment,
        rating: Int,
        comment: String
    ): Result<Unit> {
        val uid = requireUserId()
        val reviewId = "rev_${UUID.randomUUID().toString().take(12)}"
        val reviewRef = db.collection("reviews").document(reviewId)
        val apptRef = db.collection("appointments").document(appointment.id)
        val salonRef = db.collection("salons").document(appointment.salonId)

        return try {
            db.runTransaction { tx ->
                val salonSnap = tx.get(salonRef)
                val currentCount = (salonSnap.getLong("reviewCount") ?: 0L).toInt()
                val currentAvg = salonSnap.getDouble("averageRating") ?: 0.0
                val newCount = currentCount + 1
                val newAvg = ((currentAvg * currentCount) + rating) / newCount

                val reviewPayload = mapOf(
                    "salonId" to appointment.salonId,
                    "barberId" to appointment.barberId,
                    "appointmentId" to appointment.id,
                    "customerId" to uid,
                    "customerName" to appointment.customerName,
                    "rating" to rating.coerceIn(1, 5),
                    "comment" to comment.trim().takeIf { it.isNotBlank() },
                    "createdAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }.mapValues { it.value!! }

                tx.set(reviewRef, reviewPayload)
                tx.update(
                    apptRef,
                    mapOf(
                        "hasReview" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.update(
                    salonRef,
                    mapOf(
                        "averageRating" to newAvg,
                        "reviewCount" to newCount,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, reviewRef.path)
            Result.failure(e)
        }
    }

    // --- FAVOURITES ---

    fun observeFavourites(uid: String): Flow<List<FavouriteSalon>> =
        safeSnapshotFlow("users/$uid/favourites", uid) {
            db.collection("users").document(uid).collection("favourites")
                .snapshots()
                .map { snap ->
                    snap.toObjects(FavouriteSalon::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
        }

    suspend fun toggleFavourite(salon: SalonProfile, isCurrentlyFav: Boolean): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("favourites").document(salon.id)
        return try {
            if (isCurrentlyFav) {
                docRef.delete().await()
            } else {
                docRef.set(
                    mapOf(
                        "salonId" to salon.id,
                        "salonName" to salon.salonName,
                        "salonAddress" to salon.address,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // --- NOTIFICATIONS ---

    fun observeNotifications(uid: String): Flow<List<AppNotification>> =
        safeSnapshotFlow("notifications", uid) {
            db.collection("notifications")
                .whereEqualTo("recipientId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
        }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("notifications").document(notificationId)
        return try {
            docRef.update("isRead", true).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- APP SETTINGS ---

    fun observeAppSettings(): Flow<AppSettings> =
        safeSnapshotFlow("app_settings/global") {
            db.collection("app_settings").document("global")
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(AppSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?: AppSettings()
                    } else {
                        AppSettings()
                    }
                }
        }

    suspend fun saveAppSettingsAdmin(timezone: String, holdMinutes: Int): Result<Unit> {
        requireUserId()
        val docRef = db.collection("app_settings").document("global")
        return try {
            docRef.set(
                mapOf(
                    "defaultTimezone" to timezone,
                    "defaultHoldMinutes" to holdMinutes.coerceIn(1, 60),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }
}

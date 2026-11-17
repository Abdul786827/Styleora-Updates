package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class UserRole(val value: String, val label: String) {
    CUSTOMER("CUSTOMER", "Customer"),
    BARBER("BARBER", "Barber / Salon Owner")
}

enum class ServiceCategory(val value: String, val label: String) {
    MALE("MALE", "Male"),
    FEMALE("FEMALE", "Female"),
    BOTH("BOTH", "Unisex / Both")
}

enum class AppointmentStatus(val value: String, val label: String) {
    PENDING("PENDING", "Pending"),
    BOOKED("BOOKED", "Booked"),
    COMPLETED("COMPLETED", "Completed"),
    CANCELLED("CANCELLED", "Cancelled"),
    REJECTED("REJECTED", "Rejected")
}

enum class SlotStatus(val value: String, val label: String) {
    AVAILABLE("AVAILABLE", "Available"),
    SELECTED("SELECTED", "Selected"),
    BOOKED("BOOKED", "Booked"),
    CANCELLED("CANCELLED", "Cancelled")
}

enum class PaymentMethod(val value: String, val label: String) {
    PAY_AT_SALON("PAY_AT_SALON", "Pay at Salon"),
    CASH("CASH", "Cash Payment"),
    ONLINE("ONLINE", "Online Payment"),
    DEPOSIT("DEPOSIT", "Booking Deposit")
}

enum class PaymentStatus(val value: String, val label: String) {
    PENDING("PENDING", "Pending"),
    PAID("PAID", "Paid"),
    REFUNDED("REFUNDED", "Refunded")
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val mobileNumber: String = "",
    val userType: String = UserRole.CUSTOMER.value,
    val accountStatus: String = "ACTIVE",
    val profilePhotoUri: String? = null,
    val preferredTimezone: String = "Asia/Karachi",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "uid" to uid,
        "fullName" to fullName,
        "email" to email,
        "mobileNumber" to mobileNumber,
        "userType" to userType,
        "accountStatus" to accountStatus,
        "profilePhotoUri" to profilePhotoUri,
        "preferredTimezone" to preferredTimezone,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }
}

@IgnoreExtraProperties
data class FavouriteSalon(
    @DocumentId val id: String = "",
    val salonId: String = "",
    val salonName: String = "",
    val salonAddress: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SalonProfile(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val salonName: String = "",
    val ownerName: String = "",
    val logoUri: String? = null,
    val coverImageUri: String? = null,
    val contactNumber: String = "",
    val whatsappNumber: String? = null,
    val address: String = "",
    val mapsLocation: String? = null,
    val workingDays: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat"),
    val openingTimeMinutes: Int = 540,   // 09:00 AM
    val closingTimeMinutes: Int = 1320,  // 10:00 PM
    val breakStartMinutes: Int = 810,    // 01:30 PM
    val breakEndMinutes: Int = 870,      // 02:30 PM
    val description: String? = null,
    val acceptedPaymentMethods: List<String> = listOf(PaymentMethod.PAY_AT_SALON.value, PaymentMethod.CASH.value),
    val depositPercent: Int = 0,
    val allowCustomerCancellation: Boolean = true,
    val minCancellationHours: Int = 2,
    val cancellationRequiresApproval: Boolean = false,
    val cancellationPolicyText: String? = null,
    val holdTimeoutMinutes: Int = 10,
    val timezone: String = "Asia/Karachi",
    val averageRating: Double = 0.0,
    val reviewCount: Int = 0,
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SalonService(
    @DocumentId val id: String = "",
    val salonId: String = "",
    val barberId: String = "",
    val salonName: String = "",
    val name: String = "",
    val category: String = ServiceCategory.MALE.value,
    val description: String? = null,
    val imageUri: String? = null,
    val price: Double = 0.0,
    val discountPrice: Double? = null,
    val durationMinutes: Int = 30,
    val isAvailable: Boolean = true,
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val effectivePrice: Double
        get() = if (discountPrice != null && discountPrice > 0 && discountPrice <= price) discountPrice else price
}

@IgnoreExtraProperties
data class SlotLock(
    @DocumentId val id: String = "",
    val salonId: String = "",
    val barberId: String = "",
    val date: String = "",
    val startMinutes: Int = 0,
    val endMinutes: Int = 30,
    val status: String = SlotStatus.SELECTED.value,
    val holderUid: String = "",
    val appointmentId: String? = null,
    val expiresAtEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Appointment(
    @DocumentId val id: String = "",
    val salonId: String = "",
    val salonName: String = "",
    val barberId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerEmail: String = "",
    val customerGender: String = "MALE",
    val category: String = "MALE",
    val serviceIds: List<String> = emptyList(),
    val serviceNames: List<String> = emptyList(),
    val totalPrice: Double = 0.0,
    val totalDurationMinutes: Int = 30,
    val date: String = "",
    val startMinutes: Int = 540,
    val endMinutes: Int = 570,
    val specialInstructions: String? = null,
    val status: String = AppointmentStatus.BOOKED.value,
    val paymentMethod: String = PaymentMethod.PAY_AT_SALON.value,
    val paymentStatus: String = PaymentStatus.PENDING.value,
    val slotLockId: String? = null,
    val cancellationReason: String? = null,
    val cancelledByUid: String? = null,
    val cancelledByRole: String? = null,
    val cancelledAtEpochMs: Long? = null,
    val hasReview: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SalonReview(
    @DocumentId val id: String = "",
    val salonId: String = "",
    val barberId: String = "",
    val appointmentId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val rating: Int = 5,
    val comment: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "BOOKING_NEW",
    val appointmentId: String? = null,
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AppSettings(
    @DocumentId val id: String = "",
    val defaultTimezone: String = "Asia/Karachi",
    val defaultHoldMinutes: Int = 10,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class ServicePreset(
    val name: String,
    val category: String,
    val defaultPrice: Double,
    val defaultDurationMinutes: Int,
    val description: String
)

object ServicePresets {
    val maleServices = listOf(
        ServicePreset("Haircut", "MALE", 1500.0, 30, "Tailored scissor & clipper cut with precision finish."),
        ServicePreset("Beard Trimming", "MALE", 800.0, 20, "Clean neckline and sculpted beard shaping."),
        ServicePreset("Beard Styling", "MALE", 1200.0, 30, "Hot towel beard sculpt with conditioning balm."),
        ServicePreset("Hair Styling", "MALE", 1000.0, 25, "Blow-dry and signature matte or glossy styling."),
        ServicePreset("Hair Coloring", "MALE", 3500.0, 60, "Rich ammonia-free tone and grey blending."),
        ServicePreset("Hair Highlighting", "MALE", 4500.0, 75, "Custom dimensional streaks and toning."),
        ServicePreset("Hair Wash", "MALE", 600.0, 15, "Clarifying scalp wash and nourishing rinse."),
        ServicePreset("Hair Spa", "MALE", 2800.0, 45, "Deep hydration steam ritual for strong hair."),
        ServicePreset("Hair Straightening", "MALE", 5000.0, 90, "Sleek thermal and botanical smoothing."),
        ServicePreset("Hair Smoothening", "MALE", 5500.0, 90, "Frizz-free silky texture treatment."),
        ServicePreset("Hair Keratin Treatment", "MALE", 8500.0, 120, "Long-lasting keratin protein infusion."),
        ServicePreset("Facial", "MALE", 3000.0, 45, "Deep pore cleansing and revitalizing mask."),
        ServicePreset("Skin Care", "MALE", 2500.0, 40, "Exfoliation, extraction, and soothing hydration."),
        ServicePreset("Head Massage", "MALE", 1000.0, 20, "Warm aromatic oil scalp pressure massage."),
        ServicePreset("Hair Treatment", "MALE", 3200.0, 45, "Targeted follicle strengthening serum therapy."),
        ServicePreset("Dandruff Treatment", "MALE", 2600.0, 40, "Soothing anti-flake scalp detox therapy."),
        ServicePreset("Hair Shaving", "MALE", 900.0, 25, "Traditional straight-razor hot towel shave."),
        ServicePreset("Grooming Package", "MALE", 4500.0, 75, "Haircut, beard styling, hair wash & express facial."),
        ServicePreset("Bridal Grooming", "MALE", 12000.0, 150, "Complete groom luxury facial, cut, beard & styling."),
        ServicePreset("Custom Services", "MALE", 2000.0, 30, "Bespoke grooming session tailored to your request.")
    )

    val femaleServices = listOf(
        ServicePreset("Haircut", "FEMALE", 2500.0, 45, "Layered precision cut with signature blow-dry."),
        ServicePreset("Hair Styling", "FEMALE", 2200.0, 40, "Editorial waves, sleek press, or occasion updo."),
        ServicePreset("Hair Coloring", "FEMALE", 7500.0, 90, "Luminous full-coverage salon gloss color."),
        ServicePreset("Hair Highlights", "FEMALE", 9500.0, 120, "Balayage, foil highlights, and custom toner."),
        ServicePreset("Hair Straightening", "FEMALE", 9000.0, 120, "Pin-straight silk press and thermal seal."),
        ServicePreset("Hair Smoothening", "FEMALE", 11000.0, 120, "Smooth humidity-resistant shine treatment."),
        ServicePreset("Keratin Treatment", "FEMALE", 14000.0, 150, "Salon-grade keratin protein restoration."),
        ServicePreset("Hair Rebonding", "FEMALE", 15000.0, 180, "Permanent sleek structural hair rebonding."),
        ServicePreset("Hair Spa", "FEMALE", 3800.0, 50, "Botanical steam mask and restorative scalp massage."),
        ServicePreset("Hair Wash", "FEMALE", 1000.0, 20, "Luxury shampoo, conditioning & towel wrap."),
        ServicePreset("Facial", "FEMALE", 4500.0, 60, "Radiant glow facial with lymphatic massage."),
        ServicePreset("Makeup", "FEMALE", 6500.0, 60, "Soft glam daytime or evening artistry."),
        ServicePreset("Bridal Makeup", "FEMALE", 35000.0, 150, "Signature luxury bridal look with lashes & hair."),
        ServicePreset("Party Makeup", "FEMALE", 9000.0, 75, "Red-carpet evening makeup & hair styling."),
        ServicePreset("Eyebrow Threading", "FEMALE", 500.0, 15, "Clean brow mapping and precision threading."),
        ServicePreset("Waxing", "FEMALE", 2500.0, 45, "Gentle warm honey or strip wax session."),
        ServicePreset("Manicure", "FEMALE", 1800.0, 35, "Cuticle care, exfoliation, massage & polish."),
        ServicePreset("Pedicure", "FEMALE", 2400.0, 45, "Warm mineral soak, callus care & polish."),
        ServicePreset("Nail Art", "FEMALE", 3200.0, 60, "Custom chrome, French, or artisan nail design."),
        ServicePreset("Skin Care", "FEMALE", 4200.0, 50, "Hydrating glass-skin facial peel and serum."),
        ServicePreset("Hair Treatment", "FEMALE", 4800.0, 50, "Bond-repair reconstructive hair mask."),
        ServicePreset("Head Massage", "FEMALE", 1500.0, 30, "Relaxing warm oil scalp & neck therapy."),
        ServicePreset("Custom Services", "FEMALE", 3000.0, 45, "Custom beauty & salon treatment.")
    )

    val cancellationReasons = listOf(
        "Schedule conflict",
        "Found another time slot",
        "Personal emergency",
        "Travel / weather delay",
        "Change of plans"
    )
}

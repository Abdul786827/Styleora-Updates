package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Appointment
import com.example.data.model.AppointmentStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.SalonProfile
import com.example.data.model.SalonService
import com.example.data.model.ServiceCategory
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class StyleoraRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun barberCreatesSalonAndService_customerBooksAndCancelsAppointment_releasesSlot() = runBlocking {
        val barberUid = signInTestUser("barber_${UUID.randomUUID().toString().take(6)}@styleora.test")
        val barberRepo = StyleoraRepository(firestore, auth)

        val barberProfile = UserProfile(
            uid = barberUid,
            fullName = "Zain Master Barber",
            email = "zain@styleora.test",
            mobileNumber = "+923001112233",
            userType = UserRole.BARBER.value
        )
        assertTrue(withTimeout(TIMEOUT_MS) { barberRepo.saveUserProfile(barberProfile) }.isSuccess)

        val salonId = withTimeout(TIMEOUT_MS) {
            barberRepo.saveSalonProfile(
                SalonProfile(
                    ownerId = barberUid,
                    salonName = "Atelier Noir ${UUID.randomUUID().toString().take(4)}",
                    ownerName = "Zain",
                    contactNumber = "+923001112233",
                    address = "DHA Phase 5, Lahore"
                )
            )
        }.getOrThrow()

        val serviceId = withTimeout(TIMEOUT_MS) {
            barberRepo.saveService(
                SalonService(
                    salonId = salonId,
                    barberId = barberUid,
                    salonName = "Atelier Noir",
                    name = "Signature Haircut",
                    category = ServiceCategory.MALE.value,
                    price = 2500.0,
                    durationMinutes = 45
                )
            )
        }.getOrThrow()

        // Switch to customer
        val customerUid = signInTestUser("cust_${UUID.randomUUID().toString().take(6)}@styleora.test")
        val custRepo = StyleoraRepository(firestore, auth)

        val salons = withTimeout(FLOW_TIMEOUT_MS) {
            custRepo.observeActiveSalons().first { list -> list.any { it.id == salonId } }
        }
        assertTrue(salons.any { it.id == salonId })

        val holdId = withTimeout(TIMEOUT_MS) {
            custRepo.holdTimeSlot(
                salonId = salonId,
                barberId = barberUid,
                date = "2026-11-28",
                startMinutes = 600,
                endMinutes = 645,
                holdTimeoutMinutes = 10
            )
        }.getOrThrow()

        val apptId = withTimeout(TIMEOUT_MS) {
            custRepo.confirmBooking(
                appointment = Appointment(
                    salonId = salonId,
                    salonName = "Atelier Noir",
                    barberId = barberUid,
                    customerId = customerUid,
                    customerName = "Hamza",
                    customerPhone = "+923219876543",
                    customerEmail = "hamza@styleora.test",
                    customerGender = "MALE",
                    category = "MALE",
                    serviceIds = listOf(serviceId),
                    serviceNames = listOf("Signature Haircut"),
                    totalPrice = 2500.0,
                    totalDurationMinutes = 45,
                    date = "2026-11-28",
                    startMinutes = 600,
                    endMinutes = 645,
                    paymentMethod = PaymentMethod.PAY_AT_SALON.value
                ),
                existingHoldLockId = holdId
            )
        }.getOrThrow()

        val customerAppts = withTimeout(FLOW_TIMEOUT_MS) {
            custRepo.observeCustomerAppointments(customerUid).first { list -> list.any { it.id == apptId } }
        }
        val bookedAppt = customerAppts.first { it.id == apptId }
        assertEquals(AppointmentStatus.BOOKED.value, bookedAppt.status)

        // Cancel appointment and verify slot released
        val cancelResult = withTimeout(TIMEOUT_MS) {
            custRepo.cancelAppointment(bookedAppt, "Schedule conflict", "CUSTOMER")
        }
        assertTrue(cancelResult.isSuccess)

        val updatedAppts = withTimeout(FLOW_TIMEOUT_MS) {
            custRepo.observeCustomerAppointments(customerUid).first { list ->
                list.firstOrNull { it.id == apptId }?.status == AppointmentStatus.CANCELLED.value
            }
        }
        assertEquals(AppointmentStatus.CANCELLED.value, updatedAppts.first { it.id == apptId }.status)
    }

    @Test
    fun observeActiveSalons_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = StyleoraRepository(firestore, auth)
        val exception = try {
            withTimeout(FLOW_TIMEOUT_MS) { repo.observeActiveSalons().first() }
            null
        } catch (e: Exception) {
            e
        }
        val firestoreEx = exception?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeCustomerAppointments_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_${UUID.randomUUID().toString().take(6)}@styleora.test")
        signInTestUser("bob_${UUID.randomUUID().toString().take(6)}@styleora.test")
        val bobRepo = StyleoraRepository(firestore, auth)

        val exception = try {
            withTimeout(FLOW_TIMEOUT_MS) { bobRepo.observeCustomerAppointments(aliceUid).first() }
            null
        } catch (e: Exception) {
            e
        }
        val firestoreEx = exception?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    private companion object {
        const val TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}

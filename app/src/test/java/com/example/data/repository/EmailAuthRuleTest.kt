package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.UserRole
import com.example.ui.auth.EmailAuthHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class EmailAuthRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun validateInputs_rejectsInvalidFields_andAcceptsValidFields() {
        assertNotNull(EmailAuthHelper.validateSignUpInputs("A", "bad", "123", "123", "456", false))
        assertNotNull(EmailAuthHelper.validateSignUpInputs("Ali Khan", "bad-email", "+923001112233", "Secret12", "Secret12", true))
        assertNotNull(EmailAuthHelper.validateSignUpInputs("Ali Khan", "ali@styleora.test", "+923001112233", "Secret12", "Mismatch1", true))
        assertNotNull(EmailAuthHelper.validateSignUpInputs("Ali Khan", "ali@styleora.test", "+923001112233", "Secret12", "Secret12", false))
        assertNull(EmailAuthHelper.validateSignUpInputs("Ali Khan", "ali@styleora.test", "+923001112233", "Secret12", "Secret12", true))

        assertNotNull(EmailAuthHelper.validateLoginInputs("bad", "Secret12"))
        assertNotNull(EmailAuthHelper.validateLoginInputs("ali@styleora.test", ""))
        assertNull(EmailAuthHelper.validateLoginInputs("ali@styleora.test", "Secret12"))
    }

    @Test
    fun emailSignUp_customerAndBarber_savesFirestoreProfilesWithUid_andBlocksDuplicateEmail() = runBlocking {
        val repo = StyleoraRepository(firestore, auth)
        val suffix = UUID.randomUUID().toString().take(8)
        val barberEmail = "barber_$suffix@styleora.test"
        val customerEmail = "cust_$suffix@styleora.test"

        // 1. Barber sign up with email
        val barberSignUp = withTimeout(TIMEOUT_MS) {
            EmailAuthHelper.signUpWithEmail(
                auth = auth,
                repository = repo,
                fullName = "Usman Barber",
                email = barberEmail,
                mobileNumber = "+923001112233",
                password = "Password123!",
                userType = UserRole.BARBER.value
            )
        }
        assertTrue(barberSignUp.isSuccess)
        val barberOutcome = barberSignUp.getOrThrow()
        assertTrue(barberOutcome.profileSaved)
        assertTrue(barberOutcome.verificationEmailSent)

        // New email account starts unverified until verification link is clicked
        val verifiedInitially = withTimeout(TIMEOUT_MS) {
            EmailAuthHelper.reloadAndCheckVerified(auth).getOrThrow()
        }
        assertFalse(verifiedInitially)

        // Profile persisted in Firestore under exact Firebase UID
        val savedBarber = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserProfile(barberOutcome.uid).first { it != null }
        }
        assertEquals(UserRole.BARBER.value, savedBarber?.userType)
        assertEquals("Usman Barber", savedBarber?.fullName)
        assertEquals(barberEmail, savedBarber?.email)

        // Resend verification succeeds
        val resendRes = withTimeout(TIMEOUT_MS) { EmailAuthHelper.resendVerificationEmail(auth) }
        assertTrue(resendRes.isSuccess)

        // Logout
        auth.signOut()
        assertNull(auth.currentUser)

        // 2. Duplicate email registration must fail
        val dupResult = withTimeout(TIMEOUT_MS) {
            EmailAuthHelper.signUpWithEmail(
                auth = auth,
                repository = repo,
                fullName = "Duplicate User",
                email = barberEmail,
                mobileNumber = "+923009998877",
                password = "Password123!",
                userType = UserRole.CUSTOMER.value
            )
        }
        assertTrue(dupResult.isFailure)
        assertTrue(dupResult.exceptionOrNull()?.message?.contains("already exists") == true)

        // 3. Customer sign up with email
        val custSignUp = withTimeout(TIMEOUT_MS) {
            EmailAuthHelper.signUpWithEmail(
                auth = auth,
                repository = repo,
                fullName = "Ayesha Noor",
                email = customerEmail,
                mobileNumber = "+923215556677",
                password = "Password123!",
                userType = UserRole.CUSTOMER.value
            )
        }
        assertTrue(custSignUp.isSuccess)
        val custOutcome = custSignUp.getOrThrow()
        val savedCustomer = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserProfile(custOutcome.uid).first { it != null }
        }
        assertEquals(UserRole.CUSTOMER.value, savedCustomer?.userType)
        assertEquals("Ayesha Noor", savedCustomer?.fullName)
    }

    @Test
    fun emailLogin_wrongPasswordFails_validLoginSucceeds_andPasswordResetSucceeds() = runBlocking {
        val repo = StyleoraRepository(firestore, auth)
        val suffix = UUID.randomUUID().toString().take(8)
        val email = "login_$suffix@styleora.test"
        val password = "Password123!"

        val signUp = withTimeout(TIMEOUT_MS) {
            EmailAuthHelper.signUpWithEmail(
                auth = auth,
                repository = repo,
                fullName = "Sara Ali",
                email = email,
                mobileNumber = "+923331112233",
                password = password,
                userType = UserRole.CUSTOMER.value
            )
        }
        assertTrue(signUp.isSuccess)
        auth.signOut()

        // Wrong password fails cleanly
        val badLogin = withTimeout(TIMEOUT_MS) {
            EmailAuthHelper.signInWithEmail(auth, email, "WrongPass999")
        }
        assertTrue(badLogin.isFailure)

        // Valid login succeeds and reports unverified status (blocking dashboard access)
        val goodLogin = withTimeout(TIMEOUT_MS) {
            EmailAuthHelper.signInWithEmail(auth, email, password)
        }
        assertTrue(goodLogin.isSuccess)
        assertFalse(goodLogin.getOrThrow())
        assertEquals(email, auth.currentUser?.email)

        // Forgot password reset link call succeeds against Auth emulator
        val resetRes = withTimeout(TIMEOUT_MS) {
            EmailAuthHelper.sendPasswordReset(auth, email)
        }
        assertTrue(resetRes.isSuccess)
    }

    private companion object {
        const val TIMEOUT_MS = 8000L
        const val FLOW_TIMEOUT_MS = 5000L
    }
}

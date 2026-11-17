package com.example.ui.auth

import android.util.Log
import com.example.data.model.UserProfile
import com.example.data.repository.StyleoraRepository
import com.example.util.DateTimeUtils
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val EMAIL_AUTH_TAG = "StyleoraEmailAuth"

data class EmailSignUpOutcome(
    val uid: String,
    val email: String,
    val verificationEmailSent: Boolean,
    val profileSaved: Boolean,
    val warningMessage: String? = null
)

object EmailAuthHelper {

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

    fun validateSignUpInputs(
        fullName: String,
        email: String,
        mobileNumber: String,
        password: String,
        confirmPassword: String,
        agreedToTerms: Boolean
    ): String? {
        return when {
            fullName.trim().length < 2 -> "Please enter your full name (at least 2 characters)."
            !isValidEmail(email) -> "Please enter a valid email address."
            mobileNumber.trim().length < 7 -> "Please enter a valid mobile number."
            password.length < 6 -> "Password must be at least 6 characters long."
            password != confirmPassword -> "Passwords do not match."
            !agreedToTerms -> "Please agree to the Terms & Conditions to continue."
            else -> null
        }
    }

    fun validateLoginInputs(email: String, password: String): String? {
        return when {
            !isValidEmail(email) -> "Please enter a valid email address."
            password.isBlank() -> "Please enter your password."
            else -> null
        }
    }

    suspend fun signUpWithEmail(
        auth: FirebaseAuth,
        repository: StyleoraRepository,
        fullName: String,
        email: String,
        mobileNumber: String,
        password: String,
        userType: String,
        profilePhotoUri: String? = null,
        timezone: String = DateTimeUtils.DEFAULT_TIMEZONE
    ): Result<EmailSignUpOutcome> = withContext(NonCancellable) {
        val cleanName = fullName.trim()
        val cleanEmail = email.trim()
        val cleanPhone = mobileNumber.trim()
        try {
            val authResult = auth.createUserWithEmailAndPassword(cleanEmail, password).await()
            val user = authResult.user
                ?: return@withContext Result.failure(IllegalStateException("Account creation failed. Please try again."))

            try {
                user.updateProfile(
                    userProfileChangeRequest { displayName = cleanName }
                ).await()
            } catch (e: Exception) {
                Log.w(EMAIL_AUTH_TAG, "Display name update failed (non-fatal): ${e.message}")
            }

            val profileResult = repository.saveUserProfile(
                UserProfile(
                    uid = user.uid,
                    fullName = cleanName,
                    email = cleanEmail,
                    mobileNumber = cleanPhone,
                    userType = userType,
                    profilePhotoUri = profilePhotoUri,
                    preferredTimezone = timezone
                )
            )
            val profileSaved = profileResult.isSuccess
            if (!profileSaved) {
                Log.w(EMAIL_AUTH_TAG, "Profile Firestore save failed on signup", profileResult.exceptionOrNull())
            }

            var verificationSent = false
            var warning: String? = null
            try {
                user.sendEmailVerification().await()
                verificationSent = true
            } catch (e: Exception) {
                Log.w(EMAIL_AUTH_TAG, "Initial sendEmailVerification failed: ${e.message}")
                warning = "Account created, but the verification email could not be sent right now (${mapAuthError(e)}). Tap Resend Verification Email."
            }

            if (!profileSaved && warning == null) {
                warning = "Account created. Your profile details will finish syncing once verified."
            }

            Result.success(
                EmailSignUpOutcome(
                    uid = user.uid,
                    email = cleanEmail,
                    verificationEmailSent = verificationSent,
                    profileSaved = profileSaved,
                    warningMessage = warning
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(EMAIL_AUTH_TAG, "Email sign up failed", e)
            Result.failure(Exception(mapAuthError(e), e))
        }
    }

    suspend fun signInWithEmail(
        auth: FirebaseAuth,
        email: String,
        password: String
    ): Result<Boolean> {
        return try {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = result.user
                ?: return Result.failure(IllegalStateException("Sign in failed. Please try again."))
            try {
                user.reload().await()
            } catch (e: Exception) {
                Log.w(EMAIL_AUTH_TAG, "User reload after sign-in failed: ${e.message}")
            }
            Result.success(auth.currentUser?.isEmailVerified == true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(EMAIL_AUTH_TAG, "Email sign in failed", e)
            Result.failure(Exception(mapAuthError(e), e))
        }
    }

    suspend fun sendPasswordReset(
        auth: FirebaseAuth,
        email: String
    ): Result<Unit> {
        val cleanEmail = email.trim()
        if (!isValidEmail(cleanEmail)) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        return try {
            auth.sendPasswordResetEmail(cleanEmail).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(EMAIL_AUTH_TAG, "Password reset failed", e)
            Result.failure(Exception(mapAuthError(e), e))
        }
    }

    suspend fun resendVerificationEmail(auth: FirebaseAuth): Result<Unit> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("No signed-in user found."))
        return try {
            user.sendEmailVerification().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(EMAIL_AUTH_TAG, "Resend verification email failed", e)
            Result.failure(Exception(mapAuthError(e), e))
        }
    }

    suspend fun reloadAndCheckVerified(auth: FirebaseAuth): Result<Boolean> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("No signed-in user found."))
        return try {
            user.reload().await()
            Result.success(auth.currentUser?.isEmailVerified == true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(EMAIL_AUTH_TAG, "Reload verification check failed", e)
            Result.failure(Exception(mapAuthError(e), e))
        }
    }

    fun mapAuthError(error: Throwable): String {
        return when (error) {
            is FirebaseAuthWeakPasswordException ->
                "Password is too weak. Use at least 6 characters with a mix of letters and numbers."
            is FirebaseAuthUserCollisionException ->
                "An account with this email already exists. Please log in or use Continue with Google."
            is FirebaseAuthInvalidUserException ->
                "No account found with this email address."
            is FirebaseAuthInvalidCredentialsException -> {
                when (error.errorCode) {
                    "ERROR_INVALID_EMAIL" -> "Please enter a valid email address."
                    else -> "Incorrect email or password. Please check your credentials."
                }
            }
            is FirebaseTooManyRequestsException ->
                "Too many attempts right now. Please wait a moment before trying again."
            is FirebaseNetworkException ->
                "Network error. Please check your internet connection and try again."
            is FirebaseAuthException -> {
                if (error.errorCode == "ERROR_OPERATION_NOT_ALLOWED" ||
                    error.message?.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) == true
                ) {
                    "Email/Password authentication is not enabled in the Firebase Console yet."
                } else {
                    error.localizedMessage ?: "Authentication error (${error.errorCode})."
                }
            }
            else -> error.localizedMessage ?: "Authentication failed. Please try again."
        }
    }
}

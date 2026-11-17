package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.UserRole
import com.example.data.repository.StyleoraRepository
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.components.StyleoraCrest
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneGoldLight
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.WarmWhite
import com.example.util.DateTimeUtils

private enum class AuthMode {
    CHOICE,
    EMAIL_SIGNUP,
    EMAIL_LOGIN
}

@Composable
fun AuthScreen(
    repository: StyleoraRepository,
    rememberedEmail: String,
    rememberMeEnabled: Boolean,
    onRememberMePersist: suspend (rememberMe: Boolean, email: String) -> Unit,
    onAuthSuccess: () -> Unit,
    onEmailSignUpNotice: (String?) -> Unit,
    onEmailLoginSuccess: (isEmailVerified: Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var authMode by remember { mutableStateOf(AuthMode.CHOICE) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = authMode != AuthMode.CHOICE) {
        authMode = AuthMode.CHOICE
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MatteBlack)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_styleora_still_life),
            contentDescription = "Luxury Salon Atmosphere",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.34f
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MatteBlack.copy(alpha = 0.55f),
                            MatteBlack.copy(alpha = 0.85f),
                            MatteBlack
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier.widthIn(max = 460.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StyleoraCrest(size = if (authMode == AuthMode.CHOICE) 92.dp else 64.dp)
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "STYLEORA",
                    style = if (authMode == AuthMode.CHOICE) MaterialTheme.typography.displayLarge else MaterialTheme.typography.headlineLarge,
                    color = ChampagneGoldLight,
                    letterSpacing = androidx.compose.ui.unit.TextUnit(3f, androidx.compose.ui.unit.TextUnitType.Sp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Your Style, Your Time.",
                    style = MaterialTheme.typography.titleMedium,
                    color = WarmWhite.copy(alpha = 0.9f)
                )
                if (authMode == AuthMode.CHOICE) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Reserve luxury haircuts, beard artistry, bridal makeup, and curated grooming sessions with real-time availability.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmWhite.copy(alpha = 0.72f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            when (authMode) {
                AuthMode.CHOICE -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 460.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1C18).copy(alpha = 0.95f)),
                        border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Get Started",
                                style = MaterialTheme.typography.headlineMedium,
                                color = WarmWhite
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Choose Google or Email authentication for Barber and Customer access.",
                                style = MaterialTheme.typography.bodySmall,
                                color = WarmWhite.copy(alpha = 0.75f),
                                textAlign = TextAlign.Center
                            )
                            if (!errorMessage.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                AuthErrorBanner(errorMessage!!)
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    errorMessage = null
                                    isGoogleLoading = true
                                    onGoogleSignInClicked(
                                        context = context,
                                        credentialManager = credentialManager,
                                        onAuthSuccess = {
                                            isGoogleLoading = false
                                            onAuthSuccess()
                                        },
                                        onAuthError = { msg ->
                                            isGoogleLoading = false
                                            errorMessage = msg
                                        },
                                        scope = scope,
                                        onAuthCancelled = { isGoogleLoading = false }
                                    )
                                },
                                enabled = !isGoogleLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ChampagneGold,
                                    contentColor = MatteBlack
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("google_sign_in_button")
                            ) {
                                if (isGoogleLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = MatteBlack,
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Icon(Icons.Filled.AccountCircle, contentDescription = null)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Continue with Google",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HorizontalDivider(modifier = Modifier.weight(1f), color = WarmWhite.copy(alpha = 0.2f))
                                Text(
                                    text = "OR",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WarmWhite.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                HorizontalDivider(modifier = Modifier.weight(1f), color = WarmWhite.copy(alpha = 0.2f))
                            }
                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedButton(
                                onClick = {
                                    errorMessage = null
                                    authMode = AuthMode.EMAIL_SIGNUP
                                },
                                enabled = !isGoogleLoading,
                                border = BorderStroke(1.2.dp, ChampagneGold),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("email_signup_nav_button")
                            ) {
                                Icon(Icons.Filled.Email, contentDescription = null, tint = ChampagneGold)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Sign Up with Email",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = ChampagneGoldLight
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Already have an account?",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WarmWhite.copy(alpha = 0.78f)
                                )
                                TextButton(
                                    onClick = {
                                        errorMessage = null
                                        authMode = AuthMode.EMAIL_LOGIN
                                    },
                                    enabled = !isGoogleLoading,
                                    modifier = Modifier.testTag("email_login_nav_button")
                                ) {
                                    Text(
                                        text = "Login",
                                        color = ChampagneGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                AuthMode.EMAIL_SIGNUP -> {
                    EmailSignUpCard(
                        repository = repository,
                        onSignUpComplete = { notice ->
                            onEmailSignUpNotice(notice)
                        },
                        onSwitchToLogin = { authMode = AuthMode.EMAIL_LOGIN },
                        onBack = { authMode = AuthMode.CHOICE }
                    )
                }

                AuthMode.EMAIL_LOGIN -> {
                    EmailLoginCard(
                        initialEmail = rememberedEmail,
                        initialRememberMe = rememberMeEnabled,
                        onRememberMePersist = onRememberMePersist,
                        onLoginSuccess = onEmailLoginSuccess,
                        onSwitchToSignUp = { authMode = AuthMode.EMAIL_SIGNUP },
                        onBack = { authMode = AuthMode.CHOICE }
                    )
                }
            }
        }
    }
}

@Composable
fun RoleOnboardingScreen(
    defaultName: String,
    defaultEmail: String,
    defaultPhotoUrl: String? = null,
    isBusy: Boolean,
    onSubmitProfile: (
        fullName: String,
        email: String,
        mobileNumber: String,
        userType: String,
        profilePhotoUri: String?,
        timezone: String
    ) -> Unit,
    onSignOutClick: () -> Unit
) {
    var fullName by remember { mutableStateOf(defaultName) }
    var email by remember { mutableStateOf(defaultEmail) }
    var mobileNumber by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.CUSTOMER.value) }
    var photoUri by remember { mutableStateOf(defaultPhotoUrl) }
    var agreedToTerms by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) photoUri = uri.toString()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = 540.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StyleoraCrest(size = 42.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("STYLEORA", style = MaterialTheme.typography.headlineMedium)
                }
                OutlinedButton(onClick = onSignOutClick) {
                    Text("Sign Out")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Complete Your Profile", style = MaterialTheme.typography.displayMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Choose how you would like to use STYLEORA. You can book salons as a Customer or manage your business as a Barber / Salon Owner.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(2.dp, ChampagneGold, CircleShape)
                    .clickable {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    .testTag("onboarding_photo_picker"),
                contentAlignment = Alignment.Center
            ) {
                if (photoUri != null) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "Selected profile picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = "Upload profile photo",
                        tint = ChampagneGold,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tap to select profile photo",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(20.dp))
            Text("Account Role", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RoleCard(
                    title = "Customer",
                    subtitle = "Browse & book slots",
                    icon = Icons.Filled.Person,
                    selected = selectedRole == UserRole.CUSTOMER.value,
                    onClick = { selectedRole = UserRole.CUSTOMER.value },
                    modifier = Modifier.weight(1f).testTag("role_customer_card")
                )
                RoleCard(
                    title = "Barber / Owner",
                    subtitle = "Manage salon & slots",
                    icon = Icons.Filled.ContentCut,
                    selected = selectedRole == UserRole.BARBER.value,
                    onClick = { selectedRole = UserRole.BARBER.value },
                    modifier = Modifier.weight(1f).testTag("role_barber_card")
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("onboarding_name_input")
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { if (defaultEmail.isBlank()) email = it },
                readOnly = defaultEmail.isNotBlank(),
                label = { Text("Email Address *") },
                supportingText = if (defaultEmail.isNotBlank()) {
                    {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                tint = ChampagneGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Verified Account Email", color = ChampagneGold)
                        }
                    }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("onboarding_email_input")
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = mobileNumber,
                onValueChange = { mobileNumber = it },
                label = { Text("Mobile Number *") },
                placeholder = { Text("+92 300 1234567") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("onboarding_phone_input")
            )

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { agreedToTerms = !agreedToTerms },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = agreedToTerms,
                    onCheckedChange = { agreedToTerms = it },
                    modifier = Modifier.testTag("agree_terms_checkbox")
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "I agree to the STYLEORA Terms & Conditions",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { showTermsDialog = true }) {
                    Text("Read", color = ChampagneGold)
                }
            }

            if (!validationError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = validationError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    when {
                        fullName.trim().length < 2 -> validationError = "Please enter your full name."
                        email.trim().length < 3 || !email.contains("@") -> validationError = "Please enter a valid email address."
                        mobileNumber.trim().length < 7 -> validationError = "Please enter a valid mobile number."
                        !agreedToTerms -> validationError = "Please agree to the Terms & Conditions to continue."
                        else -> {
                            validationError = null
                            onSubmitProfile(
                                fullName.trim(),
                                email.trim(),
                                mobileNumber.trim(),
                                selectedRole,
                                photoUri,
                                DateTimeUtils.DEFAULT_TIMEZONE
                            )
                        }
                    }
                },
                enabled = !isBusy,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("complete_registration_button")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text("Continue to STYLEORA", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    if (showTermsDialog) {
        StyleoraTermsDialog(
            onAgree = {
                agreedToTerms = true
                showTermsDialog = false
            },
            onDismiss = { showTermsDialog = false }
        )
    }
}

@Composable
private fun RoleCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) ChampagneGold else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) ChampagneGold else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

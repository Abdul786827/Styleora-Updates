package com.example.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.UserRole
import com.example.data.repository.StyleoraRepository
import com.example.ui.auth.EmailAuthHelper
import com.example.ui.components.StyleoraCrest
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneGoldLight
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.WarmWhite
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EmailSignUpCard(
    repository: StyleoraRepository,
    onSignUpComplete: (warningOrNotice: String?) -> Unit,
    onSwitchToLogin: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(UserRole.CUSTOMER.value) }
    var photoUri by remember { mutableStateOf<String?>(null) }
    var agreedToTerms by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) photoUri = uri.toString()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 480.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1C18).copy(alpha = 0.96f)),
        border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    enabled = !isLoading,
                    modifier = Modifier.testTag("email_signup_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WarmWhite)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Sign Up with Email",
                        style = MaterialTheme.typography.headlineSmall,
                        color = WarmWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Create your verified STYLEORA account",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmWhite.copy(alpha = 0.72f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Optional avatar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2C2722))
                        .border(1.5.dp, ChampagneGold, CircleShape)
                        .clickable(enabled = !isLoading) {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                        .testTag("email_signup_photo_picker"),
                    contentAlignment = Alignment.Center
                ) {
                    if (photoUri != null) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "Profile photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.PhotoCamera,
                            contentDescription = "Add profile photo",
                            tint = ChampagneGold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Account Type", style = MaterialTheme.typography.labelMedium, color = ChampagneGoldLight)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RoleMiniChip(
                            label = "Customer",
                            icon = Icons.Filled.Person,
                            selected = selectedRole == UserRole.CUSTOMER.value,
                            onClick = { selectedRole = UserRole.CUSTOMER.value },
                            modifier = Modifier.weight(1f).testTag("signup_role_customer")
                        )
                        RoleMiniChip(
                            label = "Barber",
                            icon = Icons.Filled.ContentCut,
                            selected = selectedRole == UserRole.BARBER.value,
                            onClick = { selectedRole = UserRole.BARBER.value },
                            modifier = Modifier.weight(1f).testTag("signup_role_barber")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            val fieldColors = darkAuthFieldColors()

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name *") },
                singleLine = true,
                enabled = !isLoading,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth().testTag("signup_name_input")
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address *") },
                singleLine = true,
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth().testTag("signup_email_input")
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = mobileNumber,
                onValueChange = { mobileNumber = it },
                label = { Text("Mobile Number *") },
                placeholder = { Text("+92 300 1234567") },
                singleLine = true,
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth().testTag("signup_phone_input")
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password *") },
                singleLine = true,
                enabled = !isLoading,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = WarmWhite.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth().testTag("signup_password_input")
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password *") },
                singleLine = true,
                enabled = !isLoading,
                visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { confirmVisible = !confirmVisible }) {
                        Icon(
                            imageVector = if (confirmVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (confirmVisible) "Hide confirm password" else "Show confirm password",
                            tint = WarmWhite.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth().testTag("signup_confirm_password_input")
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isLoading) { agreedToTerms = !agreedToTerms },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = agreedToTerms,
                    onCheckedChange = { agreedToTerms = it },
                    enabled = !isLoading,
                    colors = CheckboxDefaults.colors(checkedColor = ChampagneGold, checkmarkColor = MatteBlack, uncheckedColor = WarmWhite.copy(alpha = 0.7f)),
                    modifier = Modifier.testTag("signup_terms_checkbox")
                )
                Text(
                    text = "I agree to Terms & Conditions",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmWhite.copy(alpha = 0.9f),
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { showTermsDialog = true }) {
                    Text("Read", color = ChampagneGold)
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                AuthErrorBanner(errorMessage!!)
            }

            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = {
                    val validation = EmailAuthHelper.validateSignUpInputs(
                        fullName = fullName,
                        email = email,
                        mobileNumber = mobileNumber,
                        password = password,
                        confirmPassword = confirmPassword,
                        agreedToTerms = agreedToTerms
                    )
                    if (validation != null) {
                        errorMessage = validation
                        return@Button
                    }
                    errorMessage = null
                    isLoading = true
                    scope.launch {
                        val result = EmailAuthHelper.signUpWithEmail(
                            auth = Firebase.auth,
                            repository = repository,
                            fullName = fullName,
                            email = email,
                            mobileNumber = mobileNumber,
                            password = password,
                            userType = selectedRole,
                            profilePhotoUri = photoUri
                        )
                        isLoading = false
                        result.fold(
                            onSuccess = { outcome ->
                                onSignUpComplete(
                                    outcome.warningMessage
                                        ?: "Verification link sent to ${outcome.email}."
                                )
                            },
                            onFailure = { err ->
                                errorMessage = err.message ?: "Registration failed."
                            }
                        )
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = MatteBlack),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("email_signup_submit_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MatteBlack, strokeWidth = 2.5.dp)
                } else {
                    Text("Create Account", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Already have an account?", style = MaterialTheme.typography.bodySmall, color = WarmWhite.copy(alpha = 0.75f))
                TextButton(onClick = onSwitchToLogin, enabled = !isLoading) {
                    Text("Login", color = ChampagneGold, fontWeight = FontWeight.Bold)
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
fun EmailLoginCard(
    initialEmail: String,
    initialRememberMe: Boolean,
    onRememberMePersist: suspend (rememberMe: Boolean, email: String) -> Unit,
    onLoginSuccess: (isEmailVerified: Boolean) -> Unit,
    onSwitchToSignUp: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var email by remember(initialEmail) { mutableStateOf(initialEmail) }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember(initialRememberMe) { mutableStateOf(initialRememberMe) }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var showForgotDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 480.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1C18).copy(alpha = 0.96f)),
        border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    enabled = !isLoading,
                    modifier = Modifier.testTag("email_login_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WarmWhite)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Login with Email",
                        style = MaterialTheme.typography.headlineSmall,
                        color = WarmWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sign in to your Barber or Customer account",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmWhite.copy(alpha = 0.72f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            val fieldColors = darkAuthFieldColors()

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address *") },
                singleLine = true,
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth().testTag("login_email_input")
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password *") },
                singleLine = true,
                enabled = !isLoading,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = WarmWhite.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth().testTag("login_password_input")
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.clickable(enabled = !isLoading) { rememberMe = !rememberMe },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        enabled = !isLoading,
                        colors = CheckboxDefaults.colors(checkedColor = ChampagneGold, checkmarkColor = MatteBlack, uncheckedColor = WarmWhite.copy(alpha = 0.7f)),
                        modifier = Modifier.testTag("remember_me_checkbox")
                    )
                    Text("Remember Me", style = MaterialTheme.typography.bodySmall, color = WarmWhite.copy(alpha = 0.9f))
                }
                TextButton(
                    onClick = { showForgotDialog = true },
                    enabled = !isLoading,
                    modifier = Modifier.testTag("forgot_password_button")
                ) {
                    Text("Forgot Password?", color = ChampagneGold)
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                AuthErrorBanner(errorMessage!!)
            }
            if (!infoMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                AuthInfoBanner(infoMessage!!)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val validation = EmailAuthHelper.validateLoginInputs(email, password)
                    if (validation != null) {
                        errorMessage = validation
                        infoMessage = null
                        return@Button
                    }
                    errorMessage = null
                    infoMessage = null
                    isLoading = true
                    scope.launch {
                        val result = EmailAuthHelper.signInWithEmail(Firebase.auth, email, password)
                        if (result.isSuccess) {
                            onRememberMePersist(rememberMe, email)
                        }
                        isLoading = false
                        result.fold(
                            onSuccess = { verified -> onLoginSuccess(verified) },
                            onFailure = { err -> errorMessage = err.message ?: "Login failed." }
                        )
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = MatteBlack),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("email_login_submit_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MatteBlack, strokeWidth = 2.5.dp)
                } else {
                    Text("Login", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("New to STYLEORA?", style = MaterialTheme.typography.bodySmall, color = WarmWhite.copy(alpha = 0.75f))
                TextButton(onClick = onSwitchToSignUp, enabled = !isLoading) {
                    Text("Sign Up with Email", color = ChampagneGold, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showForgotDialog) {
        ForgotPasswordDialog(
            initialEmail = email,
            onSent = { sentEmail ->
                showForgotDialog = false
                errorMessage = null
                infoMessage = "Password reset link sent to $sentEmail. Please check your inbox and spam folder."
            },
            onDismiss = { showForgotDialog = false }
        )
    }
}

@Composable
fun EmailVerificationScreen(
    userEmail: String,
    initialNotice: String?,
    onVerified: () -> Unit,
    onSignOut: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isChecking by remember { mutableStateOf(false) }
    var isResending by remember { mutableStateOf(false) }
    var cooldownSeconds by remember { mutableIntStateOf(0) }
    var statusMessage by remember(initialNotice) { mutableStateOf(initialNotice) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(cooldownSeconds) {
        if (cooldownSeconds > 0) {
            delay(1000L)
            cooldownSeconds -= 1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MatteBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1C18)),
            border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StyleoraCrest(size = 68.dp)
                Spacer(modifier = Modifier.height(16.dp))
                Icon(
                    imageVector = Icons.Filled.MarkEmailRead,
                    contentDescription = "Email verification required",
                    tint = ChampagneGold,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Verify Your Email Address",
                    style = MaterialTheme.typography.headlineMedium,
                    color = WarmWhite,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your account requires email verification before accessing Customer or Barber dashboards. Please click the verification link sent to:",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmWhite.copy(alpha = 0.78f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = ChampagneGold.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Email, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = userEmail.ifBlank { "your registered email" },
                            color = ChampagneGoldLight,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (!statusMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    AuthInfoBanner(statusMessage!!)
                }
                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    AuthErrorBanner(errorMessage!!)
                }

                Spacer(modifier = Modifier.height(22.dp))
                Button(
                    onClick = {
                        errorMessage = null
                        statusMessage = null
                        isChecking = true
                        scope.launch {
                            val result = EmailAuthHelper.reloadAndCheckVerified(Firebase.auth)
                            isChecking = false
                            result.fold(
                                onSuccess = { verified ->
                                    if (verified) {
                                        onVerified()
                                    } else {
                                        errorMessage = "Email is not verified yet. Please click the verification link in your inbox (check spam if needed), then tap again."
                                    }
                                },
                                onFailure = { err ->
                                    errorMessage = err.message ?: "Unable to verify status right now."
                                }
                            )
                        }
                    },
                    enabled = !isChecking && !isResending,
                    colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = MatteBlack),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("check_verification_button")
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MatteBlack, strokeWidth = 2.5.dp)
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("I've Verified — Continue", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        errorMessage = null
                        statusMessage = null
                        isResending = true
                        scope.launch {
                            val result = EmailAuthHelper.resendVerificationEmail(Firebase.auth)
                            isResending = false
                            result.fold(
                                onSuccess = {
                                    cooldownSeconds = 30
                                    statusMessage = "Verification email resent to $userEmail."
                                },
                                onFailure = { err ->
                                    errorMessage = err.message ?: "Could not resend verification email."
                                }
                            )
                        }
                    },
                    enabled = !isChecking && !isResending && cooldownSeconds == 0,
                    border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.65f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("resend_verification_button")
                ) {
                    if (isResending) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ChampagneGold, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = if (cooldownSeconds > 0) "Resend Email (${cooldownSeconds}s)" else "Resend Verification Email",
                            color = ChampagneGoldLight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = onSignOut,
                    modifier = Modifier.testTag("verification_logout_button")
                ) {
                    Text("Logout / Use Another Account", color = WarmWhite.copy(alpha = 0.78f))
                }
            }
        }
    }
}

@Composable
private fun ForgotPasswordDialog(
    initialEmail: String,
    onSent: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var resetEmail by remember { mutableStateOf(initialEmail) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Reset Password") },
        text = {
            Column {
                Text(
                    text = "Enter your registered email address and we will send you a password reset link.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = resetEmail,
                    onValueChange = { resetEmail = it },
                    label = { Text("Email Address") },
                    singleLine = true,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth().testTag("forgot_password_email_input")
                )
                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    errorMessage = null
                    isLoading = true
                    scope.launch {
                        val result = EmailAuthHelper.sendPasswordReset(Firebase.auth, resetEmail)
                        isLoading = false
                        result.fold(
                            onSuccess = { onSent(resetEmail.trim()) },
                            onFailure = { err -> errorMessage = err.message ?: "Failed to send reset link." }
                        )
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.testTag("forgot_password_submit_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Send Reset Link")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun StyleoraTermsDialog(
    onAgree: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Terms & Conditions") },
        text = {
            Text(
                text = "By creating a STYLEORA Barber or Customer account, you agree to provide accurate contact details, verify your email address, respect salon appointment and cancellation policies, and protect the privacy of appointment records.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onAgree) {
                Text("Agree & Close")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun RoleMiniChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) ChampagneGold else Color(0xFF2C2722),
        border = BorderStroke(1.dp, if (selected) ChampagneGold else WarmWhite.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MatteBlack else WarmWhite,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (selected) MatteBlack else WarmWhite
            )
        }
    }
}

@Composable
fun AuthErrorBanner(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.18f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFFF8A80),
            modifier = Modifier.padding(12.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AuthInfoBanner(message: String) {
    Surface(
        color = ChampagneGold.copy(alpha = 0.16f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = ChampagneGoldLight,
            modifier = Modifier.padding(12.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun darkAuthFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = WarmWhite,
    unfocusedTextColor = WarmWhite,
    focusedBorderColor = ChampagneGold,
    unfocusedBorderColor = WarmWhite.copy(alpha = 0.3f),
    focusedLabelColor = ChampagneGoldLight,
    unfocusedLabelColor = WarmWhite.copy(alpha = 0.7f),
    cursorColor = ChampagneGold
)

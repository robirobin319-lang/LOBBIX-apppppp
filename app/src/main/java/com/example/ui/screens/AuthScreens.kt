package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.attemptAutoSignIn
import com.example.data.onGoogleSignInClicked
import com.example.ui.components.LobbixBrandLockup
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.GunmetalCard
import com.example.ui.theme.GunmetalElevated
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.NeonScarlet
import com.example.ui.theme.SteelBorder

private enum class AuthRoute {
    LOGIN,
    SIGNUP,
    FORGOT_PASSWORD
}

@Composable
fun AuthFlowScreen(
    onAuthSuccess: (pendingFullName: String?, pendingPhone: String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var authRoute by rememberSaveable { mutableStateOf(AuthRoute.LOGIN) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }
    var pendingName by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingPhone by rememberSaveable { mutableStateOf<String?>(null) }
    var providerInfoDialogMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = { onAuthSuccess(pendingName, pendingPhone) },
            onUnauthenticated = { /* Stay on Login screen */ },
            scope = scope
        )
    }

    fun launchGoogleSignIn() {
        authErrorMessage = null
        isGoogleLoading = true
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isGoogleLoading = false
                onAuthSuccess(pendingName, pendingPhone)
            },
            onAuthError = { msg ->
                isGoogleLoading = false
                authErrorMessage = msg
            },
            scope = scope,
            onAuthCancelled = {
                isGoogleLoading = false
                authErrorMessage = "Google Sign-In was cancelled. Tap Continue with Google to sign in."
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MatteBlack)
    ) {
        // Atmospheric esports header artwork
        Image(
            painter = painterResource(id = R.drawable.img_hero_bermuda),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MatteBlack.copy(alpha = 0.40f),
                            MatteBlack.copy(alpha = 0.88f),
                            MatteBlack
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 20.dp, start = 20.dp, end = 20.dp, bottom = 16.dp)
            ) {
                when (authRoute) {
                    AuthRoute.LOGIN -> {
                        LoginContent(
                            isGoogleLoading = isGoogleLoading,
                            authErrorMessage = authErrorMessage,
                            onGoogleSignInClick = ::launchGoogleSignIn,
                            onCredentialsSubmit = { identifier ->
                                providerInfoDialogMessage =
                                    "Email/Password authentication for \"$identifier\" requires enabling the Email/Password provider in Firebase Console. For instant verified player login in this build, continue with Google Sign-In."
                            },
                            onForgotPasswordClick = {
                                authErrorMessage = null
                                authRoute = AuthRoute.FORGOT_PASSWORD
                            },
                            onCreateAccountClick = {
                                authErrorMessage = null
                                authRoute = AuthRoute.SIGNUP
                            }
                        )
                    }

                    AuthRoute.SIGNUP -> {
                        BackHandler { authRoute = AuthRoute.LOGIN }
                        SignupContent(
                            isGoogleLoading = isGoogleLoading,
                            authErrorMessage = authErrorMessage,
                            onBackClick = { authRoute = AuthRoute.LOGIN },
                            onSignupSubmit = { fullName, contact ->
                                pendingName = fullName
                                pendingPhone = if (contact.contains("@")) null else contact
                                providerInfoDialogMessage =
                                    "Your LOBBIX player details ($fullName) are staged. Complete account verification via Google Sign-In below (Email/Password registration requires enabling that provider in Firebase Console)."
                            },
                            onGoogleSignInClick = ::launchGoogleSignIn
                        )
                    }

                    AuthRoute.FORGOT_PASSWORD -> {
                        BackHandler { authRoute = AuthRoute.LOGIN }
                        ForgotPasswordContent(
                            onBackClick = { authRoute = AuthRoute.LOGIN },
                            onSubmitReset = { identifier ->
                                providerInfoDialogMessage =
                                    "Password reset for \"$identifier\" is managed through Firebase Console once Email/Password Auth is enabled. You can sign in immediately using your Google account."
                            },
                            onGoogleSignInClick = ::launchGoogleSignIn
                        )
                    }
                }
            }
        }
    }

    if (providerInfoDialogMessage != null) {
        AlertDialog(
            onDismissRequest = { providerInfoDialogMessage = null },
            containerColor = GunmetalCard,
            titleContentColor = CrispWhite,
            textContentColor = CoolSlate,
            icon = {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = CrimsonRed
                )
            },
            title = {
                Text(
                    text = "Firebase Authentication",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = providerInfoDialogMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        providerInfoDialogMessage = null
                        launchGoogleSignIn()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                    modifier = Modifier.testTag("dialog_google_signin_button")
                ) {
                    Text("Continue with Google")
                }
            },
            dismissButton = {
                TextButton(onClick = { providerInfoDialogMessage = null }) {
                    Text("Close", color = CoolSlate)
                }
            }
        )
    }
}

@Composable
private fun LoginContent(
    isGoogleLoading: Boolean,
    authErrorMessage: String?,
    onGoogleSignInClick: () -> Unit,
    onCredentialsSubmit: (String) -> Unit,
    onForgotPasswordClick: () -> Unit,
    onCreateAccountClick: () -> Unit
) {
    var identifier by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    Spacer(modifier = Modifier.height(16.dp))
    LobbixBrandLockup(logoSize = 48.dp, showTagline = true)
    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = "PLAYER ARENA LOGIN",
        style = MaterialTheme.typography.displayLarge,
        color = CrispWhite
    )
    Text(
        text = "Compete in Free Fire Solo, Duo, Squad, Lone Wolf & 1v1 tournaments.",
        style = MaterialTheme.typography.bodyMedium,
        color = CoolSlate
    )

    Spacer(modifier = Modifier.height(24.dp))

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = GunmetalCard.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, SteelBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Button(
                onClick = onGoogleSignInClick,
                enabled = !isGoogleLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("google_sign_in_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CrimsonRed,
                    contentColor = CrispWhite
                )
            ) {
                if (isGoogleLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = CrispWhite,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "CONTINUE WITH GOOGLE",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            if (authErrorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = authErrorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonScarlet
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = SteelBorder)
                Text(
                    text = "  OR SIGN IN WITH CREDENTIALS  ",
                    style = MaterialTheme.typography.labelSmall,
                    color = CoolSlate
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = SteelBorder)
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = identifier,
                onValueChange = {
                    identifier = it
                    formError = null
                },
                label = { Text("Mobile Number or Email") },
                leadingIcon = {
                    Icon(Icons.Filled.Email, contentDescription = null, tint = CoolSlate)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_identifier_input"),
                colors = lobbixFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    formError = null
                },
                label = { Text("Password") },
                leadingIcon = {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = CoolSlate)
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = "Toggle password visibility",
                            tint = CoolSlate
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_password_input"),
                colors = lobbixFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )

            if (formError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonScarlet
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onForgotPasswordClick,
                    modifier = Modifier.testTag("forgot_password_button")
                ) {
                    Text("Forgot Password?", color = NeonScarlet)
                }
            }

            OutlinedButton(
                onClick = {
                    when {
                        identifier.isBlank() -> formError = "Enter your mobile number or email."
                        password.length < 6 -> formError = "Password must be at least 6 characters."
                        else -> onCredentialsSubmit(identifier.trim())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("login_submit_button"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.7f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CrispWhite)
            ) {
                Text("SIGN IN", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New to LOBBIX?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CoolSlate
                )
                TextButton(
                    onClick = onCreateAccountClick,
                    modifier = Modifier.testTag("create_account_button")
                ) {
                    Text("Create Account", color = CrimsonRed, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun SignupContent(
    isGoogleLoading: Boolean,
    authErrorMessage: String?,
    onBackClick: () -> Unit,
    onSignupSubmit: (fullName: String, contact: String) -> Unit,
    onGoogleSignInClick: () -> Unit
) {
    var fullName by rememberSaveable { mutableStateOf("") }
    var contact by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var acceptedTerms by rememberSaveable { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .size(48.dp)
                .testTag("back_to_login_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to Login",
                tint = CrispWhite
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        LobbixBrandLockup(logoSize = 36.dp)
    }

    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = "CREATE PLAYER ACCOUNT",
        style = MaterialTheme.typography.headlineLarge,
        color = CrispWhite
    )
    Text(
        text = "Set up your Free Fire competitive profile on LOBBIX.",
        style = MaterialTheme.typography.bodyMedium,
        color = CoolSlate
    )
    Spacer(modifier = Modifier.height(18.dp))

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = GunmetalCard.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, SteelBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it; formError = null },
                label = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = CoolSlate) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_fullname_input"),
                colors = lobbixFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = contact,
                onValueChange = { contact = it; formError = null },
                label = { Text("Mobile Number or Email") },
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = CoolSlate) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_contact_input"),
                colors = lobbixFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; formError = null },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = CoolSlate) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_password_input"),
                colors = lobbixFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; formError = null },
                label = { Text("Confirm Password") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = CoolSlate) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_confirm_password_input"),
                colors = lobbixFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { acceptedTerms = !acceptedTerms; formError = null }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = acceptedTerms,
                    onCheckedChange = { acceptedTerms = it; formError = null },
                    colors = CheckboxDefaults.colors(
                        checkedColor = CrimsonRed,
                        uncheckedColor = CoolSlate
                    ),
                    modifier = Modifier.testTag("signup_terms_checkbox")
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "I accept the LOBBIX Fair-Play Terms & Privacy Policy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CoolSlate
                )
            }

            if (formError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(formError!!, style = MaterialTheme.typography.bodySmall, color = NeonScarlet)
            }
            if (authErrorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(authErrorMessage, style = MaterialTheme.typography.bodySmall, color = NeonScarlet)
            }

            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = {
                    when {
                        fullName.trim().length < 2 -> formError = "Enter your full name."
                        contact.trim().length < 5 -> formError = "Enter a valid mobile number or email."
                        password.length < 6 -> formError = "Password must be at least 6 characters."
                        password != confirmPassword -> formError = "Passwords do not match."
                        !acceptedTerms -> formError = "Please accept the Terms & Privacy Policy."
                        else -> onSignupSubmit(fullName.trim(), contact.trim())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("signup_submit_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
            ) {
                Text("CREATE ACCOUNT", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onGoogleSignInClick,
                enabled = !isGoogleLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("signup_google_button"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SteelBorder)
            ) {
                Text("INSTANT SIGN UP WITH GOOGLE", color = CrispWhite, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ForgotPasswordContent(
    onBackClick: () -> Unit,
    onSubmitReset: (String) -> Unit,
    onGoogleSignInClick: () -> Unit
) {
    var recoveryInput by rememberSaveable { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .size(48.dp)
                .testTag("forgot_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to Login",
                tint = CrispWhite
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        LobbixBrandLockup(logoSize = 36.dp)
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "RESET PASSWORD",
        style = MaterialTheme.typography.headlineLarge,
        color = CrispWhite
    )
    Text(
        text = "Enter your registered Mobile Number or Email to recover access.",
        style = MaterialTheme.typography.bodyMedium,
        color = CoolSlate
    )
    Spacer(modifier = Modifier.height(20.dp))

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = GunmetalCard.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, SteelBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            OutlinedTextField(
                value = recoveryInput,
                onValueChange = { recoveryInput = it; errorMsg = null },
                label = { Text("Mobile Number or Email") },
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = CoolSlate) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("forgot_identifier_input"),
                colors = lobbixFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )

            if (errorMsg != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(errorMsg!!, style = MaterialTheme.typography.bodySmall, color = NeonScarlet)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (recoveryInput.trim().length < 5) {
                        errorMsg = "Enter a valid mobile number or email."
                    } else {
                        onSubmitReset(recoveryInput.trim())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("forgot_submit_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
            ) {
                Text("REQUEST RECOVERY", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                onClick = onGoogleSignInClick,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Or Continue Instantly with Google", color = CoolSlate)
            }
        }
    }
}

@Composable
fun lobbixFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CrimsonRed,
    unfocusedBorderColor = SteelBorder,
    focusedLabelColor = CrimsonRed,
    unfocusedLabelColor = CoolSlate,
    focusedTextColor = CrispWhite,
    unfocusedTextColor = CrispWhite,
    cursorColor = CrimsonRed,
    focusedContainerColor = CarbonSurface,
    unfocusedContainerColor = CarbonSurface
)

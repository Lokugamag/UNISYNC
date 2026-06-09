package com.example.unisync.ui.auth

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.unisync.R
import com.example.unisync.data.DataRepository
import com.example.unisync.theme.*
import com.example.unisync.ui.common.LoadingOverlay
import com.example.unisync.ui.common.UniSyncBackground
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

// Replace with your actual Web Client ID from Firebase Console > Authentication > Google > Web SDK Config
private const val WEB_CLIENT_ID = "1037494857276-lu7l84s3fqebetbl73jup84p3ieev0tc.apps.googleusercontent.com"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    UniSyncBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(48.dp))

                // Logo Icon
                Image(
                    painter = painterResource(id = R.drawable.ic_logo_symbol),
                    contentDescription = "UniSync Logo",
                    modifier = Modifier.size(90.dp)
                )

                Spacer(Modifier.height(12.dp))

                // UniSync logo text (Patrick Hand Font)
                Text(
                    text = "Unisync",
                    fontFamily = PatrickHandFontFamily,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(4.dp))

                // Subtitle (Lora Font, Italicized Serif)
                Text(
                    text = "Your smart study space",
                    fontFamily = LoraFontFamily,
                    fontStyle = FontStyle.Italic,
                    fontSize = 16.sp,
                    color = Color.Black.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(56.dp))

                // Email Field (Rounded Corner Shape to match Figma)
                TextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = "" },
                    placeholder = {
                        Text(
                            text = "Email",
                            color = Color.Black.copy(alpha = 0.5f),
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        textAlign = TextAlign.Start,
                        color = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                         focusedContainerColor = FigmaCardMediumBg,
                         unfocusedContainerColor = FigmaCardMediumBg,
                         disabledContainerColor = FigmaCardMediumBg,
                         focusedIndicatorColor = Color.Transparent,
                         unfocusedIndicatorColor = Color.Transparent,
                         disabledIndicatorColor = Color.Transparent,
                         focusedTextColor = Color.Black,
                         unfocusedTextColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                )

                Spacer(Modifier.height(16.dp))

                // Password Field (Rounded Corner Shape to match Figma)
                TextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = "" },
                    placeholder = {
                        Text(
                            text = "Password",
                            color = Color.Black.copy(alpha = 0.5f),
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = Color.Black.copy(alpha = 0.6f)
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        textAlign = TextAlign.Start,
                        color = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                         focusedContainerColor = FigmaCardMediumBg,
                         unfocusedContainerColor = FigmaCardMediumBg,
                         disabledContainerColor = FigmaCardMediumBg,
                         focusedIndicatorColor = Color.Transparent,
                         unfocusedIndicatorColor = Color.Transparent,
                         disabledIndicatorColor = Color.Transparent,
                         focusedTextColor = Color.Black,
                         unfocusedTextColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                )

                Spacer(Modifier.height(12.dp))

                // Forgot Password? Link
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = "Forgot password?",
                        fontStyle = FontStyle.Italic,
                        fontSize = 13.sp,
                        color = Color.Black.copy(alpha = 0.85f),
                        modifier = Modifier.clickable { onNavigateToForgotPassword() }
                    )
                }

                if (errorMessage.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = errorMessage,
                        color = Color(0xFFC62828),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(28.dp))

                // Log In Button (Rounded Corner Shape & Narrower to match Figma)
                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "Please fill in all fields"
                            return@Button
                        }
                        isLoading = true
                        scope.launch {
                            val result = DataRepository.login(email.trim(), password)
                            isLoading = false
                            result.fold(
                                onSuccess = { onLoginSuccess() },
                                onFailure = { errorMessage = it.message ?: "Login failed" }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.55f) // Narrower width exactly as mockup
                        .height(46.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FigmaButtonPurple),
                    enabled = !isLoading
                ) {
                    Text(
                        text = "Log In",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black // Black text as per Figma mockup
                    )
                }

                Spacer(Modifier.height(28.dp))

                // "Or Continue With" Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = Color.Black.copy(alpha = 0.6f),
                        thickness = 1.dp
                    )
                    Text(
                        text = "Or Continue With",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = Color.Black.copy(alpha = 0.6f),
                        thickness = 1.dp
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Google Login Button (Rounded Corner Shape & Figma Colors)
                Button(
                    onClick = {
                        isLoading = true
                        scope.launch {
                            try {
                                val credentialManager = CredentialManager.create(context)
                                val googleIdOption = GetGoogleIdOption.Builder()
                                    .setServerClientId(WEB_CLIENT_ID)
                                    .setFilterByAuthorizedAccounts(false)
                                    .build()
                                val request = GetCredentialRequest.Builder()
                                    .addCredentialOption(googleIdOption)
                                    .build()
                                val credResult = credentialManager.getCredential(
                                    request = request,
                                    context = context as Activity
                                )
                                val credential = credResult.credential
                                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                val idToken = googleIdTokenCredential.idToken
                                val loginResult = DataRepository.loginWithGoogle(idToken)
                                isLoading = false
                                loginResult.fold(
                                    onSuccess = { onLoginSuccess() },
                                    onFailure = { errorMessage = it.message ?: "Google login failed" }
                                )
                            } catch (e: GetCredentialException) {
                                isLoading = false
                                errorMessage = "Google Sign-In failed: ${e.message}"
                            } catch (e: Exception) {
                                isLoading = false
                                errorMessage = "Error: ${e.message}"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.82f) // Slightly wider to fit text on a single line
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FigmaCardMediumBg),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    enabled = !isLoading
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google),
                            contentDescription = "Google Icon",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp)) // Reduced spacer width
                        Text(
                            text = "Continue With Google",
                            fontSize = 13.sp, // Reduced font size to fit on one line
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            maxLines = 1 // Prevent text wrapping
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))
                HorizontalDivider(
                    color = Color.Black.copy(alpha = 0.6f),
                    thickness = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))

                // Sign Up Footer Text
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "Don't you have an account? ",
                        fontSize = 15.sp,
                        color = Color.Black
                    )
                    Text(
                        text = "Sign Up",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.clickable { onNavigateToRegister() }
                    )
                }
            }

            if (isLoading) LoadingOverlay()
        }
    }
}

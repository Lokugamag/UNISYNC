package com.example.unisync.ui.auth

import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unisync.R
import com.example.unisync.data.DataRepository
import com.example.unisync.theme.*
import com.example.unisync.ui.common.LoadingOverlay
import com.example.unisync.ui.common.UniSyncBackground
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    UniSyncBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(32.dp))

                // Logo Icon
                Image(
                    painter = painterResource(id = R.drawable.ic_logo_symbol),
                    contentDescription = "UniSync Logo",
                    modifier = Modifier.size(80.dp)
                )

                Spacer(Modifier.height(8.dp))
                
                // Title (Patrick Hand Font)
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

                Spacer(Modifier.height(28.dp))

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFFEDE5EE),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Join UniSync",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "Create your account",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(20.dp))

                        // Full Name Field
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it; errorMessage = "" },
                            placeholder = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Default.Person, null, tint = FigmaButtonPurple) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = FigmaButtonPurple,
                                unfocusedBorderColor = Color(0xFFD2C4D3),
                                focusedLabelColor = FigmaButtonPurple,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            )
                        )

                        Spacer(Modifier.height(12.dp))

                        // Student ID Field
                        OutlinedTextField(
                            value = studentId,
                            onValueChange = { studentId = it; errorMessage = "" },
                            placeholder = { Text("Student ID") },
                            leadingIcon = { Icon(Icons.Default.Badge, null, tint = FigmaButtonPurple) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = FigmaButtonPurple,
                                unfocusedBorderColor = Color(0xFFD2C4D3),
                                focusedLabelColor = FigmaButtonPurple,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            )
                        )

                        Spacer(Modifier.height(12.dp))

                        // Email Field
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; errorMessage = "" },
                            placeholder = { Text("Email") },
                            leadingIcon = { Icon(Icons.Default.Email, null, tint = FigmaButtonPurple) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = FigmaButtonPurple,
                                unfocusedBorderColor = Color(0xFFD2C4D3),
                                focusedLabelColor = FigmaButtonPurple,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            )
                        )

                        Spacer(Modifier.height(12.dp))

                        // Password Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; errorMessage = "" },
                            placeholder = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = FigmaButtonPurple) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                        tint = FigmaButtonPurple
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = FigmaButtonPurple,
                                unfocusedBorderColor = Color(0xFFD2C4D3),
                                focusedLabelColor = FigmaButtonPurple,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            )
                        )

                        Spacer(Modifier.height(12.dp))

                        // Confirm Password Field
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it; errorMessage = "" },
                            placeholder = { Text("Confirm Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = FigmaButtonPurple) },
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                                        tint = FigmaButtonPurple
                                    )
                                }
                            },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = FigmaButtonPurple,
                                unfocusedBorderColor = Color(0xFFD2C4D3),
                                focusedLabelColor = FigmaButtonPurple,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            )
                        )

                        if (errorMessage.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = errorMessage,
                                color = Color(0xFFC62828),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        // Register Button
                        Button(
                            onClick = {
                                when {
                                    name.isBlank() || email.isBlank() || studentId.isBlank() || password.isBlank() -> errorMessage = "Please fill in all fields"
                                    password != confirmPassword -> errorMessage = "Passwords do not match"
                                    else -> {
                                        val ruleError = validatePasswordRules(password)
                                        if (ruleError != null) {
                                            errorMessage = ruleError
                                        } else {
                                            isLoading = true
                                            scope.launch {
                                                val result = DataRepository.register(email.trim(), password, name.trim(), studentId.trim())
                                                isLoading = false
                                                result.fold(
                                                    onSuccess = { onRegisterSuccess() },
                                                    onFailure = { errorMessage = it.message ?: "Registration failed" }
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(50.dp),
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = FigmaButtonPurple)
                        ) {
                            Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        }

                        Spacer(Modifier.height(20.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Already have an account? ", style = MaterialTheme.typography.bodyMedium, color = Color.Black.copy(alpha = 0.6f))
                            Text(
                                text = "Sign In",
                                style = MaterialTheme.typography.bodyMedium,
                                color = FigmaDarkText,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { onNavigateToLogin() }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
            if (isLoading) LoadingOverlay()
        }
    }
}

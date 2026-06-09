package com.example.unisync.ui.auth

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unisync.data.DataRepository
import com.example.unisync.theme.*
import com.example.unisync.ui.common.UniSyncBackground
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    UniSyncBackground {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Reset Password",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Enter your email address to receive a secure password reset link.",
                    fontStyle = FontStyle.Italic,
                    fontSize = 14.sp,
                    color = Color.Black.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(32.dp))

                // Email Field
                TextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = "" },
                    placeholder = {
                        Text(
                            text = "Enter Email Address",
                            color = Color.Black.copy(alpha = 0.5f),
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    enabled = !isLoading,
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

                Spacer(Modifier.height(36.dp))

                // Send Reset Email Button
                Button(
                    onClick = {
                        if (email.isBlank()) {
                            errorMessage = "Please enter your email address"
                        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                            errorMessage = "Please enter a valid email address"
                        } else {
                            isLoading = true
                            scope.launch {
                                val result = DataRepository.sendPasswordResetEmail(email.trim())
                                isLoading = false
                                result.onSuccess {
                                    Toast.makeText(context, "Password reset link sent to your email!", Toast.LENGTH_LONG).show()
                                    onSuccess()
                                }.onFailure { error ->
                                    errorMessage = error.localizedMessage ?: "Failed to send reset email"
                                }
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(46.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FigmaButtonPurple)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = "Send Reset Link",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Back to Login Link
                Text(
                    text = "Back to Login",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier
                        .clickable(enabled = !isLoading) { onBack() }
                        .padding(bottom = 32.dp)
                )
            }
        }
    }
}

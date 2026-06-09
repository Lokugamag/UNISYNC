package com.example.unisync.ui.profile

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
fun ProfileScreen(onBack: () -> Unit, onLogout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uid = DataRepository.currentUserId ?: ""
    val user by DataRepository.getUserProfile(uid).collectAsState(initial = null)
    
    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editStudentId by remember { mutableStateOf("") }
    var editFaculty by remember { mutableStateOf("") }
    var editProgram by remember { mutableStateOf("") }
    var editYear by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(user) {
        user?.let {
            if (!isEditing) {
                editName = it.name
                editStudentId = it.studentId
                editFaculty = it.faculty
                editProgram = it.program
                editYear = it.year
            }
        }
    }

    UniSyncBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            // ── Custom Top Header (matching "< Profile" exactly in Figma) ─────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Back",
                    tint = Color.Black,
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onBack() }
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Profile",
                    fontFamily = PatrickHandFontFamily,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            // ── Main Unified Profile Card ───────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = FigmaCardBodyBg, // 0xFFD2C4D3
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Profile Info Header Section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar Image
                        Image(
                            painter = painterResource(id = R.drawable.ic_avatar),
                            contentDescription = "Profile Pic",
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                        )

                        Spacer(Modifier.width(16.dp))

                        // User Info
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user?.name ?: "User Name",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = user?.email ?: "email@uni.edu.lk",
                                fontSize = 13.sp,
                                color = Color.Black.copy(alpha = 0.85f)
                            )
                            Text(
                                text = if (user?.studentId?.isNotEmpty() == true) user!!.studentId else "No Serial ID",
                                fontSize = 13.sp,
                                color = Color.Black.copy(alpha = 0.85f)
                            )
                        }

                        // Edit Button
                        Button(
                            onClick = { isEditing = !isEditing },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FigmaButtonPurple),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = "Edit",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }

                    // Collapsible Profile Edit Fields
                    if (isEditing) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Edit Profile Settings",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.Black
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = { Text("Full Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FigmaButtonPurple,
                                    focusedLabelColor = FigmaButtonPurple
                                )
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = editStudentId,
                                onValueChange = { editStudentId = it },
                                label = { Text("Student ID") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FigmaButtonPurple,
                                    focusedLabelColor = FigmaButtonPurple
                                )
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = editFaculty,
                                onValueChange = { editFaculty = it },
                                label = { Text("Faculty") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FigmaButtonPurple,
                                    focusedLabelColor = FigmaButtonPurple
                                )
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = editProgram,
                                onValueChange = { editProgram = it },
                                label = { Text("Program") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FigmaButtonPurple,
                                    focusedLabelColor = FigmaButtonPurple
                                )
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = editYear,
                                onValueChange = { editYear = it },
                                label = { Text("Year (e.g. Year - 3)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FigmaButtonPurple,
                                    focusedLabelColor = FigmaButtonPurple
                                )
                            )
                            Spacer(Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { isEditing = false }) {
                                    Text("Cancel", color = Color.Black)
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        isLoading = true
                                        scope.launch {
                                            val res = DataRepository.updateUserProfile(
                                                uid = uid,
                                                name = editName.trim(),
                                                studentId = editStudentId.trim(),
                                                faculty = editFaculty.trim(),
                                                program = editProgram.trim(),
                                                year = editYear.trim()
                                            )
                                            isLoading = false
                                            res.fold(
                                                onSuccess = {
                                                    isEditing = false
                                                    Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                                                },
                                                onFailure = {
                                                    Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = FigmaButtonPurple)
                                ) {
                                    Text("Save", color = Color.Black)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    // ── Academic Info Section Title Banner ──────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .background(FigmaCardHeaderBg),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Academic Info",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    // Academic Info Rows
                    AcademicInfoRow(label = "Student ID:", value = user?.studentId ?: "")
                    HorizontalDivider(color = Color.Black.copy(alpha = 0.08f), thickness = 1.dp)
                    AcademicInfoRow(label = "Faculty:", value = user?.faculty ?: "")
                    HorizontalDivider(color = Color.Black.copy(alpha = 0.08f), thickness = 1.dp)
                    AcademicInfoRow(label = "Program:", value = user?.program ?: "")
                    HorizontalDivider(color = Color.Black.copy(alpha = 0.08f), thickness = 1.dp)
                    AcademicInfoRow(label = "Year:", value = user?.year ?: "")
                    HorizontalDivider(color = Color.Black.copy(alpha = 0.08f), thickness = 1.dp)

                    // ── Dashboard Link Row ──────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { Toast.makeText(context, "Opening Dashboard...", Toast.LENGTH_SHORT).show() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dashboard",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Chevron",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // ── Account Section Title Banner ────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .background(FigmaCardHeaderBg),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Account",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    // Account Option Rows
                    AccountOptionRow(
                        icon = Icons.Default.Settings,
                        label = "Settings",
                        onClick = { Toast.makeText(context, "Settings", Toast.LENGTH_SHORT).show() }
                    )
                    HorizontalDivider(color = Color.Black.copy(alpha = 0.08f), thickness = 1.dp)
                    AccountOptionRow(
                        icon = Icons.Default.Notifications,
                        label = "Notifications",
                        onClick = { Toast.makeText(context, "Notifications", Toast.LENGTH_SHORT).show() }
                    )
                    HorizontalDivider(color = Color.Black.copy(alpha = 0.08f), thickness = 1.dp)
                    AccountOptionRow(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        label = "Log Out",
                        onClick = {
                            DataRepository.logout()
                            onLogout()
                        }
                    )
                    HorizontalDivider(color = Color.Black.copy(alpha = 0.08f), thickness = 1.dp)

                    Spacer(Modifier.height(20.dp))

                    // ── Bottom Action Row (3 purple cards side-by-side) ──────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProfileActionButton(
                            icon = Icons.Default.AddShoppingCart,
                            label = "Cart Items",
                            modifier = Modifier.weight(1f)
                        )
                        ProfileActionButton(
                            icon = Icons.Default.LocalMall,
                            label = "Sold Items",
                            modifier = Modifier.weight(1f)
                        )
                        ProfileActionButton(
                            icon = Icons.Default.AccountBalanceWallet,
                            label = "My Wallet",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if (isLoading) LoadingOverlay()
    }
}

@Composable
fun AcademicInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.Black,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value.ifEmpty { "Not set" },
            fontSize = 14.sp,
            color = Color.Black
        )
    }
}

@Composable
fun AccountOptionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.Black,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Black
        )
    }
}

@Composable
fun ProfileActionButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = FigmaButtonPurple,
        modifier = modifier.height(84.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.Black,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center
            )
        }
    }
}

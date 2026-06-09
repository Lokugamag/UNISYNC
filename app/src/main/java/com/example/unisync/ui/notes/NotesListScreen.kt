package com.example.unisync.ui.notes

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unisync.R
import com.example.unisync.theme.*
import com.example.unisync.ui.common.UniSyncBackground
import com.example.unisync.ui.home.HomeBottomNav

// ─── Study Note Hub 1 (Main page of Notes) ───────────────────────────────────

@Composable
fun NotesListScreen(
    onNavigateToManageNotes: () -> Unit,
    onNavigateToViewNotes: () -> Unit,
    onBack: () -> Unit,
    onAddClick: () -> Unit
) {
    UniSyncBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Row (Padded)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 52.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "<",
                        fontFamily = PatrickHandFontFamily,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier
                            .clickable { onBack() }
                            .padding(end = 12.dp)
                    )
                    Text(
                        text = "Study Note Hub",
                        fontFamily = PatrickHandFontFamily,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                // Full-width Divider line (No horizontal padding)
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = Color.Black.copy(alpha = 0.2f)
                )

                // Cards Container centered vertically in remaining space
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Manage Notes Card
                    NotesMenuCard(
                        iconResId = R.drawable.home_ic_manage_notes,
                        title = "Manage Notes",
                        onClick = onNavigateToManageNotes
                    )

                    Spacer(Modifier.height(20.dp))

                    // View Notes Card
                    NotesMenuCard(
                        iconResId = R.drawable.home_ic_notes,
                        title = "View Notes",
                        onClick = onNavigateToViewNotes
                    )
                }

                // Bottom Nav Bar offset space
                Spacer(Modifier.height(64.dp))
            }

            // Bottom Nav Bar
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                HomeBottomNav(
                    onHomeClick = { onBack() },
                    onAddClick = onAddClick,
                    onNotificationsClick = { /* No-op */ }
                )
            }
        }
    }
}

// ─── Study Note Hub 2 (Manage Notes sub-menu) ────────────────────────────────

@Composable
fun ManageNotesScreen(
    onCreateNotes: () -> Unit,
    onUploadNotes: () -> Unit,
    onEditNotes: () -> Unit,
    onBack: () -> Unit
) {
    UniSyncBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Row (Padded)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 52.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "<",
                        fontFamily = PatrickHandFontFamily,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier
                            .clickable { onBack() }
                            .padding(end = 12.dp)
                    )
                    Text(
                        text = "Manage Notes",
                        fontFamily = PatrickHandFontFamily,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                // Full-width Divider line (No horizontal padding)
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = Color.Black.copy(alpha = 0.2f)
                )

                // Cards Container centered vertically in remaining space
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Create Notes Card
                    NotesMenuCard(
                        iconVector = Icons.Default.AddCircleOutline, // (+)
                        title = "Create Notes",
                        onClick = onCreateNotes
                    )

                    Spacer(Modifier.height(20.dp))

                    // Upload Notes Card
                    NotesMenuCard(
                        iconVector = Icons.Default.CameraAlt, // Camera
                        title = "Upload Notes",
                        onClick = onUploadNotes
                    )

                    Spacer(Modifier.height(20.dp))

                    // Edit Notes Card
                    NotesMenuCard(
                        iconVector = Icons.Default.EditNote, // Edit note pencil
                        title = "Edit Notes",
                        onClick = onEditNotes
                    )
                }

                // Bottom Nav Bar offset space
                Spacer(Modifier.height(64.dp))
            }

            // Bottom Nav Bar
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                HomeBottomNav(
                    onHomeClick = { onBack() },
                    onAddClick = { onCreateNotes() },
                    onNotificationsClick = { /* No-op */ }
                )
            }
        }
    }
}

// ─── Study Note Hub 9 (View Notes sub-menu) ──────────────────────────────────

@Composable
fun ViewNotesScreen(
    onMyNotes: () -> Unit,
    onSavedNotes: () -> Unit,
    onChooseFromInternet: () -> Unit,
    onBack: () -> Unit,
    onAddClick: () -> Unit
) {
    UniSyncBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Row (Padded)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 52.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "<",
                        fontFamily = PatrickHandFontFamily,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier
                            .clickable { onBack() }
                            .padding(end = 12.dp)
                    )
                    Text(
                        text = "View Notes",
                        fontFamily = PatrickHandFontFamily,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                // Full-width Divider line (No horizontal padding)
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = Color.Black.copy(alpha = 0.2f)
                )

                // Cards Container centered vertically in remaining space
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    // My Notes Card
                    NotesMenuCard(
                        iconVector = Icons.Default.EditNote, // notepad/checklist icon with pencil
                        title = "My Notes",
                        onClick = onMyNotes
                    )

                    Spacer(Modifier.height(20.dp))

                    // Saved Notes Card
                    NotesMenuCard(
                        iconVector = Icons.Default.Bookmark, // bookmark
                        title = "Saved Notes",
                        onClick = onSavedNotes
                    )

                    Spacer(Modifier.height(20.dp))

                    // Choose from Internet Card
                    NotesMenuCard(
                        iconVector = Icons.Default.Language, // Globe
                        title = "Choose from the Internet",
                        onClick = onChooseFromInternet
                    )
                }

                // Bottom Nav Bar offset space
                Spacer(Modifier.height(64.dp))
            }

            // Bottom Nav Bar
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                HomeBottomNav(
                    onHomeClick = { onBack() },
                    onAddClick = onAddClick,
                    onNotificationsClick = { /* No-op */ }
                )
            }
        }
    }
}

// ─── Reusable Notes Menu Card component ──────────────────────────────────────

@Composable
fun NotesMenuCard(
    title: String,
    iconVector: ImageVector? = null,
    iconResId: Int? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = FigmaCardMediumBg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                if (iconResId != null) {
                    Image(
                        painter = painterResource(id = iconResId),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                } else if (iconVector != null) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(Modifier.width(18.dp))
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black.copy(alpha = 0.85f),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}




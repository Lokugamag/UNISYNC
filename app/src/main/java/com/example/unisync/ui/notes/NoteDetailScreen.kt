package com.example.unisync.ui.notes

import android.widget.Toast
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unisync.R
import com.example.unisync.data.DataRepository
import com.example.unisync.theme.*
import com.example.unisync.ui.common.UniSyncBackground
import com.example.unisync.ui.home.HomeBottomNav
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: String,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    onEditDesignClick: (String, String) -> Unit,
    onHomeClick: () -> Unit,
    onAddClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val note by DataRepository.getNoteById(noteId).collectAsState(initial = null)
    val currentUid = DataRepository.currentUserId ?: ""

    var isSaved by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDownloadAlert by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }

    LaunchedEffect(noteId, currentUid) {
        if (currentUid.isNotEmpty()) {
            isSaved = DataRepository.isNoteSaved(currentUid, noteId)
        }
    }

    UniSyncBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 72.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 52.dp, bottom = 4.dp),
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
                    text = "My Notes",
                    fontFamily = PatrickHandFontFamily,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            // Note Sub-header showing note title (e.g. "Note 2")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = note?.title ?: "Note",
                    fontFamily = PatrickHandFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(Modifier.height(12.dp))

            // Large Note Document Preview Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.2.dp, FigmaCardBorder, RoundedCornerShape(12.dp))
                ) {
                    note?.let { n ->
                        if (n.fileUrl.startsWith("design:")) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable {
                                        onEditDesignClick(n.noteId, n.fileUrl.substringAfter("design:"))
                                    }
                            ) {
                                NoteDesignPreview(
                                    note = n,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF8A3D73).copy(alpha = 0.85f))
                                        .align(Alignment.BottomCenter)
                                        .padding(10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "✏️ Tap to Edit Document",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        } else {
                            Image(
                                painter = getNotePreviewPainter(n),
                                contentDescription = n.title,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            )
                        }
                    } ?: Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = FigmaDarkText)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Bottom toolbar: Share, Download, Bookmark, Delete
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Share Icon
                IconButton(onClick = { showShareSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Download Icon
                IconButton(
                    onClick = {
                        val downloaded = downloadFileToPublicFolder(context, Uri.parse(note?.fileUrl ?: ""))
                        showDownloadAlert = true
                        scope.launch {
                            delay(3000)
                            showDownloadAlert = false
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Bookmark Icon
                IconButton(
                    onClick = {
                        if (!isSaving) {
                            isSaving = true
                            scope.launch {
                                val result = DataRepository.toggleSaveNote(currentUid, noteId)
                                result.onSuccess { saved -> isSaved = saved }
                                isSaving = false
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Delete Icon
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        // Floating Toast Alert: "Successfully Downloaded" (Hub 12)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 96.dp), // Positioned above bottom nav
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = showDownloadAlert,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .wrapContentHeight()
                        .border(1.2.dp, FigmaCardBorder, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Alert",
                            tint = Color(0xFFFFB300), // Gold icon
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Message Alert!",
                                fontFamily = PatrickHandFontFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "✓ Successfully Downloaded.",
                                fontSize = 13.sp,
                                color = Color.Black.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Custom Bottom Share Sheet (Hub 13)
        if (showShareSheet) {
            ModalBottomSheet(
                onDismissRequest = { showShareSheet = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Share Note",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = FigmaDarkText
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        note?.let { n ->
                            ShareAppOption(R.drawable.ic_launcher_foreground, "Message", Color(0xFF81C784)) {
                                showShareSheet = false
                                shareNote(context, n, "com.google.android.apps.messaging")
                            }
                            ShareAppOption(R.drawable.ic_launcher_foreground, "Mail", Color(0xFF64B5F6)) {
                                showShareSheet = false
                                shareNote(context, n, "com.google.android.gm")
                            }
                            ShareAppOption(R.drawable.ic_launcher_foreground, "Whatsapp", Color(0xFF4CAF50)) {
                                showShareSheet = false
                                shareNote(context, n, "com.whatsapp")
                            }
                            ShareAppOption(R.drawable.ic_launcher_foreground, "More", Color.Gray) {
                                showShareSheet = false
                                shareNote(context, n, null)
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        // Warning Delete Dialog (Hub 14)
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = Color(0xFFFFB300), // Yellow warning triangle
                        modifier = Modifier.size(48.dp)
                    )
                },
                title = {
                    Text(
                        text = "Message Alert!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = FigmaDarkText,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete this file?",
                        fontSize = 14.sp,
                        color = Color.Black.copy(0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            scope.launch {
                                DataRepository.deleteNote(noteId)
                                Toast.makeText(context, "Note deleted successfully!", Toast.LENGTH_SHORT).show()
                                onDeleted()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FigmaCardMediumBg,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Delete", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showDeleteDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FigmaCardMediumBg,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color.White
            )
        }

        // Home Navigation Bar at bottom
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            HomeBottomNav(
                onHomeClick = onHomeClick,
                onAddClick = onAddClick,
                onNotificationsClick = { }
            )
        }
    }
}

@Composable
fun ShareAppOption(
    iconRes: Int,
    label: String,
    bgColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            // Using a simple fallback or matching text if image is launcher background
            Icon(
                imageVector = when (label) {
                    "Message" -> Icons.Default.ChatBubble
                    "Mail" -> Icons.Default.Email
                    "Whatsapp" -> Icons.Default.Phone
                    else -> Icons.Default.MoreHoriz
                },
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}

fun shareNote(context: android.content.Context, note: com.example.unisync.data.Note, targetPackage: String? = null) {
    val fileUrl = note.fileUrl
    val title = note.title
    val subject = note.subject
    val topic = note.topic
    val description = note.description
    
    val shareText = "Check out my UniSync Note: $title\nSubject: $subject\nTopic: $topic\nDescription: $description"
    
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        if (targetPackage != null) {
            setPackage(targetPackage)
        }
        
        if (fileUrl.startsWith("file://")) {
            try {
                val file = java.io.File(android.net.Uri.parse(fileUrl).path ?: "")
                if (file.exists()) {
                    val contentUri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "com.example.unisync.fileprovider",
                        file
                    )
                    type = if (note.fileType == "pdf") "application/pdf" else if (note.fileType == "image") "image/*" else "*/*"
                    putExtra(android.content.Intent.EXTRA_STREAM, contentUri)
                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, "$shareText\n(Note file not found locally)")
                }
            } catch (e: Exception) {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, "$shareText\nError loading file: ${e.message}")
            }
        } else if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, "$shareText\nLink: $fileUrl")
        } else {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
        }
    }
    
    try {
        if (targetPackage == null) {
            context.startActivity(android.content.Intent.createChooser(intent, "Share Note"))
        } else {
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        try {
            val chooserIntent = android.content.Intent.createChooser(intent.apply { setPackage(null) }, "Share Note")
            context.startActivity(chooserIntent)
        } catch (ex: Exception) {
            android.widget.Toast.makeText(context, "No app available to share this note.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

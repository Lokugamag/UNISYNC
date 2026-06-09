package com.example.unisync.ui.notes

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.unisync.data.DataRepository
import com.example.unisync.theme.*
import com.example.unisync.ui.common.LoadingOverlay
import com.example.unisync.ui.common.UniSyncBackground
import com.example.unisync.ui.common.UniSyncTopBar
import kotlinx.coroutines.launch

@Composable
fun EditNoteScreen(noteId: String, onSuccess: () -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val note by DataRepository.getNoteById(noteId).collectAsState(initial = null)
    var title by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var newFileUri by remember { mutableStateOf<Uri?>(null) }
    var newFileName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(note) {
        note?.let { n ->
            if (!initialized) {
                title = n.title; topic = n.topic; subject = n.subject; description = n.description
                initialized = true
            }
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { newFileUri = it; newFileName = it.lastPathSegment ?: "file" }
    }

    UniSyncBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { UniSyncTopBar(title = "Edit Note", onBack = onBack) }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                    Spacer(Modifier.height(8.dp))
                    Surface(shape = RoundedCornerShape(20.dp), color = CardBackground, shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Edit Note Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(16.dp))
                            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LavenderPrimary, unfocusedBorderColor = MediumGray, focusedLabelColor = LavenderPrimary))
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LavenderPrimary, unfocusedBorderColor = MediumGray, focusedLabelColor = LavenderPrimary))
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(value = topic, onValueChange = { topic = it }, label = { Text("Topic") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LavenderPrimary, unfocusedBorderColor = MediumGray, focusedLabelColor = LavenderPrimary))
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 6, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LavenderPrimary, unfocusedBorderColor = MediumGray, focusedLabelColor = LavenderPrimary))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Surface(shape = RoundedCornerShape(20.dp), color = CardBackground, shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Replace File (Optional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            if (note?.fileName?.isNotBlank() == true) Text("Current: ${note?.fileName}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Spacer(Modifier.height(12.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(12.dp)).border(2.dp, if (newFileUri != null) LavenderPrimary else MediumGray, RoundedCornerShape(12.dp)).clickable { fileLauncher.launch("*/*") }, contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(if (newFileUri != null) Icons.Default.CheckCircle else Icons.Default.CloudUpload, null, tint = if (newFileUri != null) FoundGreen else LavenderLight, modifier = Modifier.size(36.dp))
                                    Text(if (newFileUri != null) newFileName.take(25) else "Tap to replace file", style = MaterialTheme.typography.bodySmall, color = if (newFileUri != null) FoundGreen else TextSecondary)
                                }
                            }
                        }
                    }
                    if (errorMessage.isNotEmpty()) { Spacer(Modifier.height(12.dp)); Surface(shape = RoundedCornerShape(8.dp), color = LostRed.copy(alpha = 0.1f), modifier = Modifier.fillMaxWidth()) { Text(errorMessage, color = LostRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp)) } }
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            if (title.isBlank()) { errorMessage = "Title is required"; return@Button }
                            isLoading = true
                            scope.launch {
                                val result = DataRepository.updateNote(noteId, title.trim(), topic.trim(), subject.trim(), description.trim(), newFileUri)
                                isLoading = false
                                result.fold(onSuccess = { onSuccess() }, onFailure = { errorMessage = it.message ?: "Update failed" })
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = LavenderPrimary)
                    ) { Icon(Icons.Default.Save, null, tint = Color.White); Spacer(Modifier.width(8.dp)); Text("Update Note", fontWeight = FontWeight.Bold, color = Color.White) }
                    Spacer(Modifier.height(32.dp))
                }
                if (isLoading) LoadingOverlay()
            }
        }
    }
}

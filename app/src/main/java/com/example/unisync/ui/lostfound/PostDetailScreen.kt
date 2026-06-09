package com.example.unisync.ui.lostfound

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.unisync.data.DataRepository
import com.example.unisync.theme.*
import com.example.unisync.ui.common.StatusBadge
import com.example.unisync.ui.common.UniSyncBackground
import com.example.unisync.ui.common.UniSyncTopBar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PostDetailScreen(
    postId: String,
    onEdit: (String) -> Unit,
    onBack: () -> Unit,
    onDeleted: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val post by DataRepository.getPostById(postId).collectAsState(initial = null)
    val currentUid = DataRepository.currentUserId ?: ""
    var showDeleteDialog by remember { mutableStateOf(false) }
    val typeColor = if (post?.type == "LOST") LostRed else FoundGreen

    UniSyncBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                UniSyncTopBar(
                    title = "Item Details",
                    onBack = onBack,
                    actions = {
                        IconButton(onClick = { onEdit(postId) }) { Icon(Icons.Default.Edit, "Edit", tint = LavenderPrimary) }
                        IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Default.Delete, "Delete", tint = LostRed) }
                    }
                )
            }
        ) { padding ->
            post?.let { p ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
                ) {
                    Spacer(Modifier.height(8.dp))

                    // Image
                    if (p.imageUrl.isNotBlank()) {
                        Surface(shape = RoundedCornerShape(20.dp), shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth().height(220.dp)) {
                            AsyncImage(model = p.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)))
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    Surface(shape = RoundedCornerShape(20.dp), color = CardBackground, shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(p.itemName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                                StatusBadge(p.type)
                            }
                            if (p.status == "RESOLVED") { Spacer(Modifier.height(4.dp)); StatusBadge("RESOLVED") }

                            Divider(modifier = Modifier.padding(vertical = 16.dp), color = CardBorder)

                            if (p.category.isNotBlank()) InfoRowPost(Icons.Default.Category, "Category", p.category)
                            if (p.description.isNotBlank()) InfoRowPost(Icons.Default.Description, "Description", p.description)
                            if (p.location.isNotBlank()) InfoRowPost(Icons.Default.LocationOn, "Location", p.location)
                            if (p.timePeriod.isNotBlank()) InfoRowPost(Icons.Default.Schedule, "Time Period", p.timePeriod)
                            Divider(modifier = Modifier.padding(vertical = 12.dp), color = CardBorder)
                            InfoRowPost(Icons.Default.Person, "Reported by", "${p.posterName} (${p.posterStudentId})")
                            if (p.posterContact.isNotBlank()) InfoRowPost(Icons.Default.Email, "Contact", p.posterContact)
                            InfoRowPost(Icons.Default.CalendarToday, "Date Posted", SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(p.createdAt)))
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Contact button
                    if (p.posterContact.isNotBlank()) {
                        Button(
                            onClick = { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${p.posterContact}"))) },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = typeColor)
                        ) {
                            Icon(Icons.Default.Email, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Contact Reporter", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            } ?: Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = LavenderPrimary)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = LostRed,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    "Are You Sure?",
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    "This post will be permanently deleted and cannot be recovered.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        scope.launch {
                            DataRepository.deletePost(postId)
                            onDeleted()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LostRed),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Yes, Delete", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Cancel") }
            },
            containerColor = CardBackground
        )
    }
}

@Composable
fun InfoRowPost(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = LavenderPrimary, modifier = Modifier.size(20.dp).padding(top = 2.dp))
        Spacer(Modifier.width(10.dp))
        Column { Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary); Text(value, style = MaterialTheme.typography.bodyMedium, color = TextPrimary) }
    }
}

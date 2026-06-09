package com.example.unisync.ui.lostfound

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.unisync.data.DataRepository
import com.example.unisync.theme.*
import com.example.unisync.ui.common.LoadingOverlay
import com.example.unisync.ui.common.UniSyncBackground
import com.example.unisync.ui.home.HomeBottomNav
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPostScreen(
    postId: String,
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    onHomeClick: () -> Unit,
    onAddClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val post by DataRepository.getPostById(postId).collectAsState(initial = null)
    var itemName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    val categories = listOf("Stationery & Accessories", "Electronics", "Wallets", "Bags", "Keys", "Misc")
    var type by remember { mutableStateOf("LOST") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var timePeriod by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("ACTIVE") }
    var newImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(post) {
        post?.let { p ->
            if (!initialized) {
                itemName = p.itemName; category = p.category; type = p.type
                description = p.description; location = p.location; timePeriod = p.timePeriod; status = p.status
                initialized = true
            }
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { newImageUri = it }
    }

    UniSyncBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 72.dp)
        ) {
            // Header Row (Matches mockup)
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
                    text = "Report",
                    fontFamily = PatrickHandFontFamily,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    ReportLabel("Item - Name:")
                    ReportTextField(value = itemName, onValueChange = { itemName = it })
                }

                Column {
                    ReportLabel("Item - Category:")
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(Color(0xFFD4C8D8), RoundedCornerShape(12.dp))
                                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = category.ifEmpty { "Select Category" },
                                    color = Color.Black,
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily.Serif
                                )
                                Icon(
                                    imageVector = if (categoryExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color.Black
                                )
                            }
                        }
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat, fontFamily = FontFamily.Serif) },
                                    onClick = { category = cat; categoryExpanded = false }
                                )
                            }
                        }
                    }
                }

                Column {
                    ReportLabel("Location:")
                    ReportTextField(value = location, onValueChange = { location = it })
                }

                Column {
                    ReportLabel("Time Period:")
                    ReportTextField(value = timePeriod, onValueChange = { timePeriod = it })
                }

                Column {
                    ReportLabel("Description:")
                    ReportTextField(value = description, onValueChange = { description = it })
                }

                Column {
                    ReportLabel("Status:")
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ReportRadioButton(
                            selected = type == "LOST",
                            label = "Lost Item",
                            onClick = { type = "LOST" }
                        )
                        Spacer(Modifier.height(6.dp))
                        ReportRadioButton(
                            selected = type == "FOUND",
                            label = "Found Item",
                            onClick = { type = "FOUND" }
                        )
                    }
                }

                // If editing, also show resolved status
                Column {
                    ReportLabel("Post Status:")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FilterChip(
                            selected = status == "ACTIVE",
                            onClick = { status = "ACTIVE" },
                            label = { Text("Active", fontFamily = FontFamily.Serif) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ActivePurple,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = status == "RESOLVED",
                            onClick = { status = "RESOLVED" },
                            label = { Text("Resolved", fontFamily = FontFamily.Serif) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FoundGreen,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Column {
                    ReportLabel("Upload an image:")
                    val currentImageUrl = post?.imageUrl ?: ""
                    val currentUri = if (newImageUri != null) newImageUri else if (currentImageUrl.isNotEmpty()) Uri.parse(currentImageUrl) else null
                    ReportUploadBox(imageUri = currentUri, onClick = { imageLauncher.launch("image/*") })
                }

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Serif,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            if (itemName.isBlank()) { errorMessage = "Item name is required"; return@Button }
                            isLoading = true
                            scope.launch {
                                val result = DataRepository.updatePost(
                                    postId = postId,
                                    itemName = itemName.trim(),
                                    category = category.trim(),
                                    type = type,
                                    description = description.trim(),
                                    location = location.trim(),
                                    timePeriod = timePeriod.trim(),
                                    status = status,
                                    newImageUri = newImageUri
                                )
                                isLoading = false
                                result.fold(onSuccess = { onSuccess() }, onFailure = { errorMessage = it.message ?: "Update failed" })
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF87778C),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .width(130.dp)
                            .height(38.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Update", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            HomeBottomNav(
                onHomeClick = onHomeClick,
                onAddClick = onAddClick,
                onNotificationsClick = { }
            )
        }

        if (isLoading) LoadingOverlay()
    }
}

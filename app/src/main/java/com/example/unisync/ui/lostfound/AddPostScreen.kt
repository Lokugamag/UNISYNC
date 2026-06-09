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
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.unisync.data.DataRepository
import com.example.unisync.data.LostFoundPost
import com.example.unisync.theme.*
import com.example.unisync.ui.common.LoadingOverlay
import com.example.unisync.ui.common.UniSyncBackground
import com.example.unisync.ui.home.HomeBottomNav
import kotlinx.coroutines.launch

@Composable
fun ReportLabel(text: String) {
    Text(
        text = text,
        fontSize = 17.sp,
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        color = Color.Black,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
fun ReportTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = ""
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color(0xFFD4C8D8), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                color = Color.Black,
                fontSize = 15.sp,
                fontFamily = FontFamily.Serif
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        color = Color.Black.copy(alpha = 0.5f),
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Serif
                    )
                }
                innerTextField()
            }
        )
    }
}

@Composable
fun ReportRadioButton(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(2.dp, Color.Black, CircleShape)
                .padding(3.dp),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black, CircleShape)
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            fontSize = 16.sp,
            fontFamily = FontFamily.Serif,
            color = Color.Black
        )
    }
}

@Composable
fun ReportUploadBox(
    imageUri: Uri?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(Color(0xFFC7BCCB), RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (imageUri != null) {
            AsyncImage(
                model = imageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
            )
        } else {
            Box(modifier = Modifier.size(90.dp)) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = Color(0xFF8E8491),
                    modifier = Modifier
                        .size(72.dp)
                        .align(Alignment.TopStart)
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(Color(0xFF8E8491), CircleShape)
                        .border(2.dp, Color(0xFFC7BCCB), CircleShape)
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPostScreen(
    preselectedType: String = "LOST",
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    onHomeClick: () -> Unit,
    onAddClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var itemName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Stationery & Accessories") }
    var categoryExpanded by remember { mutableStateOf(false) }
    val categories = listOf("Stationery & Accessories", "Electronics", "Wallets", "Bags", "Keys", "Misc")
    var type by remember { mutableStateOf(preselectedType) }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var timePeriod by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val uid = DataRepository.currentUserId ?: ""
    val user by DataRepository.getUserProfile(uid).collectAsState(initial = null)

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { imageUri = it }
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
                    ReportTextField(value = itemName, onValueChange = { itemName = it; errorMessage = "" })
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
                                    text = category,
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

                Column {
                    ReportLabel("Upload an image:")
                    ReportUploadBox(imageUri = imageUri, onClick = { imageLauncher.launch("image/*") })
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
                                val post = LostFoundPost(
                                    itemName = itemName.trim(),
                                    category = category.trim(),
                                    type = type,
                                    description = description.trim(),
                                    location = location.trim(),
                                    timePeriod = timePeriod.trim()
                                )
                                val result = DataRepository.createPost(
                                    post = post,
                                    imageUri = imageUri,
                                    posterName = user?.name ?: "",
                                    posterStudentId = user?.studentId ?: "",
                                    posterContact = user?.email ?: ""
                                )
                                isLoading = false
                                result.fold(onSuccess = { onSuccess() }, onFailure = { errorMessage = it.message ?: "Failed to post" })
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
                        Text("Post", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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

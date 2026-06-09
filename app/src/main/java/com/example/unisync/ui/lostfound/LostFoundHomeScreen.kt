package com.example.unisync.ui.lostfound

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.unisync.R
import com.example.unisync.data.DataRepository
import com.example.unisync.data.LostFoundPost
import com.example.unisync.theme.*
import com.example.unisync.ui.common.EmptyState
import com.example.unisync.ui.common.UniSyncBackground
import com.example.unisync.ui.home.HomeBottomNav
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ─── Screen 1: Lost & Found Main Screen ───────────────────────────────────────
@Composable
fun LostFoundHomeScreen(
    onViewLost: () -> Unit,
    onViewFound: () -> Unit,
    onReportLost: () -> Unit,
    onReportFound: () -> Unit,
    onMyPosts: () -> Unit,
    onBack: () -> Unit
) {
    UniSyncBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 64.dp)
        ) {
            // Header / Top Bar
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
                    text = "LOST & FOUND",
                    fontFamily = PatrickHandFontFamily,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            // Separator Line
            Divider(
                color = FigmaLineColor,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Main Card Container (No border, matches mockup)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = FigmaCardHeaderBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Lost Items Box (No border, LoraFontFamily title)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = FigmaCardBodyBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 24.dp, vertical = 24.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Lost Items",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = LoraFontFamily,
                                    color = Color.Black,
                                    modifier = Modifier.weight(1f)
                                )

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Report Button (No border, FigmaCardHeaderBg color)
                                    Surface(
                                        onClick = onReportLost,
                                        shape = RoundedCornerShape(8.dp),
                                        color = FigmaCardHeaderBg,
                                        modifier = Modifier.size(width = 84.dp, height = 36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "Report",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // View Button (No border, FigmaCardHeaderBg color)
                                    Surface(
                                        onClick = onViewLost,
                                        shape = RoundedCornerShape(8.dp),
                                        color = FigmaCardHeaderBg,
                                        modifier = Modifier.size(width = 84.dp, height = 36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "View",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Found Items Box (No border, LoraFontFamily title)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = FigmaCardBodyBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 24.dp, vertical = 24.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Found Items",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = LoraFontFamily,
                                    color = Color.Black,
                                    modifier = Modifier.weight(1f)
                                )

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Report Button (No border, FigmaCardHeaderBg color)
                                    Surface(
                                        onClick = onReportFound,
                                        shape = RoundedCornerShape(8.dp),
                                        color = FigmaCardHeaderBg,
                                        modifier = Modifier.size(width = 84.dp, height = 36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "Report",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // View Button (No border, FigmaCardHeaderBg color)
                                    Surface(
                                        onClick = onViewFound,
                                        shape = RoundedCornerShape(8.dp),
                                        color = FigmaCardHeaderBg,
                                        modifier = Modifier.size(width = 84.dp, height = 36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "View",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Nav Bar
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            HomeBottomNav(
                onHomeClick = onBack,
                onAddClick = { onReportLost() },
                onNotificationsClick = {}
            )
        }
    }
}

// ─── Screen 2: Lost & Found Categories Screen ─────────────────────────────────
@Composable
fun LostFoundCategoriesScreen(
    type: String,
    onCategoryClick: (String) -> Unit,
    onBack: () -> Unit,
    onHomeClick: () -> Unit,
    onAddClick: () -> Unit
) {
    val categoriesList = listOf(
        CategoryData("Stationery & Accessories", "Pencil cases, water bottles, and stationery items.", R.drawable.cat_stationery),
        CategoryData("Electronics", "Phones, smartwatches, chargers, and gadgets.", R.drawable.cat_electronics),
        CategoryData("Wallets", "Wallets, purses, and cardholders.", R.drawable.cat_wallets),
        CategoryData("Keys", "Room keys, vehicle keys, and keychains.", R.drawable.cat_keys),
        CategoryData("Others", "Bags, clothes, and other items.", R.drawable.item_backpack)
    )

    val postsFlow = remember(type) {
        if (type == "LOST") {
            DataRepository.getLostItems()
        } else {
            DataRepository.getFoundItems()
        }
    }
    val posts by postsFlow.collectAsState(initial = emptyList())

    UniSyncBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 64.dp)
        ) {
            // Header / Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 48.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "<",
                    fontSize = 32.sp,
                    fontFamily = PatrickHandFontFamily,
                    color = FigmaDarkText,
                    modifier = Modifier
                        .clickable { onBack() }
                        .padding(end = 12.dp)
                )
                Text(
                    text = "${type.uppercase()} CATEGORIES",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FigmaDarkText,
                    fontFamily = ComfortaaFontFamily,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Select a category to view reported items",
                fontSize = 13.sp,
                color = FigmaDarkText.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(categoriesList) { category ->
                    val itemCount = posts.count { post ->
                        if (category.name == "Others") {
                            post.category !in listOf("Stationery & Accessories", "Electronics", "Wallets", "Keys")
                        } else {
                            post.category == category.name
                        }
                    }
                    CategoryCard(
                        category = category,
                        itemCount = itemCount,
                        onClick = { onCategoryClick(category.name) }
                    )
                }
            }
        }

        // Bottom Nav Bar
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            HomeBottomNav(
                onHomeClick = onHomeClick,
                onAddClick = onAddClick,
                onNotificationsClick = {}
            )
        }
    }
}

data class CategoryData(
    val name: String,
    val description: String,
    val imageResId: Int
)

@Composable
fun CategoryCard(
    category: CategoryData,
    itemCount: Int,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = FigmaDarkText
                )
                Text(
                    text = category.description,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = FigmaFieldBackground,
                    modifier = Modifier.padding(0.dp)
                ) {
                    Text(
                        text = "$itemCount items",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = FigmaDarkText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Image(
                painter = painterResource(category.imageResId),
                contentDescription = category.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(86.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }
    }
}

// ─── Screen 3: Category Item List Screen ──────────────────────────────────────
@Composable
fun CategoryItemListScreen(
    type: String,
    category: String,
    onEditPost: (String) -> Unit,
    onBack: () -> Unit,
    onHomeClick: () -> Unit,
    onAddClick: () -> Unit
) {
    val uid = DataRepository.currentUserId ?: ""
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var expandedPostIds by remember { mutableStateOf(setOf<String>()) }
    var currentPage by remember { mutableStateOf(1) }
    var postToDelete by remember { mutableStateOf<LostFoundPost?>(null) }

    val postsFlow = remember(type) {
        if (type == "LOST") {
            DataRepository.getLostItems()
        } else {
            DataRepository.getFoundItems()
        }
    }
    val posts by postsFlow.collectAsState(initial = emptyList())

    val filtered = posts.filter { post ->
        val matchesCat = if (category == "Others") {
            post.category !in listOf("Stationery & Accessories", "Electronics", "Wallets", "Keys")
        } else {
            post.category == category
        }
        val matchesSearch = searchQuery.isEmpty() ||
                post.itemName.contains(searchQuery, true) ||
                post.location.contains(searchQuery, true) ||
                post.description.contains(searchQuery, true)
        matchesCat && matchesSearch
    }

    val pageSize = 4
    val totalPages = maxOf(1, (filtered.size + pageSize - 1) / pageSize)
    
    // Adjust current page if it goes out of bounds
    val activePage = if (currentPage > totalPages) totalPages else currentPage
    val startIndex = (activePage - 1) * pageSize
    val endIndex = minOf(startIndex + pageSize, filtered.size)
    val pageItems = if (startIndex < filtered.size) filtered.subList(startIndex, endIndex) else emptyList()

    val headerTitle = when {
        category == "Electronics" -> "Electronic"
        category == "Others" -> if (type == "LOST") "Lost Items" else "Found Items"
        else -> category
    }

    UniSyncBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 64.dp) // space for bottom nav
        ) {
            // Header / Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 48.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "<",
                    fontSize = 32.sp,
                    fontFamily = PatrickHandFontFamily,
                    color = FigmaDarkText,
                    modifier = Modifier
                        .clickable { onBack() }
                        .padding(end = 12.dp)
                )
                Text(
                    text = headerTitle,
                    fontSize = 28.sp,
                    fontFamily = PatrickHandFontFamily,
                    color = FigmaDarkText,
                    fontWeight = FontWeight.Bold
                )
            }

            // Search Bar Row (exact Figma style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it 
                        currentPage = 1 // reset to first page on search
                    },
                    placeholder = { 
                        Text(
                            text = "Search", 
                            color = FigmaDarkText.copy(alpha = 0.5f),
                            fontFamily = LoraFontFamily,
                            fontSize = 15.sp
                        ) 
                    },
                    trailingIcon = { 
                        Icon(
                            imageVector = Icons.Default.Search, 
                            contentDescription = "Search", 
                            tint = FigmaDarkText,
                            modifier = Modifier.size(24.dp)
                        ) 
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp), // pill shape
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = FigmaFieldBackground,
                        unfocusedContainerColor = FigmaFieldBackground,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = FigmaDarkText,
                        unfocusedTextColor = FigmaDarkText
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { /* Tune/Filter style */ },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Filter",
                        tint = FigmaDarkText,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Item List
            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        message = "No items reported in this category",
                        icon = Icons.Default.SearchOff
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(pageItems, key = { it.postId }) { post ->
                        val isExpanded = expandedPostIds.contains(post.postId)
                        ExpandableLostFoundCard(
                            post = post,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                expandedPostIds = if (isExpanded) {
                                    expandedPostIds - post.postId
                                } else {
                                    expandedPostIds + post.postId
                                }
                            },
                            canDelete = true,
                            onDeleteClick = {
                                postToDelete = post
                            },
                            onEditClick = {
                                onEditPost(post.postId)
                            }
                        )
                    }
                }

                // Pagination Row
                PaginationBar(
                    totalPages = totalPages,
                    currentPage = activePage,
                    onPageSelected = { currentPage = it }
                )
            }
        }

        // Bottom Nav Bar
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            HomeBottomNav(
                onHomeClick = onHomeClick,
                onAddClick = onAddClick,
                onNotificationsClick = {}
            )
        }
    }

    // Delete dialog
    postToDelete?.let { post ->
        DeleteConfirmationDialog(
            onConfirm = {
                scope.launch {
                    DataRepository.deletePost(post.postId)
                    postToDelete = null
                }
            },
            onDismiss = {
                postToDelete = null
            }
        )
    }
}

@Composable
fun ExpandableLostFoundCard(
    post: LostFoundPost,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    canDelete: Boolean,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(post.createdAt))
    val timeClean = post.timePeriod.trim()
    val subtitle = if (timeClean.startsWith("lost", ignoreCase = true) || timeClean.startsWith("found", ignoreCase = true)) {
        "$timeClean at ${post.location.ifBlank { "Unknown" }}"
    } else {
        val action = if (post.type == "LOST") "Lost" else "Found"
        "$action $timeClean at ${post.location.ifBlank { "Unknown" }}"
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFC5B6C8),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                // Image
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(FigmaFieldBackground),
                    contentAlignment = Alignment.Center
                ) {
                    if (post.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = post.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = if (post.type == "LOST") Icons.Default.Search else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = FigmaButtonPurple.copy(alpha = 0.6f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(86.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = post.itemName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            fontFamily = LoraFontFamily,
                            color = Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            fontFamily = LoraFontFamily,
                            fontSize = 12.sp,
                            color = Color.Black.copy(alpha = 0.8f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    if (!isExpanded) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Text(
                                text = "see more",
                                fontFamily = LoraFontFamily,
                                fontSize = 11.sp,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                color = Color.Black,
                                modifier = Modifier.clickable { onToggleExpand() }
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (post.type == "LOST") "Reporter: ${post.posterName}" else "Found by: ${post.posterName}",
                                fontFamily = LoraFontFamily,
                                fontSize = 12.sp,
                                color = Color.Black
                            )
                            Text(
                                text = "Date : $dateStr",
                                fontFamily = LoraFontFamily,
                                fontSize = 12.sp,
                                color = Color.Black
                            )
                            if (post.description.isNotBlank()) {
                                Text(
                                    text = post.description,
                                    fontFamily = LoraFontFamily,
                                    fontSize = 12.sp,
                                    color = Color.Black
                                )
                            }
                            if (post.posterContact.isNotBlank()) {
                                Text(
                                    text = post.posterContact,
                                    fontFamily = LoraFontFamily,
                                    fontSize = 12.sp,
                                    color = Color.Black
                                )
                            }
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (canDelete) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = onEditClick,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = Color.Black,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = onDeleteClick,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = Color.Black,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "see more",
                                fontFamily = LoraFontFamily,
                                fontSize = 11.sp,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                color = Color.Black,
                                modifier = Modifier
                                    .clickable { onToggleExpand() }
                                    .padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaginationBar(
    totalPages: Int,
    currentPage: Int,
    onPageSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (page in 1..totalPages) {
            val isSelected = page == currentPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .size(24.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(if (isSelected) Color(0xFF8C7991) else Color(0xFFC5B6C8).copy(alpha = 0.5f))
                    .clickable { onPageSelected(page) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$page",
                    fontSize = 11.sp,
                    fontFamily = LoraFontFamily,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else Color.Black.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .width(320.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = Color(0xFFFFB300), // Amber yellow
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Are You Sure?",
                    fontFamily = PatrickHandFontFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Do you want to delete this item permanently?",
                    fontFamily = LoraFontFamily,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB5A4BA)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Text(
                            text = "Delete",
                            color = Color.Black,
                            fontFamily = PatrickHandFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB5A4BA)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            color = Color.Black,
                            fontFamily = PatrickHandFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ─── Simple Card for Compatibility ────────────────────────────────────────────
@Composable
fun LostFoundCard(post: LostFoundPost, onClick: () -> Unit) {
    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(post.createdAt))
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(FigmaFieldBackground),
                contentAlignment = Alignment.Center
            ) {
                if (post.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = post.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = if (post.type == "LOST") Icons.Default.Search else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = FigmaButtonPurple,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = post.itemName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = FigmaDarkText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${post.type} ${post.timePeriod.ifBlank { "" }} at ${post.location.ifBlank { "Unknown" }}",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 2
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(dateStr, fontSize = 10.sp, color = Color(0xFF9E9E9E))
                    Text(
                        text = "SEE MORE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = FigmaButtonPurple,
                        modifier = Modifier.clickable { onClick() }
                    )
                }
            }
        }
    }
}

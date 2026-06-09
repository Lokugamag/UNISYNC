package com.example.unisync.ui.home

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unisync.R
import com.example.unisync.data.DataRepository
import com.example.unisync.LocalNavBackStack
import com.example.unisync.AppRoute
import com.example.unisync.theme.*
import com.example.unisync.ui.common.UniSyncBackground

@Composable
fun HomeScreen(
    onNavigateToNotes: () -> Unit,
    onNavigateToLostFound: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit,
    onAddClick: () -> Unit
) {
    val uid = DataRepository.currentUserId ?: ""
    val user by DataRepository.getUserProfile(uid).collectAsState(initial = null)

    LaunchedEffect(user) {
        user?.let {
            DataRepository.populateSampleNotesIfEmpty(it.uid, it.name)
            DataRepository.populateLostFoundIfEmpty(it.uid, it.name)
        }
    }

    UniSyncBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 64.dp)
        ) {
            // ── Top Bar ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 52.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Unisync",
                        fontFamily = PatrickHandFontFamily,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "Your smart study space",
                        fontFamily = LoraFontFamily,
                        fontStyle = FontStyle.Italic,
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                }

                // Profile avatar image (loaded from generated ic_avatar)
                Image(
                    painter = painterResource(id = R.drawable.ic_avatar),
                    contentDescription = "Profile Avatar",
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .clickable { onNavigateToProfile() }
                )
            }

            // Main Content Area (Centered vertically & Scrollable if needed)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── 2×2 Feature Card Grid (Splitted Backgrounds, No Borders) ────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        HomeFeatureCard(
                            title = "Scheduler Manager",
                            subtitle = "View Your Time Table",
                            iconResId = R.drawable.home_ic_scheduler,
                            modifier = Modifier.weight(1f),
                            onClick = { /* TODO: Schedule */ }
                        )
                        HomeFeatureCard(
                            title = "Study Notes Hub",
                            subtitle = "View Shared Notes",
                            iconResId = R.drawable.home_ic_notes,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToNotes
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        HomeFeatureCard(
                            title = "Campus Marketplace",
                            subtitle = "Buy and Sell Items",
                            iconResId = R.drawable.home_ic_marketplace,
                            modifier = Modifier.weight(1f),
                            onClick = { /* TODO: Marketplace */ }
                        )
                        HomeFeatureCard(
                            title = "Lost & Found",
                            subtitle = "Report Lost & Found items",
                            iconResId = R.drawable.home_ic_lost_found,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToLostFound
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                // ── Bottom Info Cards Row ──────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Next Class Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = FigmaCardMediumBg,
                        modifier = Modifier
                            .weight(1f)
                            .height(95.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Next Class:",
                                fontFamily = LoraFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.Black
                            )
                            Spacer(Modifier.height(2.dp))
                            HorizontalDivider(thickness = 0.8.dp, color = Color.Gray.copy(alpha = 0.3f))
                            Spacer(Modifier.weight(1f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "10:00 AM",
                                        fontFamily = LoraFontFamily,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "Statistics",
                                        fontFamily = LoraFontFamily,
                                        fontSize = 10.sp,
                                        color = Color.Black.copy(alpha = 0.8f)
                                    )
                                }
                                Image(
                                    painter = painterResource(id = R.drawable.home_ic_next_class),
                                    contentDescription = "Notification Bell",
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                    }

                    // Hot Deal Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = FigmaCardMediumBg,
                        modifier = Modifier
                            .weight(1f)
                            .height(95.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Hot Deal:",
                                fontFamily = LoraFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.Black
                            )
                            Spacer(Modifier.height(2.dp))
                            HorizontalDivider(thickness = 0.8.dp, color = Color.Gray.copy(alpha = 0.3f))
                            Spacer(Modifier.weight(1f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Laptop For Sale!",
                                        fontFamily = LoraFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        lineHeight = 14.sp
                                    )
                                    Text(
                                        text = "*Conditions apply",
                                        fontFamily = LoraFontFamily,
                                        fontSize = 9.sp,
                                        fontStyle = FontStyle.Italic,
                                        color = Color.Black.copy(alpha = 0.7f)
                                    )
                                }
                                Image(
                                    painter = painterResource(id = R.drawable.home_ic_laptop),
                                    contentDescription = "Laptop",
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(88.dp))
        }

        // ── Bottom Nav Bar ─────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            HomeBottomNav(
                onHomeClick = { },
                onAddClick = onAddClick,
                onNotificationsClick = { }
            )
        }
    }
}

@Composable
fun HomeFeatureCard(
    title: String,
    subtitle: String,
    iconResId: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        modifier = modifier.height(140.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp)),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.3f)
                    .background(FigmaCardHeaderBg),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = iconResId),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(FigmaCardBodyBg)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = LoraFontFamily,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    fontStyle = FontStyle.Italic,
                    fontFamily = LoraFontFamily,
                    color = Color.Black.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

@Composable
fun HomeBottomNav(
    onHomeClick: () -> Unit,
    onAddClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    val backStack = LocalNavBackStack.current

    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (backStack != null) {
                    backStack.clear()
                    backStack.add(AppRoute.Home)
                } else {
                    onHomeClick()
                }
            }) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            // Outlined Box with plus sign icon inside (exact Figma design)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .border(1.2.dp, Color.Black, RoundedCornerShape(6.dp))
                    .clickable {
                        if (backStack != null) {
                            backStack.add(AppRoute.CreateNotes())
                        } else {
                            onAddClick()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(onClick = {
                if (backStack != null) {
                    backStack.add(AppRoute.Notifications)
                } else {
                    onNotificationsClick()
                }
            }) {
                Icon(
                    imageVector = Icons.Default.NotificationsNone,
                    contentDescription = "Notifications",
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

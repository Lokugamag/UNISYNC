package com.example.unisync.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unisync.theme.*
import com.example.unisync.ui.common.UniSyncBackground

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
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
                    text = "NOTIFICATIONS",
                    fontFamily = PatrickHandFontFamily,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            // Separator Line
            Divider(
                color = FigmaCardBorder,
                thickness = 1.5.dp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // Main Container Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = FigmaCardHeaderBg,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, FigmaCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Notification Item 1
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = FigmaCardBodyBg,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, FigmaCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "System Update",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Sample notes and items populated successfully.",
                                    fontSize = 14.sp,
                                    color = Color.Black.copy(alpha = 0.8f)
                                )
                            }
                        }

                        // Notification Item 2
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = FigmaCardBodyBg,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, FigmaCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Campus Hub",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Check the Lost & Found section for newly reported items.",
                                    fontSize = 14.sp,
                                    color = Color.Black.copy(alpha = 0.8f)
                                )
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
                onAddClick = {},
                onNotificationsClick = {}
            )
        }
    }
}

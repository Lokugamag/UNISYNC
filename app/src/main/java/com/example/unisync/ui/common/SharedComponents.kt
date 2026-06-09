package com.example.unisync.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unisync.theme.*

@Composable
fun UniSyncBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FigmaBackground)
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Draw top-right wave outline/blob
            val topPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(w, 0f)
                lineTo(w * 0.3f, 0f)
                quadraticTo(w * 0.4f, h * 0.15f, w * 0.8f, h * 0.2f)
                quadraticTo(w * 0.95f, h * 0.22f, w, h * 0.25f)
                close()
            }
            drawPath(topPath, color = FigmaBlobColor)

            // 2. Draw bottom-left decorative soft blob (from Figma)
            val bottomPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, h)
                lineTo(0f, h * 0.72f)
                cubicTo(
                    w * 0.2f, h * 0.76f,
                    w * 0.35f, h * 0.82f,
                    w * 0.45f, h * 0.88f
                )
                cubicTo(
                    w * 0.55f, h * 0.93f,
                    w * 0.62f, h * 0.98f,
                    w * 0.7f, h
                )
                close()
            }
            drawPath(bottomPath, color = FigmaBlobColor)


        }
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniSyncTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = TextPrimary,
            navigationIconContentColor = LavenderPrimary,
            actionIconContentColor = LavenderPrimary
        )
    )
}

@Composable
fun UniSyncButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPrimary) LavenderPrimary else PinkAccent,
            contentColor = Color.White,
            disabledContainerColor = LavenderLight,
            disabledContentColor = Color.White.copy(alpha = 0.6f)
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun UniSyncTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    maxLines: Int = 1,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        modifier = modifier.fillMaxWidth(),
        minLines = minLines,
        maxLines = maxLines,
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LavenderPrimary,
            unfocusedBorderColor = MediumGray,
            focusedLabelColor = LavenderPrimary,
            unfocusedLabelColor = TextSecondary,
            focusedContainerColor = CardBackground,
            unfocusedContainerColor = LightGray
        )
    )
}

@Composable
fun StatusBadge(type: String) {
    val (color, label) = when (type.uppercase()) {
        "LOST" -> LostRed to "LOST"
        "FOUND" -> FoundGreen to "FOUND"
        "ACTIVE" -> ActivePurple to "ACTIVE"
        "RESOLVED" -> FoundGreen to "RESOLVED"
        else -> MediumGray to type
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.padding(0.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun EmptyState(message: String, icon: ImageVector = Icons.Default.SearchOff) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = LavenderLight,
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = LavenderPrimary)
    }
}

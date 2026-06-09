package com.example.unisync.ui.notes

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.unisync.R
import com.example.unisync.data.DataRepository
import com.example.unisync.data.Note
import com.example.unisync.theme.*
import com.example.unisync.ui.common.UniSyncBackground
import com.example.unisync.ui.home.HomeBottomNav
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

// Helper function to resolve note preview images
@Composable
fun getNotePreviewPainter(note: Note): Painter {
    return when {
        note.fileUrl.contains("periodic_table") -> painterResource(id = R.drawable.note_periodic_table)
        note.fileUrl.contains("graphs") -> painterResource(id = R.drawable.note_graphs)
        note.fileUrl.contains("mind_map") -> painterResource(id = R.drawable.note_mind_map)
        note.fileUrl.contains("final_note") -> painterResource(id = R.drawable.note_final_note)
        note.fileUrl.startsWith("android.resource") -> rememberAsyncImagePainter(note.fileUrl)
        note.fileUrl.isNotBlank() -> rememberAsyncImagePainter(note.fileUrl)
        else -> painterResource(id = R.drawable.note_final_note) // Default fallback
    }
}

@Composable
fun NoteDesignPreview(note: Note, modifier: Modifier = Modifier) {
    val designType = note.fileUrl.substringAfter("design:")
    val (_, spans, paths, images) = deserializeEditorContent(note.content)
    val text = note.content.let {
        try {
            org.json.JSONObject(it).optString("text", "")
        } catch (e: Exception) { "" }
    }

    Box(modifier = modifier.clip(RoundedCornerShape(8.dp))) {
        when (designType) {
            "design1" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .border(2.dp, Color(0xFF81C784), RoundedCornerShape(8.dp))
                )
            }
            "design2" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFFFFECB3), Color(0xFFE1BEE7))
                            )
                        )
                        .border(0.5.dp, Color.LightGray, RoundedCornerShape(8.dp))
                )
            }
            "design3" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .border(0.5.dp, Color.LightGray, RoundedCornerShape(8.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(18.dp)
                            .clip(RoundedCornerShape(topStart = 8.dp))
                            .background(FigmaButtonPurple)
                    )
                }
            }
            else -> {
                // Blank Note
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .border(0.5.dp, Color.LightGray, RoundedCornerShape(8.dp))
                )
            }
        }

        // Miniature ruled lines
        if (designType != "blank") {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gap = 15.dp.toPx()
                var y = 20.dp.toPx()
                while (y < size.height) {
                    drawLine(
                        color = if (designType == "design2") Color.White.copy(alpha = 0.5f) else Color.LightGray.copy(alpha = 0.4f),
                        start = Offset(6.dp.toPx(), y),
                        end = Offset(size.width - 6.dp.toPx(), y),
                        strokeWidth = 0.5.dp.toPx()
                    )
                    y += gap
                }
            }
        }

        // Miniature text
        if (text.isNotEmpty()) {
            val richAnnotated = buildRichAnnotated(text, spans)
            Text(
                text = richAnnotated,
                fontSize = 4.sp,
                lineHeight = 5.sp,
                color = Color.Black,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 22.dp, start = 8.dp, end = 8.dp, bottom = 4.dp)
            )
        }

        // Miniature images
        images.forEach { img ->
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (img.x * 0.35f).roundToInt(),
                            (img.y * 0.35f).roundToInt()
                        )
                    }
                    .size((img.width * 0.35f).dp, (img.height * 0.35f).dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(img.uri),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Miniature drawings
        if (paths.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (path in paths) {
                    for (i in 0 until path.points.size - 1) {
                        drawLine(
                            color = Color(path.colorArgb),
                            start = path.points[i] * 0.35f,
                            end = path.points[i + 1] * 0.35f,
                            strokeWidth = path.strokeWidth * 0.35f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

// ─── Study Note Hub 10 (My Notes list) ───────────────────────────────────────

@Composable
fun MyNotesGridScreen(
    onNoteClick: (String) -> Unit,
    onBack: () -> Unit,
    onHomeClick: () -> Unit,
    onAddClick: () -> Unit
) {
    val uid = DataRepository.currentUserId ?: ""
    val myNotesFlow = remember(uid) { DataRepository.getMyNotes(uid) }
    val myNotes by myNotesFlow.collectAsState(initial = emptyList())

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
                    text = "My Notes",
                    fontFamily = PatrickHandFontFamily,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            if (myNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No notes found. Create or upload some notes first!", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(myNotes, key = { it.noteId }) { note ->
                        MyNoteGridCard(note = note, onOpen = { onNoteClick(note.noteId) })
                    }
                }
            }
        }

        // Bottom Nav Bar
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
fun MyNoteGridCard(note: Note, onOpen: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = note.title.ifEmpty { "Untitled" },
            fontSize = 20.sp,
            fontFamily = PatrickHandFontFamily,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        // Note Image Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            if (note.fileUrl.startsWith("design:")) {
                NoteDesignPreview(
                    note = note,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Image(
                    painter = getNotePreviewPainter(note),
                    contentDescription = note.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onOpen,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = FigmaCardMediumBg,
                contentColor = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(36.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("Open", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}

// ─── Study Note Hub 8 (Edit Notes Screen) ────────────────────────────────────

@Composable
fun EditNotesScreen(
    onEditClick: (Note) -> Unit,
    onNoteClick: (String) -> Unit,
    onBack: () -> Unit,
    onHomeClick: () -> Unit,
    onAddClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uid = DataRepository.currentUserId ?: ""
    val myNotesFlow = remember(uid) { DataRepository.getMyNotes(uid) }
    val myNotes by myNotesFlow.collectAsState(initial = emptyList())

    var noteToDelete by remember { mutableStateOf<Note?>(null) }

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
                    text = "Edit Notes",
                    fontFamily = PatrickHandFontFamily,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            if (myNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No notes found. Create or upload some notes first!", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(myNotes, key = { it.noteId }) { note ->
                        EditNoteGridCard(
                            note = note,
                            onEdit = { onEditClick(note) },
                            onDelete = { noteToDelete = note }
                        )
                    }
                }
            }
        }

        // Bottom Nav Bar
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            HomeBottomNav(
                onHomeClick = onHomeClick,
                onAddClick = onAddClick,
                onNotificationsClick = { }
            )
        }

        // Warning Delete Dialog (matching Hub 14)
        if (noteToDelete != null) {
            AlertDialog(
                onDismissRequest = { noteToDelete = null },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = Color(0xFFFFB300), // Yellow Warning Triangle
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
                            val id = noteToDelete!!.noteId
                            noteToDelete = null
                            scope.launch {
                                DataRepository.deleteNote(id)
                                Toast.makeText(context, "Note deleted successfully!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FigmaButtonPurple),
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { noteToDelete = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Text("Cancel", color = FigmaDarkText, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color.White
            )
        }
    }
}

@Composable
fun EditNoteGridCard(
    note: Note,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = note.title.ifEmpty { "Untitled" },
            fontSize = 20.sp,
            fontFamily = PatrickHandFontFamily,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        // Note Image Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            if (note.fileUrl.startsWith("design:")) {
                NoteDesignPreview(
                    note = note,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Image(
                    painter = getNotePreviewPainter(note),
                    contentDescription = note.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.DriveFileRenameOutline,
                    contentDescription = "Edit",
                    tint = Color.Black,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(Modifier.width(36.dp))
            IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = Color.Black,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

// ─── Study Note Hub 15 (Saved Notes list page) ──────────────────────────────

@Composable
fun SavedNotesScreen(
    onNoteClick: (String) -> Unit,
    onBack: () -> Unit,
    onHomeClick: () -> Unit,
    onAddClick: () -> Unit
) {
    val uid = DataRepository.currentUserId ?: ""
    val savedNotesFlow = remember(uid) { DataRepository.getSavedNotes(uid) }
    val savedNotes by savedNotesFlow.collectAsState(initial = emptyList())

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
                    text = "Saved Notes",
                    fontFamily = PatrickHandFontFamily,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            if (savedNotes.isEmpty()) {
                // Large placeholder card: "No Saved notes"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = FigmaCardMediumBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .border(1.2.dp, FigmaCardBorder, RoundedCornerShape(12.dp))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "No Saved notes",
                                fontFamily = PatrickHandFontFamily,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(savedNotes, key = { it.noteId }) { note ->
                        MyNoteGridCard(note = note, onOpen = { onNoteClick(note.noteId) })
                    }
                }
            }
        }

        // Bottom Nav Bar
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            HomeBottomNav(
                onHomeClick = onHomeClick,
                onAddClick = onAddClick,
                onNotificationsClick = { }
            )
        }
    }
}

package com.example.unisync.ui.notes

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.firstOrNull
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unisync.data.DataRepository
import com.example.unisync.data.Note
import com.example.unisync.theme.*
import com.example.unisync.ui.common.LoadingOverlay
import com.example.unisync.ui.common.UniSyncBackground
import com.example.unisync.ui.home.HomeBottomNav
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import coil.compose.rememberAsyncImagePainter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.File
import java.io.FileOutputStream

// ─── Rich Text Data Classes ────────────────────────────────────────────────────

/** Represents an image inserted on the document canvas */
data class InsertedImage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uri: String,
    val x: Float,
    val y: Float,
    val width: Float = 200f,
    val height: Float = 200f
)

/** Captured state representing all content in the editor for undo/redo */
data class DocumentState(
    val text: TextFieldValue,
    val spans: List<FormatSpan>,
    val paths: List<DrawnPath>,
    val images: List<InsertedImage>
)

data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

/** Represents a formatting span over a range of characters in the editor */
data class FormatSpan(
    val start: Int,
    val end: Int,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val fontSize: Int = 16,
    val colorArgb: Int = android.graphics.Color.BLACK
)

/** One continuous freehand drawing stroke */
data class DrawnPath(
    val points: List<Offset>,
    val colorArgb: Int = android.graphics.Color.BLACK,
    val strokeWidth: Float = 6f
)

private fun FormatSpan.overlaps(s: Int, e: Int) = start < e && end > s

// Build an AnnotatedString from raw text + format spans
fun buildRichAnnotated(text: String, spans: List<FormatSpan>): AnnotatedString {
    if (text.isEmpty()) return AnnotatedString("")
    return buildAnnotatedString {
        append(text)
        for (span in spans) {
            val s = span.start.coerceIn(0, text.length)
            val e = span.end.coerceIn(s, text.length)
            if (s >= e) continue
            val decoration = when {
                span.underline && span.strikethrough ->
                    TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                span.underline -> TextDecoration.Underline
                span.strikethrough -> TextDecoration.LineThrough
                else -> TextDecoration.None
            }
            addStyle(
                SpanStyle(
                    fontWeight    = if (span.bold)   FontWeight.Bold   else FontWeight.Normal,
                    fontStyle     = if (span.italic) FontStyle.Italic  else FontStyle.Normal,
                    textDecoration = decoration,
                    fontSize      = span.fontSize.sp,
                    color         = Color(span.colorArgb)
                ), s, e
            )
        }
    }
}

/** Adjust format spans when text is inserted or deleted */
fun adjustSpansOnTextChange(
    spans: MutableList<FormatSpan>,
    oldText: String,
    newText: String,
    oldSel: TextRange,
    newSel: TextRange,
    activeBold: Boolean,
    activeItalic: Boolean,
    activeUnderline: Boolean,
    activeStrike: Boolean,
    activeFontSize: Int,
    activeColor: Color
) {
    val delta = newText.length - oldText.length
    if (delta == 0) return

    if (!oldSel.collapsed && delta != (newText.length - oldText.length)) {
        // Selection replacement — delete selection then possibly insert
        val delStart = oldSel.min
        val delEnd   = oldSel.max
        val delLen   = delEnd - delStart
        val insLen   = delLen + delta  // net insertion after delete (≥0)

        // 1. Remove/clip spans in deleted range
        val adjusted = spans.mapNotNull { sp ->
            when {
                sp.end   <= delStart -> sp
                sp.start >= delEnd   -> sp.copy(start = sp.start - delLen, end = sp.end - delLen)
                sp.start >= delStart && sp.end <= delEnd -> null
                sp.start < delStart  && sp.end <= delEnd -> sp.copy(end = delStart)
                sp.start >= delStart && sp.end > delEnd  -> sp.copy(start = delStart, end = sp.end - delLen)
                else -> sp.copy(end = sp.end - delLen)
            }
        }.filter { it.start < it.end }
        spans.clear(); spans.addAll(adjusted)

        // 2. Shift & insert for typed chars
        if (insLen > 0) {
            val shifted = spans.map { sp ->
                when {
                    sp.end   <= delStart -> sp
                    sp.start >= delStart -> sp.copy(start = sp.start + insLen, end = sp.end + insLen)
                    else                 -> sp.copy(end = sp.end + insLen)
                }
            }
            spans.clear(); spans.addAll(shifted)
            if (activeBold || activeItalic || activeUnderline || activeStrike ||
                activeFontSize != 16 || activeColor != Color.Black) {
                spans.add(FormatSpan(delStart, delStart + insLen,
                    activeBold, activeItalic, activeUnderline, activeStrike,
                    activeFontSize, activeColor.toArgb()))
            }
        }
        return
    }

    if (delta > 0) {
        // Pure insertion
        val insEnd   = newSel.min
        val insStart = insEnd - delta
        val adjusted = spans.map { sp ->
            when {
                sp.end   <= insStart -> sp
                sp.start >= insStart -> sp.copy(start = sp.start + delta, end = sp.end + delta)
                else                 -> sp.copy(end = sp.end + delta)
            }
        }
        spans.clear(); spans.addAll(adjusted)
        if (activeBold || activeItalic || activeUnderline || activeStrike ||
            activeFontSize != 16 || activeColor != Color.Black) {
            spans.add(FormatSpan(insStart, insEnd,
                activeBold, activeItalic, activeUnderline, activeStrike,
                activeFontSize, activeColor.toArgb()))
        }
    } else {
        // Pure deletion
        val absLen   = -delta
        val delStart = newSel.min
        val delEnd   = delStart + absLen
        val adjusted = spans.mapNotNull { sp ->
            when {
                sp.end   <= delStart -> sp
                sp.start >= delEnd   -> sp.copy(start = sp.start - absLen, end = sp.end - absLen)
                sp.start >= delStart && sp.end <= delEnd -> null
                sp.start < delStart  && sp.end <= delEnd -> sp.copy(end = delStart)
                sp.start >= delStart && sp.end > delEnd  -> sp.copy(start = delStart, end = sp.end - absLen)
                else -> sp.copy(end = sp.end - absLen)
            }
        }.filter { it.start < it.end }
        spans.clear(); spans.addAll(adjusted)
    }
}

/** Apply a format toggle to a selected range */
fun applyFormatToRange(
    spans: MutableList<FormatSpan>,
    start: Int, end: Int,
    bold: Boolean? = null, italic: Boolean? = null,
    underline: Boolean? = null, strikethrough: Boolean? = null,
    fontSize: Int? = null, colorArgb: Int? = null
) {
    if (start >= end) return
    val toRemove = spans.filter { it.overlaps(start, end) }
    val toAdd    = mutableListOf<FormatSpan>()
    for (sp in toRemove) {
        if (sp.start < start) toAdd.add(sp.copy(end = start))
        if (sp.end   > end)   toAdd.add(sp.copy(start = end))
    }
    val base = toRemove.firstOrNull() ?: FormatSpan(start, end)
    spans.removeAll(toRemove.toSet())
    spans.addAll(toAdd)
    spans.add(base.copy(
        start         = start, end = end,
        bold          = bold          ?: base.bold,
        italic        = italic        ?: base.italic,
        underline     = underline     ?: base.underline,
        strikethrough = strikethrough ?: base.strikethrough,
        fontSize      = fontSize      ?: base.fontSize,
        colorArgb     = colorArgb     ?: base.colorArgb
    ))
}

/** Check whether the entire [start,end) range is covered with bold spans */
fun isRangeBold(spans: List<FormatSpan>, start: Int, end: Int): Boolean {
    if (start >= end) return false
    val boldRanges = spans.filter { it.bold && it.overlaps(start, end) }
        .map { maxOf(it.start, start)..minOf(it.end, end) }
        .sortedBy { it.first }
    var covered = start
    for (r in boldRanges) { if (r.first > covered) return false; covered = maxOf(covered, r.last) }
    return covered >= end
}
fun isRangeItalic(spans: List<FormatSpan>, start: Int, end: Int): Boolean {
    if (start >= end) return false
    val rs = spans.filter { it.italic && it.overlaps(start, end) }
        .map { maxOf(it.start, start)..minOf(it.end, end) }.sortedBy { it.first }
    var c = start; for (r in rs) { if (r.first > c) return false; c = maxOf(c, r.last) }; return c >= end
}
fun isRangeUnderline(spans: List<FormatSpan>, start: Int, end: Int): Boolean {
    if (start >= end) return false
    val rs = spans.filter { it.underline && it.overlaps(start, end) }
        .map { maxOf(it.start, start)..minOf(it.end, end) }.sortedBy { it.first }
    var c = start; for (r in rs) { if (r.first > c) return false; c = maxOf(c, r.last) }; return c >= end
}
fun isRangeStrike(spans: List<FormatSpan>, start: Int, end: Int): Boolean {
    if (start >= end) return false
    val rs = spans.filter { it.strikethrough && it.overlaps(start, end) }
        .map { maxOf(it.start, start)..minOf(it.end, end) }.sortedBy { it.first }
    var c = start; for (r in rs) { if (r.first > c) return false; c = maxOf(c, r.last) }; return c >= end
}

// Serialize editor content (text + spans + drawing paths + images + pages) to JSON string
fun serializeEditorContent(
    text: String,
    spans: List<FormatSpan>,
    paths: List<DrawnPath>,
    images: List<InsertedImage>,
    pages: Int = 1
): String {
    val json = JSONObject()
    json.put("text", text)
    json.put("pages", pages)
    val spansArr = JSONArray()
    for (sp in spans) {
        val o = JSONObject()
        o.put("s",  sp.start); o.put("e", sp.end)
        o.put("b",  sp.bold); o.put("i", sp.italic)
        o.put("u",  sp.underline); o.put("st", sp.strikethrough)
        o.put("fs", sp.fontSize); o.put("c", sp.colorArgb)
        spansArr.put(o)
    }
    json.put("spans", spansArr)
    val pathsArr = JSONArray()
    for (path in paths) {
        val po = JSONObject()
        po.put("c",  path.colorArgb); po.put("sw", path.strokeWidth)
        val pts = JSONArray()
        for (pt in path.points) { val p = JSONObject(); p.put("x", pt.x); p.put("y", pt.y); pts.put(p) }
        po.put("pts", pts)
        pathsArr.put(po)
    }
    json.put("paths", pathsArr)
    val imagesArr = JSONArray()
    for (img in images) {
        val io = JSONObject()
        io.put("id", img.id)
        io.put("u", img.uri)
        io.put("x", img.x)
        io.put("y", img.y)
        io.put("w", img.width)
        io.put("h", img.height)
        imagesArr.put(io)
    }
    json.put("images", imagesArr)
    return json.toString()
}

// Deserialize JSON back into editor state
fun deserializeEditorContent(content: String): Quadruple<String, List<FormatSpan>, List<DrawnPath>, List<InsertedImage>> {
    if (content.isBlank()) return Quadruple("", emptyList(), emptyList(), emptyList())
    return try {
        val json     = JSONObject(content)
        val text     = json.optString("text", "")
        val spansArr = json.optJSONArray("spans") ?: JSONArray()
        val spans    = mutableListOf<FormatSpan>()
        for (i in 0 until spansArr.length()) {
            val o = spansArr.getJSONObject(i)
            spans.add(FormatSpan(
                start         = o.optInt("s"), end = o.optInt("e"),
                bold          = o.optBoolean("b"), italic = o.optBoolean("i"),
                underline     = o.optBoolean("u"), strikethrough = o.optBoolean("st"),
                fontSize      = o.optInt("fs", 16),
                colorArgb     = o.optInt("c", android.graphics.Color.BLACK)
            ))
        }
        val pathsArr = json.optJSONArray("paths") ?: JSONArray()
        val paths    = mutableListOf<DrawnPath>()
        for (i in 0 until pathsArr.length()) {
            val po     = pathsArr.getJSONObject(i)
            val pts    = po.optJSONArray("pts") ?: JSONArray()
            val points = (0 until pts.length()).map {
                val p = pts.getJSONObject(it)
                Offset(p.optDouble("x").toFloat(), p.optDouble("y").toFloat())
            }
            paths.add(DrawnPath(points, po.optInt("c", android.graphics.Color.BLACK), po.optDouble("sw", 6.0).toFloat()))
        }
        val imagesArr = json.optJSONArray("images") ?: JSONArray()
        val images    = mutableListOf<InsertedImage>()
        for (i in 0 until imagesArr.length()) {
            val io = imagesArr.getJSONObject(i)
            images.add(InsertedImage(
                id     = io.optString("id", java.util.UUID.randomUUID().toString()),
                uri    = io.optString("u", ""),
                x      = io.optDouble("x", 0.0).toFloat(),
                y      = io.optDouble("y", 0.0).toFloat(),
                width  = io.optDouble("w", 200.0).toFloat(),
                height = io.optDouble("h", 200.0).toFloat()
            ))
        }
        Quadruple(text, spans, paths, images)
    } catch (e: Exception) { Quadruple("", emptyList(), emptyList(), emptyList()) }
}


@Composable
fun SmallNoteCard(
    note: Note,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier
            .width(100.dp)
            .height(130.dp)
            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = note.title.ifEmpty { "Untitled" },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = FigmaDarkText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            if (note.fileUrl.startsWith("design:")) {
                NoteDesignPreview(
                    note = note,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                )
            } else {
                androidx.compose.foundation.Image(
                    painter = getNotePreviewPainter(note),
                    contentDescription = note.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                )
            }
        }
    }
}

// ─── Study Note Hub 3 & 4 (Create Notes screen) ──────────────────────────────

@Composable
fun CreateNotesScreen(
    initialSection: String? = null,
    onCreateDesignClick: (String) -> Unit,
    onNoteClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val uid = DataRepository.currentUserId ?: ""
    val recentNotesFlow = remember(uid) { DataRepository.getMyNotes(uid) }
    val favoriteNotesFlow = remember(uid) { DataRepository.getSavedNotes(uid) }
    val recentNotes by recentNotesFlow.collectAsState(initial = emptyList())
    val favoriteNotes by favoriteNotesFlow.collectAsState(initial = emptyList())

    var isNewNoteExpanded by remember { mutableStateOf(initialSection == null || initialSection == "new") }
    var isRecentNotesExpanded by remember { mutableStateOf(initialSection == "recent") }
    var isFavoriteExpanded by remember { mutableStateOf(initialSection == "favorite") }

    LaunchedEffect(initialSection) {
        if (initialSection != null) {
            isNewNoteExpanded = initialSection == "new"
            isRecentNotesExpanded = initialSection == "recent"
            isFavoriteExpanded = initialSection == "favorite"
        }
    }

    val screenTitle = when {
        isRecentNotesExpanded -> "Recent Notes"
        isFavoriteExpanded -> "Favorite Notes"
        else -> "Create Notes"
    }

    var showMoreDesigns by remember { mutableStateOf(false) }

    UniSyncBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 72.dp) // Leave space for bottom nav
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 52.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Back",
                    tint = Color.Black,
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onBack() }
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = screenTitle,
                    fontFamily = PatrickHandFontFamily,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            HorizontalDivider(
                color = Color.Black.copy(alpha = 0.15f),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(Modifier.height(16.dp))

            if (!showMoreDesigns) {
                // 1. Collapsible New Note section
                CollapsibleSection(
                    title = "New Note",
                    isExpanded = isNewNoteExpanded,
                    onToggle = {
                        isNewNoteExpanded = !isNewNoteExpanded
                        if (isNewNoteExpanded) {
                            isRecentNotesExpanded = false
                            isFavoriteExpanded = false
                        }
                    }
                ) {
                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                        Spacer(Modifier.height(8.dp))

                        // Grid of designs (Row 1: Blank Note, Design 1)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Blank Note
                            DesignTemplateCard(
                                title = "Blank note",
                                modifier = Modifier.weight(1f),
                                onClick = { onCreateDesignClick("blank") }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.White)
                                )
                            }

                            // Design 1 (Green lined border)
                            DesignTemplateCard(
                                title = "Design 1",
                                modifier = Modifier.weight(1f),
                                onClick = { onCreateDesignClick("design1") }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.White)
                                        .border(3.dp, Color(0xFF81C784))
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("Notes", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                        Spacer(Modifier.height(4.dp))
                                        repeat(5) {
                                            HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Grid of designs (Row 2: Design 2, More Designs >>)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Design 2 (Pastel gradient)
                            DesignTemplateCard(
                                title = "Design 2",
                                modifier = Modifier.weight(1f),
                                onClick = { onCreateDesignClick("design2") }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color(0xFFFFECB3), Color(0xFFE1BEE7))
                                            )
                                        )
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("Notes", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FigmaDarkText)
                                        Spacer(Modifier.height(4.dp))
                                        repeat(5) {
                                            HorizontalDivider(color = Color.White.copy(0.7f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                                        }
                                    }
                                }
                            }

                            // "More Designs >>" clickable button box that matches the grid cell height
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(140.dp)
                                    .clickable { showMoreDesigns = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "More Designs >>",
                                    fontFamily = PatrickHandFontFamily,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FigmaDarkText
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 2. Collapsible Recent Notes
                CollapsibleSection(
                    title = "Recent Notes",
                    isExpanded = isRecentNotesExpanded,
                    onToggle = {
                        isRecentNotesExpanded = !isRecentNotesExpanded
                        if (isRecentNotesExpanded) {
                            isNewNoteExpanded = false
                            isFavoriteExpanded = false
                        }
                    }
                ) {
                    if (recentNotes.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = FigmaFieldBackground.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No recent notes",
                                    fontSize = 14.sp,
                                    color = FigmaDarkText
                                )
                            }
                        }
                    } else {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(recentNotes) { note ->
                                SmallNoteCard(note = note, onClick = { onNoteClick(note.noteId) })
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 3. Collapsible Favorite Notes
                CollapsibleSection(
                    title = "Favorite",
                    isExpanded = isFavoriteExpanded,
                    onToggle = {
                        isFavoriteExpanded = !isFavoriteExpanded
                        if (isFavoriteExpanded) {
                            isNewNoteExpanded = false
                            isRecentNotesExpanded = false
                        }
                    }
                ) {
                    if (favoriteNotes.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = FigmaFieldBackground.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No favorite notes",
                                    fontSize = 14.sp,
                                    color = FigmaDarkText
                                )
                            }
                        }
                    } else {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(favoriteNotes) { note ->
                                SmallNoteCard(note = note, onClick = { onNoteClick(note.noteId) })
                            }
                        }
                    }
                }
            } else {
                // showMoreDesigns == true (Figma Image 3)
                // "Show less" text button at top
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showMoreDesigns = false }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "<< Show less",
                        fontFamily = PatrickHandFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = FigmaDarkText
                    )
                }

                // Grid of designs (Row 1: Design 2, Design 3)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Design 2
                    DesignTemplateCard(
                        title = "Design 2",
                        modifier = Modifier.weight(1f),
                        onClick = { onCreateDesignClick("design2") }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFFFFECB3), Color(0xFFE1BEE7))
                                    )
                                )
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Notes", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FigmaDarkText)
                                Spacer(Modifier.height(4.dp))
                                repeat(5) {
                                    HorizontalDivider(color = Color.White.copy(0.7f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                        }
                    }

                    // Design 3
                    DesignTemplateCard(
                        title = "Design 3",
                        modifier = Modifier.weight(1f),
                        onClick = { onCreateDesignClick("design3") }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("NOTES", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF2E7D32))
                                Spacer(Modifier.height(4.dp))
                                repeat(5) {
                                    HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(topStart = 16.dp))
                                    .background(FigmaButtonPurple)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Row 2: Create Design (+ card), No more designs placeholder
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Create Note Card
                    DesignTemplateCard(
                        title = "Create Note",
                        modifier = Modifier.weight(1f),
                        onClick = { onCreateDesignClick("blank") }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, null, tint = FigmaDarkText, modifier = Modifier.size(36.dp))
                        }
                    }

                    // No more designs placeholder
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No more designs",
                            fontFamily = PatrickHandFontFamily,
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        // Home Navigation Bar at bottom
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            HomeBottomNav(
                onHomeClick = { onBack() },
                onAddClick = { onCreateDesignClick("blank") },
                onNotificationsClick = { }
            )
        }
    }
}

@Composable
fun CollapsibleSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            onClick = onToggle,
            color = FigmaCardMediumBg,
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    fontFamily = PatrickHandFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            content()
        }
    }
}

@Composable
fun DesignTemplateCard(
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    templateContent: @Composable BoxScope.() -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                templateContent()
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = FigmaDarkText
        )
    }
}
fun renderCanvasToBitmap(
    context: android.content.Context,
    designType: String,
    title: String,
    text: String,
    spans: List<FormatSpan>,
    paths: List<DrawnPath>,
    images: List<InsertedImage>,
    pageCount: Int,
    targetWidth: Int = 1080,
    targetHeight: Int = 1530
): Bitmap {
    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight * pageCount, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    
    // Draw content page by page
    for (page in 0 until pageCount) {
        canvas.save()
        canvas.translate(0f, (page * targetHeight).toFloat())
        
        // 1. Draw Template Background
        when (designType) {
            "design1" -> {
                canvas.drawColor(AndroidColor.WHITE)
                val borderPaint = Paint().apply {
                    color = AndroidColor.parseColor("#81C784")
                    style = Paint.Style.STROKE
                    strokeWidth = 12f
                }
                canvas.drawRoundRect(6f, 6f, targetWidth - 6f, targetHeight - 6f, 20f, 20f, borderPaint)
                
                if (page == 0) {
                    val textPaint = Paint().apply {
                        color = AndroidColor.parseColor("#2E7D32")
                        textSize = 48f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    canvas.drawText("Notes", 40f, 80f, textPaint)
                }
            }
            "design2" -> {
                val colors = intArrayOf(
                    AndroidColor.parseColor("#FFFFECB3"),
                    AndroidColor.parseColor("#E1BEE7")
                )
                val gradient = LinearGradient(0f, 0f, 0f, targetHeight.toFloat(), colors, null, Shader.TileMode.CLAMP)
                val bgPaint = Paint().apply { shader = gradient }
                canvas.drawRect(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat(), bgPaint)
                
                if (page == 0) {
                    val textPaint = Paint().apply {
                        color = AndroidColor.parseColor("#1A1A1A")
                        textSize = 48f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    canvas.drawText("Notes", 40f, 80f, textPaint)
                }
            }
            "design3" -> {
                canvas.drawColor(AndroidColor.WHITE)
                if (page == 0) {
                    val textPaint = Paint().apply {
                        color = AndroidColor.parseColor("#2E7D32")
                        textSize = 48f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    canvas.drawText("NOTES", 40f, 80f, textPaint)
                    
                    // Purple corner triangle at bottom-end
                    val path = AndroidPath().apply {
                        moveTo(targetWidth - 120f, targetHeight.toFloat())
                        lineTo(targetWidth.toFloat(), targetHeight - 120f)
                        lineTo(targetWidth.toFloat(), targetHeight.toFloat())
                        close()
                    }
                    val trianglePaint = Paint().apply {
                        color = AndroidColor.parseColor("#8A3D73")
                        style = Paint.Style.FILL
                    }
                    canvas.drawPath(path, trianglePaint)
                }
            }
            else -> {
                canvas.drawColor(AndroidColor.WHITE)
            }
        }
        
        // 2. Draw Ruled Lines
        if (designType != "blank") {
            val linePaint = Paint().apply {
                color = if (designType == "design2") AndroidColor.parseColor("#66FFFFFF") else AndroidColor.parseColor("#22000000")
                strokeWidth = 2f
            }
            var y = 140f
            while (y < targetHeight - 30f) {
                canvas.drawLine(40f, y, targetWidth - 40f, y, linePaint)
                y += 60f
            }
        }
        
        canvas.restore()
    }
    
    // Scale factors from screen canvas coordinates (width, height) to our target output width/height
    val srcWidth = 800f
    val srcHeight = 1240f
    
    val scaleX = targetWidth.toFloat() / srcWidth
    val scaleY = (targetHeight * pageCount).toFloat() / (srcHeight * pageCount)
    
    // 3. Draw Text Layer
    val textPaint = TextPaint().apply {
        color = AndroidColor.BLACK
        textSize = 32f
        isAntiAlias = true
    }
    
    val spannable = android.text.SpannableStringBuilder(text)
    for (span in spans) {
        val start = span.start.coerceIn(0, text.length)
        val end = span.end.coerceIn(start, text.length)
        if (start >= end) continue
        
        var style = Typeface.NORMAL
        if (span.bold && span.italic) style = Typeface.BOLD_ITALIC
        else if (span.bold) style = Typeface.BOLD
        else if (span.italic) style = Typeface.ITALIC
        
        spannable.setSpan(
            android.text.style.StyleSpan(style),
            start, end,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            android.text.style.AbsoluteSizeSpan((span.fontSize * 1.8).toInt()),
            start, end,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            android.text.style.ForegroundColorSpan(span.colorArgb),
            start, end,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        if (span.underline) {
            spannable.setSpan(android.text.style.UnderlineSpan(), start, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        if (span.strikethrough) {
            spannable.setSpan(android.text.style.StrikethroughSpan(), start, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
    
    val textLeft = 40f
    val textTop = 140f
    val textWidth = targetWidth - 80f
    
    val staticLayout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        StaticLayout.Builder.obtain(spannable, 0, spannable.length, textPaint, textWidth.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.8f)
            .build()
    } else {
        StaticLayout(spannable, textPaint, textWidth.toInt(), Layout.Alignment.ALIGN_NORMAL, 1.8f, 0f, false)
    }
    
    canvas.save()
    canvas.translate(textLeft, textTop)
    staticLayout.draw(canvas)
    canvas.restore()
    
    // 4. Draw Committed Paths
    val pathPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    for (path in paths) {
        if (path.points.size < 2) continue
        pathPaint.color = path.colorArgb
        pathPaint.strokeWidth = path.strokeWidth * scaleX
        val drawingPath = AndroidPath()
        drawingPath.moveTo(path.points[0].x * scaleX, path.points[0].y * scaleY)
        for (i in 1 until path.points.size) {
            drawingPath.lineTo(path.points[i].x * scaleX, path.points[i].y * scaleY)
        }
        canvas.drawPath(drawingPath, pathPaint)
    }
    
    // 5. Draw Inserted Images
    for (img in images) {
        try {
            val imgUri = Uri.parse(img.uri)
            val inputStream = context.contentResolver.openInputStream(imgUri)
            val imgBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (imgBitmap != null) {
                val left = img.x * scaleX
                val top = img.y * scaleY
                val right = left + (img.width * 2.5f * scaleX)
                val bottom = top + (img.height * 2.5f * scaleY)
                canvas.drawBitmap(imgBitmap, null, RectF(left, top, right, bottom), null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    return bitmap
}

fun saveBitmapToGallery(context: android.content.Context, bitmap: Bitmap, title: String): Boolean {
    val resolver = context.contentResolver
    val filename = "${title.replace(" ", "_").lowercase()}_${System.currentTimeMillis()}.jpg"
    
    val values = android.content.ContentValues().apply {
        put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES)
            put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    
    val collection = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val imageUri = resolver.insert(collection, values) ?: return false
    
    return try {
        val outputStream = resolver.openOutputStream(imageUri) ?: return false
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            values.clear()
            values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(imageUri, values, null, null)
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun renderCanvasToPdf(
    context: android.content.Context,
    designType: String,
    title: String,
    text: String,
    spans: List<FormatSpan>,
    paths: List<DrawnPath>,
    images: List<InsertedImage>,
    pageCount: Int
): File? {
    val targetWidth = 595 // A4 width
    val targetHeight = 842 // A4 height
    val pdfDocument = PdfDocument()
    
    for (page in 0 until pageCount) {
        val pageInfo = PdfDocument.PageInfo.Builder(targetWidth, targetHeight, page + 1).create()
        val pdfPage = pdfDocument.startPage(pageInfo)
        val canvas = pdfPage.canvas
        
        // ─── 1. Template Background ───
        when (designType) {
            "design1" -> {
                canvas.drawColor(AndroidColor.WHITE)
                val borderPaint = Paint().apply {
                    color = AndroidColor.parseColor("#81C784")
                    style = Paint.Style.STROKE
                    strokeWidth = 6f
                }
                canvas.drawRoundRect(3f, 3f, targetWidth - 3f, targetHeight - 3f, 10f, 10f, borderPaint)
                
                if (page == 0) {
                    val textPaint = Paint().apply {
                        color = AndroidColor.parseColor("#2E7D32")
                        textSize = 24f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    canvas.drawText("Notes", 20f, 40f, textPaint)
                }
            }
            "design2" -> {
                val colors = intArrayOf(
                    AndroidColor.parseColor("#FFFFECB3"),
                    AndroidColor.parseColor("#E1BEE7")
                )
                val gradient = LinearGradient(0f, 0f, 0f, targetHeight.toFloat(), colors, null, Shader.TileMode.CLAMP)
                val bgPaint = Paint().apply { shader = gradient }
                canvas.drawRect(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat(), bgPaint)
                
                if (page == 0) {
                    val textPaint = Paint().apply {
                        color = AndroidColor.parseColor("#1A1A1A")
                        textSize = 24f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    canvas.drawText("Notes", 20f, 40f, textPaint)
                }
            }
            "design3" -> {
                canvas.drawColor(AndroidColor.WHITE)
                if (page == 0) {
                    val textPaint = Paint().apply {
                        color = AndroidColor.parseColor("#2E7D32")
                        textSize = 24f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    canvas.drawText("NOTES", 20f, 40f, textPaint)
                    
                    // Corner triangle
                    val path = AndroidPath().apply {
                        moveTo(targetWidth - 60f, targetHeight.toFloat())
                        lineTo(targetWidth.toFloat(), targetHeight - 60f)
                        lineTo(targetWidth.toFloat(), targetHeight.toFloat())
                        close()
                    }
                    val trianglePaint = Paint().apply {
                        color = AndroidColor.parseColor("#8A3D73")
                        style = Paint.Style.FILL
                    }
                    canvas.drawPath(path, trianglePaint)
                }
            }
            else -> {
                canvas.drawColor(AndroidColor.WHITE)
            }
        }
        
        // ─── 2. Ruled Lines ───
        if (designType != "blank") {
            val linePaint = Paint().apply {
                color = if (designType == "design2") AndroidColor.parseColor("#66FFFFFF") else AndroidColor.parseColor("#22000000")
                strokeWidth = 1f
            }
            var y = 70f
            while (y < targetHeight - 15f) {
                canvas.drawLine(20f, y, targetWidth - 20f, y, linePaint)
                y += 30f
            }
        }
        
        val srcWidth = 800f
        val srcHeight = 1240f
        val scaleX = targetWidth.toFloat() / srcWidth
        val scaleY = targetHeight.toFloat() / srcHeight
        
        // ─── 3. Draw Text with clip ───
        val textPaint = TextPaint().apply {
            color = AndroidColor.BLACK
            textSize = 18f
            isAntiAlias = true
        }
        
        val spannable = android.text.SpannableStringBuilder(text)
        for (span in spans) {
            val start = span.start.coerceIn(0, text.length)
            val end = span.end.coerceIn(start, text.length)
            if (start >= end) continue
            var style = Typeface.NORMAL
            if (span.bold && span.italic) style = Typeface.BOLD_ITALIC
            else if (span.bold) style = Typeface.BOLD
            else if (span.italic) style = Typeface.ITALIC
            
            spannable.setSpan(android.text.style.StyleSpan(style), start, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(android.text.style.AbsoluteSizeSpan(span.fontSize), start, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(android.text.style.ForegroundColorSpan(span.colorArgb), start, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (span.underline) spannable.setSpan(android.text.style.UnderlineSpan(), start, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (span.strikethrough) spannable.setSpan(android.text.style.StrikethroughSpan(), start, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        
        val textLeft = 20f
        val textTop = 70f
        val textWidth = targetWidth - 40f
        
        val staticLayout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(spannable, 0, spannable.length, textPaint, textWidth.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.8f)
                .build()
        } else {
            StaticLayout(spannable, textPaint, textWidth.toInt(), Layout.Alignment.ALIGN_NORMAL, 1.8f, 0f, false)
        }
        
        canvas.save()
        canvas.clipRect(20f, 50f, targetWidth - 20f, targetHeight - 20f)
        canvas.translate(textLeft, textTop - (page * targetHeight))
        staticLayout.draw(canvas)
        canvas.restore()
        
        // ─── 4. Draw Committed Paths ───
        canvas.save()
        canvas.clipRect(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat())
        val pathPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        for (path in paths) {
            if (path.points.size < 2) continue
            val pageStartY = page * srcHeight
            val pageEndY = (page + 1) * srcHeight
            val hasPointsOnPage = path.points.any { it.y in pageStartY..pageEndY }
            if (!hasPointsOnPage) continue
            
            pathPaint.color = path.colorArgb
            pathPaint.strokeWidth = path.strokeWidth * scaleX
            val drawingPath = AndroidPath()
            
            val pt0 = path.points[0]
            drawingPath.moveTo(pt0.x * scaleX, (pt0.y - pageStartY) * scaleY)
            for (i in 1 until path.points.size) {
                val pt = path.points[i]
                drawingPath.lineTo(pt.x * scaleX, (pt.y - pageStartY) * scaleY)
            }
            canvas.drawPath(drawingPath, pathPaint)
        }
        canvas.restore()
        
        // ─── 5. Draw Inserted Images ───
        canvas.save()
        canvas.clipRect(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat())
        for (img in images) {
            val pageStartY = page * srcHeight
            val pageEndY = (page + 1) * srcHeight
            if (img.y + img.height * 2f < pageStartY || img.y > pageEndY) continue
            
            try {
                val imgUri = Uri.parse(img.uri)
                val inputStream = context.contentResolver.openInputStream(imgUri)
                val imgBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (imgBitmap != null) {
                    val left = img.x * scaleX
                    val top = (img.y - pageStartY) * scaleY
                    val right = left + (img.width * 1.5f * scaleX)
                    val bottom = top + (img.height * 1.5f * scaleY)
                    canvas.drawBitmap(imgBitmap, null, RectF(left, top, right, bottom), null)
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
        canvas.restore()
        
        pdfDocument.finishPage(pdfPage)
    }
    
    return try {
        val pdfFile = File(context.cacheDir, "${title.replace(" ", "_").lowercase()}_note.pdf")
        val fos = FileOutputStream(pdfFile)
        pdfDocument.writeTo(fos)
        fos.close()
        pdfDocument.close()
        pdfFile
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun CreateDesignScreen(
    designType: String,
    noteId: String? = null,
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val scope   = rememberCoroutineScope()
    val context = LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current

    // ── User / Firebase ───────────────────────────────────────────────────────
    val uid      = DataRepository.currentUserId ?: ""
    val user by DataRepository.getUserProfile(uid).collectAsState(initial = null)

    // Pre-generate a Firestore document ID (or use the one passed for editing)
    val editingNoteId   = remember { noteId ?: java.util.UUID.randomUUID().toString() }
    var isCreatedInDb   by remember { mutableStateOf(noteId != null) }
    var hasUnsaved      by remember { mutableStateOf(false) }
    var savedIndicator  by remember { mutableStateOf(false) }
    var isLoading       by remember { mutableStateOf(false) }
    var isFavorite      by remember { mutableStateOf(false) }

    // ── Editor text state ─────────────────────────────────────────────────────
    var tfValue         by remember { mutableStateOf(TextFieldValue("")) }
    val formatSpans     = remember { mutableStateListOf<FormatSpan>() }
    val insertedImages  = remember { mutableStateListOf<InsertedImage>() }
    val undoStack       = remember { mutableStateListOf<DocumentState>() }
    val redoStack       = remember { mutableStateListOf<DocumentState>() }

    // ── Active format state ───────────────────────────────────────────────────
    var activeBold      by remember { mutableStateOf(false) }
    var activeItalic    by remember { mutableStateOf(false) }
    var activeUnderline by remember { mutableStateOf(false) }
    var activeStrike    by remember { mutableStateOf(false) }
    var activeFontSize  by remember { mutableStateOf(16) }
    var activeColor     by remember { mutableStateOf(Color.Black) }
    var textAlign       by remember { mutableStateOf(TextAlign.Start) }
    var bulletMode      by remember { mutableStateOf(false) }
    var numberedMode    by remember { mutableStateOf(false) }

    // ── Drawing state ─────────────────────────────────────────────────────────
    var isDrawingMode   by remember { mutableStateOf(false) }
    var isEraserActive  by remember { mutableStateOf(false) }
    val drawnPaths      = remember { mutableStateListOf<DrawnPath>() }
    val currentPoints   = remember { mutableStateListOf<Offset>() }
    var drawColor       by remember { mutableStateOf(Color.Black) }
    var drawStroke      by remember { mutableStateOf(6f) }

    // ── UI dialog state ───────────────────────────────────────────────────────
    var showFontPicker  by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showLayersDialog by remember { mutableStateOf(false) }
    var showGrid        by remember { mutableStateOf(false) }
    var documentTitle   by remember { mutableStateOf("Document 1") }

    // ── Focus Requester for keyboard ──────────────────────────────────────────
    val focusRequester = remember { FocusRequester() }

    // ── Trash bin drag-to-delete states ───────────────────────────────────────
    var isDraggingImage by remember { mutableStateOf(false) }
    var isHoveringTrashBin by remember { mutableStateOf(false) }
    var isPointerDownOnImage by remember { mutableStateOf(false) }
    var canvasWidth     by remember { mutableStateOf(0f) }
    var canvasHeight    by remember { mutableStateOf(0f) }
    var pageCount       by remember { mutableStateOf(1) }
    val canvasScrollState = rememberScrollState()

    // Load existing note if editing
    LaunchedEffect(noteId, uid) {
        if (uid.isNotEmpty()) {
            isFavorite = DataRepository.isNoteSaved(uid, editingNoteId)
        }
        if (noteId != null) {
            isLoading = true
            val noteObj = DataRepository.getNoteById(noteId).firstOrNull()
            if (noteObj != null) {
                documentTitle = noteObj.title
                val (t, sp, pt, img) = deserializeEditorContent(noteObj.content)
                tfValue = TextFieldValue(t)
                formatSpans.clear()
                formatSpans.addAll(sp)
                drawnPaths.clear()
                drawnPaths.addAll(pt)
                insertedImages.clear()
                insertedImages.addAll(img)
                pageCount = try { JSONObject(noteObj.content).optInt("pages", 1) } catch(e: Exception) { 1 }
            }
            isLoading = false
        }
    }

    // ── Computed display annotated string ─────────────────────────────────────
    val displayAnnotated = remember(tfValue.text, formatSpans.toList()) {
        buildRichAnnotated(tfValue.text, formatSpans.toList())
    }

    // ── Active format derived from cursor position ────────────────────────────
    val sel = tfValue.selection
    val selBold      = if (sel.collapsed) activeBold      else isRangeBold(formatSpans, sel.min, sel.max)
    val selItalic    = if (sel.collapsed) activeItalic    else isRangeItalic(formatSpans, sel.min, sel.max)
    val selUnderline = if (sel.collapsed) activeUnderline else isRangeUnderline(formatSpans, sel.min, sel.max)
    val selStrike    = if (sel.collapsed) activeStrike    else isRangeStrike(formatSpans, sel.min, sel.max)

    // Synchronize active style cursor formats as the cursor moves
    LaunchedEffect(tfValue.selection) {
        val cursor = tfValue.selection.min
        if (tfValue.selection.collapsed && cursor > 0) {
            val idx = cursor - 1
            val span = formatSpans.find { idx >= it.start && idx < it.end }
            if (span != null) {
                activeBold = span.bold
                activeItalic = span.italic
                activeUnderline = span.underline
                activeStrike = span.strikethrough
                activeFontSize = span.fontSize
                activeColor = Color(span.colorArgb)
            } else {
                activeBold = false
                activeItalic = false
                activeUnderline = false
                activeStrike = false
                activeFontSize = 16
                activeColor = Color.Black
            }
        }
    }

    // ── Auto-save to Firestore (2 s debounce) ─────────────────────────────────
    LaunchedEffect(tfValue.text, drawnPaths.size, insertedImages.size, pageCount, hasUnsaved) {
        if (!hasUnsaved) return@LaunchedEffect
        delay(2000L)
        if (!hasUnsaved) return@LaunchedEffect
        val content = serializeEditorContent(tfValue.text, formatSpans.toList(), drawnPaths.toList(), insertedImages.toList(), pageCount)
        val result = if (!isCreatedInDb) {
            val note = Note(
                noteId           = editingNoteId,
                title            = documentTitle,
                subject          = "Design Study",
                topic            = "Canvas Sketch",
                description      = "Created in the UniSync design editor.",
                fileUrl          = "design:$designType",
                fileType         = "design",
                fileName          = "${documentTitle.replace(" ", "_").lowercase()}.unisync",
                uploaderName     = user?.name ?: "Student",
                uploaderStudentId = user?.studentId ?: "",
                content          = content
            )
            DataRepository.createDesignNote(note)
        } else {
            DataRepository.updateDocumentContent(editingNoteId, content, documentTitle)
        }
        if (result.isSuccess) {
            isCreatedInDb  = true
            hasUnsaved     = false
            savedIndicator = true
            delay(1500L)
            savedIndicator = false
        }
    }

    // ── Helper: push undo state ───────────────────────────────────────────────
    fun pushUndo() {
        if (undoStack.size >= 50) undoStack.removeAt(0)
        undoStack.add(
            DocumentState(
                text = tfValue,
                spans = formatSpans.toList(),
                paths = drawnPaths.toList(),
                images = insertedImages.toList()
            )
        )
        redoStack.clear()
    }

    // ── Helper: List toggle operations ────────────────────────────────────────
    fun toggleBulletOnCurrentLine() {
        val text = tfValue.text
        val sel = tfValue.selection
        val lines = text.split("\n").toMutableList()
        var charCount = 0
        var targetLineIndex = -1
        for (i in lines.indices) {
            val lineStart = charCount
            val lineEnd = charCount + lines[i].length
            if (sel.min in lineStart..lineEnd) {
                targetLineIndex = i
                break
            }
            charCount += lines[i].length + 1
        }
        if (targetLineIndex != -1) {
            val currentLine = lines[targetLineIndex]
            val isBullet = currentLine.startsWith("• ")
            val isNumber = currentLine.matches(Regex("^\\d+\\.\\s.*")) || currentLine.matches(Regex("^\\d+\\.\\s*"))
            pushUndo()
            val newLine = when {
                isBullet -> currentLine.substring(2)
                isNumber -> {
                    val parts = currentLine.split(" ", limit = 2)
                    if (parts.size > 1) parts[1] else ""
                }
                else -> "• $currentLine"
            }
            lines[targetLineIndex] = newLine
            val newText = lines.joinToString("\n")
            val diff = newLine.length - currentLine.length
            val newCursor = (sel.min + diff).coerceIn(0, newText.length)
            tfValue = TextFieldValue(newText, TextRange(newCursor))
            hasUnsaved = true
        }
    }

    fun toggleNumberOnCurrentLine() {
        val text = tfValue.text
        val sel = tfValue.selection
        val lines = text.split("\n").toMutableList()
        var charCount = 0
        var targetLineIndex = -1
        for (i in lines.indices) {
            val lineStart = charCount
            val lineEnd = charCount + lines[i].length
            if (sel.min in lineStart..lineEnd) {
                targetLineIndex = i
                break
            }
            charCount += lines[i].length + 1
        }
        if (targetLineIndex != -1) {
            val currentLine = lines[targetLineIndex]
            val isBullet = currentLine.startsWith("• ")
            val isNumber = currentLine.matches(Regex("^\\d+\\.\\s.*")) || currentLine.matches(Regex("^\\d+\\.\\s*"))
            pushUndo()
            val newLine = when {
                isNumber -> {
                    val parts = currentLine.split(" ", limit = 2)
                    if (parts.size > 1) parts[1] else ""
                }
                isBullet -> {
                    "1. " + currentLine.substring(2)
                }
                else -> {
                    val prevLineNum = if (targetLineIndex > 0) {
                        val prevLine = lines[targetLineIndex - 1]
                        val match = Regex("^(\\d+)\\.\\s.*").find(prevLine) ?: Regex("^(\\d+)\\.\\s*").find(prevLine)
                        match?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    } else 0
                    "${prevLineNum + 1}. $currentLine"
                }
            }
            lines[targetLineIndex] = newLine
            val newText = lines.joinToString("\n")
            val diff = newLine.length - currentLine.length
            val newCursor = (sel.min + diff).coerceIn(0, newText.length)
            tfValue = TextFieldValue(newText, TextRange(newCursor))
            hasUnsaved = true
        }
    }

    // ── Image Picker Launcher ─────────────────────────────────────────────────
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pushUndo()
            insertedImages.add(
                InsertedImage(
                    uri = uri.toString(),
                    x = 100f,
                    y = 150f,
                    width = 180f,
                    height = 180f
                )
            )
            hasUnsaved = true
        }
    }

    UniSyncBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 72.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 52.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Back",
                    tint = Color.Black,
                    modifier = Modifier.size(36.dp).clickable { onBack() }
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Create Note",
                    fontFamily = PatrickHandFontFamily,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {
                    if (uid.isNotEmpty()) {
                        scope.launch {
                            if (!isCreatedInDb) {
                                val content = serializeEditorContent(tfValue.text, formatSpans.toList(), drawnPaths.toList(), insertedImages.toList(), pageCount)
                                val note = Note(
                                    noteId            = editingNoteId,
                                    title             = documentTitle,
                                    subject           = "Design Study",
                                    topic             = "Canvas Sketch",
                                    description       = "Created in the UniSync design editor.",
                                    fileUrl           = "design:$designType",
                                    fileType          = "design",
                                    fileName          = "${documentTitle.replace(" ", "_").lowercase()}.unisync",
                                    uploaderName      = user?.name ?: "Student",
                                    uploaderStudentId = user?.studentId ?: "",
                                    content           = content
                                )
                                val dbRes = DataRepository.createDesignNote(note)
                                if (dbRes.isSuccess) {
                                    isCreatedInDb = true
                                    hasUnsaved = false
                                }
                            }
                            
                            val res = DataRepository.toggleSaveNote(uid, editingNoteId)
                            res.onSuccess { saved ->
                                isFavorite = saved
                                if (saved) {
                                    Toast.makeText(context, "Added to favorites", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show()
                                }
                            }.onFailure {
                                Toast.makeText(context, "Failed to update favorites: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color.Red else Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                // Saved indicator badge
                AnimatedVisibility(
                    visible = savedIndicator,
                    enter = fadeIn() + slideInHorizontally { it },
                    exit  = fadeOut() + slideOutHorizontally { it }
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF4CAF50)
                    ) {
                        Text(
                            text = "✓ Saved",
                            fontSize = 11.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color.Black.copy(alpha = 0.15f), thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 24.dp))

            Spacer(Modifier.height(8.dp))

            // ── Editable document title ───────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                contentAlignment = Alignment.Center) {
                BasicTextField(
                    value = documentTitle,
                    onValueChange = { documentTitle = it; hasUnsaved = true },
                    textStyle = TextStyle(
                        fontFamily  = ComfortaaFontFamily,
                        fontSize    = 14.sp,
                        fontWeight  = FontWeight.SemiBold,
                        color       = Color.Black,
                        textAlign   = TextAlign.Center
                    ),
                    singleLine = true,
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier
                                .border(1.dp, FigmaButtonPurple.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) { inner() }
                    },
                    modifier = Modifier.width(200.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Canvas + Toolbars ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Top
            ) {
                // ── Left Toolbar ──────────────────────────────────────────────
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = FigmaFieldBackground,
                    shadowElevation = 2.dp,
                    modifier = Modifier.width(40.dp).fillMaxHeight()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // T – text mode (toggle off drawing)
                        ToolbarIconToggle(Icons.Default.TextFields, "Text", !isDrawingMode) {
                            isDrawingMode = false
                            isEraserActive = false
                            scope.launch {
                                delay(100)
                                focusRequester.requestFocus()
                            }
                        }
                        // Align – cycle left/center/right
                        ToolbarIconToggle(
                            icon = when (textAlign) {
                                TextAlign.Center -> Icons.Default.FormatAlignCenter
                                TextAlign.End    -> Icons.AutoMirrored.Filled.FormatAlignRight
                                else             -> Icons.AutoMirrored.Filled.FormatAlignLeft
                            },
                            description = "Align",
                            isActive = false
                        ) {
                            textAlign = when (textAlign) {
                                TextAlign.Start  -> TextAlign.Center
                                TextAlign.Center -> TextAlign.End
                                else             -> TextAlign.Start
                            }
                        }
                        // Image
                        ToolbarIconToggle(Icons.Default.Image, "Image", false) {
                            imagePickerLauncher.launch("image/*")
                        }
                        // Brush / Drawing mode
                        ToolbarIconToggle(Icons.Default.Brush, "Draw", isDrawingMode && !isEraserActive) {
                            isDrawingMode = true
                            isEraserActive = false
                        }
                        // Eraser (for drawing)
                        ToolbarIconToggle(Icons.Default.AutoFixOff, "Eraser", isDrawingMode && isEraserActive) {
                            isDrawingMode = true
                            isEraserActive = true
                        }
                        // Grid background
                        ToolbarIconToggle(Icons.Default.GridOn, "Grid", showGrid) {
                            showGrid = !showGrid
                        }
                        // Undo
                        ToolbarIconToggle(Icons.AutoMirrored.Filled.Undo, "Undo", false) {
                            if (undoStack.isNotEmpty()) {
                                val currentState = DocumentState(
                                    text = tfValue,
                                    spans = formatSpans.toList(),
                                    paths = drawnPaths.toList(),
                                    images = insertedImages.toList()
                                )
                                redoStack.add(currentState)
                                val previousState = undoStack.removeLast()
                                tfValue = previousState.text
                                formatSpans.clear()
                                formatSpans.addAll(previousState.spans)
                                drawnPaths.clear()
                                drawnPaths.addAll(previousState.paths)
                                insertedImages.clear()
                                insertedImages.addAll(previousState.images)
                                hasUnsaved = true
                            }
                        }
                        // Redo
                        ToolbarIconToggle(Icons.AutoMirrored.Filled.Redo, "Redo", false) {
                            if (redoStack.isNotEmpty()) {
                                val currentState = DocumentState(
                                    text = tfValue,
                                    spans = formatSpans.toList(),
                                    paths = drawnPaths.toList(),
                                    images = insertedImages.toList()
                                )
                                undoStack.add(currentState)
                                val nextState = redoStack.removeLast()
                                tfValue = nextState.text
                                formatSpans.clear()
                                formatSpans.addAll(nextState.spans)
                                drawnPaths.clear()
                                drawnPaths.addAll(nextState.paths)
                                insertedImages.clear()
                                insertedImages.addAll(nextState.images)
                                hasUnsaved = true
                            }
                        }
                        // Settings
                        ToolbarIconToggle(Icons.Default.Settings, "Settings", showRenameDialog) {
                            showRenameDialog = true
                        }
                        // Layers
                        ToolbarIconToggle(Icons.Default.Layers, "Layers", showLayersDialog) {
                            showLayersDialog = true
                        }
                        // Add Page Fallback
                        ToolbarIconToggle(Icons.Default.NoteAdd, "Add Page", false) {
                            pushUndo()
                            pageCount += 1
                            hasUnsaved = true
                            scope.launch {
                                delay(100L)
                                canvasScrollState.animateScrollTo(canvasScrollState.maxValue)
                            }
                            Toast.makeText(context, "Page $pageCount added", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                // ── Document Canvas ───────────────────────────────────────────
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(canvasScrollState, enabled = !isPointerDownOnImage && !isDrawingMode)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((620 * pageCount).dp)
                                .onSizeChanged { size ->
                                    canvasWidth = size.width.toFloat()
                                    canvasHeight = size.height.toFloat()
                                }
                        ) {
                            // Page Separators
                            repeat(pageCount - 1) { i ->
                                val pageY = (620 * (i + 1)).dp
                                HorizontalDivider(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset(y = pageY),
                                    color = Color.LightGray,
                                    thickness = 1.dp
                                )
                            }

                        // 1. Design template background
                        when (designType) {
                            "design1" -> Box(
                                modifier = Modifier.fillMaxSize().background(Color.White)
                                    .border(4.dp, Color(0xFF81C784), RoundedCornerShape(14.dp))
                            ) {
                                Text("Notes", fontFamily = PatrickHandFontFamily, fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32),
                                    modifier = Modifier.padding(14.dp))
                            }
                            "design2" -> Box(
                                modifier = Modifier.fillMaxSize().background(
                                    Brush.verticalGradient(listOf(Color(0xFFFFECB3), Color(0xFFE1BEE7))))
                            ) {
                                Text("Notes", fontFamily = PatrickHandFontFamily, fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold, color = FigmaDarkText,
                                    modifier = Modifier.padding(14.dp))
                            }
                            "design3" -> Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
                                Text("NOTES", fontFamily = PatrickHandFontFamily, fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold, color = Color(0xFF2E7D32),
                                    modifier = Modifier.padding(14.dp))
                                Box(modifier = Modifier.align(Alignment.BottomEnd).size(52.dp)
                                    .clip(RoundedCornerShape(topStart = 26.dp)).background(FigmaButtonPurple))
                            }
                            else -> Box(modifier = Modifier.fillMaxSize().background(Color.White))
                        }

                        // 2. Horizontal ruled lines
                        if (designType != "blank") {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val gap   = 30.dp.toPx()
                                val startY = 58.dp.toPx()
                                var y = startY
                                while (y < size.height - 10.dp.toPx()) {
                                    drawLine(
                                        color       = if (designType == "design2") Color.White.copy(0.6f) else Color.LightGray.copy(0.5f),
                                        start       = Offset(14.dp.toPx(), y),
                                        end         = Offset(size.width - 14.dp.toPx(), y),
                                        strokeWidth = 0.8.dp.toPx()
                                    )
                                    y += gap
                                }
                            }
                        }

                        // 3. Grid overlay (optional)
                        if (showGrid) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val gap = 28.dp.toPx()
                                var x = gap; while (x < size.width)  { drawLine(Color.LightGray.copy(0.3f), Offset(x,0f), Offset(x,size.height), 0.5.dp.toPx()); x+=gap }
                                var y = gap; while (y < size.height) { drawLine(Color.LightGray.copy(0.3f), Offset(0f,y), Offset(size.width,y), 0.5.dp.toPx()); y+=gap }
                            }
                        }

                        // 4. Text editor (disabled in drawing mode)
                        if (!isDrawingMode) {
                            BasicTextField(
                                value = TextFieldValue(
                                    annotatedString = displayAnnotated,
                                    selection       = tfValue.selection
                                ),
                                onValueChange = { newVal ->
                                    val oldText = tfValue.text
                                    val oldSel  = tfValue.selection
                                    val spansMut = formatSpans.toMutableList()

                                    // Handle bullet/numbered prefix insertion on newline
                                    val newText = newVal.text
                                    val insertedNL = newText.length > oldText.length &&
                                        newText.substring(
                                            (newVal.selection.min - (newText.length - oldText.length))
                                                .coerceAtLeast(0),
                                            newVal.selection.min.coerceAtLeast(0)
                                        ).contains('\n')
                                    val finalText: String
                                    val finalSel: TextRange
                                    if (insertedNL && bulletMode) {
                                        val insertPos = newVal.selection.min
                                        // If previous line was just a bullet, end bullet mode
                                        val prevLineIndex = oldText.lastIndexOf('\n')
                                        val prevLine = if (prevLineIndex != -1) oldText.substring(prevLineIndex + 1) else oldText
                                        if (prevLine == "• ") {
                                            finalText = oldText.substring(0, prevLineIndex.coerceAtLeast(0)) + "\n"
                                            finalSel = TextRange(finalText.length)
                                        } else {
                                            val prefix = "• "
                                            finalText = newText.substring(0, insertPos) + prefix + newText.substring(insertPos)
                                            finalSel  = TextRange(insertPos + prefix.length)
                                        }
                                    } else if (insertedNL && numberedMode) {
                                        val insertPos = newVal.selection.min
                                        // If previous line was just a number prefix, end numbered mode
                                        val prevLineIndex = oldText.lastIndexOf('\n')
                                        val prevLine = if (prevLineIndex != -1) oldText.substring(prevLineIndex + 1) else oldText
                                        if (prevLine.matches(Regex("^\\d+\\.\\s*"))) {
                                            finalText = oldText.substring(0, prevLineIndex.coerceAtLeast(0)) + "\n"
                                            finalSel = TextRange(finalText.length)
                                        } else {
                                            val lineNum   = oldText.count { it == '\n' } + 2
                                            val prefix    = "$lineNum. "
                                            finalText = newText.substring(0, insertPos) + prefix + newText.substring(insertPos)
                                            finalSel  = TextRange(insertPos + prefix.length)
                                        }
                                    } else {
                                        finalText = newText
                                        finalSel  = newVal.selection
                                    }

                                    pushUndo()
                                    adjustSpansOnTextChange(spansMut, oldText, finalText, oldSel, finalSel,
                                        activeBold, activeItalic, activeUnderline, activeStrike,
                                        activeFontSize, activeColor)
                                    formatSpans.clear(); formatSpans.addAll(spansMut)
                                    tfValue    = TextFieldValue(finalText, finalSel)
                                    hasUnsaved = true
                                },
                                textStyle = TextStyle(
                                    fontSize  = activeFontSize.sp,
                                    textAlign = textAlign,
                                    color     = Color.Black,
                                    lineHeight = (activeFontSize * 1.8).sp
                                ),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .focusRequester(focusRequester)
                                    .padding(top = 56.dp, start = 14.dp, end = 14.dp, bottom = 14.dp)
                            )
                        } else {
                            // In drawing mode, show text as non-interactive
                            Text(
                                text = displayAnnotated,
                                style = TextStyle(
                                    fontSize  = activeFontSize.sp,
                                    textAlign = textAlign,
                                    color     = Color.Black,
                                    lineHeight = (activeFontSize * 1.8).sp
                                ),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 56.dp, start = 14.dp, end = 14.dp, bottom = 14.dp)
                            )
                        }

                        // 5. Drawing layer (pointer capture + canvas render)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (isDrawingMode) {
                                        Modifier.pointerInput(isEraserActive) {
                                            detectDragGestures(
                                                onDragStart = { offset ->
                                                    if (!isEraserActive) {
                                                        currentPoints.clear()
                                                        currentPoints.add(offset)
                                                    }
                                                },
                                                onDrag = { change, _ ->
                                                    change.consume()
                                                    if (isEraserActive) {
                                                        val touchPoint = change.position
                                                        val toRemove = drawnPaths.filter { path ->
                                                            path.points.any { pt ->
                                                                val dx = pt.x - touchPoint.x
                                                                val dy = pt.y - touchPoint.y
                                                                (dx * dx + dy * dy) < 900f // 30px touch radius
                                                            }
                                                        }
                                                        if (toRemove.isNotEmpty()) {
                                                            pushUndo()
                                                            drawnPaths.removeAll(toRemove)
                                                            hasUnsaved = true
                                                        }
                                                    } else {
                                                        currentPoints.add(change.position)
                                                    }
                                                },
                                                onDragEnd = {
                                                    if (!isEraserActive && currentPoints.size > 1) {
                                                        pushUndo()
                                                        drawnPaths.add(DrawnPath(currentPoints.toList(), drawColor.toArgb(), drawStroke))
                                                        hasUnsaved = true
                                                    }
                                                    currentPoints.clear()
                                                },
                                                onDragCancel = { currentPoints.clear() }
                                            )
                                        }
                                    } else Modifier
                                )
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Committed paths
                                for (path in drawnPaths) {
                                    for (i in 0 until path.points.size - 1) {
                                        drawLine(
                                            color       = Color(path.colorArgb),
                                            start       = path.points[i],
                                            end         = path.points[i + 1],
                                            strokeWidth = path.strokeWidth,
                                            cap         = StrokeCap.Round
                                        )
                                    }
                                }
                                // Current in-progress stroke
                                for (i in 0 until currentPoints.size - 1) {
                                    drawLine(
                                        color       = drawColor,
                                        start       = currentPoints[i],
                                        end         = currentPoints[i + 1],
                                        strokeWidth = drawStroke,
                                        cap         = StrokeCap.Round
                                    )
                                }
                            }
                        }

                        // 4.5. Inserted images layer
                        var selectedImageId by remember { mutableStateOf<String?>(null) }
                        insertedImages.forEach { img ->
                            val isSelected = selectedImageId == img.id
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(img.x.roundToInt(), img.y.roundToInt()) }
                                    .size(img.width.dp, img.height.dp)
                                    .then(
                                        if (isSelected) Modifier.border(2.dp, FigmaButtonPurple, RoundedCornerShape(4.dp))
                                        else Modifier
                                    )
                                    .pointerInput(img.id) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                awaitFirstDown(requireUnconsumed = false)
                                                isPointerDownOnImage = true
                                                do {
                                                    val event = awaitPointerEvent()
                                                    val anyPressed = event.changes.any { it.pressed }
                                                } while (anyPressed)
                                                isPointerDownOnImage = false
                                            }
                                        }
                                    }
                                    .clickable(
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        selectedImageId = if (isSelected) null else img.id
                                    }
                                    .pointerInput(img.id) {
                                        detectDragGestures(
                                            onDragStart = {
                                                pushUndo()
                                                isDraggingImage = true
                                                selectedImageId = img.id
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val idx = insertedImages.indexOfFirst { it.id == img.id }
                                                if (idx != -1) {
                                                    val currentImg = insertedImages[idx]
                                                    val nextX = currentImg.x + dragAmount.x
                                                    val nextY = currentImg.y + dragAmount.y
                                                    insertedImages[idx] = currentImg.copy(
                                                        x = nextX,
                                                        y = nextY
                                                    )
                                                    
                                                    // Calculate proximity to trash bin
                                                    val imgW = with(density) { currentImg.width.dp.toPx() }
                                                    val imgH = with(density) { currentImg.height.dp.toPx() }
                                                    val imgCenterX = nextX + imgW / 2f
                                                    val imgCenterY = nextY + imgH / 2f
                                                    
                                                    val binCenterX = canvasWidth / 2f
                                                    val binCenterY = canvasHeight - with(density) { 60.dp.toPx() }
                                                    
                                                    val distSq = (imgCenterX - binCenterX) * (imgCenterX - binCenterX) + 
                                                                 (imgCenterY - binCenterY) * (imgCenterY - binCenterY)
                                                    val binRadiusPx = with(density) { 60.dp.toPx() }
                                                    isHoveringTrashBin = distSq < (binRadiusPx * binRadiusPx)
                                                    hasUnsaved = true
                                                }
                                            },
                                            onDragEnd = {
                                                isDraggingImage = false
                                                if (isHoveringTrashBin) {
                                                    pushUndo()
                                                    insertedImages.remove(img)
                                                    selectedImageId = null
                                                    isHoveringTrashBin = false
                                                    Toast.makeText(context, "Image deleted", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            onDragCancel = {
                                                isDraggingImage = false
                                                isHoveringTrashBin = false
                                            }
                                        )
                                    }
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = rememberAsyncImagePainter(img.uri),
                                    contentDescription = "Inserted Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isSelected) {
                                    // Delete button (❌)
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(20.dp)
                                            .background(Color.Red, CircleShape)
                                            .clickable {
                                                pushUndo()
                                                insertedImages.remove(img)
                                                selectedImageId = null
                                                hasUnsaved = true
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                    // Resize handle (Aspect Ratio drag)
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(20.dp)
                                            .background(FigmaButtonPurple, CircleShape)
                                            .pointerInput(img.id) {
                                                detectDragGestures(
                                                    onDragStart = {
                                                        pushUndo()
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        val idx = insertedImages.indexOfFirst { it.id == img.id }
                                                        if (idx != -1) {
                                                            val currentImg = insertedImages[idx]
                                                            insertedImages[idx] = currentImg.copy(
                                                                width = (currentImg.width + dragAmount.x / 3f).coerceAtLeast(50f),
                                                                height = (currentImg.height + dragAmount.y / 3f).coerceAtLeast(50f)
                                                            )
                                                            hasUnsaved = true
                                                        }
                                                    }
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.AspectRatio, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }

                        // 4.6. Floating Trash Bin Overlay
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isDraggingImage,
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut(),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 24.dp)
                        ) {
                            val binSize = if (isHoveringTrashBin) 68.dp else 56.dp
                            val binBg = if (isHoveringTrashBin) Color(0xFFD32F2F) else Color.DarkGray.copy(alpha = 0.85f)
                            
                            Box(
                                modifier = Modifier
                                    .size(binSize)
                                    .clip(CircleShape)
                                    .background(binBg)
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Bin",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                        }
                    }
                }
            }
                }

                // ── Right Toolbar ─────────────────────────────────────────────
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = FigmaFieldBackground,
                    shadowElevation = 2.dp,
                    modifier = Modifier.width(40.dp).fillMaxHeight()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Aa – Font size picker
                        Box(
                            modifier = Modifier.size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (showFontPicker) FigmaButtonPurple.copy(0.25f) else Color.Transparent)
                                .clickable { showFontPicker = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Aa", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FigmaDarkText)
                        }

                        // Bullet list
                        ToolbarIconToggle(Icons.AutoMirrored.Filled.FormatListBulleted, "Bullets", bulletMode) {
                            bulletMode   = !bulletMode
                            numberedMode = false
                            toggleBulletOnCurrentLine()
                        }

                        // Numbered list
                        ToolbarIconToggle(Icons.Default.FormatListNumbered, "Numbers", numberedMode) {
                            numberedMode = !numberedMode
                            bulletMode   = false
                            toggleNumberOnCurrentLine()
                        }

                        // Bold – B
                        Box(
                            modifier = Modifier.size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selBold) FigmaButtonPurple.copy(0.25f) else Color.Transparent)
                                .clickable {
                                    if (!sel.collapsed) {
                                        val newBold = !isRangeBold(formatSpans, sel.min, sel.max)
                                        applyFormatToRange(formatSpans, sel.min, sel.max, bold = newBold)
                                        activeBold = newBold
                                    } else { activeBold = !activeBold }
                                    hasUnsaved = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("B", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                                color = if (selBold) FigmaButtonPurple else FigmaDarkText)
                        }

                        // Italic – I
                        Box(
                            modifier = Modifier.size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selItalic) FigmaButtonPurple.copy(0.25f) else Color.Transparent)
                                .clickable {
                                    if (!sel.collapsed) {
                                        val newItalic = !isRangeItalic(formatSpans, sel.min, sel.max)
                                        applyFormatToRange(formatSpans, sel.min, sel.max, italic = newItalic)
                                        activeItalic = newItalic
                                    } else { activeItalic = !activeItalic }
                                    hasUnsaved = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("I", fontSize = 15.sp,
                                fontWeight  = FontWeight.Bold,
                                fontStyle   = FontStyle.Italic,
                                color       = if (selItalic) FigmaButtonPurple else FigmaDarkText)
                        }

                        // Underline – U
                        Box(
                            modifier = Modifier.size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selUnderline) FigmaButtonPurple.copy(0.25f) else Color.Transparent)
                                .clickable {
                                    if (!sel.collapsed) {
                                        val newUnder = !isRangeUnderline(formatSpans, sel.min, sel.max)
                                        applyFormatToRange(formatSpans, sel.min, sel.max, underline = newUnder)
                                        activeUnderline = newUnder
                                    } else { activeUnderline = !activeUnderline }
                                    hasUnsaved = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("U", fontSize = 15.sp,
                                fontWeight    = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline,
                                color         = if (selUnderline) FigmaButtonPurple else FigmaDarkText)
                        }

                        // Strikethrough – S
                        Box(
                            modifier = Modifier.size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selStrike) FigmaButtonPurple.copy(0.25f) else Color.Transparent)
                                .clickable {
                                    if (!sel.collapsed) {
                                        val newStrike = !isRangeStrike(formatSpans, sel.min, sel.max)
                                        applyFormatToRange(formatSpans, sel.min, sel.max, strikethrough = newStrike)
                                        activeStrike = newStrike
                                    } else { activeStrike = !activeStrike }
                                    hasUnsaved = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("S", fontSize = 15.sp,
                                fontWeight     = FontWeight.Bold,
                                textDecoration = TextDecoration.LineThrough,
                                color          = if (selStrike) FigmaButtonPurple else FigmaDarkText)
                        }

                        // Text color – A (opens color picker)
                        Box(
                            modifier = Modifier.size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (showColorPicker) FigmaButtonPurple.copy(0.25f) else Color.Transparent)
                                .clickable { showColorPicker = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("A", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = activeColor)
                                    Box(modifier = Modifier.width(18.dp).height(3.dp)
                                        .clip(RoundedCornerShape(2.dp)).background(activeColor))
                            }
                        }

                        // Draw color picker (when in drawing mode – shows 4 stroke colors)
                        if (isDrawingMode) {
                            listOf(Color.Black, Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047)).forEach { c ->
                                Box(
                                    modifier = Modifier.size(22.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(2.dp, if (drawColor == c) Color.White else Color.Transparent, CircleShape)
                                        .clickable { drawColor = c }
                                )
                            }
                            // Stroke width
                            listOf(4f, 8f, 14f).forEach { sw ->
                                Box(
                                    modifier = Modifier.size(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (drawStroke == sw) FigmaButtonPurple.copy(0.3f) else Color.Transparent)
                                        .clickable { drawStroke = sw },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(modifier = Modifier.width(16.dp).height(sw.dp.coerceAtMost(8.dp))
                                        .clip(RoundedCornerShape(2.dp)).background(FigmaDarkText))
                                }
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        // Share
                        ToolbarIconToggle(Icons.Default.Share, "Share", false) {
                            val shareText = buildString {
                                appendLine("--- UniSync Document: $documentTitle ---")
                                if (tfValue.text.isNotEmpty()) {
                                    appendLine(tfValue.text)
                                }
                                if (drawnPaths.isNotEmpty()) {
                                    appendLine("[Contains ${drawnPaths.size} drawing strokes]")
                                }
                                if (insertedImages.isNotEmpty()) {
                                    appendLine("[Contains ${insertedImages.size} inserted images]")
                                }
                                appendLine("\nCreated with UniSync App")
                            }
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share note"))
                        }

                        // Download (save note image to gallery)
                        ToolbarIconToggle(Icons.Default.Download, "Download", false) {
                            isLoading = true
                            scope.launch {
                                val bitmap = with(kotlinx.coroutines.Dispatchers.IO) {
                                    renderCanvasToBitmap(
                                        context        = context,
                                        designType     = designType,
                                        title          = documentTitle,
                                        text           = tfValue.text,
                                        spans          = formatSpans.toList(),
                                        paths          = drawnPaths.toList(),
                                        images         = insertedImages.toList(),
                                        pageCount      = pageCount
                                    )
                                }
                                val success = saveBitmapToGallery(context, bitmap, documentTitle)
                                isLoading = false
                                if (success) {
                                    Toast.makeText(context, "Saved to phone gallery!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Failed to save to gallery.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        // Explicit Save (💾)
                        ToolbarIconToggle(Icons.Default.Save, "Save", false) {
                            isLoading  = true
                            scope.launch {
                                val content = serializeEditorContent(tfValue.text, formatSpans.toList(), drawnPaths.toList(), insertedImages.toList(), pageCount)
                                val result = if (!isCreatedInDb) {
                                    val note = Note(
                                        noteId            = editingNoteId,
                                        title             = documentTitle,
                                        subject           = "Design Study",
                                        topic             = "Canvas Sketch",
                                        description       = "Created in the UniSync design editor.",
                                        fileUrl           = "design:$designType",
                                        fileType          = "design",
                                        fileName          = "${documentTitle.replace(" ", "_").lowercase()}.unisync",
                                        uploaderName      = user?.name ?: "Student",
                                        uploaderStudentId = user?.studentId ?: "",
                                        content           = content
                                    )
                                    DataRepository.createDesignNote(note)
                                } else {
                                    DataRepository.updateDocumentContent(editingNoteId, content, documentTitle)
                                }
                                
                                val pdfFile = with(kotlinx.coroutines.Dispatchers.IO) {
                                    renderCanvasToPdf(
                                        context    = context,
                                        designType = designType,
                                        title      = documentTitle,
                                        text       = tfValue.text,
                                        spans      = formatSpans.toList(),
                                        paths      = drawnPaths.toList(),
                                        images     = insertedImages.toList(),
                                        pageCount  = pageCount
                                    )
                                }
                                var pdfSaved = false
                                if (pdfFile != null) {
                                    val pdfUri = Uri.fromFile(pdfFile)
                                    val cloudRes = DataRepository.uploadFile(
                                        pdfUri,
                                        "notes/$editingNoteId/${documentTitle.replace(" ", "_").lowercase()}.pdf"
                                    )
                                    if (cloudRes.isSuccess) {
                                        pdfSaved = true
                                    }
                                    downloadFileToPublicFolder(context, pdfUri)
                                }
                                
                                isLoading = false
                                if (result.isSuccess) {
                                    isCreatedInDb  = true
                                    hasUnsaved     = false
                                    savedIndicator = true
                                    delay(1500L)
                                    savedIndicator = false
                                    val msg = if (pdfSaved) "Design and PDF saved to Drive!" else "Design saved successfully!"
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    onSuccess()
                                } else {
                                    val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Unknown Firestore error"
                                    Toast.makeText(context, "Save failed: $errorMsg", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // + Add new page button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable {
                        pushUndo()
                        pageCount += 1
                        hasUnsaved = true
                        scope.launch {
                            delay(100L)
                            canvasScrollState.animateScrollTo(canvasScrollState.maxValue)
                        }
                        Toast.makeText(context, "Page $pageCount added", Toast.LENGTH_SHORT).show()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("+ Add new page", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FigmaDarkText)
            }
        }

        // Home Navigation Bar
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            HomeBottomNav(onHomeClick = {}, onAddClick = {}, onNotificationsClick = {})
        }

        // ── Font Size Picker Dialog ───────────────────────────────────────────
        if (showFontPicker) {
            AlertDialog(
                onDismissRequest = { showFontPicker = false },
                title = { Text("Font Size", fontWeight = FontWeight.Bold) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Current: ${activeFontSize}sp", fontSize = 14.sp, color = Color.Gray)
                        Spacer(Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(onClick = { if (activeFontSize > 8) activeFontSize -= 2 }) {
                                Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("$activeFontSize", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                                color = FigmaButtonPurple)
                            FilledTonalButton(onClick = { if (activeFontSize < 48) activeFontSize += 2 }) {
                                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        // Quick size buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(10, 12, 14, 16, 18, 22, 28, 36).forEach { size ->
                                Surface(
                                    onClick = { activeFontSize = size },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (activeFontSize == size) FigmaButtonPurple else FigmaFieldBackground,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("$size", fontSize = 10.sp,
                                            color = if (activeFontSize == size) Color.White else FigmaDarkText)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        // Apply to selection
                        if (!sel.collapsed) {
                            FilledTonalButton(onClick = {
                                applyFormatToRange(formatSpans, sel.min, sel.max, fontSize = activeFontSize)
                                hasUnsaved = true
                            }) { Text("Apply to selection") }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFontPicker = false }) { Text("Done", color = FigmaButtonPurple) }
                }
            )
        }

        // ── Text Color Picker Dialog ──────────────────────────────────────────
        if (showColorPicker) {
            val colorOptions = listOf(
                Color.Black       to "Black",
                Color(0xFFE53935) to "Red",
                Color(0xFF1E88E5) to "Blue",
                Color(0xFF43A047) to "Green",
                Color(0xFF8A3D73) to "Purple",
                Color(0xFFF57C00) to "Orange",
                Color(0xFF00ACC1) to "Teal",
                Color(0xFF8D6E63) to "Brown"
            )
            AlertDialog(
                onDismissRequest = { showColorPicker = false },
                title = { Text("Text Color", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        // Color grid
                        for (row in colorOptions.chunked(4)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(vertical = 6.dp)) {
                                row.forEach { (c, name) ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier.size(40.dp).clip(CircleShape)
                                                .background(c)
                                                .border(3.dp,
                                                    if (activeColor == c) Color.White else Color.Transparent,
                                                    CircleShape)
                                                .clickable {
                                                    activeColor = c
                                                    if (!sel.collapsed) {
                                                        applyFormatToRange(formatSpans, sel.min, sel.max,
                                                            colorArgb = c.toArgb())
                                                        hasUnsaved = true
                                                    }
                                                }
                                        )
                                        Text(name, fontSize = 9.sp, color = Color.Gray,
                                            modifier = Modifier.padding(top = 2.dp))
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showColorPicker = false }) { Text("Done", color = FigmaButtonPurple) }
                }
            )
        }

        // ── Rename Dialog ────────────────────────────────────────────────────
        if (showRenameDialog) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = false },
                title = { Text("Rename Document", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = documentTitle,
                        onValueChange = { documentTitle = it; hasUnsaved = true },
                        label = { Text("Title") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showRenameDialog = false }) { Text("Done", color = FigmaButtonPurple) }
                }
            )
        }

        // ── Layers Dialog ────────────────────────────────────────────────────
        if (showLayersDialog) {
            AlertDialog(
                onDismissRequest = { showLayersDialog = false },
                title = { Text("Document Layers", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("• Background Layer (${designType.replaceFirstChar { it.uppercase() }})", fontSize = 14.sp)
                        Text("• Ruled Lines Layer (Ruled Line Canvas)", fontSize = 14.sp)
                        Text("• Text Editor Layer (${tfValue.text.length} characters)", fontSize = 14.sp)
                        Text("• Drawing Layer (${drawnPaths.size} strokes)", fontSize = 14.sp)
                        Text("• Inserted Images Layer (${insertedImages.size} images)", fontSize = 14.sp)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLayersDialog = false }) { Text("Close", color = FigmaButtonPurple) }
                }
            )
        }

        if (isLoading) LoadingOverlay()
    }
}

// ── Toolbar Icon with active-state highlight ──────────────────────────────────

@Composable
fun ToolbarIconToggle(
    icon: ImageVector,
    description: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) FigmaButtonPurple.copy(alpha = 0.25f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = description,
            tint               = if (isActive) FigmaButtonPurple else FigmaDarkText,
            modifier           = Modifier.size(20.dp)
        )
    }
}

@Composable
fun ToolbarIcon(icon: ImageVector, description: String, onClick: () -> Unit = {}) {
    Icon(
        imageVector        = icon,
        contentDescription = description,
        tint               = FigmaDarkText,
        modifier           = Modifier.size(24.dp).clickable(onClick = onClick)
    )
}



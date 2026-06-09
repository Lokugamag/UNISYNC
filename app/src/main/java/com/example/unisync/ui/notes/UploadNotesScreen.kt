package com.example.unisync.ui.notes

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.graphics.RectF
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.unisync.data.DataRepository
import com.example.unisync.data.Note
import com.example.unisync.theme.*
import com.example.unisync.ui.common.LoadingOverlay
import com.example.unisync.ui.common.UniSyncBackground
import com.example.unisync.ui.home.HomeBottomNav
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

// Helper function to extract filename from Uri
fun getFileName(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "document"
}

// Convert Bitmap to cache file Uri
fun bitmapToUri(context: Context, bitmap: Bitmap): Uri? {
    val cachePath = File(context.cacheDir, "camera_images")
    cachePath.mkdirs()
    val file = File(cachePath, "captured_${System.currentTimeMillis()}.jpg")
    return try {
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        stream.flush()
        stream.close()
        Uri.fromFile(file)
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}

// Convert Image Uri to PDF document
fun convertImageToPdf(context: Context, imageUri: Uri): Uri? {
    var inputStream: java.io.InputStream? = null
    return try {
        inputStream = if (imageUri.scheme == "file") {
            java.io.FileInputStream(java.io.File(imageUri.path!!))
        } else {
            context.contentResolver.openInputStream(imageUri)
        }
        val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null
        
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        
        val margin = 20f
        val printableWidth = 595f - 2 * margin
        val printableHeight = 842f - 2 * margin
        
        val scaleX = printableWidth / bitmap.width
        val scaleY = printableHeight / bitmap.height
        val scale = minOf(scaleX, scaleY)
        
        val scaledWidth = bitmap.width * scale
        val scaledHeight = bitmap.height * scale
        
        val left = margin + (printableWidth - scaledWidth) / 2
        val top = margin + (printableHeight - scaledHeight) / 2
        
        val rect = RectF(left, top, left + scaledWidth, top + scaledHeight)
        canvas.drawBitmap(bitmap, null, rect, null)
        pdfDocument.finishPage(page)
        
        val pdfFile = File(context.cacheDir, "note_${System.currentTimeMillis()}.pdf")
        val fos = FileOutputStream(pdfFile)
        pdfDocument.writeTo(fos)
        fos.close()
        pdfDocument.close()
        Uri.fromFile(pdfFile)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } finally {
        try { inputStream?.close() } catch (ex: Exception) {}
    }
}

// Download file to public Downloads folder
fun downloadFileToPublicFolder(context: Context, uri: Uri): Boolean {
    val resolver = context.contentResolver
    val fileName = getFileName(context, uri)
    val ext = fileName.substringAfterLast('.', "").lowercase()
    val mimeType = resolver.getType(uri) ?: when (ext) {
        "pdf" -> "application/pdf"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        else -> "*/*"
    }
    
    return try {
        val inputStream = if (uri.scheme == "file") {
            java.io.FileInputStream(java.io.File(uri.path!!))
        } else {
            resolver.openInputStream(uri)
        } ?: return false
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            val collection = android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val destUri = resolver.insert(collection, values) ?: return false
            val outputStream = resolver.openOutputStream(destUri) ?: return false
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            true
        } else {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val targetFile = File(downloadsDir, fileName)
            val outputStream = FileOutputStream(targetFile)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            true
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

@Composable
fun UploadNotesScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val uid = DataRepository.currentUserId ?: ""
    val userFlow = DataRepository.getUserProfile(uid)
    val user by userFlow.collectAsState(initial = null)

    // Check if the selected file is an image, PDF or other doc
    val mimeType = selectedImageUri?.let { context.contentResolver.getType(it) } ?: ""
    val isImage = mimeType.startsWith("image/") || selectedImageUri?.path?.lowercase()?.let {
        it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".png") || it.endsWith(".webp")
    } == true
    val isPdf = mimeType.contains("pdf") || selectedImageUri?.path?.lowercase()?.endsWith(".pdf") == true

    // Launchers for choosing files
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedImageUri = it }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val uri = bitmapToUri(context, bitmap)
            uri?.let { selectedImageUri = it }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "Camera permission is required to capture photos", Toast.LENGTH_SHORT).show()
        }
    }

    // Custom Google Drive triangle logo Vector
    val DriveIcon = remember {
        ImageVector.Builder(
            name = "GoogleDrive",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = androidx.compose.ui.graphics.SolidColor(FigmaDarkText),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(8.5f, 3.5f)
            lineTo(15.5f, 3.5f)
            lineTo(21.5f, 14f)
            lineTo(18f, 20f)
            lineTo(6f, 20f)
            lineTo(2.5f, 14f)
            close()
            moveTo(8.5f, 3.5f)
            lineTo(15f, 14f)
            lineTo(6f, 20f)
            moveTo(15.5f, 3.5f)
            lineTo(18f, 20f)
        }.build()
    }

    UniSyncBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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
                    text = "Upload Notes",
                    fontFamily = PatrickHandFontFamily,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(Modifier.height(16.dp))

            // Main Content Area with Side Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Center Upload Frame
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    modifier = Modifier
                        .weight(1f)
                        .height(340.dp)
                        .border(1.2.dp, FigmaCardBorder, RoundedCornerShape(12.dp))
                        .clickable { fileLauncher.launch("image/*") }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) {
                            if (isImage) {
                                Image(
                                    painter = rememberAsyncImagePainter(selectedImageUri),
                                    contentDescription = "Preview",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                )
                            } else {
                                // PDF or Document File Placeholder Preview
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPdf) Icons.Default.Description else Icons.Default.FolderOpen,
                                        contentDescription = "File Type Icon",
                                        tint = FigmaDarkText,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = getFileName(context, selectedImageUri!!),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FigmaDarkText,
                                        maxLines = 2,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = if (isPdf) "PDF Document" else "Document File",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        } else {
                            // Upload Placeholder Graphic (Mockup Image Outline + Text)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier.size(72.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Custom visual layout resembling mockup picture frame upload icon
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = "Image Frame Icon",
                                        tint = Color.Gray.copy(alpha = 0.5f),
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Up Arrow Icon",
                                        tint = FigmaDarkText,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(Color.White, RoundedCornerShape(50.dp))
                                            .border(1.dp, Color.Gray, RoundedCornerShape(50.dp))
                                            .padding(3.dp)
                                            .align(Alignment.BottomEnd)
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "Upload image",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.width(16.dp))

                // 2. Right Side Upload Options Toolbar (direct icons on background)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(28.dp),
                    modifier = Modifier.width(42.dp)
                ) {
                    IconButton(onClick = {
                        val hasCameraPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.CAMERA
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (hasCameraPermission) {
                            cameraLauncher.launch(null)
                        } else {
                            permissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Take Photo",
                            tint = FigmaDarkText,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    IconButton(onClick = { fileLauncher.launch("image/*") }) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Choose from gallery",
                            tint = FigmaDarkText,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    IconButton(onClick = { fileLauncher.launch("*/*") }) {
                        Icon(
                            imageVector = DriveIcon,
                            contentDescription = "Choose from Drive",
                            tint = FigmaDarkText,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // Bottom Actions Row (3 square purple buttons)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Share Icon Button
                UploadActionButton(
                    icon = Icons.Default.Share,
                    description = "Share Note",
                    modifier = Modifier.weight(1f)
                ) {
                    if (selectedImageUri == null) {
                        Toast.makeText(context, "Please upload/select a file first!", Toast.LENGTH_SHORT).show()
                        return@UploadActionButton
                    }
                    try {
                        val shareUri = if (selectedImageUri!!.scheme == "file") {
                            val file = File(selectedImageUri!!.path!!)
                            androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "com.example.unisync.fileprovider",
                                file
                            )
                        } else {
                            selectedImageUri!!
                        }
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            val ext = getFileName(context, selectedImageUri!!).substringAfterLast('.', "").lowercase()
                            type = context.contentResolver.getType(shareUri) ?: when (ext) {
                                "pdf" -> "application/pdf"
                                "jpg", "jpeg" -> "image/jpeg"
                                "png" -> "image/png"
                                else -> "*/*"
                            }
                            putExtra(Intent.EXTRA_STREAM, shareUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Note"))
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error sharing file: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                // Download Icon Button
                UploadActionButton(
                    icon = Icons.Default.Download,
                    description = "Download Note",
                    modifier = Modifier.weight(1f)
                ) {
                    if (selectedImageUri == null) {
                        Toast.makeText(context, "Please upload/select a file first!", Toast.LENGTH_SHORT).show()
                        return@UploadActionButton
                    }
                    val downloaded = downloadFileToPublicFolder(context, selectedImageUri!!)
                    if (downloaded) {
                        Toast.makeText(context, "Saved to device Downloads folder!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Failed to download file.", Toast.LENGTH_SHORT).show()
                    }
                }

                // Save Icon Button (Uploads and writes to Firestore database)
                UploadActionButton(
                    icon = Icons.Default.Save,
                    description = "Save Note",
                    modifier = Modifier.weight(1f)
                ) {
                    if (selectedImageUri == null) {
                        Toast.makeText(context, "Please upload/select a file first!", Toast.LENGTH_SHORT).show()
                        return@UploadActionButton
                    }
                    isLoading = true
                    scope.launch {
                        val originalName = getFileName(context, selectedImageUri!!)
                        val sampleNote = Note(
                            title = originalName.substringBeforeLast("."),
                            subject = "Uploaded Subject",
                            topic = "Uploaded Topic",
                            description = "Note file uploaded and saved via UniSync app.",
                            fileUrl = selectedImageUri.toString(),
                            fileName = originalName
                        )
                        val res = DataRepository.createNote(
                            context = context,
                            note = sampleNote,
                            fileUri = selectedImageUri,
                            uploaderName = user?.name ?: "Student",
                            uploaderStudentId = user?.studentId ?: ""
                        )
                        isLoading = false
                        res.fold(
                            onSuccess = {
                                Toast.makeText(context, "Note uploaded and saved successfully!", Toast.LENGTH_SHORT).show()
                                onSuccess()
                            },
                            onFailure = {
                                Toast.makeText(context, "Failed to upload: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // wide button: "Convert into a PDF"
            Button(
                onClick = {
                    if (selectedImageUri == null) {
                        Toast.makeText(context, "Please upload/select an image first!", Toast.LENGTH_SHORT).show()
                    } else if (isPdf) {
                        Toast.makeText(context, "Document is already a PDF!", Toast.LENGTH_SHORT).show()
                    } else if (!isImage) {
                        Toast.makeText(context, "PDF conversion only supported for images.", Toast.LENGTH_SHORT).show()
                    } else {
                        isLoading = true
                        scope.launch {
                            val pdfUri = convertImageToPdf(context, selectedImageUri!!)
                            isLoading = false
                            if (pdfUri != null) {
                                selectedImageUri = pdfUri
                                Toast.makeText(context, "Successfully converted to PDF!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to convert image to PDF.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FigmaCardMediumBg,
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = "Convert into a PDF",
                    fontFamily = PatrickHandFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }

        // Home Navigation Bar at bottom
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            HomeBottomNav(
                onHomeClick = { onBack() },
                onAddClick = { onSuccess() }, // goes back or Create
                onNotificationsClick = { }
            )
        }

        if (isLoading) LoadingOverlay()
    }
}

@Composable
fun UploadActionButton(
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = FigmaCardMediumBg,
        modifier = modifier.height(56.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = Color.Black,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

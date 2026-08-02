package com.ezcanvas

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/*
 * One-call helpers so consumers get export, share and image-background with no plumbing of
 * their own — no FileProvider setup, no bitmap I/O, no image decoding.
 */

/**
 * Turn [raw] into a safe single-segment PNG file name. Names may come from user input, so path
 * separators and traversal are stripped rather than trusted.
 */
private fun safeFileName(raw: String): String {
    val cleaned = raw.trim()
        .replace(Regex("""[^A-Za-z0-9 ._-]"""), "_")
        .take(64)
        .trim('.', ' ')
    val base = cleaned.ifBlank { "drawing" }
    return if (base.endsWith(".png", ignoreCase = true)) base else "$base.png"
}

/**
 * Render the drawing to a PNG in the app cache and return a shareable [Uri] (backed by the
 * library's bundled FileProvider). Returns null if the canvas has not been laid out yet.
 *
 * [fileName] is sanitised, and ".png" is appended when missing.
 */
fun EzCanvasState.exportPngToCache(context: Context, fileName: String = drawingName): Uri? {
    val bitmap = exportBitmap() ?: return null
    val dir = File(context.cacheDir, "ezcanvas_shared").apply { mkdirs() }
    val file = File(dir, safeFileName(fileName))
    FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
    return FileProvider.getUriForFile(context, "${context.packageName}.ezcanvas.fileprovider", file)
}

/**
 * Export the drawing to PNG and open the system share sheet. A single call for consumers.
 * Defaults to [EzCanvasState.drawingName]; pass [fileName] to override it for one call.
 */
fun EzCanvasState.shareAsPng(
    context: Context,
    chooserTitle: String = "Share drawing",
    fileName: String = drawingName,
) {
    val uri = exportPngToCache(context, fileName) ?: return
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}

/** Decode [uri] off the main thread and set it as the canvas [EzCanvasState.backgroundImage]. */
suspend fun EzCanvasState.loadBackgroundImageFromUri(context: Context, uri: Uri): Boolean {
    val image = withContext(Dispatchers.IO) {
        try {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    } ?: return false
    backgroundImage = image
    return true
}

/**
 * Returns a ready-to-use callback that opens the system photo picker and sets the chosen image
 * as the canvas background. Wire it straight to a button: `onClick = rememberBackgroundImagePicker(state)`.
 */
@Composable
fun rememberBackgroundImagePicker(state: EzCanvasState): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch { state.loadBackgroundImageFromUri(context, uri) }
    }
    return remember(launcher) { { launcher.launch("image/*") } }
}

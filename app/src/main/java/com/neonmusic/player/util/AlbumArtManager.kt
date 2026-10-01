package com.neonmusic.player.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.LruCache
import android.util.Size
import com.neonmusic.player.data.db.AppMusicDatabase
import com.neonmusic.player.data.db.entity.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

object AlbumArtManager {
    private const val TAG = "AlbumArtManager"
    private const val MAX_ARTWORK_SOURCE_BYTES = 4 * 1024 * 1024
    private const val MAX_ARTWORK_DIMENSION = 1024

    // In-memory cache for artwork bytes (up to 8 MB)
    private val memoryCache = object : LruCache<String, ByteArray>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ByteArray): Int {
            return value.size
        }
    }

    // Negative cache to quickly skip tracks that have no artwork
    private val negativeCache = object : LruCache<String, Boolean>(1000) {}

    fun clearCache() {
        memoryCache.evictAll()
        negativeCache.evictAll()
    }

    fun getArtworkBytes(context: Context, songId: Long): ByteArray? {
        val cacheKey = "art_$songId"
        if (negativeCache.get(cacheKey) == true) {
            return null
        }

        val cachedBytes = memoryCache.get(cacheKey)
        if (cachedBytes != null) {
            return cachedBytes
        }

        val db = AppMusicDatabase.getInstance(context)
        val song: SongEntity? = try {
            runBlocking(Dispatchers.IO) {
                db.songDao().getSongById(songId)
            }
        } catch (e: Exception) {
            null
        }

        val bytes = loadArtworkBytes(context, song, songId)
        return if (bytes != null && bytes.isNotEmpty()) {
            memoryCache.put(cacheKey, bytes)
            bytes
        } else {
            negativeCache.put(cacheKey, true)
            null
        }
    }

    fun getArtStream(context: Context, uri: Uri): Pair<InputStream, String>? {
        // Legacy MediaStore album-art URIs contain an album ID, not a song ID.
        // Never feed that value into getArtworkBytes(), otherwise artwork can be
        // looked up for an unrelated song with the same numeric ID.
        if (uri.scheme == "content" &&
            uri.authority == MediaStore.AUTHORITY &&
            uri.path?.contains("/audio/albumart") == true
        ) {
            return try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = readAndNormalize(stream)
                    if (bytes != null && bytes.isNotEmpty()) Pair(ByteArrayInputStream(bytes), detectMimeType(bytes)) else null
                }
            } catch (_: Exception) {
                null
            }
        }

        val lastSegment = uri.lastPathSegment ?: return null
        val id = lastSegment.toLongOrNull() ?: return null
        val bytes = getArtworkBytes(context, id)
        return if (bytes != null && bytes.isNotEmpty()) {
            Pair(ByteArrayInputStream(bytes), detectMimeType(bytes))
        } else {
            null
        }
    }

    private fun loadArtworkBytes(context: Context, song: SongEntity?, id: Long): ByteArray? {
        if (song != null) {
            // 1. Try ContentResolver.loadThumbnail for Android Q+ (API 29+) first
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && song.uri.isNotBlank()) {
                try {
                    val uri = Uri.parse(song.uri)
                    if (uri.scheme == "content") {
                        val bitmap = context.contentResolver.loadThumbnail(
                            uri,
                            Size(512, 512),
                            null
                        )
                        val bos = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, bos)
                        val thumbnailBytes = bos.toByteArray()
                        if (thumbnailBytes.isNotEmpty()) {
                            return thumbnailBytes
                        }
                    }
                } catch (e: Exception) {
                    // Ignore thumbnail errors and fall through
                }
            }

            // 2. Try albumart content provider by albumId
            if (song.albumId > 0) {
                try {
                    val albumArtUri = Uri.parse("content://media/external/audio/albumart/${song.albumId}")
                    context.contentResolver.openInputStream(albumArtUri)?.use { input ->
                        val bytes = readAndNormalize(input)
                        if (bytes != null && bytes.isNotEmpty()) {
                            return bytes
                        }
                    }
                } catch (e: Exception) {
                    // Not found in albumart provider
                }
            }

            // 3. Extract embedded picture using MediaMetadataRetriever only if file header has artwork marker
            val hasArtMarker = if (song.path.isNotBlank()) {
                hasEmbeddedArtworkMarker(song.path)
            } else false

            if (hasArtMarker) {
                val retriever = MediaMetadataRetriever()
                try {
                    if (song.path.isNotBlank() && File(song.path).exists()) {
                        retriever.setDataSource(song.path)
                    } else if (song.uri.isNotBlank()) {
                        retriever.setDataSource(context, Uri.parse(song.uri))
                    }
                    val pic = retriever.embeddedPicture
                    if (pic != null && pic.isNotEmpty()) {
                        return normalizeArtworkBytes(pic)
                    }
                } catch (e: Exception) {
                    // Ignore retrieval errors
                } finally {
                    try {
                        retriever.release()
                    } catch (e: Exception) {
                        // Ignore release errors
                    }
                }
            }
        }

        // 4. Try legacy content resolver albumart uri only on pre-Q
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            try {
                val legacyUri = Uri.parse("content://media/external/audio/albumart/$id")
                context.contentResolver.openInputStream(legacyUri)?.use { input ->
                    val bytes = readAndNormalize(input)
                    if (bytes != null && bytes.isNotEmpty()) return bytes
                }
            } catch (e: Exception) {
                // Not found in legacy provider
            }
        }

        return null
    }

    private fun readAndNormalize(input: InputStream): ByteArray? {
        return try {
            val buffer = ByteArray(16 * 1024)
            val output = ByteArrayOutputStream()
            var total = 0
            while (true) {
                val count = input.read(buffer)
                if (count <= 0) break
                total += count
                if (total > MAX_ARTWORK_SOURCE_BYTES) return null
                output.write(buffer, 0, count)
            }
            normalizeArtworkBytes(output.toByteArray())
        } catch (e: Exception) {
            null
        }
    }

    private fun normalizeArtworkBytes(bytes: ByteArray): ByteArray? {
        if (bytes.isEmpty()) return null
        if (bytes.size <= 2 * 1024 * 1024) return bytes
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sample = 1
            while (bounds.outWidth / sample > MAX_ARTWORK_DIMENSION || bounds.outHeight / sample > MAX_ARTWORK_DIMENSION) {
                sample *= 2
            }
            val options = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 86, out)
            bitmap.recycle()
            out.toByteArray().takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            Log.w(TAG, "Artwork normalization failed: ${e.message}")
            null
        }
    }

    private fun detectMimeType(bytes: ByteArray): String {
        if (bytes.size >= 8 && bytes.copyOfRange(0, 8).contentEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))) return "image/png"
        if (bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) return "image/jpeg"
        if (bytes.size >= 6 && String(bytes, 0, 6, Charsets.US_ASCII) in listOf("GIF87a", "GIF89a")) return "image/gif"
        return "image/jpeg"
    }

    /**
     * Inspects the first 64KB of an audio file to check if it contains common
     * picture frame markers (e.g. APIC/PIC in ID3v2, covr in MP4/M4A, or FLAC picture block)
     * before invoking MediaMetadataRetriever.embeddedPicture, preventing native JNI error spam.
     */
    private fun hasEmbeddedArtworkMarker(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (!file.exists() || !file.canRead() || file.length() < 128) return false
            val ext = file.extension.lowercase()
            file.inputStream().use { stream ->
                val buffer = ByteArray(65536)
                val bytesRead = stream.read(buffer)
                if (bytesRead <= 0) return false

                when (ext) {
                    "mp3" -> {
                        // Look for "APIC" (ID3v2.3/2.4) or "PIC" (ID3v2.2)
                        containsByteSequence(buffer, bytesRead, "APIC".toByteArray(Charsets.ISO_8859_1)) ||
                        containsByteSequence(buffer, bytesRead, "PIC".toByteArray(Charsets.ISO_8859_1))
                    }
                    "m4a", "mp4", "aac" -> {
                        // Look for "covr" atom
                        containsByteSequence(buffer, bytesRead, "covr".toByteArray(Charsets.ISO_8859_1))
                    }
                    "flac" -> {
                        // FLAC header starts with "fLaC"
                        buffer[0] == 'f'.code.toByte() && buffer[1] == 'L'.code.toByte() &&
                        buffer[2] == 'a'.code.toByte() && buffer[3] == 'C'.code.toByte()
                    }
                    else -> false
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun containsByteSequence(data: ByteArray, length: Int, target: ByteArray): Boolean {
        if (target.isEmpty() || length < target.size) return false
        val max = length - target.size
        for (i in 0..max) {
            var match = true
            for (j in target.indices) {
                if (data[i + j] != target[j]) {
                    match = false
                    break
                }
            }
            if (match) return true
        }
        return false
    }
}

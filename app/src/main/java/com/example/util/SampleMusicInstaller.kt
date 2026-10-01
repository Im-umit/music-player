package com.example.util

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.example.data.db.AppMusicDatabase
import com.example.data.db.entity.FolderEntity
import com.example.data.db.entity.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object SampleMusicInstaller {

    private const val TAG = "SampleMusicInstaller"
    const val SAMPLE_SONG_ID = 1001L

    suspend fun installSampleMusic(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = AppMusicDatabase.getInstance(context)

            // Determine best music directory on device
            var musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            if (!musicDir.exists()) {
                musicDir.mkdirs()
            }
            if (!musicDir.canWrite()) {
                musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: File(context.filesDir, "Music")
                if (!musicDir.exists()) {
                    musicDir.mkdirs()
                }
            }

            val targetFile = File(musicDir, "Neon_Horizon.mp3")

            // Copy from assets if not present or too small
            if (!targetFile.exists() || targetFile.length() < 100_000L) {
                context.assets.open("sample_music/neon_horizon.mp3").use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.i(TAG, "Sample music installed to: ${targetFile.absolutePath} (${targetFile.length()} bytes)")
            }

            // Register with Android MediaStore
            try {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath),
                    arrayOf("audio/mpeg")
                ) { path, uri ->
                    Log.i(TAG, "MediaScanner registered sample music: $path -> $uri")
                }
            } catch (e: Exception) {
                Log.w(TAG, "MediaScanner error: ${e.message}")
            }

            // Ensure Room database has the song entity
            val fileLength = targetFile.length()
            val uriStr = Uri.fromFile(targetFile).toString()

            val songEntity = SongEntity(
                id = SAMPLE_SONG_ID,
                title = "Neon Horizon",
                artist = "Cyberwave",
                album = "Neon City Nights",
                albumId = 101L,
                duration = 75050L,
                uri = uriStr,
                path = targetFile.absolutePath,
                size = fileLength,
                dateAdded = System.currentTimeMillis() / 1000,
                dateModified = targetFile.lastModified() / 1000,
                mimeType = "audio/mpeg",
                trackNumber = 1,
                discNumber = 1,
                genre = "Synthwave",
                year = 2026,
                folderName = "Music",
                relativePath = "Music/",
                isFavorite = false,
                albumArtUri = "https://music.local/albumart/$SAMPLE_SONG_ID"
            )

            db.songDao().insertSongs(listOf(songEntity))

            val folderEntity = FolderEntity(
                path = targetFile.parent ?: "/sdcard/Music",
                displayName = "Music",
                songCount = 1,
                isSaf = false,
                treeUri = null
            )
            db.folderDao().insertFolder(folderEntity)

            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install sample music: ${e.message}", e)
            return@withContext false
        }
    }
}

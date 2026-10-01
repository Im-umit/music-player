package com.example.data.mediastore

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.example.data.db.AppMusicDatabase
import com.example.data.db.entity.FolderEntity
import com.example.data.db.entity.SongEntity
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreScanner(private val context: Context) {

    private val db = AppMusicDatabase.getInstance(context)
    private val audioExtensions = setOf("mp3", "m4a", "wav", "flac", "aac", "ogg", "opus", "wma", "mid", "x-flac", "amr", "3gp")

    companion object {
        const val MIN_SONG_DURATION_MS = 8_000L // 8 saniye ve üzeri tüm müzik dosyaları
    }

    suspend fun scanMediaStore(): List<Song> = withContext(Dispatchers.IO) {
        // Ensure sample track is installed on device
        try {
            com.example.util.SampleMusicInstaller.installSampleMusic(context)
        } catch (e: Exception) {
            // Ignore
        }

        val songList = mutableListOf<Song>()
        val folderMap = mutableMapOf<String, Int>() // folderPath -> count
        val scannedPaths = mutableSetOf<String>()

        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection.add(MediaStore.Audio.Media.RELATIVE_PATH)
        }

        val collectionUris = mutableListOf(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
        try {
            collectionUris.add(MediaStore.Audio.Media.INTERNAL_CONTENT_URI)
        } catch (e: Exception) {
            // ignore
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val volumeNames = MediaStore.getExternalVolumeNames(context)
                for (vol in volumeNames) {
                    collectionUris.add(MediaStore.Audio.Media.getContentUri(vol))
                }
            } catch (e: Exception) {
                try {
                    collectionUris.add(MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL))
                } catch (e2: Exception) {
                    // ignore
                }
            }
        }

        // 1. Query MediaStore Audio Content Providers
        for (collectionUri in collectionUris.distinct()) {
            try {
                context.contentResolver.query(
                    collectionUri,
                    projection.toTypedArray(),
                    null,
                    null,
                    "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
                    val titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                    val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                    val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                    val durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                    val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                    val nameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                    val sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
                    val dateAddedCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                    val dateModifiedCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
                    val mimeTypeCol = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                    val trackCol = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)
                    val yearCol = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
                    val relativePathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                    } else -1

                    while (cursor.moveToNext()) {
                        val id = if (idCol != -1) cursor.getLong(idCol) else continue
                        val path = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""
                        val displayName = if (nameCol != -1) cursor.getString(nameCol) ?: "" else ""
                        val mimeType = if (mimeTypeCol != -1) cursor.getString(mimeTypeCol) ?: "" else ""
                        val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L

                        val extension = (if (path.isNotBlank()) path else displayName).substringAfterLast('.', "").lowercase()
                        val isAudioMime = mimeType.startsWith("audio/", ignoreCase = true)
                        val isAudioExt = audioExtensions.contains(extension)

                        if (!isAudioMime && !isAudioExt) continue
                        if (size in 1..4096) continue // Skip micro-files under 4KB
                        if (displayName.contains("AutoBackup", ignoreCase = true) || path.contains("AutoBackup", ignoreCase = true)) continue

                        val canonicalPath = if (path.isNotBlank()) {
                            try { File(path).canonicalPath.lowercase() } catch (e: Exception) { path.lowercase() }
                        } else ""
                        val key = if (canonicalPath.isNotBlank()) canonicalPath else "${collectionUri}/$id"
                        if (!scannedPaths.add(key)) continue

                        var title = if (titleCol != -1) cursor.getString(titleCol) else null
                        if (title.isNullOrBlank() || title == "<unknown>") {
                            title = when {
                                displayName.isNotBlank() -> displayName.substringBeforeLast('.')
                                path.isNotBlank() -> File(path).nameWithoutExtension
                                else -> "Bilinmeyen Şarkı"
                            }
                        }

                        var artist = if (artistCol != -1) cursor.getString(artistCol) else null
                        if (artist.isNullOrBlank() || artist == "<unknown>") {
                            artist = "Bilinmeyen Sanatçı"
                        }

                        var album = if (albumCol != -1) cursor.getString(albumCol) else null
                        if (album.isNullOrBlank() || album == "<unknown>") {
                            album = "Bilinmeyen Albüm"
                        }

                        val albumId = if (albumIdCol != -1) cursor.getLong(albumIdCol) else 0L
                        var duration = if (durationCol != -1) cursor.getLong(durationCol) else 0L

                        // If duration is 0, attempt metadata retrieval
                        if (duration <= 0L && path.isNotBlank() && File(path).exists()) {
                            try {
                                val retriever = MediaMetadataRetriever()
                                retriever.setDataSource(path)
                                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let {
                                    if (it > 0) duration = it
                                }
                                retriever.release()
                            } catch (e: Exception) {
                                // ignore
                            }
                        }

                        // Skip corrupted/empty 0-duration files if they are not playable
                        if (duration <= 0L && size < 16384L) continue

                        // Yalnızca 1 dakika (60.000 ms) ve üzerindeki şarkıları kütüphaneye ekle
                        if (duration < MIN_SONG_DURATION_MS) continue

                        val dateAdded = if (dateAddedCol != -1) cursor.getLong(dateAddedCol) else 0L
                        val dateModified = if (dateModifiedCol != -1) cursor.getLong(dateModifiedCol) else 0L
                        val trackNumber = if (trackCol != -1) cursor.getInt(trackCol) else 0
                        val year = if (yearCol != -1) cursor.getInt(yearCol) else 0

                        val relativePath = if (relativePathCol != -1) {
                            cursor.getString(relativePathCol) ?: ""
                        } else {
                            if (path.isNotEmpty()) File(path).parentFile?.name ?: "" else ""
                        }

                        val folderName = if (relativePath.isNotBlank()) {
                            relativePath.trim('/').substringAfterLast('/').ifEmpty { "Müzik" }
                        } else {
                            if (path.isNotEmpty()) File(path).parentFile?.name ?: "Müzik" else "Müzik"
                        }

                        val uniqueId = if (collectionUri.toString().contains("internal", ignoreCase = true)) {
                            id + 50_000_000L
                        } else {
                            id
                        }

                        val contentUri = ContentUris.withAppendedId(collectionUri, id).toString()
                        val albumArtUri = "https://music.local/albumart/$uniqueId"

                        val song = Song(
                            id = uniqueId,
                            title = title,
                            artist = artist,
                            album = album,
                            albumId = albumId,
                            duration = duration,
                            uri = contentUri,
                            path = path,
                            size = size,
                            dateAdded = dateAdded,
                            dateModified = dateModified,
                            mimeType = if (mimeType.isNotBlank()) mimeType else "audio/$extension",
                            trackNumber = trackNumber,
                            discNumber = 1,
                            genre = "",
                            year = year,
                            folderName = folderName,
                            relativePath = relativePath,
                            isFavorite = false,
                            albumArtUri = albumArtUri
                        )

                        songList.add(song)

                        val folderKey = if (relativePath.isNotBlank()) relativePath else folderName
                        folderMap[folderKey] = (folderMap[folderKey] ?: 0) + 1
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Direct Device Storage Traversal for unindexed audio files (MediaStore.Files query removed to prevent duplicates)
        val standardDirs = mutableListOf<File>()
        try {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)?.let { standardDirs.add(it) }
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.let { standardDirs.add(it) }
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS)?.let { standardDirs.add(it) }
            Environment.getExternalStorageDirectory()?.let { extRoot ->
                standardDirs.add(File(extRoot, "Music"))
                standardDirs.add(File(extRoot, "Download"))
                standardDirs.add(File(extRoot, "Audio"))
                standardDirs.add(File(extRoot, "Recordings"))
                standardDirs.add(File(extRoot, "Bluetooth"))
                standardDirs.add(File(extRoot, "media"))
            }
            standardDirs.add(File("/storage/emulated/0/Music"))
            standardDirs.add(File("/storage/emulated/0/Download"))
            standardDirs.add(File("/storage/emulated/0/Audio"))
            standardDirs.add(File("/sdcard/Music"))
            standardDirs.add(File("/sdcard/Download"))
        } catch (e: Exception) {
            // ignore directory resolution errors
        }

        for (dir in standardDirs.distinct()) {
            if (dir.exists() && dir.isDirectory) {
                scanDirectoryForAudioFiles(dir, 0, 5, songList, folderMap, scannedPaths)
            }
        }

        // 3. Automatically re-scan all previously saved SAF folders so their songs are always kept up-to-date
        try {
            val safFolders = db.folderDao().getSafFolders()
            for (safFolder in safFolders) {
                val uriStr = safFolder.path
                if (uriStr.startsWith("content://")) {
                    val treeUri = Uri.parse(uriStr)
                    val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                    if (rootDoc != null) {
                        val safSongs = mutableListOf<Song>()
                        scanDocumentFileRecursive(rootDoc, safSongs)
                        for (s in safSongs) {
                            val canonical = if (s.path.isNotBlank()) {
                                try { File(s.path).canonicalPath.lowercase() } catch (e: Exception) { s.path.lowercase() }
                            } else s.uri
                            if (scannedPaths.add(canonical)) {
                                songList.add(s)
                                val fName = s.folderName.ifEmpty { safFolder.displayName }
                                folderMap[fName] = (folderMap[fName] ?: 0) + 1
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // ignore saf error
        }

        // Sync with existing database to preserve favorite flags and play counts
        val existingSongs = db.songDao().getAllSongsAsc()
        val existingById = existingSongs.associateBy { it.id }
        val existingByPath = existingSongs.filter { it.path.isNotBlank() }.associateBy { it.path.lowercase() }

        val entitiesToSave = songList.map { song ->
            val existing = existingById[song.id] ?: if (song.path.isNotBlank()) existingByPath[song.path.lowercase()] else null
            val favorite = existing?.isFavorite ?: false
            val playCount = existing?.playCount ?: 0
            val lastPlayed = existing?.lastPlayed ?: 0L
            song.isFavorite = favorite
            SongEntity.fromSong(song).copy(
                isFavorite = favorite,
                playCount = playCount,
                lastPlayed = lastPlayed
            )
        }

        // Save songs to Room DB
        db.songDao().insertSongs(entitiesToSave)

        // Purge any short tracks under 1 minute (ringtones, notifications)
        try {
            db.songDao().deleteShortSongs()
        } catch (e: Exception) {
            // ignore
        }

        // Clean up stale songs that no longer exist on device (preserving valid scanned songs)
        try {
            val scannedIds = songList.map { it.id }.toSet()
            val allExisting = db.songDao().getAllSongsAsc()
            val staleIds = allExisting.filter { song ->
                !scannedIds.contains(song.id) && (song.path.isBlank() || !File(song.path).exists())
            }.map { it.id }
            if (staleIds.isNotEmpty()) {
                staleIds.chunked(200).forEach { chunk ->
                    db.songDao().deleteSongsByIds(chunk)
                }
            }
        } catch (e: Exception) {
            // ignore stale deletion error
        }

        // Clear AlbumArtManager cache so newly updated or scanned album arts are fresh
        try {
            com.example.util.AlbumArtManager.clearCache()
        } catch (e: Exception) {
            // ignore
        }
        try {
            db.songDao().updateLegacyAlbumArtUris()
        } catch (e: Exception) {
            // ignore
        }

        // Save scanned folders to Room DB
        db.folderDao().clearNonSafFolders()
        val folderEntities = folderMap.map { (path, count) ->
            val name = path.trim('/').substringAfterLast('/').ifEmpty { path }
            FolderEntity(
                path = path,
                displayName = name,
                songCount = count,
                isSaf = false,
                treeUri = null
            )
        }
        db.folderDao().insertFolders(folderEntities)

        return@withContext if (songList.isNotEmpty()) songList else db.songDao().getAllSongsAsc().map { it.toSong() }
    }

    private fun scanDirectoryForAudioFiles(
        dir: File,
        depth: Int,
        maxDepth: Int,
        foundSongs: MutableList<Song>,
        folderMap: MutableMap<String, Int>,
        scannedPaths: MutableSet<String>
    ) {
        if (!dir.exists() || !dir.isDirectory || depth > maxDepth) return
        val files = try {
            dir.listFiles()
        } catch (e: Exception) {
            null
        } ?: return

        for (file in files) {
            if (file.isDirectory) {
                val name = file.name
                if (!name.startsWith(".") && !name.equals("Android", ignoreCase = true) && !name.contains("AutoBackup", ignoreCase = true) && !name.contains(".backup", ignoreCase = true)) {
                    scanDirectoryForAudioFiles(file, depth + 1, maxDepth, foundSongs, folderMap, scannedPaths)
                }
            } else if (file.isFile && file.length() > 4096) { // > 4KB
                val name = file.name
                if (name.startsWith(".") || name.contains("AutoBackup", ignoreCase = true) || name.contains(".backup", ignoreCase = true)) continue
                val ext = file.extension.lowercase()
                if (audioExtensions.contains(ext)) {
                    val absPath = file.absolutePath
                    val canonical = try { file.canonicalPath.lowercase() } catch (e: Exception) { absPath.lowercase() }
                    if (scannedPaths.add(canonical)) {
                        val song = extractSongFromFile(file)
                        if (song != null) {
                            foundSongs.add(song)
                            val folderName = file.parentFile?.name ?: "Müzik"
                            folderMap[folderName] = (folderMap[folderName] ?: 0) + 1

                            try {
                                MediaScannerConnection.scanFile(context, arrayOf(absPath), null, null)
                            } catch (e: Exception) {
                                // ignore scanner notification error
                            }
                        }
                    }
                }
            }
        }
    }

    private fun extractSongFromFile(file: File): Song? {
        if (file.name.startsWith(".") || file.name.contains("AutoBackup", ignoreCase = true)) return null
        var title = file.nameWithoutExtension
        var artist = "Bilinmeyen Sanatçı"
        var album = file.parentFile?.name ?: "Bilinmeyen Albüm"
        var duration = 0L
        var year = 0

        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let {
                if (it.isNotBlank()) title = it
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let {
                if (it.isNotBlank()) artist = it
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.let {
                if (it.isNotBlank()) album = it
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let {
                if (it > 0) duration = it
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toIntOrNull()?.let {
                year = it
            }
            retriever.release()
        } catch (e: Exception) {
            // fallback
        }

        if (duration < MIN_SONG_DURATION_MS) return null

        val uri = Uri.fromFile(file).toString()
        val folderName = file.parentFile?.name ?: "Müzik"
        val id = (file.absolutePath.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL).coerceAtLeast(1L)

        return Song(
            id = id,
            title = title,
            artist = artist,
            album = album,
            albumId = 0L,
            duration = duration,
            uri = uri,
            path = file.absolutePath,
            size = file.length(),
            dateAdded = file.lastModified() / 1000,
            dateModified = file.lastModified() / 1000,
            mimeType = "audio/${file.extension.lowercase()}",
            folderName = folderName,
            relativePath = folderName,
            isFavorite = false,
            albumArtUri = "https://music.local/albumart/$id"
        )
    }

    suspend fun scanSafFolder(treeUri: Uri): List<Song> = withContext(Dispatchers.IO) {
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        val scanned = mutableListOf<Song>()
        scanDocumentFileRecursive(rootDoc, scanned)

        if (scanned.isNotEmpty()) {
            val entities = scanned.map { SongEntity.fromSong(it) }
            db.songDao().insertSongs(entities)

            val folderName = rootDoc.name ?: "Özel Klasör"
            db.folderDao().insertFolder(
                FolderEntity(
                    path = treeUri.toString(),
                    displayName = folderName,
                    songCount = scanned.size,
                    isSaf = true,
                    treeUri = treeUri.toString()
                )
            )
        }
        return@withContext scanned
    }

    private fun scanDocumentFileRecursive(dir: DocumentFile, result: MutableList<Song>) {
        val files = dir.listFiles()
        val supportedExtensions = setOf("mp3", "m4a", "aac", "flac", "wav", "ogg", "opus", "amr", "3gp")

        for (file in files) {
            if (file.isDirectory) {
                scanDocumentFileRecursive(file, result)
            } else if (file.isFile) {
                val name = file.name ?: continue
                val ext = name.substringAfterLast('.', "").lowercase()
                if (ext in supportedExtensions || file.type?.startsWith("audio/") == true) {
                    val uri = file.uri
                    var title = name.substringBeforeLast('.')
                    var artist = "Bilinmeyen Sanatçı"
                    var album = "Bilinmeyen Albüm"
                    var duration = 0L

                    try {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(context, uri)
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let {
                            if (it.isNotBlank()) title = it
                        }
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let {
                            if (it.isNotBlank()) artist = it
                        }
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.let {
                            if (it.isNotBlank()) album = it
                        }
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let {
                            duration = it
                        }
                        retriever.release()
                    } catch (e: Exception) {
                        // ignore and use fallback file metadata
                    }

                    if (duration < MIN_SONG_DURATION_MS) continue

                    val songId = uri.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL
                    val song = Song(
                        id = songId,
                        title = title,
                        artist = artist,
                        album = album,
                        albumId = 0L,
                        duration = duration,
                        uri = uri.toString(),
                        path = uri.path ?: "",
                        size = file.length(),
                        dateAdded = System.currentTimeMillis() / 1000,
                        dateModified = file.lastModified() / 1000,
                        mimeType = file.type ?: "audio/*",
                        folderName = dir.name ?: "SAF",
                        relativePath = dir.name ?: "SAF",
                        albumArtUri = "https://music.local/albumart/$songId"
                    )
                    result.add(song)
                }
            }
        }
    }
}

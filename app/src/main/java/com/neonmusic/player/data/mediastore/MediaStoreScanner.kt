package com.neonmusic.player.data.mediastore

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.neonmusic.player.data.db.AppMusicDatabase
import com.neonmusic.player.data.db.entity.FolderEntity
import com.neonmusic.player.data.db.entity.SongEntity
import com.neonmusic.player.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreScanner(private val context: Context) {

    companion object {
        const val LEGACY_DATA_COLUMN = "_data"
        const val MIN_SONG_DURATION_MS = 8_000L // 8 saniye ve üzeri tüm müzik dosyaları
    }

    private val db = AppMusicDatabase.getInstance(context)
    private val audioExtensions = setOf("mp3", "m4a", "wav", "flac", "aac", "ogg", "opus", "wma", "mid", "x-flac", "amr", "3gp")
    private val safAudioExtensions = setOf("mp3", "m4a", "aac", "flac", "wav", "ogg", "opus", "amr", "3gp")

    suspend fun scanMediaStore(): List<Song> = withContext(Dispatchers.IO) {

        val songList = mutableListOf<Song>()
        val folderMap = mutableMapOf<String, Int>() // folderPath -> count
        val scannedPaths = mutableSetOf<String>()
        var mediaStoreQuerySucceeded = false
        var mediaStoreQueryFailed = false

        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
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
        } else {
            // DATA is deprecated on modern Android; request it only on legacy releases
            // where broad filesystem access is still supported.
            projection.add(5, LEGACY_DATA_COLUMN)
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
                    val dataCol = cursor.getColumnIndex(LEGACY_DATA_COLUMN)
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

                    mediaStoreQuerySucceeded = true
                    var rowCount = 0
                    while (cursor.moveToNext()) {
                        if ((rowCount++ and 63) == 0) currentCoroutineContext().ensureActive()
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
                            val retriever = MediaMetadataRetriever()
                            try {
                                retriever.setDataSource(path)
                                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let {
                                    if (it > 0) duration = it
                                }
                            } catch (_: Exception) {
                                // ignore
                            } finally {
                                try { retriever.release() } catch (_: Exception) { }
                            }
                        }

                        // Skip corrupted/empty 0-duration files if they are not playable
                        if (duration <= 0L && size < 16384L) continue

                        // 8 saniyeden kısa sesleri (bildirim/ringtone vb.) kütüphaneye alma
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
                mediaStoreQueryFailed = true
                e.printStackTrace()
            }
        }

        // 2. Legacy fallback for Android 9 and older.
        // On Android 10+ scoped storage makes broad filesystem traversal both expensive
        // and unreliable. MediaStore is the source of truth there; user-selected SAF
        // folders are handled below.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
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
            } catch (e: Exception) {
                // ignore directory resolution errors
            }

            for ((dirIndex, dir) in standardDirs.distinct().withIndex()) {
                if ((dirIndex and 3) == 0) currentCoroutineContext().ensureActive()
                if (dir.exists() && dir.isDirectory) {
                    scanDirectoryForAudioFiles(dir, 0, 5, songList, folderMap, scannedPaths)
                }
            }
        }

        // 3. Re-scan user-selected SAF folders so explicitly granted folders remain available.
        try {
            val safFolders = db.folderDao().getSafFolders()
            for (safFolder in safFolders) {
                val uriStr = safFolder.path
                if (uriStr.startsWith("content://")) {
                    val treeUri = Uri.parse(uriStr)
                    val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                    if (rootDoc != null) {
                        val safSongs = mutableListOf<Song>()
                        scanDocumentFileRecursive(rootDoc, safSongs, depth = 0)
                        for (s in safSongs) {
                            // SAF content URIs are the stable identity; using Uri.path here can
                            // collapse unrelated document IDs on different providers.
                            val canonical = if (s.uri.isNotBlank()) s.uri else if (s.path.isNotBlank()) {
                                try { File(s.path).canonicalPath.lowercase() } catch (e: Exception) { s.path.lowercase() }
                            } else ""
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

        // Match existing songs by URI first, then by stable ID/path. This keeps user data
        // attached even when a MediaStore ID changes or a volume is re-enumerated.
        val existingSongs = db.songDao().getAllSongsAsc()
        val existingByUri = existingSongs.associateBy { it.uri }
        val existingById = existingSongs.associateBy { it.id }
        val existingByPath = existingSongs.filter { it.path.isNotBlank() }.associateBy { it.path.lowercase() }
        val usedIds = existingSongs.map { it.id }.toMutableSet()
        val assignedIds = mutableSetOf<Long>()

        val normalizedSongs = songList.map { song ->
            val existing = existingByUri[song.uri]
                ?: existingById[song.id]?.takeIf { it.uri == song.uri }
                ?: if (song.path.isNotBlank()) existingByPath[song.path.lowercase()] else null

            val candidateId = existing?.id ?: song.id
            val resolvedId = if (existing != null) {
                existing.id
            } else if ((candidateId <= 0L) ||
                ((candidateId in usedIds) && existingById[candidateId]?.uri != song.uri) ||
                candidateId in assignedIds
            ) {
                var id = (song.uri.hashCode().toLong() and Long.MAX_VALUE).coerceAtLeast(1L)
                while (id in usedIds || id in assignedIds || id <= 0L) {
                    id = (id + 1L) and Long.MAX_VALUE
                    if (id == 0L) id = 1L
                }
                id
            } else {
                candidateId
            }
            assignedIds.add(resolvedId)
            usedIds.add(resolvedId)
            song.copy(id = resolvedId)
        }

        val entitiesToSave = normalizedSongs.map { song ->
            val existing = existingByUri[song.uri]
                ?: existingById[song.id]?.takeIf { it.uri == song.uri }
                ?: if (song.path.isNotBlank()) existingByPath[song.path.lowercase()] else null
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
        songList.clear()
        songList.addAll(normalizedSongs)

        // Save songs to Room DB
        db.songDao().insertSongs(entitiesToSave)

        // Purge any short tracks under 1 minute (ringtones, notifications)
        try {
            db.songDao().deleteShortSongs()
        } catch (e: Exception) {
            // ignore
        }

        // Never purge the database after a failed MediaStore query. A transient provider
        // error or revoked permission must not look like "all songs were deleted".
        if (mediaStoreQuerySucceeded && !mediaStoreQueryFailed) {
            try {
                val scannedIds = songList.map { it.id }.toSet()
                val allExisting = db.songDao().getAllSongsAsc()
                val staleIds = allExisting.filter { song ->
                    !scannedIds.contains(song.id) && isSongStillDefinitelyMissing(song)
                }.map { it.id }
                if (staleIds.isNotEmpty()) {
                    staleIds.chunked(200).forEach { chunk ->
                        db.songDao().deleteSongsByIds(chunk)
                    }
                }
            } catch (e: Exception) {
                // ignore stale deletion error
            }
        }

        // Clear AlbumArtManager cache so newly updated or scanned album arts are fresh
        try {
            com.neonmusic.player.util.AlbumArtManager.clearCache()
        } catch (e: Exception) {
            // ignore
        }
        try {
            db.songDao().updateLegacyAlbumArtUris()
        } catch (e: Exception) {
            // ignore
        }

        // Replace non-SAF folders only after a complete MediaStore pass. A provider
        // failure must not erase a previously valid folder listing.
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
        if (mediaStoreQuerySucceeded && !mediaStoreQueryFailed) {
            // Rebuild the non-SAF folder projection so folders whose files were
            // deleted or moved do not remain as stale empty entries. SAF folders
            // are persisted independently and therefore are intentionally kept.
            try {
                db.folderDao().clearNonSafFolders()
                if (folderEntities.isNotEmpty()) {
                    db.folderDao().insertFolders(folderEntities)
                }
            } catch (e: Exception) {
                // Folder cache cleanup must never invalidate an otherwise valid song scan.
            }
        }

        currentCoroutineContext().ensureActive()
        return@withContext if (songList.isNotEmpty()) songList else db.songDao().getAllSongsAsc().map { it.toSong() }
    }

    /**
     * Returns true only when we can confidently say the media is gone. A null result
     * means access could not be verified (for example a revoked permission), so the
     * caller must preserve the database row rather than treating it as stale.
     */
    private fun isSongStillDefinitelyMissing(song: SongEntity): Boolean {
        return try {
            when {
                song.uri.startsWith("content://") -> {
                    try {
                        context.contentResolver.openFileDescriptor(Uri.parse(song.uri), "r")
                            ?.use { false } ?: true
                    } catch (_: SecurityException) {
                        false
                    }
                }
                song.uri.startsWith("file://") -> !File(Uri.parse(song.uri).path.orEmpty()).canRead()
                song.path.isNotBlank() -> !File(song.path).canRead()
                else -> false
            }
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
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
            try {
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
            } finally {
                try { retriever.release() } catch (_: Exception) {}
            }
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
        scanDocumentFileRecursive(rootDoc, scanned, depth = 0)

        if (scanned.isNotEmpty()) {
            val existingSongs = db.songDao().getAllSongsAsc()
            val existingByUri = existingSongs.associateBy { it.uri }
            val existingByPath = existingSongs.filter { it.path.isNotBlank() }.associateBy { it.path.lowercase() }
            val usedIds = existingSongs.map { it.id }.toMutableSet()
            val assignedIds = mutableSetOf<Long>()

            val normalized = scanned.map { song ->
                val existing = existingByUri[song.uri]
                    ?: if (song.path.isNotBlank()) existingByPath[song.path.lowercase()] else null
                val resolvedId = if (existing != null) existing.id else {
                    var id = (song.id).coerceAtLeast(1L)
                    if (id in usedIds || id in assignedIds) {
                        id = (song.uri.hashCode().toLong() and Long.MAX_VALUE).coerceAtLeast(1L)
                        while (id in usedIds || id in assignedIds) {
                            id = (id + 1L) and Long.MAX_VALUE
                            if (id == 0L) id = 1L
                        }
                    }
                    id
                }
                assignedIds.add(resolvedId)
                usedIds.add(resolvedId)
                val keep = existing
                song.copy(
                    id = resolvedId,
                    isFavorite = keep?.isFavorite ?: false,
                    // SAF does not expose a stable creation timestamp. Preserve the
                    // existing value so rescans do not make every track look newly added.
                    dateAdded = keep?.dateAdded?.takeIf { it > 0L } ?: song.dateAdded,
                    dateModified = if (song.dateModified > 0L) song.dateModified else (keep?.dateModified ?: 0L)
                )
            }
            db.songDao().insertSongs(normalized.map { song ->
                val existing = existingByUri[song.uri] ?: if (song.path.isNotBlank()) existingByPath[song.path.lowercase()] else null
                SongEntity.fromSong(song).copy(
                    isFavorite = existing?.isFavorite ?: song.isFavorite,
                    playCount = existing?.playCount ?: song.playCount,
                    lastPlayed = existing?.lastPlayed ?: song.lastPlayed,
                    dateAdded = existing?.dateAdded?.takeIf { it > 0L } ?: song.dateAdded,
                    dateModified = if (song.dateModified > 0L) song.dateModified else (existing?.dateModified ?: 0L)
                )
            })

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

    private suspend fun scanDocumentFileRecursive(dir: DocumentFile, result: MutableList<Song>, depth: Int) {
        if (depth > 32) return
        currentCoroutineContext().ensureActive()
        val files = try { dir.listFiles() } catch (_: SecurityException) { return }

        for ((index, file) in files.withIndex()) {
            if ((index and 31) == 0) currentCoroutineContext().ensureActive()
            if (file.isDirectory) {
                scanDocumentFileRecursive(file, result, depth + 1)
            } else if (file.isFile) {
                val name = file.name ?: continue
                val ext = name.substringAfterLast('.', "").lowercase()
                if (ext in safAudioExtensions || file.type?.startsWith("audio/") == true) {
                    val uri = file.uri
                    var title = name.substringBeforeLast('.')
                    var artist = "Bilinmeyen Sanatçı"
                    var album = "Bilinmeyen Albüm"
                    var duration = 0L

                    val retriever = MediaMetadataRetriever()
                    try {
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
                    } catch (_: Exception) {
                        // ignore and use fallback file metadata
                    } finally {
                        try { retriever.release() } catch (_: Exception) { }
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

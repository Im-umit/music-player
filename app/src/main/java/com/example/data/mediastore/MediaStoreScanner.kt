package com.example.data.mediastore

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
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

    suspend fun scanMediaStore(): List<Song> = withContext(Dispatchers.IO) {
        val songList = mutableListOf<Song>()
        val folderMap = mutableMapOf<String, Int>() // folderPath -> count

        val collectionUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
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

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 3000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        try {
            context.contentResolver.query(
                collectionUri,
                projection.toTypedArray(),
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
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
                    val id = cursor.getLong(idCol)
                    val title = if (titleCol != -1) cursor.getString(titleCol) ?: "Bilinmeyen Şarkı" else "Bilinmeyen Şarkı"
                    val artist = if (artistCol != -1) {
                        val a = cursor.getString(artistCol)
                        if (a.isNullOrBlank() || a == "<unknown>") "Bilinmeyen Sanatçı" else a
                    } else "Bilinmeyen Sanatçı"

                    val album = if (albumCol != -1) {
                        val al = cursor.getString(albumCol)
                        if (al.isNullOrBlank() || al == "<unknown>") "Bilinmeyen Albüm" else al
                    } else "Bilinmeyen Albüm"

                    val albumId = if (albumIdCol != -1) cursor.getLong(albumIdCol) else 0L
                    val duration = if (durationCol != -1) cursor.getLong(durationCol) else 0L
                    val path = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""
                    val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                    val dateAdded = if (dateAddedCol != -1) cursor.getLong(dateAddedCol) else 0L
                    val dateModified = if (dateModifiedCol != -1) cursor.getLong(dateModifiedCol) else 0L
                    val mimeType = if (mimeTypeCol != -1) cursor.getString(mimeTypeCol) ?: "audio/mpeg" else "audio/mpeg"
                    val trackNumber = if (trackCol != -1) cursor.getInt(trackCol) else 0
                    val year = if (yearCol != -1) cursor.getInt(yearCol) else 0

                    val relativePath = if (relativePathCol != -1) {
                        cursor.getString(relativePathCol) ?: ""
                    } else {
                        if (path.isNotEmpty()) {
                            val file = File(path)
                            file.parentFile?.name ?: ""
                        } else ""
                    }

                    val folderName = if (relativePath.isNotBlank()) {
                        relativePath.trim('/').substringAfterLast('/')
                    } else {
                        if (path.isNotEmpty()) File(path).parentFile?.name ?: "Müzik" else "Müzik"
                    }

                    val contentUri = ContentUris.withAppendedId(collectionUri, id).toString()
                    val albumArtUri = "content://media/external/audio/albumart/$albumId"

                    val song = Song(
                        id = id,
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
                        mimeType = mimeType,
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

                    // Track folders
                    val folderKey = if (relativePath.isNotBlank()) relativePath else folderName
                    folderMap[folderKey] = (folderMap[folderKey] ?: 0) + 1
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // If no music files found on device (e.g. simulator or empty device), provide demo synthwave songs
        if (songList.isEmpty()) {
            val demoSongs = getDemoSongs()
            songList.addAll(demoSongs)
            folderMap["Demo Müzikler"] = demoSongs.size
        }

        // Sync with existing database to preserve favorite flags and play counts
        val existingSongs = db.songDao().getAllSongsAsc().associateBy { it.id }
        val entitiesToSave = songList.map { song ->
            val existing = existingSongs[song.id]
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

        return@withContext songList
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

                    val song = Song(
                        id = uri.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL,
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
                        albumArtUri = ""
                    )
                    result.add(song)
                }
            }
        }
    }

    fun getDemoSongs(): List<Song> {
        val now = System.currentTimeMillis() / 1000
        return listOf(
            Song(
                id = 999001L,
                title = "Cyber Pulse (Neon Synthwave)",
                artist = "Neon Studio",
                album = "Synthwave Nights",
                albumId = 99901L,
                duration = 20000L,
                uri = "asset:///web/demo_cyber_pulse.wav",
                path = "asset:///web/demo_cyber_pulse.wav",
                size = 2560000L,
                dateAdded = now,
                dateModified = now,
                mimeType = "audio/wav",
                trackNumber = 1,
                genre = "Synthwave",
                folderName = "Demo Müzikler",
                relativePath = "Demo Müzikler/",
                albumArtUri = ""
            ),
            Song(
                id = 999002L,
                title = "Neon Overdrive (Electro Beat)",
                artist = "Cyber Runner",
                album = "Cyber Horizon",
                albumId = 99902L,
                duration = 20000L,
                uri = "asset:///web/demo_neon_drive.wav",
                path = "asset:///web/demo_neon_drive.wav",
                size = 2560000L,
                dateAdded = now,
                dateModified = now,
                mimeType = "audio/wav",
                trackNumber = 2,
                genre = "Cyberpunk",
                folderName = "Demo Müzikler",
                relativePath = "Demo Müzikler/",
                albumArtUri = ""
            )
        )
    }

    suspend fun loadDemoTracks(): List<Song> = withContext(Dispatchers.IO) {
        val demoSongs = getDemoSongs()
        val entities = demoSongs.map { SongEntity.fromSong(it) }
        db.songDao().insertSongs(entities)
        db.folderDao().insertFolder(
            FolderEntity(
                path = "Demo Müzikler",
                displayName = "Demo Müzikler",
                songCount = demoSongs.size,
                isSaf = false,
                treeUri = null
            )
        )
        return@withContext demoSongs
    }
}

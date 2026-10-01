package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.db.entity.FolderEntity

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY displayName COLLATE NOCASE ASC")
    suspend fun getAllFolders(): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE isSaf = 1")
    suspend fun getSafFolders(): List<FolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<FolderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE path = :path")
    suspend fun deleteFolder(path: String)

    @Query("DELETE FROM folders WHERE isSaf = 0")
    suspend fun clearNonSafFolders()

    @Query("DELETE FROM folders WHERE displayName = 'Demo Müzikler' OR path = 'Demo Müzikler'")
    suspend fun deleteDemoFolders()
}

package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StyleProfileDao {
    @Query("SELECT * FROM style_profiles WHERE id = 1")
    fun getStyleProfile(): Flow<StyleProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: StyleProfileEntity)
}

package com.alijafari.red.astronomy.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserOccasionDao {

    @Query("SELECT * FROM user_occasions ORDER BY timestampMs ASC")
    fun getAllOccasionsFlow(): Flow<List<UserOccasionEntity>>

    @Query("SELECT COUNT(*) FROM user_occasions")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(occasion: UserOccasionEntity)

    @Update
    suspend fun update(occasion: UserOccasionEntity)

    @Query("DELETE FROM user_occasions WHERE id = :id")
    suspend fun deleteById(id: String)
}

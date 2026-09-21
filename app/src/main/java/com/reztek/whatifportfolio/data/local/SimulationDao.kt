package com.reztek.whatifportfolio.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SimulationDao {

    @Query("SELECT * FROM saved_simulations ORDER BY createdAt DESC")
    fun getAllSimulations(): Flow<List<SavedSimulationEntity>>

    @Query("SELECT * FROM saved_simulations WHERE id = :id LIMIT 1")
    suspend fun getSimulationById(id: Long): SavedSimulationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSimulation(simulation: SavedSimulationEntity): Long

    @Delete
    suspend fun deleteSimulation(simulation: SavedSimulationEntity)

    @Query("DELETE FROM saved_simulations WHERE id = :id")
    suspend fun deleteSimulationById(id: Long)
}
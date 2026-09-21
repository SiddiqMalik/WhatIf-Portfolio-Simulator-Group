package com.reztek.whatifportfolio.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SavedSimulationEntity::class], version = 1, exportSchema = false)
abstract class SimulationDatabase : RoomDatabase() {

    abstract fun simulationDao(): SimulationDao

    companion object {
        @Volatile
        private var INSTANCE: SimulationDatabase? = null

        fun getDatabase(context: Context): SimulationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SimulationDatabase::class.java,
                    "whatif_portfolio_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
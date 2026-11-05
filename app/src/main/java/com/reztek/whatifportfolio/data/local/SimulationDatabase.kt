package com.reztek.whatifportfolio.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SavedSimulationEntity::class, CachedSimulationEntity::class],
    version = 2,
    exportSchema = false
)
abstract class SimulationDatabase : RoomDatabase() {

    abstract fun simulationDao(): SimulationDao

    // PART3-ASH: offline cache of cloud simulations + sync bookkeeping.
    abstract fun cachedSimulationDao(): CachedSimulationDao

    companion object {
        @Volatile
        private var INSTANCE: SimulationDatabase? = null

        /**
         * v1 -> v2: adds the offline cache table. A real migration (rather than the
         * destructive fallback) so existing on-device projections are preserved.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cached_simulations` (" +
                        "`id` TEXT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`startDate` TEXT NOT NULL, " +
                        "`endDate` TEXT NOT NULL, " +
                        "`initialInvestment` REAL NOT NULL, " +
                        "`recurringContribution` REAL NOT NULL, " +
                        "`frequency` TEXT NOT NULL, " +
                        "`status` TEXT NOT NULL, " +
                        "`totalContributed` REAL NOT NULL, " +
                        "`finalValue` REAL NOT NULL, " +
                        "`profitLoss` REAL NOT NULL, " +
                        "`percentReturn` REAL NOT NULL, " +
                        "`realValue` REAL, " +
                        "`realProfitLoss` REAL, " +
                        "`realReturnPct` REAL, " +
                        "`createdAt` TEXT NOT NULL, " +
                        "`updatedAt` TEXT NOT NULL, " +
                        "`syncStatus` TEXT NOT NULL, " +
                        "`retryCount` INTEGER NOT NULL, " +
                        "`lastError` TEXT, " +
                        "`localEditedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
            }
        }

        fun getDatabase(context: Context): SimulationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SimulationDatabase::class.java,
                    "whatif_portfolio_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

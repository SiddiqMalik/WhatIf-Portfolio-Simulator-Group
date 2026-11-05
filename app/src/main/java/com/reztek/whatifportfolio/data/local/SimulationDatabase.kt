package com.reztek.whatifportfolio.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SavedSimulationEntity::class,
        CachedSimulationEntity::class,
        CachedSimulationDetailEntity::class,
        CachedAssetSearchEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class SimulationDatabase : RoomDatabase() {

    abstract fun simulationDao(): SimulationDao

    // PART3-ASH: offline cache of cloud simulations + sync bookkeeping.
    abstract fun cachedSimulationDao(): CachedSimulationDao

    // PART3-ASH: local copies of simulation details and asset searches.
    abstract fun offlineCacheDao(): OfflineCacheDao

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

        /** v2 -> v3: adds the simulation-detail and asset-search caches. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cached_simulation_details` (" +
                        "`id` TEXT NOT NULL, " +
                        "`json` TEXT NOT NULL, " +
                        "`updatedAt` TEXT NOT NULL, " +
                        "`cachedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cached_asset_searches` (" +
                        "`cacheKey` TEXT NOT NULL, " +
                        "`json` TEXT NOT NULL, " +
                        "`cachedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`cacheKey`))"
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

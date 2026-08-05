package com.alijafari.red.astronomy.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FavoriteEntity::class,
        SettingEntity::class,
        CityEntity::class,
        TleEntity::class,
        ObservationLogEntity::class,
        CelestialObjectEntity::class,
        ConstellationEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun favoriteDao(): FavoriteDao
    abstract fun settingDao(): SettingDao
    abstract fun cityDao(): CityDao
    abstract fun observationLogDao(): ObservationLogDao
    abstract fun celestialObjectDao(): CelestialObjectDao
    abstract fun constellationDao(): ConstellationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "red_astronomy_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

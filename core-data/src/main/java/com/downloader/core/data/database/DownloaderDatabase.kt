package com.downloader.core.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.downloader.core.data.entity.*

@Database(
    entities = [
        DownloadJobEntity::class,
        DownloadProgressEntity::class,
        AppSettingsEntity::class,
        MediaInfoCacheEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class DownloaderDatabase : RoomDatabase() {
    abstract fun downloadJobDao(): DownloadJobDao
    abstract fun downloadProgressDao(): DownloadProgressDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun mediaInfoCacheDao(): MediaInfoCacheDao

    companion object {
        @Volatile
        private var INSTANCE: DownloaderDatabase? = null

        fun getDatabase(context: Context): DownloaderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DownloaderDatabase::class.java,
                    "downloader_database"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Insert default settings
            db.execSQL("""
                INSERT INTO app_settings (
                    id, download_folder_uri, default_audio_only, default_include_subtitles,
                    default_include_thumbnail, default_video_format, default_audio_format,
                    max_concurrent_downloads, filename_template
                ) VALUES (
                    1, NULL, 0, 0, 0, 'best', 'best', 1, '%(title)s.%(ext)s'
                )
            """.trimIndent())
        }
    }
}
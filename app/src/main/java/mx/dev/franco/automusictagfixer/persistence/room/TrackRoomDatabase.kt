package mx.dev.franco.automusictagfixer.persistence.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Track::class], version = 3)
abstract class TrackRoomDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDAO

    companion object {
        private var INSTANCE: TrackRoomDatabase? = null

        @Synchronized
        fun getDatabase(context: Context): TrackRoomDatabase {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(
                    context.applicationContext,
                    TrackRoomDatabase::class.java, "DataTrack.db"
                ).addMigrations(MIGRATION_2_3).build()
            }
            return INSTANCE!!
        }

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS tracks_table")
            }
        }
        private val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE track_table ADD COLUMN version INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
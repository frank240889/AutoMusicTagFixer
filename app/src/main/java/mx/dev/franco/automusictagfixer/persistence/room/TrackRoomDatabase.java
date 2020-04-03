package mx.dev.franco.automusictagfixer.persistence.room;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Track.class}, version = 3)
public abstract class TrackRoomDatabase extends RoomDatabase {
    private static TrackRoomDatabase INSTANCE;
    public abstract TrackDAO trackDao();

    public static TrackRoomDatabase getDatabase(final Context context){
        if(INSTANCE == null){
            synchronized (TrackRoomDatabase.class){
                if(INSTANCE == null){
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            TrackRoomDatabase.class,"DataTrack.db").
                            addMigrations(MIGRATION_2_3).
                            build();
                }
            }
        }
        return INSTANCE;
    }

    static final Migration MIGRATION_1_2 = new Migration(1,2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("DROP TABLE IF EXISTS tracks_table");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2,3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE track_table ADD COLUMN version INTEGER NOT NULL DEFAULT 0");
        }
    };
}

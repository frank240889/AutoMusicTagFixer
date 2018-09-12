package mx.dev.franco.automusictagfixer.persistence.room;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.support.annotation.NonNull;

@Database(entities = {Track.class}, version = 2)
public abstract class TrackRoomDatabase extends RoomDatabase {
    private static TrackRoomDatabase INSTANCE;
    public abstract TrackDAO trackDao();

    public static TrackRoomDatabase getDatabase(final Context context){
        if(INSTANCE == null){
            synchronized (TrackRoomDatabase.class){
                if(INSTANCE == null){
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            TrackRoomDatabase.class,"DataTrack.db").
                            addMigrations(MIGRATION_1_2).
                            //addCallback(sRoomDatabaseCallback).
                            build();
                }
            }
        }
        return INSTANCE;
    }

    static final Migration MIGRATION_1_2 = new Migration(1,2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("DROP TABLE IF EXISTS tracks_table");        }
    };
}

package mx.dev.franco.automusictagfixer.persistence.room;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import mx.dev.franco.automusictagfixer.utilities.Constants;

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
                            fallbackToDestructiveMigration().
                            addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    SharedPreferences sharedPreferences =
                                            context.getSharedPreferences(
                                                    Constants.Application.FULL_QUALIFIED_NAME,
                                                    Context.MODE_PRIVATE);
                                    sharedPreferences.edit().putBoolean("research", true).apply();
                                }

                                @Override
                                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                                    super.onOpen(db);
                                }
                            }).
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
}

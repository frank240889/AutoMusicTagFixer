package mx.dev.franco.automusictagfixer.persistence.room;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteQuery;

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
                            fallbackToDestructiveMigrationFrom(1).
                            addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                }

                                @Override
                                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                                    super.onOpen(db);
                                    if(db.getVersion() == 2) {
                                        String query = "SELECT * FROM track_table";
                                        SupportSQLiteQuery sqLiteQuery = new SimpleSQLiteQuery(query);
                                        Cursor cursor = db.query(sqLiteQuery);
                                        boolean hasPermission = ContextCompat.
                                                checkSelfPermission(context,
                                                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                                == PackageManager.PERMISSION_GRANTED;
                                        if (!cursor.moveToFirst() && hasPermission) {
                                            Intent intent = new Intent(Constants.Actions.ACTION_RESCAN);
                                            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                                        }
                                    }
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

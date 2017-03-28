package mx.dev.franco.musicallibraryorganizer;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by franco on 7/03/17.
 */

public class DataTrackDbHelper extends SQLiteOpenHelper {
    private static final String TEXT_TYPE = " TEXT";
    private static final String BOOLEAN_TYPE = " BOOLEAN";
    private static final String COMA_SEP = ",";
    private static final String SQL_CREATE_ENTTRIES = "CREATE TABLE " + TrackContract.TracKData.TABLE_NAME + " (" +
            TrackContract.TracKData._ID + " INTEGER PRIMARY KEY," +
            TrackContract.TracKData.COLUMN_NAME_TITLE + TEXT_TYPE + COMA_SEP +
            TrackContract.TracKData.COLUMN_NAME_AUTHOR + TEXT_TYPE + COMA_SEP +
            TrackContract.TracKData.COLUMN_NAME_ALBUM + TEXT_TYPE + COMA_SEP +
            TrackContract.TracKData.COLUMN_NAME_PATH + TEXT_TYPE + COMA_SEP +
            TrackContract.TracKData.COLUMN_NAME_PROCESSED_TRACK + BOOLEAN_TYPE + " )";
    private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + TrackContract.TracKData.TABLE_NAME;
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "DataTrack.db";


    public DataTrackDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        db.execSQL(SQL_CREATE_ENTTRIES);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion){
        onUpgrade(db,oldVersion,newVersion);
    }
}

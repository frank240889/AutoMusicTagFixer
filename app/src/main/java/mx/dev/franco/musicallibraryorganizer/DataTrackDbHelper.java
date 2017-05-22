package mx.dev.franco.musicallibraryorganizer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * Created by franco on 7/03/17.
 */

/**
 * Class that extends from a helper database class,
 * it helps us, overriding certain methods, to create, update, drop
 * and define the sql statements and querys for operations over the DB.
 */
public class DataTrackDbHelper extends SQLiteOpenHelper {
    // We define the SQL statements for create the database and one table
    private static final String TEXT_TYPE = " TEXT";
    private static final String BOOLEAN_TYPE = " BOOLEAN";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String REAL_TYPE = " REAL";
    private static final String COMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES_FOR_TRACKDATA = "CREATE TABLE " + TrackContract.TrackData.TABLE_NAME + " (" +
            TrackContract.TrackData._ID + " INTEGER PRIMARY KEY," +
            TrackContract.TrackData.COLUMN_NAME_TITLE + TEXT_TYPE + COMA_SEP +
            TrackContract.TrackData.COLUMN_NAME_ARTIST + TEXT_TYPE + COMA_SEP +
            TrackContract.TrackData.COLUMN_NAME_ALBUM + TEXT_TYPE + COMA_SEP +
            TrackContract.TrackData.COLUMN_NAME_DURATION + INTEGER_TYPE + COMA_SEP +
            TrackContract.TrackData.COLUMN_NAME_FILE_SIZE + TEXT_TYPE + COMA_SEP +
            TrackContract.TrackData.COLUMN_NAME_CURRENT_FILENAME + TEXT_TYPE + COMA_SEP +
            TrackContract.TrackData.COLUMN_NAME_CURRENT_FULL_PATH + TEXT_TYPE + COMA_SEP +
            TrackContract.TrackData.COLUMN_NAME_STATUS + INTEGER_TYPE + " )";

    private static final String SQL_CREATE_ENTRIES_FOR_TRACK_SELECTED = "CREATE TABLE " + TrackContract.TrackSelected.TABLE_NAME + " (" +
            TrackContract.TrackSelected._ID + " INTEGER PRIMARY KEY," +
            TrackContract.TrackSelected.COLUMN_NAME_ID_TRACK + INTEGER_TYPE + COMA_SEP +
            TrackContract.TrackSelected.COLUMN_NAME_FILE_TYPE + TEXT_TYPE + COMA_SEP +
            TrackContract.TrackSelected.COLUMN_NAME_CURRENT_FULL_PATH + TEXT_TYPE + COMA_SEP +
            TrackContract.TrackSelected.COLUMN_NAME_STATUS + INTEGER_TYPE + " )";

    private static final String SQL_DELETE_ENTRIES_FOR_TRACKDATA = "DROP TABLE IF EXISTS " + TrackContract.TrackData.TABLE_NAME;
    private static final String SQL_DELETE_ENTRIES_FOR_TRACK_SELECTED = "DROP TABLE IF EXISTS " + TrackContract.TrackSelected.TABLE_NAME;
    //Initial version of DB,
    private static final int DATABASE_VERSION = 1;
    static final String DATABASE_NAME = "DataTrack.db";


    DataTrackDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(SQL_CREATE_ENTRIES_FOR_TRACKDATA);
        db.execSQL(SQL_CREATE_ENTRIES_FOR_TRACK_SELECTED);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES_FOR_TRACKDATA);
        db.execSQL(SQL_CREATE_ENTRIES_FOR_TRACKDATA);
        db.execSQL(SQL_DELETE_ENTRIES_FOR_TRACK_SELECTED);
        db.execSQL(SQL_CREATE_ENTRIES_FOR_TRACK_SELECTED);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion){
        onUpgrade(db,oldVersion,newVersion);
    }

    void removeItem(long id, String tableName){
        String selection;
        if(tableName.equals(TrackContract.TrackData.TABLE_NAME)){
            selection = BaseColumns._ID + " = ?";
        }
        else {
            selection = TrackContract.TrackSelected.COLUMN_NAME_ID_TRACK + " = ?";
        }

        Log.d("REMOVE_ITEM",id+"");

        String[] selectionArgs = {id+""};
        int result = getWritableDatabase().delete(tableName, selection, selectionArgs);
        Log.d("RESULT_REMOVE",result+"");
    }

    long saveFileData(ContentValues data, String tableName){
        // Inserta nuevo registro y devuelve el id del registro
        long id = getWritableDatabase().insert(tableName, null, data);
        return id;
    }


    Cursor getDataFromDB(){
        String[] projection = {
                TrackContract.TrackData._ID,
                TrackContract.TrackData.COLUMN_NAME_TITLE,
                TrackContract.TrackData.COLUMN_NAME_ARTIST,
                TrackContract.TrackData.COLUMN_NAME_ALBUM,
                TrackContract.TrackData.COLUMN_NAME_DURATION,
                TrackContract.TrackData.COLUMN_NAME_FILE_SIZE,
                TrackContract.TrackData.COLUMN_NAME_CURRENT_FILENAME,
                TrackContract.TrackData.COLUMN_NAME_CURRENT_FULL_PATH,
                TrackContract.TrackData.COLUMN_NAME_STATUS
        };

        Cursor c = getReadableDatabase().query(
                TrackContract.TrackData.TABLE_NAME,                     // Tabla a consultar
                projection,                               // Qué columnas se obtendran
                null,                                // Las columnas a consultar en el WHERE
                null,                            // El valor buscado en el WHERE
                null,                                     // null para no agrupar
                null,                                     // null para no filtrar
                null                                 // null para no ordenar
        );

        return (c != null && c.getCount() > 0)?c:null;
    }

    Cursor getStatus(long id){

        String selection = TrackContract.TrackData._ID + " = ?";

        String[] projection = {
                TrackContract.TrackData._ID
        };

        String[] selectionArgs = {String.valueOf(id)};
        Cursor c = getReadableDatabase().query(
                TrackContract.TrackData.TABLE_NAME,                     // Tabla a consultar
                projection,                               // Qué columnas se obtendran
                selection,                                // Las columnas a consultar en el WHERE
                selectionArgs,                            // El valor buscado en el WHERE
                null,                                     // null para no agrupar
                null,                                     // null para no filtrar
                null                                 // null para no ordenar
        );

        return (c != null && c.getCount() > 0)?c:null;
    }

    String getPath(long id, String tableName){
        String[] projection = {
                TrackContract.TrackData._ID,
                TrackContract.TrackData.COLUMN_NAME_CURRENT_FULL_PATH,
        };

        String selection = TrackContract.TrackData._ID + " = ?";
        String[] selectionArgs = {id+""};

        Cursor c = getReadableDatabase().query(
                tableName,                     // Tabla a consultar
                projection,                               // Qué columnas se obtendran
                selection,                                // Las columnas a consultar en el WHERE
                selectionArgs,                            // El valor buscado en el WHERE
                null,                                     // null para no agrupar
                null,                                     // null para no filtrar
                null                                 // null para no ordenar
        );
        if(c != null && c.getCount() > 0) {
            c.moveToFirst();
            return c.getString(c.getColumnIndex(TrackContract.TrackData.COLUMN_NAME_CURRENT_FULL_PATH));
        }

        return "";
    }

    boolean existInDatabase(String path){
        String[] projection = {
                TrackContract.TrackData._ID,
                TrackContract.TrackData.COLUMN_NAME_CURRENT_FULL_PATH,
        };

        String selection = TrackContract.TrackData.COLUMN_NAME_CURRENT_FULL_PATH + " = ?";
        String[] selectionArgs = {path};

        Cursor c = getReadableDatabase().query(
                TrackContract.TrackData.TABLE_NAME,                     // Tabla a consultar
                projection,                               // Qué columnas se obtendran
                selection,                                // Las columnas a consultar en el WHERE
                selectionArgs,                            // El valor buscado en el WHERE
                null,                                     // null para no agrupar
                null,                                     // null para no filtrar
                null                                 // null para no ordenar
        );
        if(c != null) {
            c.moveToFirst();
            if(c.getCount() > 0 && c.getString(c.getColumnIndex(TrackContract.TrackData.COLUMN_NAME_CURRENT_FULL_PATH)).equals(path)){
                return true;
            }
        }
        return false;
    }

    int setStatus(long id, ContentValues contentValues){
        String selection = TrackContract.TrackData._ID + " = ?";
        String[] selectionArgs = {id+""};
        int updatedRow = getWritableDatabase().update(TrackContract.TrackData.TABLE_NAME, contentValues, selection, selectionArgs);
        //Log.d("ID", String.valueOf(updatedRow));
        return updatedRow;
    }

    int updateData(Long id, ContentValues contentValues){
        Log.d("ID", String.valueOf(id));
        String selection = TrackContract.TrackData._ID + " = ?";
        String[] selectionArgs = {id+""};
        int updatedRow = getWritableDatabase().update(TrackContract.TrackData.TABLE_NAME, contentValues, selection, selectionArgs);
        //Log.d("ROWS AFFECTED", String.valueOf(updatedRow));
        return updatedRow;
    }

    int[] clearDb(){

        int[] res = new int[2];
        int result1 = getWritableDatabase().delete(TrackContract.TrackData.TABLE_NAME, null, null);
        int result2 = getWritableDatabase().delete(TrackContract.TrackSelected.TABLE_NAME, null, null);
        res[0] = result1;
        res[1] = result2;
        Log.d("RESULT_REMOVE",result1+" - " +result2);
        return res;
    }

}

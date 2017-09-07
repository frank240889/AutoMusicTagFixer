package mx.dev.franco.musicallibraryorganizer;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.Arrays;

/*
  Created by franco on 7/03/17.
  DbHelper for Database Operations
 */

/**
 * Class that extends from a helper database class,
 * it helps us, overriding certain methods, to create, update, drop
 * and define the sql statements and querys for operations over the DB.
 */
public class DataTrackDbHelper extends SQLiteOpenHelper {
    // We define the SQL statements for create the database and one table
    @SuppressLint("StaticFieldLeak")
    static boolean existDatabase = false;
    private static DataTrackDbHelper dbHelper;
    private static final String TEXT_TYPE = " TEXT";
    private static final String BLOB_TYPE = " BLOB";
    private static final String BOOLEAN_TYPE = " BOOLEAN";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String REAL_TYPE = " REAL";
    private static final String COMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES_FOR_TRACK_DATA = "CREATE TABLE " + TrackContract.TrackData.TABLE_NAME + " (" +
            TrackContract.TrackData._ID + " INTEGER PRIMARY KEY," +
            TrackContract.TrackData.COLUMN_NAME_MEDIASTORE_ID + INTEGER_TYPE + COMA_SEP +
            TrackContract.TrackData.COLUMN_NAME_TITLE + TEXT_TYPE + COMA_SEP +
            TrackContract.TrackData.COLUMN_NAME_ARTIST + TEXT_TYPE + COMA_SEP +
            TrackContract.TrackData.COLUMN_NAME_ALBUM + TEXT_TYPE + COMA_SEP +
            TrackContract.TrackData.COLUMN_NAME_DURATION + INTEGER_TYPE + COMA_SEP +
            TrackContract.TrackData.COLUMN_NAME_FILE_SIZE + TEXT_TYPE + COMA_SEP +
            TrackContract.TrackData.COLUMN_NAME_CURRENT_FILENAME + TEXT_TYPE + COMA_SEP +
            TrackContract.TrackData.COLUMN_NAME_CURRENT_PATH + TEXT_TYPE + COMA_SEP +
            TrackContract.TrackData.COLUMN_NAME_CURRENT_FULL_PATH + TEXT_TYPE + COMA_SEP +

            TrackContract.TrackData.COLUMN_NAME_IS_SELECTED + BOOLEAN_TYPE + COMA_SEP +
            TrackContract.TrackData.COLUMN_NAME_STATUS + INTEGER_TYPE + COMA_SEP +
            TrackContract.TrackData.COLUMN_NAME_COVER_ART + COMA_SEP +
            TrackContract.TrackData.COLUMN_NAME_ADDED_RECENTLY + BOOLEAN_TYPE + " )";

    private static final String SQL_DELETE_ENTRIES_FOR_TRACK_DATA = "DROP TABLE IF EXISTS " + TrackContract.TrackData.TABLE_NAME;
    //Initial version of DB,
    private static final int DATABASE_VERSION = 1;
    static final String DATABASE_NAME = "DataTrack.db";
    private Context _context;


    private DataTrackDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        _context =  context;
    }

    /**
     * We use a singleton for save resources, and use
     * the same instance for DB operations.
     * @param context
     * @return
     */
    public static DataTrackDbHelper getInstance(Context context){
        if(dbHelper == null){
            dbHelper =  new DataTrackDbHelper(context);
        }
        return dbHelper;
    }

    /**
     * It executes only one time, to create the database.
     * @param db
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES_FOR_TRACK_DATA);
    }

    /**
     * In case we upgrade the DB, first delete the current BD,
     * and then recreate it.
     * @param db
     * @param oldVersion
     * @param newVersion
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES_FOR_TRACK_DATA);
        db.execSQL(SQL_CREATE_ENTRIES_FOR_TRACK_DATA);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion){
        onUpgrade(db,oldVersion,newVersion);
    }

    public void removeItem(long id, String tableName){
        String selection = TrackContract.TrackData._ID + " = ?";
        Log.d("REMOVE_ITEM",id+"");

        String[] selectionArgs = {id+""};
        int result = getWritableDatabase().delete(tableName, selection, selectionArgs);
        Log.d("RESULT_REMOVE",result+"");
        this.close();
    }

    public long insertRow(ContentValues data, String tableName){
        // Inserta nuevo registro y devuelve el id del registro
        long id = getWritableDatabase().insert(tableName, null,data);
        this.close();
        return id;
    }


    public Cursor getDataFromDB(){
        String[] projection = {
                TrackContract.TrackData._ID,
                TrackContract.TrackData.COLUMN_NAME_TITLE,
                TrackContract.TrackData.COLUMN_NAME_ARTIST,
                TrackContract.TrackData.COLUMN_NAME_ALBUM,
                TrackContract.TrackData.COLUMN_NAME_DURATION,
                TrackContract.TrackData.COLUMN_NAME_FILE_SIZE,
                TrackContract.TrackData.COLUMN_NAME_CURRENT_FILENAME,
                TrackContract.TrackData.COLUMN_NAME_CURRENT_PATH,
                TrackContract.TrackData.COLUMN_NAME_CURRENT_FULL_PATH,
                TrackContract.TrackData.COLUMN_NAME_IS_SELECTED,
                TrackContract.TrackData.COLUMN_NAME_STATUS,
                TrackContract.TrackData.COLUMN_NAME_COVER_ART,
                TrackContract.TrackData.COLUMN_NAME_ADDED_RECENTLY
        };


        return getReadableDatabase().query(
                TrackContract.TrackData.TABLE_NAME,                     // Tabla a consultar
                projection,                               // Qué columnas se obtendran
                null,                                // Las columnas a consultar en el WHERE
                null,                            // El valor buscado en el WHERE
                null,                                     // null para no agrupar
                null,                                     // null para no filtrar
                null                                 // null para no ordenar
        );
    }

    public Cursor getDataFromDB(String[] projection, String selection, String[] selectionArgs){
        return getReadableDatabase().query(
                TrackContract.TrackData.TABLE_NAME,                     // Tabla a consultar
                projection,                               // Qué columnas se obtendran
                selection,                                // Las columnas a consultar en el WHERE
                selectionArgs,                            // El valor buscado en el WHERE
                null,                                     // null para no agrupar
                null,                                     // null para no filtrar
                null                                 // null para no ordenar
        );

    }

    public Cursor getDataRow(long id){
        String selection = TrackContract.TrackData._ID + " = ?";

        String[] selectionArgs = {String.valueOf(id)};

        String[] projection = {
                TrackContract.TrackData._ID,
                TrackContract.TrackData.COLUMN_NAME_TITLE,
                TrackContract.TrackData.COLUMN_NAME_ARTIST,
                TrackContract.TrackData.COLUMN_NAME_ALBUM,
                TrackContract.TrackData.COLUMN_NAME_DURATION,
                TrackContract.TrackData.COLUMN_NAME_FILE_SIZE,
                TrackContract.TrackData.COLUMN_NAME_CURRENT_FILENAME,
                TrackContract.TrackData.COLUMN_NAME_CURRENT_PATH,
                TrackContract.TrackData.COLUMN_NAME_CURRENT_FULL_PATH,
                TrackContract.TrackData.COLUMN_NAME_IS_SELECTED,
                TrackContract.TrackData.COLUMN_NAME_STATUS
        };

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

    public boolean existInDatabase(long id){
        String[] projection = {
                TrackContract.TrackData.COLUMN_NAME_MEDIASTORE_ID,
        };

        String selection = TrackContract.TrackData.COLUMN_NAME_MEDIASTORE_ID + " = ?";
        String[] selectionArgs = {id+""};

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
            if(c.getCount() > 0 && c.getInt(c.getColumnIndex(TrackContract.TrackData.COLUMN_NAME_MEDIASTORE_ID)) == id){
                c.close();
                return true;
            }
        }
        return false;
    }

    public boolean existInDatabase(String path){
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
                c.close();
                return true;
            }
        }
        return false;
    }

    public int setStatus(long id, ContentValues contentValues){
        String selection = TrackContract.TrackData._ID + " = ?";
        String[] selectionArgs = {id+""};
        int updatedRow = getWritableDatabase().update(TrackContract.TrackData.TABLE_NAME, contentValues, selection, selectionArgs);
        this.close();
        //Log.d("ID", String.valueOf(updatedRow));
        return updatedRow;
    }

    public int updateData(long id, ContentValues contentValues){

        String selection = TrackContract.TrackData._ID + " = ?";
        String selectionArgs[] = {id+""};
        return getWritableDatabase().update(TrackContract.TrackData.TABLE_NAME, contentValues, selection, selectionArgs);
    }

    public int updateData(ContentValues contentValues){
        int updatedRow = getWritableDatabase().update(TrackContract.TrackData.TABLE_NAME, contentValues, null, null );
        //this.close();
        //Log.d("ROWS AFFECTED", String.valueOf(updatedRow));
        return updatedRow;
    }

    public int clearDb(){
        int res;
        int result1 = getWritableDatabase().delete(TrackContract.TrackData.TABLE_NAME, null, null);
        res = result1;
        Log.d("RESULT_REMOVE",result1+"");
        this.close();
        return res;
    }

    /**
     * This method returns total elements is table
     * @return
     */
    public int getCount(String tableName){
        String name;
        if(tableName == null || tableName.equals(""))
            name = TrackContract.TrackData.TABLE_NAME;
        else
            name = tableName;

        return getReadableDatabase().query(
                name,                     // Tabla a consultar
                null,                               // Qué columnas se obtendran
                null,                                // Las columnas a consultar en el WHERE
                null,                            // El valor buscado en el WHERE
                null,                                     // null para no agrupar
                null,                                     // null para no filtrar
                null                                 // null para no ordenar
        ).getCount();
    }

    public void closeConnection(){
        this.close();
    }

    public static boolean existDatabase(Context context){
        File db = context.getApplicationContext().getDatabasePath(DATABASE_NAME);
        return db.exists();
    }

    public int deleteItems(String[] ids) { //ids is an array
        String idsCSV = TextUtils.join(",", ids);
        String placeholders = new String(new char[ids.length-1]).replace("\0", "?,") + "?";
        Log.d("ids",idsCSV + "placeholders "+ placeholders + "_" + Arrays.toString(ids));
        int rowsAffected = getWritableDatabase().delete(TrackContract.TrackData.TABLE_NAME, "CAST(" + TrackContract.TrackData._ID+" AS TEXT) IN (" + placeholders + ")", ids);
        close();
        Log.d("items_deleted",rowsAffected+"");
        return rowsAffected;
    }
}

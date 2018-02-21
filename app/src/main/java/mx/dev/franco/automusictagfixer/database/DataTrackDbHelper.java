package mx.dev.franco.automusictagfixer.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.io.File;

/*
  Created by franco on 7/03/17.
  DbHelper for Database Operations
 */

/**
 * Class that extends from a helper database class,
 * it helps us to create, update, drop
 * and define the sql statements and queries for operations over the DB.
 */
public class DataTrackDbHelper extends SQLiteOpenHelper {
    // We define the SQL statements for create the database and one table
    @SuppressLint("StaticFieldLeak")
    private static DataTrackDbHelper dbHelper;
    private static final String TEXT_TYPE = " TEXT";
    private static final String BOOLEAN_TYPE = " BOOLEAN";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String COMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES_FOR_TRACK_DATA = "CREATE TABLE " + TrackContract.TrackData.TABLE_NAME + " (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            TrackContract.TrackData.MEDIASTORE_ID + INTEGER_TYPE + COMA_SEP +
            TrackContract.TrackData.TITLE + TEXT_TYPE+ COMA_SEP +
            TrackContract.TrackData.ARTIST + TEXT_TYPE + COMA_SEP +
            TrackContract.TrackData.ALBUM + TEXT_TYPE + COMA_SEP +
            TrackContract.TrackData.DATA + TEXT_TYPE + COMA_SEP +
            TrackContract.TrackData.STATUS + INTEGER_TYPE + " DEFAULT 0" + COMA_SEP +
            TrackContract.TrackData.IS_SELECTED + BOOLEAN_TYPE + " DEFAULT false"+COMA_SEP +
            TrackContract.TrackData.IS_PROCESSING + BOOLEAN_TYPE + " DEFAULT false" + " )";

    private static final String SQL_DELETE_ENTRIES_FOR_TRACK_DATA = "DROP TABLE IF EXISTS " + TrackContract.TrackData.TABLE_NAME;
    //Initial version of DB,
    private static final int DATABASE_VERSION = 1;
    static final String DATABASE_NAME = "DataTrack.db";
    //Remember to use getApplicationContext() to avoid memory leaks;
    private Context mContext;


    /**
     * Private constructor, we only need one connection to DB
     * @param context
     */
    private DataTrackDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        if(mContext == null)
            mContext =  context;
    }

    /**
     * We use a singleton for save resources, and use
     * the same instance for DB operations.
     * @param context
     * @return
     */
    public static synchronized DataTrackDbHelper getInstance(Context context){
        if(dbHelper == null){
            dbHelper =  new DataTrackDbHelper(context.getApplicationContext());
            dbHelper.getWritableDatabase();
        }
        return dbHelper;
    }

    /**
     * Callback that executes only one time, when DB is created.
     * @param db
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES_FOR_TRACK_DATA);
    }

    /**
     * Implements the logic when DB
     * changes,  for example, adding a new column
     * to any table
     * @param db
     * @param oldVersion
     * @param newVersion
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES_FOR_TRACK_DATA);
    }

    /**
     * Not implemented, used when a DB downgrade is
     * executed
     * @param db
     * @param oldVersion
     * @param newVersion
     */
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion){
        onUpgrade(db,oldVersion,newVersion);
    }

    /**
     * Remove a row from App DB
     * @param id
     * @param tableName
     */
    public synchronized void removeItem(long id, String tableName){
        String selection = TrackContract.TrackData.MEDIASTORE_ID + " = ?";
        String[] selectionArgs = {id+""};
        getWritableDatabase().delete(tableName, selection, selectionArgs);
    }

    /**
     * Insert new row to App DB
     * @param data
     * @param tableName
     * @return id corresponding to current row inserted
     */
    public synchronized long insertItem(ContentValues data, String tableName){
        long id = getWritableDatabase().insert(tableName, null,data);
        return id;
    }

    /**
     * Gets all data from table TrackContract.TrackData.TABLE_NAME
     * @return cursor object or null if no data
     * @param orderBy
     */
    public Cursor getDataFromDB(String orderBy){

        String[] projection = {
                TrackContract.TrackData.MEDIASTORE_ID,
                TrackContract.TrackData.TITLE,
                TrackContract.TrackData.ARTIST,
                TrackContract.TrackData.ALBUM,
                TrackContract.TrackData.DATA,
                TrackContract.TrackData.IS_SELECTED,
                TrackContract.TrackData.STATUS,
                TrackContract.TrackData.IS_PROCESSING
        };


        return getReadableDatabase().query(
                TrackContract.TrackData.TABLE_NAME,                     // Tabla a consultar
                projection,                               // Qué columnas se obtendran
                null,                                // Las columnas a consultar en el WHERE
                null,                            // El valor buscado en el WHERE
                null,                                     // null para no agrupar
                null,                                     // null para no filtrar
                orderBy                                 // null para no ordenar
        );
    }

    /**
     * get value of "is_selected" column for
     * every id passed
     * @param id
     * @return cursor object or null if no data
     */
    public Cursor getDataRow(long id){
        String selection = TrackContract.TrackData.MEDIASTORE_ID + " = ?";

        String[] selectionArgs = {String.valueOf(id)};

        String[] projection = {
                TrackContract.TrackData.MEDIASTORE_ID,
                TrackContract.TrackData.IS_SELECTED
        };

        Cursor c = getReadableDatabase().query(
                TrackContract.TrackData.TABLE_NAME,     // Table to query
                projection,                             // Which columns are going to retrieve, is the "SELECT columns FROM" part
                selection,                              // The columns against is going to be the "WHERE"
                selectionArgs,                          // Values to match, "WHERE = value"
                null,                                   // Group by, pass null for not grouping
                null,                                   // Filter, pass null for not filtering
                null                                    // Sort, pass null for not sorting
        );

        return (c != null && c.getCount() > 0)?c:null;
    }

    /**
     * Makes a match between audio files in MediaStore
     * and App DB
     * @param id the id from MediaStore to search in App DB
     * @return false in case doesn't exist in our database
     */
    public synchronized boolean existInDatabase(int id){
        String[] projection = {
                TrackContract.TrackData.MEDIASTORE_ID,
        };

        String selection = TrackContract.TrackData.MEDIASTORE_ID + " = ?";
        String[] selectionArgs = {id+""};

        Cursor c = getReadableDatabase().query(
                TrackContract.TrackData.TABLE_NAME,     // Table to query
                projection,                             // Which columns are going to retrieve, is the "SELECT columns FROM" part
                selection,                              // The columns against is going to be the "WHERE"
                selectionArgs,                          // Values to match, "WHERE = value"
                null,                                   // Group by, pass null for not grouping
                null,                                   // Filter, pass null for not filtering
                null                                    // Sort, pass null for not sorting
        );
        if(c != null) {
            c.moveToFirst();
            if(c.getCount() > 0 && c.getInt(c.getColumnIndex(TrackContract.TrackData.MEDIASTORE_ID)) == id){
                c.close();
                return true;
            }
        }
        c.close();
        return false;
    }

    /**
     * Updates one row
     * @param id
     * @param contentValues
     * @return how many rows were updated
     */
    public synchronized int updateData(long id, ContentValues contentValues){

        String selection = TrackContract.TrackData.MEDIASTORE_ID + " = ?";
        String selectionArgs[] = {id+""};
        return getWritableDatabase().update(TrackContract.TrackData.TABLE_NAME, contentValues, selection, selectionArgs);
    }

    /**
     * Updates data for all items
     * @param contentValues object that wraps columns and new values to apply
     * @return how many rows were updated
     */
    public synchronized int updateData(ContentValues contentValues){
        int updatedRow = getWritableDatabase().update(TrackContract.TrackData.TABLE_NAME, contentValues, null, null );
        return updatedRow;
    }

    /**
     * Updates "is_processing" column to
     * true or false
     * @param contentValues New value to set
     * @param columnToUpdate represents the "where columnname" condition
     * @param condition represent the " = somevalue" part of where condition
     * @return
     */
    public synchronized int updateData(ContentValues contentValues, String columnToUpdate, boolean condition){
        String whereClause = columnToUpdate + " = ?";
        String[] whereArgs = {(condition?1:0)+""};
        return getWritableDatabase().update(TrackContract.TrackData.TABLE_NAME, contentValues, whereClause, whereArgs );
    }
    /**
     * Remove all tables from DB
     * @return number of tables deleted
     */
    public synchronized int clearDb(){
        return getWritableDatabase().delete(TrackContract.TrackData.TABLE_NAME, null, null);
    }

    /**
     * Returns total elements in table
     * @return number of elements
     */
    public synchronized int getCount(){
        return getReadableDatabase().query(
                TrackContract.TrackData.TABLE_NAME,     // Table to query
                null,                             // Which columns are going to retrieve, is the "SELECT columns FROM" part
                null,                              // The columns against is going to be the "WHERE"
                null,                          // Values to match, "WHERE = value"
                null,                                   // Group by, pass null for not grouping
                null,                                   // Filter, pass null for not filtering
                null                                    // Sort, pass null for not sorting
        ).getCount();
    }

    /**
     * Checks if DB file exist in smartphone
     * @param context
     * @return true if exists
     */
    public synchronized
    static boolean existDatabase(Context context){
        File db = context.getApplicationContext().getDatabasePath(DATABASE_NAME);
        return db.exists();
    }

    /**
     * Get all items marked as 1 in its "selected" column
     * @return cursor object or null if no selected items
     */
    public Cursor getAllSelected(long id, String sort){
        String selection = null;
        String[] selectionArgs;
        //when id is -1 means that we are querying
        //all "selected" items, because when id is different that -1
        //we are querying only to one id
        if(id == -1){
            selection = TrackContract.TrackData.IS_SELECTED + " = ?";
            selectionArgs = new String[]{1 + ""}; //we cannot pass "true", because is store as integer
        }
        else {
            selection = TrackContract.TrackData.MEDIASTORE_ID + " = ?";
            selectionArgs = new String[]{id+""}; //we cannot pass "true", because is store as integer
        }

        String[] projection = {
                TrackContract.TrackData.MEDIASTORE_ID,
                TrackContract.TrackData.TITLE,
                TrackContract.TrackData.ARTIST,
                TrackContract.TrackData.ALBUM,
                TrackContract.TrackData.DATA,
                TrackContract.TrackData.IS_SELECTED,
                TrackContract.TrackData.STATUS
        };

        Cursor c = getReadableDatabase().query(
                TrackContract.TrackData.TABLE_NAME,                     // table to query
                projection,                               // Columns to get
                selection,                                // columns to query
                selectionArgs,                            // value of WHERE
                null,                                     // null for no grouping
                null,                                     // null for no filtering
                sort                                 // null for no ordering
        );
        return c;
    }

}

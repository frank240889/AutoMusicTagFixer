package mx.dev.franco.musicallibraryorganizer.database;

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
 * it helps us, overriding certain methods, to create, update, drop
 * and define the sql statements and querys for operations over the DB.
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
            dbHelper.getWritableDatabase();
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
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion){
        onUpgrade(db,oldVersion,newVersion);
    }

    /**
     * Remove an item_list from our database
     * in case already doesn't exist
     * in smartphone
     * @param id
     * @param tableName
     */
    public void removeItem(long id, String tableName){
        String selection = TrackContract.TrackData.MEDIASTORE_ID + " = ?";
        String[] selectionArgs = {id+""};
        getWritableDatabase().delete(tableName, selection, selectionArgs);
    }

    /**
     * Adds new item_list to our database
     * @param data
     * @param tableName
     * @return id corresponding to current row inserted
     */
    public long insertItem(ContentValues data, String tableName){
        long id = getWritableDatabase().insert(tableName, null,data);
        this.close();
        return id;
    }

    /**
     * get all data for populating
     * our recycler view adapter
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
     * get only the absolute path stored
     * for checking if item_list exists or not
     * in smartphone
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

    /**
     * if there are any recent audio file
     * added to smartphone, it verifies
     * that already has been added to database
     * @param id
     * @return false in case doesn't exist in our database
     */
    public boolean existInDatabase(int id){
        String[] projection = {
                TrackContract.TrackData.MEDIASTORE_ID,
        };

        String selection = TrackContract.TrackData.MEDIASTORE_ID + " = ?";
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
            if(c.getCount() > 0 && c.getInt(c.getColumnIndex(TrackContract.TrackData.MEDIASTORE_ID)) == id){
                c.close();
                return true;
            }
        }
        return false;
    }

    public boolean existInDatabase(String path){
        String[] projection = {
                TrackContract.TrackData.MEDIASTORE_ID,
                TrackContract.TrackData.DATA,
        };

        String selection = TrackContract.TrackData.DATA + " = ?";
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
            if(c.getCount() > 0 && c.getString(c.getColumnIndex(TrackContract.TrackData.DATA)).equals(path)){
                c.close();
                return true;
            }
        }
        return false;
    }

    /**
     * update row from provided
     * content values object
     * @param id
     * @param contentValues
     * @return how many rows were updated
     */
    public int updateData(long id, ContentValues contentValues){

        String selection = TrackContract.TrackData.MEDIASTORE_ID + " = ?";
        String selectionArgs[] = {id+""};
        return getWritableDatabase().update(TrackContract.TrackData.TABLE_NAME, contentValues, selection, selectionArgs);
    }

    /**
     * update any column data for all items
     * @param contentValues
     * @return how many rows were updated
     */
    public int updateData(ContentValues contentValues){
        int updatedRow = getWritableDatabase().update(TrackContract.TrackData.TABLE_NAME, contentValues, null, null );
        return updatedRow;
    }

    public int updateData(ContentValues contentValues, String columnToUpdate, boolean condition){
        String whereClause = columnToUpdate + " = ?";
        String[] whereArgs = {(condition?1:0)+""};
        return getWritableDatabase().update(TrackContract.TrackData.TABLE_NAME, contentValues, whereClause, whereArgs );
    }

    public int updateData(ContentValues contentValues, String columnToUpdate, String condition){
        String whereClause = columnToUpdate + " = ?";
        String[] whereArgs = {condition+""};
        return getWritableDatabase().update(TrackContract.TrackData.TABLE_NAME, contentValues, whereClause, whereArgs );
    }

    public int updateData(ContentValues contentValues, String columnToUpdate, int condition){
        String whereClause = columnToUpdate + " = ?";
        String[] whereArgs = {condition+""};
        return getWritableDatabase().update(TrackContract.TrackData.TABLE_NAME, contentValues, whereClause, whereArgs );
    }

    /**
     * clears db if any previous construction failed
     * @return number of items deleted
     */
    public int clearDb(){
        return getWritableDatabase().delete(TrackContract.TrackData.TABLE_NAME, null, null);
    }

    /**
     * Returns total elements in table
     * @return number of elements
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

    /**
     * verifies if database exist in smartphone
     * @param context
     * @return true if exists
     */
    public static boolean existDatabase(Context context){
        File db = context.getApplicationContext().getDatabasePath(DATABASE_NAME);
        return db.exists();
    }

    /**
     * get all items marked as true in its selected column
     * @return cursor object or null if no selected items
     */
    public Cursor getAllSelected(long id, String sort){
        String selection = null;
        String[] selectionArgs;
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

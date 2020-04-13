package mx.dev.franco.automusictagfixer.persistence.mediastore;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

/**
 * @author Franco Castillo
 * Helper class to get audio files information from MediaStore.
 */
public class MediaStoreRetriever {
    private MediaStoreRetriever(){}

    /**
     * Get all music from MediaStore.
     * @param context Context to access system resources.
     * @return A cursor containing data of audio files.
     */
    public static Cursor getAllSupportedAudioFiles(Context context) {
        //Select all music
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        //Columns to retrieve
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.AlbumColumns.ALBUM ,
                MediaStore.Audio.Media.DATA // absolute path to audio file
        };

        // Get cursor from content provider
        // the last parameter sorts the data alphanumerically by the "DATA" column in ascendant mode
        return context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null);
    }

    public static String getIdOfURI(Context context, String path) {
        //Select all music
        String selection = MediaStore.Audio.Media.DATA + "=?";

        //Columns to retrieve
        String[] projection = {
                MediaStore.Audio.Media._ID// absolute path to audio file
        };
        Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                new String[]{path},
                null);
        if (cursor != null && cursor.moveToFirst()) {
            int idIndex = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
            String value =  cursor.getString(idIndex);
            cursor.close();
            return value;
        }

        return null;
    }

    /**
     * Get all music from MediaStore.
     * @param context Context to access system resources.
     * @return A cursor containing data of audio files.
     */
    public static Cursor getAllUnsupportedAudioFiles(Context context) {
        //Select all music
        String selection = MediaStore.Files.FileColumns.DATA+ " like ?";

        //Columns to retrieve
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.AlbumColumns.ALBUM ,
                MediaStore.Audio.Media.DATA // absolute path to audio file
        };

        // Get cursor from content provider
        // the last parameter sorts the data alphanumerically by the "DATA" column in ascendant mode
        return context.getContentResolver().query(MediaStore.Files.getContentUri("external"),
                projection,
                selection,
                new String[]{"%SCHILLER%"},
                null);
    }

    public static int swapMediaStoreId(Context context, String mediaStoreId, String newMediaStoreId) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Audio.Media._ID, mediaStoreId);
        return context.getContentResolver().update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                contentValues, "_id=?", new String[]{newMediaStoreId});
    }
}

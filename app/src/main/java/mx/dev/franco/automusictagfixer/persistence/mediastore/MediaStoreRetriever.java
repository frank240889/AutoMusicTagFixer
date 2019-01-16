package mx.dev.franco.automusictagfixer.persistence.mediastore;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

public class MediaStoreRetriever {

    public static Cursor getAllFromDevice(Context context) {
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

        //get mCursorData from content provider
        //the last parameter sorts the mCursorData alphanumerically by the "DATA" column in ascendant mode
        return context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null);//Sort.setDefaultOrder());
    }
}

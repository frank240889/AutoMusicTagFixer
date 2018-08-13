package mx.dev.franco.automusictagfixer.room.database;

import android.provider.BaseColumns;

/**
 * Created by franco on 7/03/17.
 */

public final class TrackContract {
    public TrackContract(){}

    public static class TrackData implements BaseColumns{
        public static final String TABLE_NAME = "tracks_table";
        public static final String MEDIASTORE_ID = "media_store_id";
        public static final String TITLE = "title";
        public static final String ARTIST = "artist";
        public static final String ALBUM = "album";
        public static final String DATA = "_data";
        public static final String IS_PROCESSING = "is_processing";
        public static final String IS_SELECTED = "is_selected";
        public static final String STATUS = "status"; //P for "processed", I for "incomplete data", NA for "no data avaliable", N for "no processed"
    }

}
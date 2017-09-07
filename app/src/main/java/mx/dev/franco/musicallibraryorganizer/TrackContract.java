package mx.dev.franco.musicallibraryorganizer;

import android.provider.BaseColumns;

/**
 * Created by franco on 7/03/17.
 */

public final class TrackContract {
    private TrackContract(){}

    public static class TrackData implements BaseColumns{
        public static final String TABLE_NAME = "tracks_table";
        static final String COLUMN_NAME_MEDIASTORE_ID = "media store id";
        public static final String COLUMN_NAME_TITLE = "track_title";
        public static final String COLUMN_NAME_ARTIST = "track_artist";
        public static final String COLUMN_NAME_ALBUM = "track_album";
        public static final String COLUMN_NAME_DURATION = "track_duration";
        public static final String COLUMN_NAME_CURRENT_FILENAME = "track_current_filename";
        public static final String COLUMN_NAME_CURRENT_PATH = "track_current_path";
        public static final String COLUMN_NAME_CURRENT_FULL_PATH = "track_current_full_path";
        public static final String COLUMN_NAME_FILE_SIZE = "track_file_size";
        public static final String COLUMN_NAME_IS_SELECTED = "is_selected";
        public static final String COLUMN_NAME_ADDED_RECENTLY = "added_recently";
        public static final String COLUMN_NAME_STATUS = "track_status"; //P for "processed", I for "incomplete data", NA for "no data avaliable", N for "no processed"
        public static final String COLUMN_NAME_COVER_ART = "cover_art";
    }

}

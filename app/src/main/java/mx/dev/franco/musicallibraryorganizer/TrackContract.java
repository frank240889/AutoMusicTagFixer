package mx.dev.franco.musicallibraryorganizer;

import android.provider.BaseColumns;

/**
 * Created by franco on 7/03/17.
 */

public final class TrackContract {
    private TrackContract(){}

    public static class TracKData implements BaseColumns{
        public static final String TABLE_NAME = "data_track";
        public static final String COLUMN_NAME_TITLE = "track_title";
        public static final String COLUMN_NAME_AUTHOR = "track_author";
        public static final String COLUMN_NAME_ALBUM = "track_album";
        public static final String COLUMN_NAME_PATH = "track_path";
        public static final String COLUMN_NAME_PROCESSED_TRACK = "processed_track";
    }
}

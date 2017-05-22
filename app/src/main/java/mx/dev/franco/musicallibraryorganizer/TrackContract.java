package mx.dev.franco.musicallibraryorganizer;

import android.provider.BaseColumns;

/**
 * Created by franco on 7/03/17.
 */

public final class TrackContract {
    private TrackContract(){}

    public static class TrackData implements BaseColumns{
        public static final String TABLE_NAME = "tracks_table";
        public static final String COLUMN_NAME_TITLE = "track_title";
        public static final String COLUMN_NAME_ARTIST = "track_artist";
        public static final String COLUMN_NAME_ALBUM = "track_album";
        public static final String COLUMN_NAME_ALBUM_ARTIST = "track_album_artist";
        public static final String COLUMN_NAME_AUTHOR = "track_author";
        public static final String COLUMN_NAME_COMPOSER = "track_composer";
        public static final String COLUMN_NAME_TRACK_NUMBER = "track_number";
        public static final String COLUMN_NAME_YEAR = "track_year";
        public static final String COLUMN_NAME_DISC_NUMBER = "track_disc_number";
        public static final String COLUMN_NAME_DURATION = "track_duration";
        public static final String COLUMN_NAME_GENRE = "track_genre";
        public static final String COLUMN_NAME_WRITER = "track_writer";
        public static final String COLUMN_NAME_FILE_TYPE = "track_file_type";
        public static final String COLUMN_NAME_RESOLUTION = "track_resolution";
        public static final String COLUMN_NAME_SAMPLING_RATE = "track_sampling_rate";
        public static final String COLUMN_NAME_BITRATE = "track_bitrate";
        public static final String COLUMN_NAME_CHANNELS = "track_channels";
        public static final String COLUMN_NAME_ORIGINAL_FILENAME = "track_original_filename";
        public static final String COLUMN_NAME_ORIGINAL_PATH = "track_original_path";
        public static final String COLUMN_NAME_ORIGINAL_FULL_PATH = "track_original_full_path";
        public static final String COLUMN_NAME_CURRENT_FILENAME = "track_current_filename";
        public static final String COLUMN_NAME_CURRENT_PATH = "track_current_path";
        public static final String COLUMN_NAME_CURRENT_FULL_PATH = "track_current_full_path";
        public static final String COLUMN_NAME_FILE_SIZE = "track_file_size";
        public static final String COLUMN_NAME_STATUS = "track_status"; //P for "processed", I for "incomplete data", NA for "no data avaliable", N for "no processed"
    }

    public static class TrackSelected implements BaseColumns{
        public static final String TABLE_NAME = "track_selected";
        public static final String COLUMN_NAME_ID_TRACK = "id_track";
        public static final String COLUMN_NAME_FILE_TYPE = "track_file_type";
        public static final String COLUMN_NAME_CURRENT_FULL_PATH = "track_current_full_path";
        public static final String COLUMN_NAME_STATUS = "track_status"; //P for "pending", C for "complete";
    }
}

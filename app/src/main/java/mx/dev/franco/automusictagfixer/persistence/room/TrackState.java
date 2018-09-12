package mx.dev.franco.automusictagfixer.persistence.room;

public final class TrackState {
    //Status of correction of items
    public static final int NO_TAGS_SEARCHED_YET = 0;
    public static final int ALL_TAGS_FOUND = 1;
    public static final int ALL_TAGS_NOT_FOUND = 2;
    public static final int NO_TAGS_FOUND = -1;
    public static final int TAGS_EDITED_BY_USER = 3;
    public static final int FILE_IN_SD_WITHOUT_PERMISSION = 6;
    public static final int COULD_NOT_CREATE_TEMP_FILE = 7;
    public static final int COULD_NOT_CREATE_AUDIOFILE = 8;
    public static final int COULD_RESTORE_FILE_TO_ITS_LOCATION = 9;
    public static final int COULD_NOT_APPLIED_CHANGES = 10;
    public static final int FILE_ERROR_READ = 4;
    public static final int TAGS_CORRECTED_BY_SEMIAUTOMATIC_MODE = 5;
}

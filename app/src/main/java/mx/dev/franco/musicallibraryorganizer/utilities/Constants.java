package mx.dev.franco.musicallibraryorganizer.utilities;

/**
 * Created by franco on 27/10/17.
 */

public class Constants {
    public static final String MEDIASTORE_ID = "mediastore_id";
    public static final String AUDIO_ITEM = "audio_item";
    public static final String POSITION = "position";

    public static class CorrectionModes {
        public static String MODE = "correction_mode";
        public static final int AUTOMATIC = 0;
        public static final int SEMI_AUTOMATIC = 1;
        public static final int MANUAL = 2;
        public static final int VIEW_INFO = 3;
    }

    public static class TrackModes{
        public static String SINGLE_TRACK = "single_track";
        public static boolean SINGLE = true;
        public static boolean MULTIPLE = false;
    }

    public static class Actions {
        public static final String ACTION_SHOW_NOTIFICATION = "action_show_notification";
        public static final String ACTION_NOT_FOUND = "action_not_found";
        public static final String ACTION_DONE = "action_done";
        public static final String ACTION_DONE_DETAILS = "action_done_details";
        public static final String ACTION_CANCEL_TASK = "action_cancel";
        public static final String ACTION_CANCEL_TRACK_ID = "action_cancel_track_id";
        public static final String ACTION_FAIL = "action_fail";
        public static final String ACTION_COMPLETE_TASK = "action_complete_task";
        public static final String ACTION_SET_AUDIOITEM_PROCESSING = "action_set_audioitem_processing";
        public static final String ACTION_START_TASK = "action_start_task";

        public static final String ACTION_SHOULD_CONTINUE = "action_should_continue";
    }

    public static class Activities {
        public static final String FROM_EDIT_MODE = "from_edit_mode";
        public static final boolean DETAILS_ACTIVITY = true;
        public static final boolean MAIN_ACTIVITY = false;
    }

    public static class GnServiceActions{
        public static final String ACTION_API_INITIALIZED = "action_api_initialized";
    }

    public static class Conditions {
        public static  final int NO_INTERNET_CONNECTION = 0;
        public static  final int NO_INITIALIZED_API = 1;

    }

    public static class State{
        public static final String BEGIN_PROCESSING = "begin_processing";
        public static final String QUERYING_INFO = "begin_processing";
        public static final String COMPLETE_IDENTIFICATION = "begin_processing";

        public static final String BEGIN_PROCESSING_MSG = "Iniciando corrección…";
        public static final String QUERYING_INFO_MSG = "Solicitando información de canción…";
        public static final String COMPLETE_IDENTIFICATION_MSG = "Identificación completa.";
        public static final String STATUS_ERROR_MSG = "Error";
        public static final String STATUS_PROCESSING_ERROR_MSG = "Error al procesar archivvo";
    }
}

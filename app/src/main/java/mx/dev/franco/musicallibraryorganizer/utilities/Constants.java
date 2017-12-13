package mx.dev.franco.musicallibraryorganizer.utilities;

import mx.dev.franco.musicallibraryorganizer.BuildConfig;

/**
 * Created by franco on 27/10/17.
 */

public class Constants {
    public static final String MEDIASTORE_ID = "mediastore_id";
    public static final String AUDIO_ITEM = "audio_item";
    public static final String POSITION = "position";
    public static final String COMPLETE_READ = "complete_read";

    public static class Application{
        public static String FULL_QUALIFIED_NAME = BuildConfig.APPLICATION_ID;
    }

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
        public static final String ACTION_SHOW_NOTIFICATION = BuildConfig.APPLICATION_ID + "." + "action_show_notification";
        public static final String ACTION_NOT_FOUND = BuildConfig.APPLICATION_ID + "." + "action_not_found";
        public static final String ACTION_DONE = BuildConfig.APPLICATION_ID + "." + "action_done";
        public static final String ACTION_DONE_DETAILS = BuildConfig.APPLICATION_ID + "." + "action_done_details";
        public static final String ACTION_CANCEL_TASK = BuildConfig.APPLICATION_ID + "." + "action_cancel";
        public static final String ACTION_CANCEL_TRACK_ID = BuildConfig.APPLICATION_ID + "." + "action_cancel_track_id";
        public static final String ACTION_FAIL = BuildConfig.APPLICATION_ID + "." + "action_fail";
        public static final String ACTION_COMPLETE_TASK = BuildConfig.APPLICATION_ID + "." + "action_complete_task";
        public static final String ACTION_SET_AUDIOITEM_PROCESSING = BuildConfig.APPLICATION_ID + "." + "action_set_audioitem_processing";
        public static final String ACTION_START_TASK = BuildConfig.APPLICATION_ID + "." +"action_start_task";

        public static final String ACTION_SHOULD_CONTINUE = BuildConfig.APPLICATION_ID + "." + "action_should_continue";
        public static final String ACTION_INTERNET_CONNECTION = BuildConfig.APPLICATION_ID + "." + "action_internet_connection";
        public static final String ACTION_CONNECTION_LOST = BuildConfig.APPLICATION_ID + "." + "action_connection_lost" ;
        public static final String HAS_CONNECTION = BuildConfig.APPLICATION_ID + "." + "has_connection" ;

    }

    public static class Activities {
        public static final String FROM_EDIT_MODE = "from_edit_mode";
        public static final boolean DETAILS_ACTIVITY = true;
        public static final boolean MAIN_ACTIVITY = false;
    }

    public static class GnServiceActions{
        public static final String ACTION_API_INITIALIZED = BuildConfig.APPLICATION_ID + "." +"action_api_initialized";
    }

    public static class Conditions {
        public static  final int NO_INTERNET_CONNECTION = 2;
        public static  final int NO_INITIALIZED_API = 1;

    }

    public static class State{
        public static final String BEGIN_PROCESSING = "kMusicIdFileCallbackStatusProcessingBegin";
        public static final String QUERYING_INFO = "kMusicIdFileCallbackStatusFileInfoQuery";
        public static final String COMPLETE_IDENTIFICATION = "kMusicIdFileCallbackStatusProcessingComplete";
        public static final String STATUS_ERROR = "kMusicIdFileCallbackStatusError";
        public static final String STATUS_PROCESSING_ERROR = "kMusicIdFileCallbackStatusProcessingError";

        public static final String BEGIN_PROCESSING_MSG = "Iniciando corrección";
        public static final String QUERYING_INFO_MSG = "Solicitando información";
        public static final String COMPLETE_IDENTIFICATION_MSG = "Identificación completa";
        public static final String STATUS_ERROR_MSG = "Error";
        public static final String STATUS_PROCESSING_ERROR_MSG = "Error al procesar ";
    }
}

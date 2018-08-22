package mx.dev.franco.automusictagfixer.utilities;

import android.net.Uri;

import mx.dev.franco.automusictagfixer.BuildConfig;

/**
 * Created by franco on 27/10/17.
 */

public class Constants {
    public static final String MEDIA_STORE_ID = "media_store_id";
    public static final String COMPLETE_READ = "complete_read";
    public static final String SORT_KEY = "key_default_sort";
    public static final String ALL_ITEMS_CHECKED = "all_items_checked";
    public static final String ACTION_OPEN_MAIN_ACTIVITY = "action_open_main_activity";
    public static final String URI_TREE = "uri_tree";
    public static final String MESSAGE = "message";
    public static final String SELECTED_ITEM = "selected_item";
    public static final String LAST_SELECTED_ITEM = "last_selected_item";
    public static final int CACHED = 0;
    public static final int MANUAL = 1;
    public static final String PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=";

    public static class Application{
        public static String FULL_QUALIFIED_NAME = BuildConfig.APPLICATION_ID;
    }

    public static class CorrectionModes {
        public static String MODE = "correction_mode";
        public static final int SEMI_AUTOMATIC = 1;
        public static final int MANUAL = 2;
        public static final int VIEW_INFO = 3;
    }

    public static class Actions {
        public static final String ACTION_SHOW_NOTIFICATION = BuildConfig.APPLICATION_ID + "." + "action_show_notification";

        public static final String ACTION_COMPLETE_TASK = BuildConfig.APPLICATION_ID + "." + "action_complete_task";
        public static final String ACTION_START_TASK = BuildConfig.APPLICATION_ID + "." +"action_start_task";
        public static final String ACTION_SD_CARD_ERROR = BuildConfig.APPLICATION_ID + "." +"action_sd_card_error";

        public static final String ACTION_CONNECTION_LOST = BuildConfig.APPLICATION_ID + "." + "action_connection_lost" ;
        public static final String ACTION_CONNECTION_RECOVERED = BuildConfig.APPLICATION_ID + "." + "action_connection_recovered" ;

        public static final String START_PROCESSING_FOR = BuildConfig.APPLICATION_ID + ".start_identification_for";
        public static final String FINISH_TRACK_PROCESSING = BuildConfig.APPLICATION_ID + ".action_finish_track_processing";
    }

    public static class GnServiceActions{
        public static final String ACTION_API_INITIALIZED = BuildConfig.APPLICATION_ID + "." +"action_api_initialized";
    }

    public static class State{
        public static final String BEGIN_PROCESSING = "kMusicIdFileCallbackStatusProcessingBegin";
        public static final String QUERYING_INFO = "kMusicIdFileCallbackStatusFileInfoQuery";
        public static final String COMPLETE_IDENTIFICATION = "kMusicIdFileCallbackStatusProcessingComplete";
        public static final String STATUS_ERROR = "kMusicIdFileCallbackStatusError";
        public static final String STATUS_PROCESSING_ERROR = "kMusicIdFileCallbackStatusProcessingError";

        public static final String BEGIN_PROCESSING_MSG = "Iniciando identificación...";
        public static final String QUERYING_INFO_MSG = "Identificando, espere por favor...";
        public static final String COMPLETE_IDENTIFICATION_MSG = "Identificación completa";
        public static final String STATUS_ERROR_MSG = "Error";
        public static final String STATUS_PROCESSING_ERROR_MSG = "Error al procesar ";
    }
}

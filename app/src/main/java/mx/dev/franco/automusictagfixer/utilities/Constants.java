package mx.dev.franco.automusictagfixer.utilities;

import android.net.Uri;

import mx.dev.franco.automusictagfixer.BuildConfig;

/**
 * Created by franco on 27/10/17.
 */

public class Constants {
    public static final String MEDIA_STORE_ID = "media_store_id";
    public static final String AUDIO_ITEM = "audio_item";
    public static final String POSITION = "position";
    public static final String COMPLETE_READ = "complete_read";
    public static final String SORT_KEY = "key_default_sort";
    public static final String ALL_ITEMS_CHECKED = "all_items_checked";
    public static final String ACTION_OPEN_MAIN_ACTIVITY = "action_open_main_activity";
    public static final String URI_TREE = "uri_tree";
    public static final String MESSAGE = "message";
    public static final String PATH = "path";
    public static final String TRACK_NAME = "track_name";
    public static final String STATUS = "status";
    public static final String SELECTED_ITEM = "selected_item";
    public static final String LAST_SELECTED_ITEM = "lasy_selected_item";
    public static final String TASK = "task";
    public static final int CACHED = 0;
    public static final int MANUAL = 1;
    public static final int SEMIAUTOMATIC = 2;

    public static Uri URI_SD_CARD = null;

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
        public static final String ACTION_TEST_NETWORK = BuildConfig.APPLICATION_ID + "." +"action_test_network";

        public static final String ACTION_SHOULD_CONTINUE = BuildConfig.APPLICATION_ID + "." + "action_should_continue";
        public static final String ACTION_INTERNET_CONNECTION = BuildConfig.APPLICATION_ID + "." + "action_internet_connection";
        public static final String ACTION_CONNECTION_LOST = BuildConfig.APPLICATION_ID + "." + "action_connection_lost" ;
        public static final String ACTION_CONNECTION_RECOVERED = BuildConfig.APPLICATION_ID + "." + "action_connection_recovered" ;
        public static final String ACTION_ERROR = BuildConfig.APPLICATION_ID + "." + "action_error" ;
        public static final String ACTION_STOP_SERVICE = BuildConfig.APPLICATION_ID + "." + "action_stop_service";

        public static final String ACTION_REQUEST_UPDATE_LIST = BuildConfig.APPLICATION_ID + "." + "action_request_update_list";
        public static final String ACTION_SET_AUDIOITEM_STATE_PROCESSING = BuildConfig.APPLICATION_ID + "." + "action_set_audioitem_state_processing";
        public static final String ACTION_REQUEST_ITEM_PROCESSING = BuildConfig.APPLICATION_ID + "." + "action_request_item_processing";
        public static final String STATUS = BuildConfig.APPLICATION_ID + ".action_report_status";
        public static final String START_PROCESSING_FOR = BuildConfig.APPLICATION_ID + ".start_identification_for";
        public static final String IDENTIFICATION_ERROR = BuildConfig.APPLICATION_ID + ".identification_error";

        public static final String IDENTIFICATION_NOT_FOUND = BuildConfig.APPLICATION_ID + "identification_not_found";
        public static final String IDENTIFICATION_FOUND = BuildConfig.APPLICATION_ID + ".identification_found";
        public static final String IDENTIFICATION_COMPLETE = BuildConfig.APPLICATION_ID + ".identification_complete";
        public static final String CANCELLED_IDENTIFICATION = BuildConfig.APPLICATION_ID+ ".cancelled_identification";
        public static final String APPLYING_TAGS = BuildConfig.APPLICATION_ID + ".applying_tags";
        public static final String TAGS_APPLIED = BuildConfig.APPLICATION_ID + ".tags_applied";
        public static final String CORRECTION_ERROR = BuildConfig.APPLICATION_ID + ".correction_error";
        public static final String CORRECTION_CANCELLED = BuildConfig.APPLICATION_ID + ".correction_task_cancelled";
        public static final String SEND_BACK_RESULTS = BuildConfig.APPLICATION_ID + ".send_back_results";
        public static final String ACTION_FINISHED_TASK = BuildConfig.APPLICATION_ID + ".action_finished_task";
        public static final String ACTION_PENDING_TRACK = BuildConfig.APPLICATION_ID + ".action_pending_track";
        public static final String ACTION_ADD_OR_REMOVE = BuildConfig.APPLICATION_ID + ".action_add_or_remove";
        public static final String FINISH_TRACK_PROCESSING = BuildConfig.APPLICATION_ID + ".action_finish_track_processing";
    }

    public static class Activities {
        public static final String FROM_EDIT_MODE = "from_edit_mode";
        public static final boolean DETAILS_ACTIVITY = true;
    }

    public static class GnServiceActions{
        public static final String ACTION_API_INITIALIZED = BuildConfig.APPLICATION_ID + "." +"action_api_initialized";
        public static final String API_ERROR = BuildConfig.APPLICATION_ID + "." + "api_error";
    }

    public static class Conditions {
        public static  final int NO_INTERNET_CONNECTION = 2;
        public static  final int NO_INITIALIZED_API = 1;

    }

    public static class StopsReasons {
        public static final int CONTINUE_TASK = -1;
        public static final int NORMAL_TERMINATION_TASK = 0;
        public static final int USER_CANCEL_TASK = 1;
        public static final int CANCEL_TASK = 2;
        public static final int ERROR_TASK = 3;
        public static final int LOST_CONNECTION_TASK = 4;
        public static final int REMOVABLE_MEDIA_CHANGE = 5;

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

package mx.dev.franco.automusictagfixer.utilities;

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
    public static final String DATE_PATTERN = "yyyyMMdd_HHmmss";
    public static final String SELECTED_ITEM = "selected_item";
    public static final String LAST_SELECTED_ITEM = "last_selected_item";
    public static final int CACHED = 0;
    public static final int MANUAL = 1;
    public static final String PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=";
    public static final String GOOGLE_SEARCH = "https://www.google.com/#q=";

    public static class Application{
        public static String FULL_QUALIFIED_NAME = BuildConfig.APPLICATION_ID;
    }

    public static class CorrectionActions {
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

        public static final String START_PROCESSING_FOR = BuildConfig.APPLICATION_ID + ".start_identification_for";
        public static final String ACTION_RESCAN = BuildConfig.APPLICATION_ID + ".action_rescan";
        public static final String ACTION_STOP_TASK = BuildConfig.APPLICATION_ID + ".action_stop_task";
        public static final String ACTION_BROADCAST_MESSAGE = BuildConfig.APPLICATION_ID + ".action_broadcast_message";

        public static final String ACTION_SET_ITEM_LOADING = BuildConfig.APPLICATION_ID + ".action_set_item_loading";
    }
}

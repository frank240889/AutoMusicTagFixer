package mx.dev.franco.automusictagfixer.utilities

import mx.dev.franco.automusictagfixer.BuildConfig

/**
 * Created by franco on 27/10/17.
 */
object Constants {
    const val MEDIA_STORE_ID = "media_store_id"
    const val SORT_KEY = "key_default_sort"
    const val ALL_ITEMS_CHECKED = "all_items_checked"
    const val ACTION_OPEN_MAIN_ACTIVITY = "action_open_main_activity"
    const val URI_TREE = "uri_tree"
    const val DATE_PATTERN = "yyyyMMdd_HHmmss"
    const val SELECTED_ITEM = "selected_item"
    const val LAST_SELECTED_ITEM = "last_selected_item"
    const val CACHED = 0
    const val MANUAL = 1
    const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id="
    const val GOOGLE_SEARCH = "https://www.google.com/#q="

    object Application {
        @JvmField
        var FULL_QUALIFIED_NAME = BuildConfig.APPLICATION_ID
    }

    object CorrectionActions {
        @JvmField
        var MODE = "correction_mode"
        const val SEMI_AUTOMATIC = 1
        const val VIEW_INFO = 3
    }

    object Actions {
        const val ACTION_SHOW_NOTIFICATION =
            BuildConfig.APPLICATION_ID + "." + "action_show_notification"
        const val ACTION_COMPLETE_TASK = BuildConfig.APPLICATION_ID + "." + "action_complete_task"
        const val ACTION_START_TASK = BuildConfig.APPLICATION_ID + "." + "action_start_task"
        const val ACTION_SD_CARD_ERROR = BuildConfig.APPLICATION_ID + "." + "action_sd_card_error"
        const val START_PROCESSING_FOR = BuildConfig.APPLICATION_ID + ".start_identification_for"
        const val ACTION_STOP_TASK = BuildConfig.APPLICATION_ID + ".action_stop_task"
        const val ACTION_BROADCAST_MESSAGE =
            BuildConfig.APPLICATION_ID + ".action_broadcast_message"
    }
}
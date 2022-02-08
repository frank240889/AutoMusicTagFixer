package mx.dev.franco.automusictagfixer.persistence.room.database

import android.provider.BaseColumns

/**
 * Created by franco on 7/03/17.
 */
class TrackContract {
    object TrackData : BaseColumns {
        const val TABLE_NAME = "tracks_table"
        const val MEDIASTORE_ID = "media_store_id"
        const val TITLE = "title"
        const val ARTIST = "artist"
        const val ALBUM = "album"
        const val DATA = "_data"
        const val IS_PROCESSING = "is_processing"
        const val IS_SELECTED = "is_selected"
        const val STATUS =
            "status" //P for "processed", I for "incomplete data", NA for "no data avaliable", N for "no processed"
    }
}
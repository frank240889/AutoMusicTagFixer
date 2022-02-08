package mx.dev.franco.automusictagfixer.persistence.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "track_table")
class Track(
    @field:ColumnInfo(name = "title") var title: String, @field:ColumnInfo(
        name = "artist"
    ) var artist: String, @field:ColumnInfo(name = "album") var album: String, @field:ColumnInfo(
        name = "_data"
    ) var path: String, version: Int
) {
    @PrimaryKey
    @ColumnInfo(name = "mediastore_id")
    var mediaStoreId = 0

    @ColumnInfo(name = "selected")
    private var mChecked = 0

    @ColumnInfo(name = "state")
    var state = TrackState.NO_TAGS_SEARCHED_YET

    @ColumnInfo(name = "processing")
    private var mProcessing = 0

    @ColumnInfo(name = "version")
    var version = 0

    constructor(track: Track) : this(
        track.title,
        track.artist,
        track.album,
        track.path,
        track.version
    ) {
    }

    fun checked(): Int {
        return mChecked
    }

    fun setChecked(isChecked: Int) {
        mChecked = isChecked
    }

    fun processing(): Int {
        return mProcessing
    }

    fun setProcessing(isProcessing: Int) {
        mProcessing = isProcessing
    }

    init {
        this.version = version
    }
}
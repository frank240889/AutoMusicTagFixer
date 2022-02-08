package mx.dev.franco.automusictagfixer.persistence.repository

import androidx.annotation.IntegerRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import mx.dev.franco.automusictagfixer.persistence.repository.AsyncOperation.*
import mx.dev.franco.automusictagfixer.persistence.room.Track
import mx.dev.franco.automusictagfixer.persistence.room.TrackDAO
import mx.dev.franco.automusictagfixer.persistence.room.TrackRoomDatabase
import mx.dev.franco.automusictagfixer.utilities.Constants
import mx.dev.franco.automusictagfixer.utilities.Resource
import mx.dev.franco.automusictagfixer.utilities.Resource.Companion.success
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences
import java.io.File
import javax.inject.Inject

class TrackRepository
@Inject
constructor(
    private val db: TrackRoomDatabase,
    private val sharedPreferences: AbstractSharedPreferences
) {
    private var backingData: LiveData<List<Track>>

    private val resultSearchLiveData: MediatorLiveData<List<Track>?> by lazy {
        MediatorLiveData()
    }

    private lateinit var resultsSearchTracks: LiveData<List<Track>>

    private val tracks: MediatorLiveData<Resource<List<Track>?>> by lazy {
        MediatorLiveData()
    }

    val observeTracks: LiveData<Resource<List<Track>?>>
        get() = tracks

    private var currentOrder: String? = null

    companion object {
        const val ASC = 0
        const val DESC = 1
    }

    init {
        currentOrder = sharedPreferences.getString(Constants.SORT_KEY)

        // Check the last sort order saved.
        if (currentOrder == null)
            currentOrder = TrackDAO.DEFAULT_ORDER
        val query = TrackDAO.SELECT_SENTENCE_BY_ORDER + currentOrder
        val sqLiteQuery: SupportSQLiteQuery = SimpleSQLiteQuery(query)
        backingData = db.trackDao().getAllTracks(sqLiteQuery)

        tracks.addSource(backingData) { t: List<Track>? ->
            tracks.setValue(success(t))
        }
    }

    fun observeResultSearch() = resultSearchLiveData as LiveData<List<Track>>

    fun sortTracks(sort: Sort) {
        val orderBy: String = if (sort.sortType == ASC) {
            " " + sort.by + " COLLATE NOCASE ASC "
        } else {
            " " + sort.by + " COLLATE NOCASE DESC "
        }
        currentOrder = orderBy
        val query = "SELECT * FROM track_table ORDER BY$currentOrder"
        val sqLiteQuery: SupportSQLiteQuery = SimpleSQLiteQuery(query)
        tracks.removeSource(backingData)
        backingData = db.trackDao().getAllTracks(sqLiteQuery)
        tracks.addSource(backingData) { t: List<Track>? ->
            tracks.setValue(success(t))
        }

    }

    /**
     * Search a track in the DB.
     * @param query The query as param to search in DB.
     */
    fun trackSearch(query: String?) {
        resultsSearchTracks = db.trackDao().search(query)
        resultSearchLiveData.addSource(resultsSearchTracks) { tracks: List<Track> ->
            resultSearchLiveData.setValue(tracks)
        }
    }

    suspend fun delete(track: Track) {
        db.trackDao().delete(track)
    }

    fun tracks(): List<Track>? {
        return if (tracks.value != null) tracks.value!!.data else null
    }

    fun resultSearchTracks() = resultSearchLiveData.value as List<Track>

    fun clearResults() {
        resultSearchLiveData.value = null
    }

    suspend fun addTracks(trackList: MutableList<Track>) {
        removeInexistentTracks()
        db.trackDao().insert(trackList)
    }

    private fun removeInexistentTracks() {
        val tracksToRemove: MutableList<Track> = java.util.ArrayList()
        val currentTracks = db.trackDao().tracks
        if (currentTracks.isNullOrEmpty())
            return

        for (track in currentTracks) {
            val file = File(track.path)
            if (!file.exists()) {
                tracksToRemove.add(track)
            }
        }
        if (tracksToRemove.size > 0)
            db.trackDao().deleteBatch(tracksToRemove)
    }

    fun cancelSearch() {
        if (this::resultsSearchTracks.isInitialized)
            resultSearchLiveData.removeSource(resultsSearchTracks)
    }

    class Sort(var by: String, var sortType: Int, @field:IntegerRes var idResource: Int)
}
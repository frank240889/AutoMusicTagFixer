package mx.dev.franco.automusictagfixer.persistence.room

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
interface TrackDAO {
    @RawQuery(observedEntities = [Track::class])
    fun getAllTracks(query: SupportSQLiteQuery?): LiveData<List<Track>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(tracks: List<Track?>?)

    @Update
    fun update(track: Track?): Int

    @Delete
    fun delete(track: Track?)

    @Delete
    fun deleteBatch(tracks: List<Track?>?)

    @Query("UPDATE track_table SET selected = 1 WHERE selected = 0 ")
    fun checkAll()

    @Query("UPDATE track_table SET selected = 0 WHERE selected = 1 ")
    fun uncheckAll()

    @Query("SELECT _data FROM track_table where mediastore_id = :id")
    fun getPath(id: Int): String?

    @Query("SELECT * FROM track_table WHERE title LIKE :param" + " OR artist LIKE :param" + " OR album like :param")
    fun search(param: String?): LiveData<List<Track>>

    @Query("SELECT * FROM track_table where mediastore_id = :id")
    fun search(id: Int): LiveData<Track>

    @Query("SELECT * FROM track_table where mediastore_id = :id")
    fun getTrack(id: Int): Track

    @get:Query("SELECT * FROM track_table")
    val tracks: List<Track>?

    @RawQuery(observedEntities = [Track::class])
    fun findNextSelected(query: SupportSQLiteQuery?): Track?

    @Query("UPDATE track_table SET processing = 0 WHERE processing = 1")
    fun unprocessTracks()

    companion object {
        const val SELECT_SENTENCE_BY_ORDER = "SELECT * FROM track_table ORDER BY "
        const val DEFAULT_ORDER = " title COLLATE NOCASE ASC "
    }
}
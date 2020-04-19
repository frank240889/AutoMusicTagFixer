package mx.dev.franco.automusictagfixer.persistence.room;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.room.Update;
import androidx.sqlite.db.SupportSQLiteQuery;

import java.util.List;

@Dao
public interface TrackDAO {
    String SELECT_SENTENCE_BY_ORDER = "SELECT * FROM track_table ORDER BY ";
    String DEFAULT_ORDER = " title COLLATE NOCASE ASC ";


    @RawQuery(observedEntities = Track.class)
    LiveData<List<Track>> getAllTracks(SupportSQLiteQuery query);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(List<Track> tracks);

    @Update
    int update(Track track);

    @Delete
    void delete(Track track);

    @Delete
    void deleteBatch(List<Track> tracks);

    @Query("UPDATE track_table SET selected = 1 WHERE selected = 0 ")
    void checkAll();

    @Query("UPDATE track_table SET selected = 0 WHERE selected = 1 ")
    void uncheckAll();

    @Query("SELECT _data FROM track_table where mediastore_id = :id")
    String getPath(int id);

    @Query("SELECT * FROM track_table WHERE title LIKE :param" + " OR artist LIKE :param" + " OR album like :param")
    LiveData<List<Track>> search(String param);

    @Query("SELECT * FROM track_table where mediastore_id = :id")
    LiveData<Track> search(int id);

    @Query("SELECT * FROM track_table")
    List<Track> getTracks();

    @RawQuery(observedEntities = Track.class)
    Track findNextSelected(SupportSQLiteQuery query);

}

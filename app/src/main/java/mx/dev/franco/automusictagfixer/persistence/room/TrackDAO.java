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

    @RawQuery(observedEntities = Track.class)
    LiveData<List<Track>> getAllTracks(SupportSQLiteQuery query);

    @RawQuery(observedEntities = Track.class)
    List<Integer> getCheckedTracks(SupportSQLiteQuery sqLiteQuery);

    @Query("SELECT * FROM track_table WHERE selected = 1 ORDER BY title ASC")
    List<Track> getSelectedTracks();

    @Query("SELECT * FROM track_table WHERE mediastore_id = :id ORDER BY title ASC")
    List<Track> getSelectedTrack(int id);

    @Query("SELECT COUNT(*) FROM track_table")
    int count();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Track track);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(List<Track> tracks);

    @Update
    void update(Track track);

    @Delete
    void delete(Track track);

    @Delete
    void deleteBatch(List<Track> tracks);

    @Query("UPDATE track_table SET selected = 1 WHERE selected = 0 ")
    void checkAll();

    @Query("UPDATE track_table SET selected = 0 WHERE selected = 1 ")
    void uncheckAll();

    @Query("SELECT mediastore_id FROM track_table where mediastore_id = :id")
    boolean findTrackById(int id);

    @Query("SELECT _data FROM track_table where mediastore_id = :id")
    String getPath(int id);

    @Query("SELECT * FROM track_table WHERE title LIKE :param" + " OR artist LIKE :param" + " OR album like :param")
    LiveData<List<Track>> search(String param);

    @Query("SELECT * FROM track_table")
    List<Track> getTracks();

}

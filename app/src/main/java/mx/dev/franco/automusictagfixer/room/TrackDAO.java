package mx.dev.franco.automusictagfixer.room;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.db.SupportSQLiteQuery;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;
import android.arch.persistence.room.RawQuery;

import java.util.List;

@Dao
public interface TrackDAO {

    @RawQuery(observedEntities = Track.class)
    LiveData<List<Track>> getAllTracks(SupportSQLiteQuery query);

    @Query("SELECT * FROM track_table WHERE selected = 1 ORDER BY title ASC")
    List<Track> getSelectedTracks();

    @Query("SELECT * FROM track_table WHERE mediastore_id = :id ORDER BY title ASC")
    List<Track> getSelectedTrack(int id);

    @Insert
    void insert(Track track);

    @Insert
    void insertAll(List<Track> tracks);

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

    @RawQuery(observedEntities = Track.class)
    List<Integer> getCheckedTracks(SupportSQLiteQuery sqLiteQuery);

    @Query("SELECT mediastore_id FROM track_table where mediastore_id = :id")
    boolean findTrackById(int id);

    @Query("SELECT _data FROM track_table where mediastore_id = :id")
    String getPath(int id);

    @Query("SELECT * FROM track_table WHERE title LIKE :param" + " OR artist LIKE :param" + " OR album like :param")
    List<Track> search(String param);

    @Query("SELECT * FROM track_table")
    List<Track> getTracks();

}

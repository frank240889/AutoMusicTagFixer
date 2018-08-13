package mx.dev.franco.automusictagfixer.room;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface TrackDAO {

    @Query("SELECT * FROM track_table ORDER BY :order")
    LiveData<List<Track>> getAllTracks(String order);

    @Query("SELECT * FROM track_table WHERE selected = 1 ORDER BY TITLE ASC")
    List<Track> getSelectedTracks();

    @Query("SELECT * FROM track_table WHERE mediastore_id = :id ORDER BY TITLE ASC")
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

    @Query("SELECT COUNT(*) FROM track_table")
    int count();

    @Query("UPDATE track_table SET selected = 1 WHERE selected = 0 ")
    void checkAll();

    @Query("UPDATE track_table SET selected = 0 WHERE selected = 1 ")
    void uncheckAll();

    @Query("SELECT mediastore_id FROM track_table WHERE selected = 1 ORDER BY TITLE ASC")
    List<Integer> getCheckedTracks();

    @Query("SELECT * FROM track_table WHERE mediastore_id=:id")
    LiveData<List<Track>> getTrackById(int id);

    @Query("SELECT mediastore_id FROM track_table where mediastore_id = :id")
    boolean findTrackById(int id);

    @Query("SELECT _data FROM track_table where mediastore_id = :id")
    String getPath(int id);

    @Query("SELECT * FROM track_table WHERE title LIKE :param" + " OR artist LIKE :param" + " OR album like :param")
    List<Track> search(String param);

}

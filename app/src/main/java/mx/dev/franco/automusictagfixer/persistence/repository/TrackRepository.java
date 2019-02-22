package mx.dev.franco.automusictagfixer.persistence.repository;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.persistence.db.SimpleSQLiteQuery;
import android.arch.persistence.db.SupportSQLiteQuery;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;

import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.modelsUI.AsyncOperation.TrackChecker;
import mx.dev.franco.automusictagfixer.modelsUI.AsyncOperation.TrackRemover;
import mx.dev.franco.automusictagfixer.modelsUI.AsyncOperation.TrackUnchecker;
import mx.dev.franco.automusictagfixer.modelsUI.AsyncOperation.TrackUpdater;
import mx.dev.franco.automusictagfixer.persistence.mediastore.AsyncFileReader;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.persistence.room.TrackDAO;
import mx.dev.franco.automusictagfixer.persistence.room.TrackRoomDatabase;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.Resource;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;

import static mx.dev.franco.automusictagfixer.UI.main.TrackAdapter.ASC;

public class TrackRepository {

    private TrackDAO mTrackDao;
    private LiveData<List<Track>> mAllTrack;
    private MediatorLiveData<List<Track>> mResultSearch = new MediatorLiveData<>();
    private MediatorLiveData<Resource<List<Track>>> mMediatorTrackData = new MediatorLiveData<>();
    private LiveData<List<Track>> liveDataTracks;
    private AbstractSharedPreferences mAbstractSharedPreferences;
    private String mCurrentOrder;
    public TrackRepository(TrackRoomDatabase db, AbstractSharedPreferences abstractSharedPreferences){
        mTrackDao = db.trackDao();
        mAbstractSharedPreferences = abstractSharedPreferences;
        mCurrentOrder = mAbstractSharedPreferences.getString(Constants.SORT_KEY);
        if(mCurrentOrder == null)
            mCurrentOrder = " title COLLATE NOCASE ASC ";

        String query = "SELECT * FROM track_table ORDER BY" + mCurrentOrder;

        SupportSQLiteQuery sqLiteQuery = new SimpleSQLiteQuery(query);
        mAllTrack = mTrackDao.getAllTracks(sqLiteQuery);
        mMediatorTrackData.setValue(Resource.loading(null));
        mMediatorTrackData.addSource(mAllTrack, tracks -> {
            if(tracks == null || tracks.size() == 0) {
                mMediatorTrackData.setValue(Resource.success(new ArrayList<>()));
            }
            else {
                mMediatorTrackData.setValue(Resource.success(tracks));
            }
        });
    }

    public LiveData<Resource<List<Track>>> getAllTracks(){
        return mMediatorTrackData;
    }

    public void getDataFromTracksFirst(final AsyncOperation<Void, Boolean, Void, Void> iRetriever){
        boolean databaseCreationCompleted = mAbstractSharedPreferences.getBoolean(Constants.COMPLETE_READ);
        if(!databaseCreationCompleted) {
            AsyncFileReader asyncFileReader = new AsyncFileReader();
            asyncFileReader.setTask(AsyncFileReader.INSERT_ALL);
            asyncFileReader.setListener(iRetriever);
            asyncFileReader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        else {
            iRetriever.onAsyncOperationFinished(false);
        }
    }

    public void getNewTracks(final AsyncOperation<Void, Boolean, Void, Void> iRetriever){
            AsyncFileReader asyncFileReader = new AsyncFileReader();
            asyncFileReader.setTask(AsyncFileReader.UPDATE_LIST);
            asyncFileReader.setListener(iRetriever);
            asyncFileReader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void setChecked(Track track){
        if(track.checked() == 1){
            track.setChecked(0);
        }
        else {
            track.setChecked(1);
        }

        mAbstractSharedPreferences.putBoolean("sorting", false);
        new TrackUpdater(mTrackDao).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,track);
    }

    public void update(Track track){
        new TrackUpdater(mTrackDao).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,track);
    }

    public void checkAll(){
        mAbstractSharedPreferences.putBoolean("sorting", false);
        new TrackChecker(mTrackDao).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void uncheckAll(){
        mAbstractSharedPreferences.putBoolean("sorting", false);
        new TrackUnchecker(mTrackDao).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void delete(Track track){
        mAbstractSharedPreferences.putBoolean("sorting", false);
        new TrackRemover(mTrackDao).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, track);
    }

    public boolean sortTracks(String order, int orderType) {
        String orderBy;
        if(orderType == ASC){
            orderBy = " " + order + " COLLATE NOCASE ASC ";
        }
        else {
            orderBy = " " + order + " COLLATE NOCASE DESC ";
        }

        //No need to re sort if is the same order
        if(orderBy.equals(mCurrentOrder))
            return true;

        //Save this flag to indicate to adapter that the tracks are sorting
        mAbstractSharedPreferences.putBoolean("sorting", true);
        mCurrentOrder = orderBy;
        String query = "SELECT * FROM track_table ORDER BY" + mCurrentOrder;
        SupportSQLiteQuery  sqLiteQuery = new SimpleSQLiteQuery(query);
        mAbstractSharedPreferences.putString(Constants.SORT_KEY,mCurrentOrder);
        mMediatorTrackData.removeSource(mAllTrack);
        mAllTrack = mTrackDao.getAllTracks(sqLiteQuery);

        mMediatorTrackData.addSource(mAllTrack, tracks -> {
            if(tracks == null || tracks.size() == 0) {
                mMediatorTrackData.setValue(Resource.success(new ArrayList<>()));
            }
            else {
                mMediatorTrackData.setValue(Resource.success(tracks));
            }
        });
        return true;
    }

    public LiveData<List<Track>> getSearchResults() {
        return mResultSearch;
    }

    /**
     * Search a track in the DB.
     * @param query The query as param to search in DB.
     */
    public void search(String query) {
        if(liveDataTracks != null)
            mResultSearch.removeSource(liveDataTracks);

        liveDataTracks = mTrackDao.search(query);
        mResultSearch.addSource(liveDataTracks, tracks -> {
            mResultSearch.setValue(tracks);
        });
    }

    public void checkAllItems() {
        boolean allChecked = mAbstractSharedPreferences.getBoolean(Constants.ALL_ITEMS_CHECKED);
        if(allChecked){
            mAbstractSharedPreferences.putBoolean(Constants.ALL_ITEMS_CHECKED, false);
            uncheckAll();
        }
        else {
            mAbstractSharedPreferences.putBoolean(Constants.ALL_ITEMS_CHECKED, true);
            checkAll();
        }
    }
}

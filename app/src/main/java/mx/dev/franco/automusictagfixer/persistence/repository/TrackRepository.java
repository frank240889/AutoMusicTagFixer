package mx.dev.franco.automusictagfixer.persistence.repository;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.persistence.db.SimpleSQLiteQuery;
import android.arch.persistence.db.SupportSQLiteQuery;
import android.content.Context;
import android.os.AsyncTask;

import java.util.List;

import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.persistence.mediastore.AsyncFileReader;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.persistence.room.TrackDAO;
import mx.dev.franco.automusictagfixer.persistence.room.TrackRoomDatabase;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;

import static mx.dev.franco.automusictagfixer.UI.main.TrackAdapter.ASC;

public class TrackRepository {

    private TrackDAO mTrackDao;
    private LiveData<List<Track>> mAllTrack;
    private MediatorLiveData<List<Track>> mResultSearch = new MediatorLiveData<>();
    private MediatorLiveData<List<Track>> mMediatorTrackData = new MediatorLiveData<>();
    private LiveData<List<Track>> liveDataTracks;
    private AbstractSharedPreferences mAbstractSharedPreferences;
    private String mCurrentOrder;
    public TrackRepository(TrackRoomDatabase db, AbstractSharedPreferences abstractSharedPreferences, Context context){
        mTrackDao = db.trackDao();
        mAbstractSharedPreferences = abstractSharedPreferences;
        mCurrentOrder = mAbstractSharedPreferences.getString(Constants.SORT_KEY);
        if(mCurrentOrder == null)
            mCurrentOrder = " title COLLATE NOCASE ASC ";

        String query = "SELECT * FROM track_table ORDER BY" + mCurrentOrder;

        SupportSQLiteQuery sqLiteQuery = new SimpleSQLiteQuery(query);
        mAllTrack = mTrackDao.getAllTracks(sqLiteQuery);
        mMediatorTrackData.addSource(mAllTrack, tracks -> mMediatorTrackData.setValue(tracks));
    }

    public LiveData<List<Track>> getAllTracks(){
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

    public void update(Track track){
        mAbstractSharedPreferences.putBoolean("sorting", false);
        new updateTrack(mTrackDao).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,track);
    }

    public void checkAll(){
        mAbstractSharedPreferences.putBoolean("sorting", false);
        new checkAll(mTrackDao).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void uncheckAll(){
        mAbstractSharedPreferences.putBoolean("sorting", false);
        new uncheckAll(mTrackDao).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void delete(Track track){
        mAbstractSharedPreferences.putBoolean("sorting", false);
        new removeTrack(mTrackDao).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, track);
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

        mAbstractSharedPreferences.putBoolean("sorting", true);
        mCurrentOrder = orderBy;
        String query = "SELECT * FROM track_table ORDER BY" + mCurrentOrder;
        SupportSQLiteQuery  sqLiteQuery = new SimpleSQLiteQuery(query);
        mAbstractSharedPreferences.putString(Constants.SORT_KEY,mCurrentOrder);
        mMediatorTrackData.removeSource(mAllTrack);
        mAllTrack = mTrackDao.getAllTracks(sqLiteQuery);

        mMediatorTrackData.addSource(mAllTrack, tracks -> mMediatorTrackData.setValue(tracks));
        return true;
    }

    public LiveData<List<Track>> getSearchResults() {
        return mResultSearch;
    }

    public void search(String query) {
        if(liveDataTracks != null)
            mResultSearch.removeSource(liveDataTracks);

        liveDataTracks = mTrackDao.search(query);
        mResultSearch.addSource(liveDataTracks, tracks -> {
            mResultSearch.setValue(tracks);
        });
    }

    private static class checkAll extends AsyncTask<Void, Void, Void> {

        private TrackDAO mAsyncTaskDao;

        checkAll(TrackDAO dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Void... params) {
            mAsyncTaskDao.checkAll();
            return null;
        }
    }

    private static class uncheckAll extends AsyncTask<Void, Void, Void> {

        private TrackDAO mAsyncTaskDao;

        uncheckAll(TrackDAO dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Void... params) {
            mAsyncTaskDao.uncheckAll();
            return null;
        }
    }

    private static class updateTrack extends AsyncTask<Track, Void, Void> {

        private TrackDAO mAsyncTaskDao;

        updateTrack(TrackDAO dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Track... track) {
            mAsyncTaskDao.update(track[0]);
            return null;
        }
    }


    private static class removeTrack extends AsyncTask<Track, Void, Void> {

        private TrackDAO mAsyncTaskDao;

        removeTrack(TrackDAO dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Track... track) {
            mAsyncTaskDao.delete(track[0]);
            return null;
        }
    }

}

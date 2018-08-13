package mx.dev.franco.automusictagfixer.repository;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.os.AsyncTask;

import java.util.List;

import mx.dev.franco.automusictagfixer.media_store_retriever.AsyncFileReader;
import mx.dev.franco.automusictagfixer.room.Track;
import mx.dev.franco.automusictagfixer.room.TrackDAO;
import mx.dev.franco.automusictagfixer.room.TrackRoomDatabase;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;

public class TrackRepository {

    private TrackDAO mTrackDao;
    private LiveData<List<Track>> mAllTrack;
    private AbstractSharedPreferences mAbstractSharedPreferences;
    private Context mContext;
    public TrackRepository(TrackRoomDatabase db, AbstractSharedPreferences abstractSharedPreferences, Context context){
        mTrackDao = db.trackDao();
        mAbstractSharedPreferences = abstractSharedPreferences;
        mContext = context;
        String order = mAbstractSharedPreferences.getString(Constants.SORT_KEY);
        if(order == null)
            order = " TITLE ASC ";
        mAllTrack = mTrackDao.getAllTracks(order);
    }

    public LiveData<List<Track>> getAllTracks(){
        return mAllTrack;
    }

    public void getDataFromTracksFirst(final AsyncFileReader.IRetriever iRetriever){
        boolean databaseCreationCompleted = mAbstractSharedPreferences.getBoolean(Constants.COMPLETE_READ);
        if(!databaseCreationCompleted) {
            AsyncFileReader asyncFileReader = new AsyncFileReader();
            asyncFileReader.setTask(AsyncFileReader.INSERT_ALL);
            asyncFileReader.setListener(iRetriever);
            asyncFileReader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        else {
            iRetriever.onFinish();
        }
    }

    public void getNewTracks(final AsyncFileReader.IRetriever iRetriever){
            AsyncFileReader asyncFileReader = new AsyncFileReader();
            asyncFileReader.setTask(AsyncFileReader.UPDATE_LIST);
            asyncFileReader.setListener(iRetriever);
            asyncFileReader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public LiveData<List<Track>> getById(int id){
        return mTrackDao.getTrackById(id);
    }

    public void update(Track track){
        new updateTrack(mTrackDao).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,track);
    }

    public void insert(Track track){
        mTrackDao.insert(track);
    }

    public void checkAll(){
        new checkAll(mTrackDao).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void uncheckAll(){
        new uncheckAll(mTrackDao).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void delete(Track track){
        new removeTrack(mTrackDao).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, track);
    }

    public List<Track> search(String query){
        return mTrackDao.search(query);
    }

    private LiveData<List<Track>> updateTrackList() {
        return mAllTrack;
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

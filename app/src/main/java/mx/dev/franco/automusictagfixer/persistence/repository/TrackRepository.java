package mx.dev.franco.automusictagfixer.persistence.repository;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.persistence.db.SimpleSQLiteQuery;
import android.arch.persistence.db.SupportSQLiteQuery;
import android.content.Context;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.persistence.mediastore.MediaStoreReader;
import mx.dev.franco.automusictagfixer.persistence.repository.AsyncOperation.TrackChecker;
import mx.dev.franco.automusictagfixer.persistence.repository.AsyncOperation.TrackInserter;
import mx.dev.franco.automusictagfixer.persistence.repository.AsyncOperation.TrackRemover;
import mx.dev.franco.automusictagfixer.persistence.repository.AsyncOperation.TrackUnchecker;
import mx.dev.franco.automusictagfixer.persistence.repository.AsyncOperation.TrackUpdater;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.persistence.room.TrackDAO;
import mx.dev.franco.automusictagfixer.persistence.room.TrackRoomDatabase;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.Resource;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;

public class TrackRepository {
    public static final int ASC = 0;
    public static final int DESC = 1;
    private TrackDAO mTrackDao;
    private LiveData<List<Track>> mAllTrack;
    private MediatorLiveData<List<Track>> mResultSearch = new MediatorLiveData<>();
    private MediatorLiveData<Resource<List<Track>>> mMediatorTrackData = new MediatorLiveData<>();
    private LiveData<List<Track>> liveDataTracks;
    private MutableLiveData<String> mObservableMessage;
    private MutableLiveData<Boolean> mProgressObservable;
    private AbstractSharedPreferences mAbstractSharedPreferences;
    private String mCurrentOrder;
    private Context mContext;

    @Inject
    public TrackRepository(TrackRoomDatabase db,
                           AbstractSharedPreferences abstractSharedPreferences,
                           Context context){
        mTrackDao = db.trackDao();
        mAbstractSharedPreferences = abstractSharedPreferences;
        mContext = context;

        mProgressObservable = new MutableLiveData<>();
        mObservableMessage = new MutableLiveData<>();

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

    public LiveData<List<Track>> getSearchResults() {
        return mResultSearch;
    }

    public MutableLiveData<String> observeMessage() {
        return mObservableMessage;
    }

    public MutableLiveData<Boolean> observeProgress() {
        return mProgressObservable;
    }

    /**
     * Recover tracks from MediaStore the first time the app is opened.
     */
    public void fetchTracks(){
        boolean databaseCreationCompleted = mAbstractSharedPreferences.getBoolean(Constants.COMPLETE_READ);
        if(!databaseCreationCompleted) {
            MediaStoreReader mediaStoreReader = new MediaStoreReader(new AsyncOperation<Void, List<Track>, Void, Void>() {
                @Override
                public void onAsyncOperationStarted(Void params) {
                    mProgressObservable.setValue(true);
                }

                @Override
                public void onAsyncOperationFinished(List<Track> result) {
                    mProgressObservable.setValue(false);
                    //Save process of reading identificationCompleted and first time reading complete.
                    mAbstractSharedPreferences.putBoolean("first_time_read", true);
                    mAbstractSharedPreferences.putBoolean(Constants.COMPLETE_READ, true);
                    if(result.size() > 0) {
                        insert(result);
                    }
                }
            });
            mediaStoreReader.executeOnExecutor(Executors.newCachedThreadPool());
        }
        else {
            mProgressObservable.setValue(false);
        }
    }

    /**
     * Reescan the media store to retrieve new audio files.
     */
    public void rescan() {
        MediaStoreReader mediaStoreReader = new MediaStoreReader(new AsyncOperation<Void, List<Track>, Void, Void>() {
            @Override
            public void onAsyncOperationStarted(Void params) {
                mProgressObservable.setValue(true);
            }

            @Override
            public void onAsyncOperationFinished(List<Track> result) {
                mProgressObservable.setValue(false);
                if(result.size() > 0) {
                    insert(result);
                }
            }
        });
        mediaStoreReader.executeOnExecutor(Executors.newCachedThreadPool(), mContext);
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
        new TrackChecker(mTrackDao).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void uncheckAll(){
        new TrackUnchecker(mTrackDao).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void delete(Track track){
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

    /**
     * Search a track in the DB.
     * @param query The query as param to search in DB.
     */
    public void trackSearch(String query) {
        if(liveDataTracks != null)
            mResultSearch.removeSource(liveDataTracks);

        liveDataTracks = mTrackDao.search(query);
        mResultSearch.addSource(liveDataTracks, tracks -> mResultSearch.setValue(tracks));
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

    @SuppressWarnings("unchecked")
    private void insert(List<Track> tracks) {
        new TrackInserter(mTrackDao).
                executeOnExecutor(Executors.newCachedThreadPool(), tracks);
    }
}

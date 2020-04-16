package mx.dev.franco.automusictagfixer.persistence.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
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

@Singleton
public class TrackRepository {
    public static final int ASC = 0;
    public static final int DESC = 1;
    private TrackDAO mTrackDao;
    private LiveData<List<Track>> mAllTrack;
    private MediatorLiveData<List<Track>> mResultSearch;
    private MediatorLiveData<Resource<List<Track>>> mMediatorTrackData;
    private LiveData<List<Track>> mLiveDataTracks;
    private MutableLiveData<Boolean> mProgressObservable;
    private AbstractSharedPreferences mAbstractSharedPreferences;
    private String mCurrentOrder;
    private Context mContext;

    @Inject
    public TrackRepository(@NonNull TrackRoomDatabase db,
                           @NonNull AbstractSharedPreferences abstractSharedPreferences,
                           @NonNull Context context){
        mTrackDao = db.trackDao();
        mAbstractSharedPreferences = abstractSharedPreferences;
        mContext = context;

        mProgressObservable = new MutableLiveData<>();
        mResultSearch = new MediatorLiveData<>();
        mMediatorTrackData = new MediatorLiveData<>();

        mCurrentOrder = mAbstractSharedPreferences.getString(Constants.SORT_KEY);

        if(mCurrentOrder == null)
            mCurrentOrder = " title COLLATE NOCASE ASC ";

        String query = "SELECT * FROM track_table ORDER BY" + mCurrentOrder;

        SupportSQLiteQuery sqLiteQuery = new SimpleSQLiteQuery(query);
        mAllTrack = mTrackDao.getAllTracks(sqLiteQuery);

        mMediatorTrackData.addSource(mAllTrack, tracks -> {
            mProgressObservable.setValue(false);
            if(tracks == null || tracks.size() == 0) {
                mMediatorTrackData.setValue(Resource.error(new ArrayList<>()));
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
            mediaStoreReader.executeOnExecutor(AutoMusicTagFixer.getExecutorService(), mContext);
        }
        else {
            mProgressObservable.setValue(false);
        }
    }

    public void setChecked(Track track){
        if(track.checked() == 1){
            track.setChecked(0);
        }
        else {
            track.setChecked(1);
        }

        new TrackUpdater(mTrackDao).executeOnExecutor(AutoMusicTagFixer.getExecutorService(),track);
    }

    public void update(Track track){
        new TrackUpdater(mTrackDao).executeOnExecutor(AutoMusicTagFixer.getExecutorService(),track);
    }

    public void checkAll(){
        new TrackChecker(mTrackDao).executeOnExecutor(AutoMusicTagFixer.getExecutorService());
    }

    public void uncheckAll(){
        new TrackUnchecker(mTrackDao).executeOnExecutor(AutoMusicTagFixer.getExecutorService());
    }

    public void delete(Track track){
        new TrackRemover(mTrackDao).executeOnExecutor(AutoMusicTagFixer.getExecutorService(), track);
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

        mCurrentOrder = orderBy;
        String query = "SELECT * FROM track_table ORDER BY" + mCurrentOrder;
        SupportSQLiteQuery  sqLiteQuery = new SimpleSQLiteQuery(query);
        mAbstractSharedPreferences.putString(Constants.SORT_KEY,mCurrentOrder);

        mMediatorTrackData.removeSource(mAllTrack);
        mAllTrack = mTrackDao.getAllTracks(sqLiteQuery);

        mMediatorTrackData.addSource(mAllTrack, tracks -> {
            if(tracks == null || tracks.size() == 0) {
                mMediatorTrackData.setValue(Resource.error(new ArrayList<>()));
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
        if(mLiveDataTracks != null)
            mResultSearch.removeSource(mLiveDataTracks);

        mLiveDataTracks = mTrackDao.search(query);
        mResultSearch.addSource(mLiveDataTracks, tracks -> mResultSearch.setValue(tracks));
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
    public void insert(List<Track> tracks) {
        new TrackInserter(mTrackDao).
                executeOnExecutor(AutoMusicTagFixer.getExecutorService(), tracks);
    }
    @SuppressWarnings("unchecked")
    public void updateInternalDatabase(List<Track> tracks) {
        new TrackInserter(mTrackDao).
                executeOnExecutor(AutoMusicTagFixer.getExecutorService(), tracks);
    }

    public Track getTrack(@NonNull int id) {
        List<Track> tracks = mMediatorTrackData.getValue().data;

        if (tracks != null) {
            for (Track track : tracks) {
                if (id == track.getMediaStoreId())
                    return track;
            }
        }

        return null;
    }
}
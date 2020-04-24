package mx.dev.franco.automusictagfixer.persistence.repository;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.util.Log;

import androidx.annotation.IntegerRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
import mx.dev.franco.automusictagfixer.persistence.repository.AsyncOperation.TrackChecker;
import mx.dev.franco.automusictagfixer.persistence.repository.AsyncOperation.TrackInserter;
import mx.dev.franco.automusictagfixer.persistence.repository.AsyncOperation.TrackRemover;
import mx.dev.franco.automusictagfixer.persistence.repository.AsyncOperation.TrackUnchecker;
import mx.dev.franco.automusictagfixer.persistence.repository.AsyncOperation.TrackUpdater;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.persistence.room.TrackDAO;
import mx.dev.franco.automusictagfixer.persistence.room.TrackRoomDatabase;
import mx.dev.franco.automusictagfixer.ui.SingleLiveEvent;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.Resource;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;

public class TrackRepository {
    public static final int ASC = 0;
    public static final int DESC = 1;

    private TrackDAO mTrackDao;
    private LiveData<List<Track>> mTracks;
    private MediatorLiveData<List<Track>> mResultSearchLiveData;
    private MediatorLiveData<Resource<List<Track>>> mMediatorTrackData;
    private LiveData<List<Track>> mResultsSearchTracks;
    private MutableLiveData<Boolean> mLoadingObservable;
    private AbstractSharedPreferences mAbstractSharedPreferences;
    private String mCurrentOrder;
    private Context mContext;
    private SingleLiveEvent<Sort> mSortingEvent;
    private BroadcastReceiver mBroadcastReceiver;

    @Inject
    public TrackRepository(@NonNull TrackRoomDatabase db,
                           @NonNull AbstractSharedPreferences abstractSharedPreferences,
                           @NonNull Context context){
        mTrackDao = db.trackDao();
        mAbstractSharedPreferences = abstractSharedPreferences;
        mContext = context;

        mLoadingObservable = new MutableLiveData<>();
        mSortingEvent = new SingleLiveEvent<>();
        mResultSearchLiveData = new MediatorLiveData<>();
        mMediatorTrackData = new MediatorLiveData<>();

        mCurrentOrder = mAbstractSharedPreferences.getString(Constants.SORT_KEY);

        // Check the last sort order saved.
        if(mCurrentOrder == null)
            mCurrentOrder = TrackDAO.DEFAULT_ORDER;

        String query = TrackDAO.SELECT_SENTENCE_BY_ORDER + mCurrentOrder;

        SupportSQLiteQuery sqLiteQuery = new SimpleSQLiteQuery(query);
        
        mTracks = mTrackDao.getAllTracks(sqLiteQuery);

        mMediatorTrackData.addSource(mTracks, tracks -> {
            //mMediatorTrackData.removeSource(mTracks);
            if(tracks == null || tracks.size() == 0) {
                mMediatorTrackData.setValue(Resource.error(new ArrayList<>()));
            }
            else {
                mMediatorTrackData.setValue(Resource.success(tracks));
            }
        });
        Log.e(getClass().getName(), "CONSTRUCTOR");
    }

    public LiveData<Resource<List<Track>>> getObserveTracks(){
        return mMediatorTrackData;
    }

    public LiveData<List<Track>> observeResultSearch() {
        return mResultSearchLiveData;
    }

    public LiveData<Sort> observeSorting() {
        return mSortingEvent;
    }

    public MutableLiveData<Boolean> observeLoading() {
        return mLoadingObservable;
    }

    public void setChecked(int position) {
        mSortingEvent.setValue(null);
        List<Track> tracks = tracks();
        if (tracks != null && !tracks.isEmpty()) {
            Track track = tracks.get(position);
            if (track != null) {
                if(track.checked() == 1){
                    track.setChecked(0);
                }
                else {
                    track.setChecked(1);
                }

                new TrackUpdater(mTrackDao).executeOnExecutor(AutoMusicTagFixer.getExecutorService(),track);
            }
        }
    }

    public void sortTracks(Sort sort) {
        if (mMediatorTrackData.getValue() != null && !mMediatorTrackData.getValue().data.isEmpty()) {
            String orderBy;
            if (sort.sortType == ASC){
                orderBy = " " + sort.by + " COLLATE NOCASE ASC ";
            }
            else {
                orderBy = " " + sort.by + " COLLATE NOCASE DESC ";
            }

            mCurrentOrder = orderBy;
            String query = "SELECT * FROM track_table ORDER BY" + mCurrentOrder;
            SupportSQLiteQuery  sqLiteQuery = new SimpleSQLiteQuery(query);

            mMediatorTrackData.removeSource(mTracks);

            mTracks = mTrackDao.getAllTracks(sqLiteQuery);

            mMediatorTrackData.addSource(mTracks, tracks -> {
                mLoadingObservable.setValue(false);
                if(tracks == null || tracks.size() == 0) {
                    mSortingEvent.setValue(null);
                    mMediatorTrackData.setValue(Resource.error(new ArrayList<>()));
                }
                else {
                    mSortingEvent.setValue(sort);
                    mMediatorTrackData.setValue(Resource.success(tracks));
                    mAbstractSharedPreferences.putString(Constants.SORT_KEY,mCurrentOrder);
                }
            });
        }
        else {
            mSortingEvent.setValue(null);
        }
    }

    /**
     * Search a track in the DB.
     * @param query The query as param to search in DB.
     */
    public void trackSearch(String query) {
        if(mResultsSearchTracks != null)
            mResultSearchLiveData.removeSource(mResultsSearchTracks);

        mResultsSearchTracks = mTrackDao.search(query);
        mResultSearchLiveData.addSource(mResultsSearchTracks, tracks ->
                mResultSearchLiveData.setValue(tracks));
    }

    public void checkAllItems() {
        boolean allChecked = mAbstractSharedPreferences.getBoolean(Constants.ALL_ITEMS_CHECKED);
        mSortingEvent.setValue(null);
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
    
    public void update(Track track){
        new TrackUpdater(mTrackDao).executeOnExecutor(AutoMusicTagFixer.getExecutorService(),track);
    }

    public void checkAll(){
        new TrackChecker(mTrackDao).executeOnExecutor(AutoMusicTagFixer.getExecutorService());
    }

    void uncheckAll(){
        new TrackUnchecker(mTrackDao).executeOnExecutor(AutoMusicTagFixer.getExecutorService());
    }

    public void delete(int position){
        Track track = getTrackByPosition(position);
        if (track != null)
            new TrackRemover(mTrackDao).executeOnExecutor(AutoMusicTagFixer.getExecutorService(), track);
    }


    public Track getTrackByPosition(int position) {
        List<Track> tracks = tracks();
        if (tracks != null && !tracks.isEmpty()) {
            return tracks.get(position);
        }
        return null;
    }

    public Track getTrackById(@NonNull int id) {
        List<Track> tracks = tracks();

        if (tracks != null && !tracks.isEmpty()) {
            for (Track track : tracks) {
                if (id == track.getMediaStoreId())
                    return track;
            }
        }

        return null;
    }

    @Nullable
    public List<Track> tracks() {
        if (mMediatorTrackData.getValue() != null)
            return mMediatorTrackData.getValue().data;

        return null;
    }

    public List<Track> resultSearchTracks() {
        return mResultSearchLiveData.getValue();
    }

    public void clearResults() {
        mResultSearchLiveData.setValue(null);
    }

    public static final class Sort {
        public String by;
        public int sortType;
        @IntegerRes
        public int idResource;

        public Sort(String by, int sortType, int idResource) {
            this.by = by;
            this.sortType = sortType;
            this.idResource = idResource;
        }
    }
}
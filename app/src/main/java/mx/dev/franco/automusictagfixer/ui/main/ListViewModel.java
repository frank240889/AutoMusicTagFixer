package mx.dev.franco.automusictagfixer.ui.main;

import android.app.Application;

import androidx.annotation.IntegerRes;
import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.util.List;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;
import mx.dev.franco.automusictagfixer.persistence.mediastore.MediaStoreManager;
import mx.dev.franco.automusictagfixer.persistence.repository.TrackRepository;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.persistence.room.TrackState;
import mx.dev.franco.automusictagfixer.services.FixerTrackService;
import mx.dev.franco.automusictagfixer.ui.SingleLiveEvent;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.Message;
import mx.dev.franco.automusictagfixer.utilities.Resource;
import mx.dev.franco.automusictagfixer.utilities.ServiceUtils;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;

public class ListViewModel extends AndroidViewModel {
    private static final String TAG = ListViewModel.class.getName();
    //The list of tracks.
    private LiveData<List<Track>> mTracks;
    //MutableLiveData objects to respond to user interactions.
    private MutableLiveData<ViewWrapper> mObservableAccessibleTrack = new SingleLiveEvent<>();
    private MutableLiveData<ViewWrapper> mObservableInaccessibleTrack = new SingleLiveEvent<>();
    //LiveData to exposes data changes to observers.
    private MutableLiveData<Void> mObservableEmptyList = new SingleLiveEvent<>();
    private MutableLiveData<ViewWrapper> mObservableOpenTrackDetails = new SingleLiveEvent<>();
    private MutableLiveData<Integer> mStartAutomaticMode = new SingleLiveEvent<>();
    private MutableLiveData<Boolean> mObservableCheckAllTracks = new SingleLiveEvent<>();
    private MutableLiveData<Integer> mObservableMessage = new SingleLiveEvent<>();
    private MutableLiveData<Integer> mOnSorted = new SingleLiveEvent<>();
    private MutableLiveData<Boolean> mObservableOnSdPresent = new SingleLiveEvent<>();
    //The current list of tracks.
    private List<Track> mCurrentList;
    public TrackRepository trackRepository;

    public AbstractSharedPreferences sharedPreferences;
    public ServiceUtils serviceUtils;
    private MediaStoreManager mMediaStoreManager;
    private AbstractSharedPreferences mAbstractSharedPreferences;
    private LiveData<Message> mResultsAudioFilesMediaStore;
    private MediatorLiveData<Boolean> mLoadingState;


    @Inject
    public ListViewModel(@NonNull Application application,
                         @NonNull TrackRepository trackRepository,
                         @NonNull AbstractSharedPreferences abstractSharedPreferences,
                         @NonNull ServiceUtils serviceUtils,
                         @Nonnull MediaStoreManager mediaStoreManager) {
        super(application);
        this.trackRepository = trackRepository;
        this.sharedPreferences = abstractSharedPreferences;
        this.serviceUtils = serviceUtils;
        mMediaStoreManager = mediaStoreManager;
        mAbstractSharedPreferences = abstractSharedPreferences;

        mLoadingState = new MediatorLiveData<>();

        LiveData<Boolean> trackRepositoryLoadingState = trackRepository.observeProgress();
        LiveData<Boolean> mediaStoreLoadingState = mMediaStoreManager.observeLoadingState();

        mLoadingState.addSource(mediaStoreLoadingState, aBoolean ->
                mLoadingState.setValue(aBoolean));
        mLoadingState.addSource(trackRepositoryLoadingState, aBoolean ->
                mLoadingState.setValue(aBoolean));
    }

    public LiveData<ViewWrapper> observeAccessibleTrack(){
        return mObservableAccessibleTrack;
    }

    public LiveData<ViewWrapper> observeIsTrackInaccessible(){
        return mObservableInaccessibleTrack;
    }

    public LiveData<Void> observeResultFilesFound(){
        return mObservableEmptyList;
    }

    public LiveData<Boolean> observeLoadingState(){
        return mLoadingState;
    }

    public LiveData<ViewWrapper> observeActionCanOpenDetails(){
        return mObservableOpenTrackDetails;
    }

    public LiveData<Integer> observeActionCanStartAutomaticMode(){
        return mStartAutomaticMode;
    }

    public MutableLiveData<Boolean> observeActionCheckAll() {
        return mObservableCheckAllTracks;
    }

    public MutableLiveData<Integer> observeInformativeMessage() {
        return mObservableMessage;
    }

    public MutableLiveData<Integer> observeOnSortTracks() {
        return mOnSorted;
    }

    public LiveData<Boolean> observeOnSdPresent() {
        return mObservableOnSdPresent;
    }

    /**
     * Return the live data container that holds the reference to tracks from local DB.
     * @return The live data container.
     */
    public LiveData<List<Track>> getTracks(){
        LiveData<Resource<List<Track>>> result = trackRepository.getAllTracks();
        mTracks = Transformations.map(result, input -> {
            mCurrentList = input.data;
            return mCurrentList;
        });
        return mTracks;
    }

    public LiveData<Message> observeSizeResultsMediaStore() {
        LiveData<Resource<List<Track>>> resultsMediaStore = mMediaStoreManager.observeResult();
        mResultsAudioFilesMediaStore = Transformations.map(resultsMediaStore, new Function<Resource<List<Track>>, Message>() {
            @Override
            public Message apply(Resource<List<Track>> input) {
                Message message = null;
                if(input.status == Resource.Status.SUCCESS) {
                    //Save process of reading identificationCompleted and first time reading complete.
                    if(!mAbstractSharedPreferences.getBoolean("first_time_read")) {
                        mAbstractSharedPreferences.putBoolean("first_time_read", true);
                        mAbstractSharedPreferences.putBoolean(Constants.COMPLETE_READ, true);
                    }
                    if(input.data.size() > 0) {
                        trackRepository.insert(input.data);
                    }
                    else {
                        message = new Message(R.string.no_items_found);
                    }
                }
                else {
                    message = new Message(R.string.error);
                }
                return message;
            }
        });
        return mResultsAudioFilesMediaStore;
    }

    /**
     * Request to repository to setChecked the track
     * @param position The position of track.
     */
    public void onCheckboxClick(int position){
        Track track = mCurrentList.get(position);
        trackRepository.setChecked(track);
    }

    /**
     * Removes data of track from DB of app.
     * @param track The track to remove from local DB.
     */
    public void removeTrack(Track track){
        trackRepository.delete(track);
    }

    /**
     * Set the state of all tracks to checked = 1
     */
    public void checkAllItems(){
        if(serviceUtils.checkIfServiceIsRunning(FixerTrackService.CLASS_NAME))
            return;

        trackRepository.checkAllItems();
    }

    /**
     * Handles the click for items in list.
     * @param wrapper A {@link ViewWrapper} object containing th info if the item.
     */
    public void onItemClick(ViewWrapper wrapper){
        Track track = mCurrentList.get(wrapper.position);
        boolean isAccessible = AudioTagger.checkFileIntegrity(track.getPath());
        if(!isAccessible){
            wrapper.track = track;
            mObservableInaccessibleTrack.setValue(wrapper);
        }
        else if(track.processing() == 1){
            mObservableMessage.setValue(R.string.current_file_processing);
        }
        else {
            wrapper.track = track;
            mObservableAccessibleTrack.setValue(wrapper);
        }
    }

    /**
     * Fetches the tracks from MediaStore.
     */
    public void fetchTracks(){
        if(sharedPreferences.getBoolean("first_time_read")) {
            mMediaStoreManager.fetchAudioFiles();
        }
        else {
            trackRepository.fetchTracks();
        }
    }

    /**
     * Handles the click when the cover is clicked.
     * @param viewWrapper A {@link ViewWrapper} object containing th info if the item.
     */
    public void onClickCover(ViewWrapper viewWrapper){
        Track track = mCurrentList.get(viewWrapper.position);
        boolean isAccessible = AudioTagger.checkFileIntegrity(track.getPath());
        viewWrapper.track = track;
        if(!isAccessible){
            mObservableInaccessibleTrack.setValue(viewWrapper);
        }
        else if(track.processing() == 1){
            mObservableMessage.setValue(R.string.current_file_processing);
        }
        else {
            mObservableOpenTrackDetails.setValue(viewWrapper);
        }
    }

    public void setLoading(boolean showProgress){
        mLoadingState.setValue(showProgress);
    }

    /**
     * Sort list in desired order
     * @param by the field/column to sort by
     * @param orderType the sort type, may be ascendant or descendant
     * @param idResource Current tracks in adapter.
     */
    public void sortTracks(String by, int orderType, @IntegerRes int idResource) {
        //wait for sorting while correction task is running
        if(serviceUtils.checkIfServiceIsRunning(FixerTrackService.class.getName())){
            mOnSorted.setValue(-1);
            mObservableMessage.setValue(R.string.no_available);
        }

        if(mCurrentList == null || mCurrentList.isEmpty()) {
            mOnSorted.setValue(-1);
            mObservableMessage.setValue(R.string.no_available);
        }

        boolean sorted = trackRepository.sortTracks(by, orderType);

        mOnSorted.setValue(sorted ? idResource : -1);
    }

    public void checkAllTracks() {
        if(mCurrentList != null && mCurrentList.size() > 0) {
            mObservableCheckAllTracks.setValue(true);
            checkAllItems();
        }
        else {
            mObservableCheckAllTracks.setValue(false);
            mObservableMessage.setValue(R.string.no_available);
        }

    }

    /**
     * Re scan the media store searching new recently added tracks.
     */
    public void rescan() {
        if(serviceUtils.checkIfServiceIsRunning(FixerTrackService.class.getName())){
            mObservableMessage.setValue(R.string.no_available);
        }
        else {
            mLoadingState.setValue(true);
            mMediaStoreManager.rescan();
        }
    }

    /**
     * Shows the status message of this song.
     * @param position The position of the clicked item.
     */
    public void onCheckMarkClick(int position) {
        mObservableMessage.setValue(getStatusText(mCurrentList.get(position).getState()));
    }

    /**
     * Verifies if an slot of SD card is present and if an SD card is inserted.
     */
    public void checkSdIsPresent() {
        boolean isPresentSD = AudioTagger.StorageHelper.getInstance(getApplication().getApplicationContext()).
                isPresentRemovableStorage();
        if(AndroidUtils.getUriSD(getApplication().getApplicationContext()) == null && isPresentSD) {
            mObservableOnSdPresent.setValue(true);
        }
        else {
            mObservableOnSdPresent.setValue(false);
        }
    }

    /**
     * Get the status text.
     * @param status The id of track status.
     * @return The resource string of descriptive status of track.
     */
    private int getStatusText(int status){
        switch (status){
            case TrackState.ALL_TAGS_FOUND:
                return R.string.file_status_ok;
            case TrackState.ALL_TAGS_NOT_FOUND:
                return R.string.file_status_incomplete;
            default:
                return R.string.file_status_no_processed;
        }
    }

    /**
     * Returns the track for the id passed as parameter.
     * @param id The id of track.
     * @return A track object or null if could not be found.
     */
    private Track getTrackById(int id){
        if(mCurrentList.size() > 0){
            for(Track track: mCurrentList){
                if(track.getMediaStoreId() == id)
                    return track;
            }
        }

        return null;
    }

    /**
     * Get the index of track id.
     * @param id The id of track.
     * @return The position of the track id in current list or 0 if
     * the id is invalid.
     */
    public int getTrackPosition(int id) {
        if(id == -1)
            return 0;

        Track track = getTrackById(id);
        if(track != null)
            return mCurrentList.indexOf(track);

        return 0;
    }
}

package mx.dev.franco.automusictagfixer.ui.main;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import mx.dev.franco.automusictagfixer.services.FixerTrackService;
import mx.dev.franco.automusictagfixer.ui.SingleLiveEvent;
import mx.dev.franco.automusictagfixer.utilities.Message;
import mx.dev.franco.automusictagfixer.utilities.Resource;
import mx.dev.franco.automusictagfixer.utilities.ServiceUtils;

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
    private SingleLiveEvent<Integer> mObservableMessage = new SingleLiveEvent<>();
    //The current list of tracks.
    private List<Track> mCurrentList;
    public TrackRepository mTrackRepository;

    private ServiceUtils mServiceUtils;
    private MediaStoreManager mMediaStoreManager;
    private LiveData<Message> mResultsAudioFilesMediaStore;
    private MediatorLiveData<Boolean> mLoadingState = new MediatorLiveData<>();
    private boolean mIsSorting = false;


    @Inject
    public ListViewModel(@NonNull Application application,
                         @NonNull TrackRepository trackRepository,
                         @NonNull ServiceUtils serviceUtils,
                         @Nonnull MediaStoreManager mediaStoreManager) {
        super(application);

        mTrackRepository = trackRepository;
        mMediaStoreManager = mediaStoreManager;

        mServiceUtils = serviceUtils;

        mLoadingState.addSource(mMediaStoreManager.observeLoadingState(), aBoolean ->
                mLoadingState.setValue(aBoolean));
        mLoadingState.addSource(mTrackRepository.observeLoading(), aBoolean ->
                mLoadingState.setValue(aBoolean));

        mResultsAudioFilesMediaStore = getMediaStoreResults();
        Log.e(getClass().getName(), "CONSTRUCTOR");
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

    public LiveData<TrackRepository.Sort> observeSorting() {
        return mTrackRepository.observeSorting();
    }

    /**
     * Return the live data container that holds the reference to tracks from local DB.
     * @return The live data container.
     */
    public LiveData<List<Track>> getTracks(){
        mTracks = Transformations.map(mTrackRepository.getObserveTracks(), input -> {
            mLoadingState.setValue(input.status == Resource.Status.LOADING);
            return input.data;
        });
        return mTracks;
    }

    public LiveData<Message> observeSizeResultsMediaStore() {
        return mResultsAudioFilesMediaStore;
    }

    private LiveData<Message> getMediaStoreResults() {
        LiveData<Resource<List<Track>>> resultsMediaStore = mMediaStoreManager.observeResult();
        return Transformations.map(resultsMediaStore, input -> {
            Message message = null;
            if(input.status == Resource.Status.SUCCESS) {
                if(input.data.size() > 0) {
                    mTrackRepository.insert(input.data);
                }
                else {
                    message = new Message(R.string.no_items_found);
                }
            }
            else {
                message = new Message(R.string.error);
            }
            return message;
        });
    }

    /**
     * Request to repository to setChecked the track
     * @param position The position of track.
     */
    public void onCheckboxClick(int position){
        mIsSorting = false;
        mTrackRepository.setChecked(position);
    }

    /**
     * Removes data of track from DB of app.
     * @param position The track to remove from local DB.
     */
    public void removeTrack(int position){
        mIsSorting = false;
        mTrackRepository.delete(position);
    }

    /**
     * Set the state of all tracks to checked = 1
     */
    public void checkAllItems(){
        mIsSorting = false;
        if(mServiceUtils.checkIfServiceIsRunning(FixerTrackService.CLASS_NAME))
            return;

        mTrackRepository.checkAllItems();
    }

    /**
     * Handles the click for items in list.
     * @param wrapper A {@link ViewWrapper} object containing th info if the item.
     */
    public void onItemClick(ViewWrapper viewWrapper){
        Track track = getTrackList().get(viewWrapper.position);
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

    public void checkAllTracks() {
        if (mServiceUtils.checkIfServiceIsRunning(FixerTrackService.CLASS_NAME)) {
            mObservableMessage.setValue(R.string.please_wait_until_automatic_mode_finished);
        }
        else {
            List<Track> tracks = mTrackRepository.tracks();
            mIsSorting = false;
            if(tracks != null && tracks.size() > 0) {
                checkAllItems();
            }
            else {
                mObservableCheckAllTracks.setValue(false);
                mObservableMessage.setValue(R.string.please_wait_until_automatic_mode_finished);
            }
        }
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

        Track track = mTrackRepository.getTrackById(id);
        return track != null ? getTrackList().indexOf(track) : -1;
    }

    public void notifyPermissionNotGranted() {
        List<Track> tracks = getTrackList();
        if (tracks != null && !tracks.isEmpty())
            tracks.clear();
    }

    public void fetchTracks(@Nullable TrackRepository.Sort sort) {
        if (sort == null) {
            mMediaStoreManager.fetchAudioFiles();
        }
        else {
            if (mServiceUtils.checkIfServiceIsRunning(FixerTrackService.CLASS_NAME)) {
                mObservableMessage.setValue(R.string.please_wait_until_automatic_mode_finished);
            }
            else {
                mIsSorting = true;
                mTrackRepository.sortTracks(sort);
            }
        }
    }

    public boolean isSortingOperation() {
        return mIsSorting;
    }

    List<Track> getTrackList() {
        return mTrackRepository.tracks();
    }
}

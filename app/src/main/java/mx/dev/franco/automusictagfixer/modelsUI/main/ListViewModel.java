package mx.dev.franco.automusictagfixer.modelsUI.main;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.arch.lifecycle.ViewModel;
import android.content.Context;

import java.util.List;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.UI.main.ViewWrapper;
import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.persistence.repository.TrackRepository;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.persistence.room.TrackState;
import mx.dev.franco.automusictagfixer.services.FixerTrackService;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.Resource;
import mx.dev.franco.automusictagfixer.utilities.ServiceUtils;
import mx.dev.franco.automusictagfixer.utilities.StorageHelper;
import mx.dev.franco.automusictagfixer.utilities.Tagger;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;

public class ListViewModel extends ViewModel {
    private static final String TAG = ListViewModel.class.getName();
    //The list of tracks.
    private LiveData<List<Track>> mTracks;
    //MutableLiveData objects to respond to user interactions.
    private MutableLiveData<ViewWrapper> mTrack = new MutableLiveData<>();
    private MutableLiveData<ViewWrapper> mTrackInaccessible = new MutableLiveData<>();
    //LiveData to exposes data changes to observers.
    private MutableLiveData<Void> mEmptyList = new MutableLiveData<>();
    private MutableLiveData<ViewWrapper> mCanOpenDetails = new MutableLiveData<>();
    private MutableLiveData<Integer> mStartAutomaticMode = new MutableLiveData<>();
    private MutableLiveData<Boolean> mShowProgress = new MutableLiveData<>();
    private MutableLiveData<Boolean> mOnCheckAll = new MutableLiveData<>();
    private MutableLiveData<Integer> mOnMessage = new MutableLiveData<>();
    private MutableLiveData<Integer> mOnSorted = new MutableLiveData<>();
    private MutableLiveData<Boolean> mOnSdPresent = new MutableLiveData<>();
    //The current list of tracks.
    private List<Track> mCurrentList;
    @Inject
    public TrackRepository trackRepository;
    @Inject
    public AbstractSharedPreferences sharedPreferences;
    @Inject
    public ServiceUtils serviceUtils;


    public ListViewModel() {
        mShowProgress.setValue(true);
    }

    public LiveData<ViewWrapper> actionTrackEvaluatedSuccessfully(){
        return mTrack;
    }

    public LiveData<ViewWrapper> actionIsTrackInaccessible(){
        return mTrackInaccessible;
    }

    public LiveData<Void> noFilesFound(){
        return mEmptyList;
    }

    public LiveData<Boolean> getLoader(){
        return mShowProgress;
    }

    public LiveData<ViewWrapper> actionCanOpenDetails(){
        return mCanOpenDetails;
    }

    public LiveData<Integer> actionCanStartAutomaticMode(){
        return mStartAutomaticMode;
    }

    public MutableLiveData<Boolean> checkAll() {
        return mOnCheckAll;
    }

    public MutableLiveData<Integer> getMessage() {
        return mOnMessage;
    }

    public MutableLiveData<Integer> onSorted() {
        return mOnSorted;
    }

    public LiveData<Boolean> onSdPresent() {
        return mOnSdPresent;
    }

    /**
     * Return the live data container that holds the reference to tracks from local DB.
     * @return The live data container.
     */
    public LiveData<List<Track>> showAllTracks(){
        LiveData<Resource<List<Track>>> tracks = trackRepository.getAllTracks();
        mTracks = Transformations.map(tracks, input -> {
            mShowProgress.setValue(input.status == Resource.Status.LOADING);
            mCurrentList = input.data;
            return mCurrentList;
        });
        return mTracks;
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
     * @param track The track to remove its info.
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
        boolean isAccessible = Tagger.checkFileIntegrity(track.getPath());
        ViewWrapper viewWrapper = null;
        if(!isAccessible){
            viewWrapper = new ViewWrapper();
            viewWrapper.mode = wrapper.mode;
            viewWrapper.position = wrapper.position;
            viewWrapper.track = track;
            mTrackInaccessible.setValue(viewWrapper);
        }
        else if(track.processing() == 1){
            mOnMessage.setValue(R.string.current_file_processing);
        }
        else {
            viewWrapper = new ViewWrapper();
            viewWrapper.mode = wrapper.mode;
            viewWrapper.position = wrapper.position;
            viewWrapper.track = track;
            mTrack.setValue(viewWrapper);
        }

    }

    /**
     * Fetches the tracks from MediaStore.
     */
    public void fetchTracks(){
        if(sharedPreferences.getBoolean("first_time_read"))
            return;

        trackRepository.fetchTracks(new AsyncOperation<Void, Boolean, Void, Void>() {
            @Override
            public void onAsyncOperationStarted(Void params) {
                mShowProgress.setValue(true);
            }

            @Override
            public void onAsyncOperationFinished(Boolean result) {
                mShowProgress.setValue(false);
                sharedPreferences.putBoolean("first_time_read", true);
                if(result){
                    mEmptyList.setValue(null);
                    mOnMessage.setValue(R.string.no_items_found);
                }
            }

            @Override
            public void onAsyncOperationCancelled(Void cancellation) {
                mShowProgress.setValue(false);
            }

            @Override
            public void onAsyncOperationError(Void error) {}
        });
    }

    /**
     * Re scan the media store.
     */
    public void fetchNewTracks(){
        trackRepository.fetchNewTracks(new AsyncOperation<Void, Boolean, Void, Void>() {
            @Override
            public void onAsyncOperationStarted(Void params) {
                mShowProgress.setValue(true);
            }

            @Override
            public void onAsyncOperationFinished(Boolean result) {
                mShowProgress.setValue(false);
            }

            @Override
            public void onAsyncOperationCancelled(Void cancellation) {
                mShowProgress.setValue(false);
            }

            @Override
            public void onAsyncOperationError(Void error) {}
        });
    }

    /**
     * Handles the click when the cover is clicked.
     * @param viewWrapper A {@link ViewWrapper} object containing th info if the item.
     */
    public void onClickCover(ViewWrapper viewWrapper){
        Track track = mCurrentList.get(viewWrapper.position);
        boolean isAccessible = Tagger.checkFileIntegrity(track.getPath());
        viewWrapper.track = track;
        if(!isAccessible){
            mTrackInaccessible.setValue(viewWrapper);
        }
        else if(track.processing() == 1){
            mOnMessage.setValue(R.string.current_file_processing);
        }
        else {
            mCanOpenDetails.setValue(viewWrapper);
        }
    }

    public void setLoading(boolean showProgress){
        mShowProgress.setValue(showProgress);
    }

    /**
     * Sort list in desired order
     * @param by the field/column to sort by
     * @param orderType the sort type, may be ascendant or descendant
     * @param idResource Current tracks in adapter.
     */
    public void sortTracks(String by, int orderType, int idResource) {
        //wait for sorting while correction task is running
        if(serviceUtils.checkIfServiceIsRunning(FixerTrackService.class.getName())){
            mOnSorted.setValue(-1);
            mOnMessage.setValue(R.string.no_available);
        }

        if(mCurrentList == null || mCurrentList.isEmpty()) {
            mOnSorted.setValue(-1);
            mOnMessage.setValue(R.string.no_available);
        }

        boolean sorted = trackRepository.sortTracks(by, orderType);

        mOnSorted.setValue(sorted ? idResource : -1);
    }

    public void checkAllTracks() {
        if(mCurrentList != null && mCurrentList.size() > 0) {
            mOnCheckAll.setValue(true);
            checkAllItems();
        }
        else {
            mOnCheckAll.setValue(false);
            mOnMessage.setValue(R.string.no_available);
        }

    }

    /**
     * Re scan the media store searching new recently added tracks.
     */
    public void rescan() {
        if(serviceUtils.checkIfServiceIsRunning(FixerTrackService.class.getName())){
            mOnMessage.setValue(R.string.no_available);
        }
        else {
            fetchNewTracks();
        }
    }

    /**
     * Shows the status message of this song.
     * @param position The position of the clicked item.
     */
    public void onCheckMarkClick(int position) {
        mOnMessage.setValue(getStatusText(mCurrentList.get(position).getState()));
    }

    /**
     * Verifies if an slot of SD card is present and if an SD card is inserted.
     * @param context The context.
     */
    public void checkSdIsPresent(Context context) {
        boolean isPresentSD = StorageHelper.getInstance(context.getApplicationContext()).
                isPresentRemovableStorage();
        if(AndroidUtils.getUriSD(context.getApplicationContext()) == null && isPresentSD) {
            mOnSdPresent.setValue(true);
        }
        else {
            mOnSdPresent.setValue(false);
        }
    }

    /**
     * Handles the message when Api of identification service is started.
     */
    public void onApiInitialized() {
        if(mCurrentList != null && mCurrentList.size() > 0) {
            mOnMessage.setValue(R.string.api_initialized);
        }
        else {
            mOnMessage.setValue(R.string.add_some_tracks);
        }
    }

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
}

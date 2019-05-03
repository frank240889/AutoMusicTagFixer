package mx.dev.franco.automusictagfixer.modelsUI.main;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.arch.lifecycle.ViewModel;
import android.content.Context;

import java.util.List;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.UI.main.ListFragment;
import mx.dev.franco.automusictagfixer.identifier.GnService;
import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.network.ConnectivityDetector;
import mx.dev.franco.automusictagfixer.persistence.repository.TrackRepository;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.persistence.room.TrackState;
import mx.dev.franco.automusictagfixer.services.FixerTrackService;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.Resource;
import mx.dev.franco.automusictagfixer.utilities.ServiceUtils;
import mx.dev.franco.automusictagfixer.utilities.StorageHelper;
import mx.dev.franco.automusictagfixer.utilities.Tagger;
import mx.dev.franco.automusictagfixer.utilities.resource_manager.ResourceManager;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;

public class ListViewModel extends ViewModel {
    private static final String TAG = ListViewModel.class.getName();
    //The list of tracks.
    private LiveData<List<Track>> mTracks;
    //MutableLiveData objects to respond to user interactions.
    private MutableLiveData<ListFragment.ViewWrapper> mTrack = new MutableLiveData<>();
    private MutableLiveData<Integer> mCanRunService = new MutableLiveData<>();
    private MutableLiveData<String> mTrackIsProcessing = new MutableLiveData<>();
    private MutableLiveData<ListFragment.ViewWrapper> mTrackInaccessible = new MutableLiveData<>();
    //LiveData to inform that there are no tracks in device, this is necessary because
    //when no tracks are found, it will not insert any data in DB, meaning that this did not change
    //and it will not dispatched the event to any observer because of that.
    private MutableLiveData<Boolean> mEmptyList = new MutableLiveData<>();
    private MutableLiveData<ListFragment.ViewWrapper> mCanOpenDetails = new MutableLiveData<>();
    private MutableLiveData<Integer> mStartAutomaticMode = new MutableLiveData<>();
    private MutableLiveData<Boolean> mShowProgress = new MutableLiveData<>();
    private MutableLiveData<String> mShowMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> mOnCheckAll = new MutableLiveData<>();
    private MutableLiveData<Integer> mOnMessage = new MutableLiveData<>();
    private MutableLiveData<Integer> mOnSorted = new MutableLiveData<>();
    private MutableLiveData<Boolean> mOnSdPresent = new MutableLiveData<>();
    private List<Track> mCurrentList;
    @Inject
    public TrackRepository trackRepository;
    @Inject
    public ResourceManager resourceManager;
    @Inject
    public AbstractSharedPreferences sharedPreferences;
    @Inject
    public ServiceUtils serviceUtils;
    @Inject
    public ConnectivityDetector connectivityDetector;
    @Inject
    public GnService gnService;


    public ListViewModel() {
        AutoMusicTagFixer.getContextComponent().inject(this);
        mShowProgress.setValue(true);
    }

    /**
     * Return the live data container that holds the reference to tracks from local DB.
     * @return The live data container.
     */
    public LiveData<List<Track>> getAllTracks(){
        LiveData<Resource<List<Track>>> tracks = trackRepository.getAllTracks();
        mTracks = Transformations.map(tracks, input -> {
            mShowProgress.setValue(input.status == Resource.Status.LOADING);
            mCurrentList = input.data;
            return mCurrentList;
        });
        return mTracks;
    }

    /**
     * Retrieves the info of tracks from MediaStore.
     */
    public void getInfoForTracks(){
        if(sharedPreferences.getBoolean("first_time_read"))
            return;

        trackRepository.getDataFromTracksFirst(new AsyncOperation<Void, Boolean, Void, Void>() {
            @Override
            public void onAsyncOperationStarted(Void params) {
                mShowProgress.setValue(true);
            }

            @Override
            public void onAsyncOperationFinished(Boolean result) {
                mShowProgress.setValue(false);
                sharedPreferences.putBoolean("first_time_read", true);
                if(result){
                    mEmptyList.setValue(true);
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

    public void checkAllItems(){
        if(serviceUtils.checkIfServiceIsRunning(FixerTrackService.CLASS_NAME))
            return;

        trackRepository.checkAllItems();
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


    public void onItemClick(ListFragment.Wrapper wrapper){
        Track track = mCurrentList.get(wrapper.position);
        boolean isAccessible = Tagger.checkFileIntegrity(track.getPath());
        ListFragment.ViewWrapper viewWrapper = null;
        if(!isAccessible){
            viewWrapper = new ListFragment.ViewWrapper();
            viewWrapper.mode = wrapper.mode;
            viewWrapper.position = wrapper.position;
            viewWrapper.view = wrapper.view;
            viewWrapper.track = track;
            mTrackInaccessible.setValue(viewWrapper);
        }
        else if(track.processing() == 1){
            mOnMessage.setValue(R.string.current_file_processing);
        }
        else {
            viewWrapper = new ListFragment.ViewWrapper();
            viewWrapper.mode = wrapper.mode;
            viewWrapper.position = wrapper.position;
            viewWrapper.view = wrapper.view;
            viewWrapper.track = track;
            mTrack.setValue(viewWrapper);
        }

    }

    public void updateTrackList(){
        trackRepository.getNewTracks(new AsyncOperation<Void, Boolean, Void, Void>() {
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

    public void onClickCover(ListFragment.ViewWrapper viewWrapper){
        boolean isAccessible = Tagger.checkFileIntegrity(viewWrapper.track.getPath());
        if(!isAccessible){
            mTrackInaccessible.setValue(viewWrapper);
        }
        else if(viewWrapper.track.processing() == 1){
            mTrackIsProcessing.setValue(resourceManager.getString(R.string.current_file_processing));
        }
        else {
            this.mCanOpenDetails.setValue(viewWrapper);
        }
    }

    public void setLoading(boolean showProgress){
        mShowProgress.setValue(showProgress);
    }

    public LiveData<String> isTrackProcessing(){
        return mTrackIsProcessing;
    }

    public LiveData<Integer> actionCanRunService(){
        return mCanRunService;
    }

    public LiveData<ListFragment.ViewWrapper> actionTrackEvaluatedSuccessfully(){
        return mTrack;
    }

    public LiveData<ListFragment.ViewWrapper> actionIsTrackInaccessible(){
        return mTrackInaccessible;
    }

    public LiveData<Boolean> noFilesFound(){
        return mEmptyList;
    }

    public LiveData<Boolean> getLoader(){
        return mShowProgress;
    }

    public LiveData<String> actionShowMessage() {
        return mShowMessage;
    }

    public LiveData<ListFragment.ViewWrapper> actionCanOpenDetails(){
        return mCanOpenDetails;
    }

    public LiveData<Integer> actionCanStartAutomaticMode(){
        return mStartAutomaticMode;
    }

    public MutableLiveData<Boolean> getOnCheckAll() {
        return mOnCheckAll;
    }

    public MutableLiveData<Integer> getMessage() {
        return mOnMessage;
    }

    public MutableLiveData<Integer> onSorted() {
        return mOnSorted;
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

    public void actionSelectAll() {
        if(mCurrentList != null && mCurrentList.size() > 0) {
            mOnCheckAll.setValue(true);
            checkAllItems();
        }
        else {
            mOnCheckAll.setValue(false);
            mOnMessage.setValue(R.string.no_available);
        }

    }

    public void rescan() {
        if(serviceUtils.checkIfServiceIsRunning(FixerTrackService.class.getName())){
            mOnMessage.setValue(R.string.no_available);
        }
        else {
            updateTrackList();
        }
    }

    public void onCheckMarkClick(int position) {
        mOnMessage.setValue(getStatusText(mCurrentList.get(position).getState()));
    }

    public LiveData<Boolean> onSdPresent() {
        return mOnSdPresent;
    }

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

    public void onApiInitialized() {
        if(mCurrentList != null && mCurrentList.size() > 0) {
            mOnMessage.setValue(R.string.api_initialized);
        }
        else {
            mOnMessage.setValue(R.string.add_some_tracks);
        }
    }
}

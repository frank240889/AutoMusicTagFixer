package mx.dev.franco.automusictagfixer.modelsUI.main;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.arch.lifecycle.ViewModel;

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
import mx.dev.franco.automusictagfixer.utilities.Resource;
import mx.dev.franco.automusictagfixer.utilities.ServiceUtils;
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
            return input.data;
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
     * Request to repository to update the track
     * @param track The track to update.
     */
    public void updateTrack(Track track){
        trackRepository.update(track);
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

    public String getState(int stateCode){
        return getStatusText(stateCode);
    }

    private String getStatusText(int status){
        String msg = "";
        switch (status){
            case TrackState.ALL_TAGS_FOUND:
                msg = resourceManager.getString(R.string.file_status_ok);
                break;
            case TrackState.ALL_TAGS_NOT_FOUND:
                msg = resourceManager.getString(R.string.file_status_incomplete);
                break;
            default:
                msg = resourceManager.getString(R.string.file_status_no_processed);
                break;
        }

        return msg;
    }


    public void onItemClick(ListFragment.ViewWrapper viewWrapper){
        boolean isAccessible = Tagger.checkFileIntegrity(viewWrapper.track.getPath());
        if(!isAccessible){
            mTrackInaccessible.setValue(viewWrapper);
        }
        else if(viewWrapper.track.processing() == 1){
            mTrackIsProcessing.setValue(resourceManager.getString(R.string.current_file_processing));
        }
        else {
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

    /**
     * Sort list in desired order
     * @param by the field/column to sort by
     * @param orderType the sort type, may be ascendant or descendant
     * @param tracks Current tracks in adapter.
     */
    public boolean sortTracks(String by, int orderType, List<Track> tracks) {
        //wait for sorting while correction task is running
        if(serviceUtils.checkIfServiceIsRunning(FixerTrackService.class.getName())){
            return false;
        }

        if(tracks == null || tracks.isEmpty())
            return false;

       return trackRepository.sortTracks(by, orderType);
    }
}

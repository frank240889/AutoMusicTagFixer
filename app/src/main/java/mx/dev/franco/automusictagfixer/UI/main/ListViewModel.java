package mx.dev.franco.automusictagfixer.UI.main;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import java.util.List;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.list.AudioItem;
import mx.dev.franco.automusictagfixer.media_store_retriever.AsyncFileReader;
import mx.dev.franco.automusictagfixer.network.ConnectivityDetector;
import mx.dev.franco.automusictagfixer.repository.TrackRepository;
import mx.dev.franco.automusictagfixer.room.Track;
import mx.dev.franco.automusictagfixer.services.ServiceHelper;
import mx.dev.franco.automusictagfixer.services.gnservice.GnService;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.Tagger;
import mx.dev.franco.automusictagfixer.utilities.resource_manager.ResourceManager;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;

public class ListViewModel extends ViewModel {
    private static final String TAG = ListViewModel.class.getName();
    private MutableLiveData<ListFragment.ViewWrapper> mTrack = new MutableLiveData<>();
    private MutableLiveData<Integer> mCanRunService = new MutableLiveData<>();
    private MutableLiveData<String> mTrackIsProcessing = new MutableLiveData<>();
    private MutableLiveData<ListFragment.ViewWrapper> mTrackInaccessible = new MutableLiveData<>();
    private MutableLiveData<Boolean> mHasFinishedRetrieving = new MutableLiveData<>();
    private LiveData<List<Track>> mTracks;
    private MutableLiveData<ListFragment.ViewWrapper> mCanOpenDetails = new MutableLiveData<>();
    private MutableLiveData<Integer> mStartAutomaticMode = new MutableLiveData<>();
    private MutableLiveData<Boolean> mShowProgress = new MutableLiveData<>();
    private MutableLiveData<String> mShowMessage = new MutableLiveData<>();
    @Inject
    TrackRepository trackRepository;
    @Inject
    ResourceManager resourceManager;
    @Inject
    AbstractSharedPreferences sharedPreferences;
    @Inject
    ServiceHelper serviceHelper;
    @Inject
    ConnectivityDetector connectivityDetector;
    @Inject
    GnService gnService;

    public ListViewModel() {
        AutoMusicTagFixer.getContextComponent().inject(this);
        mTracks = trackRepository.getAllTracks();
    }

    public LiveData<List<Track>> getAllTracks(){
        mShowProgress.setValue(false);
        return mTracks;
    }

    public void getInfoForTracks(){
        if(sharedPreferences.getBoolean("first_time_read"))
            return;

        mShowProgress.setValue(true);
        trackRepository.getDataFromTracksFirst(new AsyncFileReader.IRetriever() {
            @Override
            public void onStart() {
                mShowProgress.setValue(true);
            }

            @Override
            public void onFinish() {
                mShowProgress.setValue(false);
                mHasFinishedRetrieving.setValue(true);
                sharedPreferences.putBoolean("first_time_read", true);
            }

            @Override
            public void onCancel() {
                mShowProgress.setValue(false);
            }
        });
    }

    public void updateTrack(Track track){
        if(track.checked() == 1){
            track.setChecked(0);
        }
        else {
            track.setChecked(1);
        }
        trackRepository.update(track);
    }

    public void removeTrack(Track track){
        trackRepository.delete(track);
    }

    public void checkAllItems(){
        boolean allChecked = sharedPreferences.getBoolean(Constants.ALL_ITEMS_CHECKED);
        if(allChecked){
            sharedPreferences.putBoolean(Constants.ALL_ITEMS_CHECKED, false);
            trackRepository.uncheckAll();
        }
        else {
            sharedPreferences.putBoolean(Constants.ALL_ITEMS_CHECKED, true);
            trackRepository.checkAll();
        }
    }

    public String getState(int stateCode){
        return getStatusText(stateCode);
    }

    private String getStatusText(int status){
        String msg = "";
        switch (status){
            case AudioItem.STATUS_ALL_TAGS_FOUND:
                msg = resourceManager.getString(R.string.file_status_ok);
                break;
            case AudioItem.STATUS_ALL_TAGS_NOT_FOUND:
                msg = resourceManager.getString(R.string.file_status_incomplete);
                break;
            case AudioItem.STATUS_NO_TAGS_FOUND:
                msg = resourceManager.getString(R.string.file_status_bad);
                break;
            case AudioItem.STATUS_TAGS_EDITED_BY_USER:
                msg = resourceManager.getString(R.string.file_status_edit_by_user);
                break;
            case AudioItem.FILE_ERROR_READ:
                msg = resourceManager.getString(R.string.file_status_error_read);
                break;
            case AudioItem.STATUS_TAGS_CORRECTED_BY_SEMIAUTOMATIC_MODE:
                msg = resourceManager.getString(R.string.file_status_corrected_by_semiautomatic_mode);
                break;
            case AudioItem.STATUS_FILE_IN_SD_WITHOUT_PERMISSION:
                msg = resourceManager.getString(R.string.file_status_in_sd_without_permission);
                break;
            case AudioItem.STATUS_COULD_NOT_APPLIED_CHANGES:
                msg = resourceManager.getString(R.string.could_not_apply_changes);
                break;
            case AudioItem.STATUS_COULD_RESTORE_FILE_TO_ITS_LOCATION:
                msg = resourceManager.getString(R.string.could_not_copy_to_its_original_location);
                break;
            case AudioItem.STATUS_COULD_NOT_CREATE_AUDIOFILE:
                msg = resourceManager.getString(R.string.could_not_create_audiofile);
                break;
            case AudioItem.STATUS_COULD_NOT_CREATE_TEMP_FILE:
                msg = resourceManager.getString(R.string.could_not_create_temp_file);
                break;
            default:
                msg = resourceManager.getString(R.string.file_status_no_processed);
                break;
        }

        return msg;
    }


    public void onItemClick(ListFragment.ViewWrapper viewWrapper){
        mTrack.setValue(viewWrapper);
    }

    public void setChecked(Track track){
        if(track.checked() == 1){
            track.setChecked(0);
        }
        else {
            track.setChecked(1);
        }
        trackRepository.update(track);
    }

    public void updateTrackList(){
        mShowProgress.setValue(true);
        trackRepository.getNewTracks(new AsyncFileReader.IRetriever() {
            @Override
            public void onStart() {
                mShowProgress.setValue(true);
            }

            @Override
            public void onFinish() {
                mShowProgress.setValue(false);
            }

            @Override
            public void onCancel() {
                mShowProgress.setValue(false);
            }
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

    public void onAutomaticMode(ListFragment.ViewWrapper viewWrapper){
        boolean canOpenDetail = evaluateTrack(viewWrapper);
        if(canOpenDetail){
            mStartAutomaticMode.setValue(viewWrapper.track.getMediaStoreId());
        }
    }


    public void setProgress(boolean showProgress){
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

    public LiveData<Boolean> hasFinishedRetrievedTracks(){
        return mHasFinishedRetrieving;
    }

    public LiveData<Boolean> showProgress(){
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

    private boolean evaluateTrack(ListFragment.ViewWrapper viewWrapper){
        boolean isAccessible = Tagger.checkFileIntegrity(viewWrapper.track.getPath());
        if(!isAccessible){
            mTrackInaccessible.setValue(viewWrapper);
            return false;
        }

        if(viewWrapper.track.processing() == 1){
            mTrackIsProcessing.setValue(resourceManager.getString(R.string.current_file_processing));
            return false;
        }

        if(!ConnectivityDetector.sIsConnected){
            mShowMessage.setValue(resourceManager.getString(R.string.no_internet_connection_automatic_mode));
            connectivityDetector.onStartTestingNetwork();
            return false;
        }

        if(!GnService.sApiInitialized || GnService.sIsInitializing) {
            mShowMessage.setValue(resourceManager.getString(R.string.initializing_recognition_api));
            gnService.initializeAPI();
            return false;
        }

        return true;
    }
}

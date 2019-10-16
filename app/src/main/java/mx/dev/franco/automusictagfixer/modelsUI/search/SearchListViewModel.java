package mx.dev.franco.automusictagfixer.modelsUI.search;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import java.util.List;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;
import mx.dev.franco.automusictagfixer.persistence.repository.TrackRepository;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.ui.main.ViewWrapper;
import mx.dev.franco.automusictagfixer.utilities.resource_manager.ResourceManager;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;

public class SearchListViewModel extends ViewModel {
    private static final String TAG = SearchListViewModel.class.getName();
    //MutableLiveData objects to respond to user interactions.
    private MutableLiveData<ViewWrapper> mTrack = new MutableLiveData<>();
    //The list of tracks.
    private LiveData<List<Track>> mTracks;
    private MutableLiveData<String> mTrackIsProcessing = new MutableLiveData<>();
    private MutableLiveData<ViewWrapper> mTrackInaccessible = new MutableLiveData<>();
    private MutableLiveData<Boolean> mShowProgress = new MutableLiveData<>();
    @Inject
    public TrackRepository trackRepository;
    @Inject
    public ResourceManager resourceManager;
    @Inject
    public AbstractSharedPreferences sharedPreferences;

    public SearchListViewModel() {
        mTracks = trackRepository.getSearchResults();
    }

    public LiveData<List<Track>> getSearchResults(){
        return mTracks;
    }

    public void search(String query) {
        if(query == null || query.equals(""))
            return;

        String q = "%"+query+"%";
        trackRepository.trackSearch(q);
    }

    public void onItemClick(ViewWrapper viewWrapper){
        boolean isAccessible = AudioTagger.checkFileIntegrity(viewWrapper.track.getPath());
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

    public void removeTrack(Track track){
        trackRepository.delete(track);
    }

    public void setProgress(boolean showProgress){
        mShowProgress.setValue(showProgress);
    }

    public LiveData<String> isTrackProcessing(){
        return mTrackIsProcessing;
    }

    public LiveData<ViewWrapper> actionTrackEvaluatedSuccessfully(){
        return mTrack;
    }

    public LiveData<ViewWrapper> actionIsTrackInaccessible(){
        return mTrackInaccessible;
    }

    public LiveData<Boolean> showProgress(){
        return mShowProgress;
    }

}

package mx.dev.franco.automusictagfixer.ui.trackdetail;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.identifier.CoverIdentificationResult;
import mx.dev.franco.automusictagfixer.identifier.Identifier;
import mx.dev.franco.automusictagfixer.identifier.TrackIdentificationResult;
import mx.dev.franco.automusictagfixer.interfaces.Cache;
import mx.dev.franco.automusictagfixer.persistence.cache.CoverResultsCache;
import mx.dev.franco.automusictagfixer.persistence.cache.TrackResultsCache;
import mx.dev.franco.automusictagfixer.ui.SingleLiveEvent;

public class ResultsViewModel extends AndroidViewModel {
    protected MutableLiveData<Boolean> mProgressObservable;
    protected SingleLiveEvent<List<? extends Identifier.IdentificationResults>> mObservableTrackResults;
    protected SingleLiveEvent<List<? extends Identifier.IdentificationResults>> mObservableCoverResults;
    //The cache where are stored temporally the identification results.
    private Cache<String, List<TrackIdentificationResult>> mResultsCache;
    private Cache<String, List<CoverIdentificationResult>> mCoverResultsCache;
    private List<TrackIdentificationResult> mTrackResults;
    private List<CoverIdentificationResult> mCoverResults;

    @Inject
    public ResultsViewModel(@NonNull Application application,
                            @NonNull TrackResultsCache cache,
                            @NonNull CoverResultsCache coverResultsCache) {
        super(application);
        mResultsCache = cache;
        mCoverResultsCache = coverResultsCache;
        mProgressObservable = new MutableLiveData<>();
        mObservableTrackResults = new SingleLiveEvent<>();
        mObservableCoverResults = new SingleLiveEvent<>();
    }

    public LiveData<Boolean> observeProgress() {
        return mProgressObservable;
    }

    public LiveData<List<? extends Identifier.IdentificationResults>> observeTrackResults() {
        return mObservableTrackResults;
    }

    public LiveData<List<? extends Identifier.IdentificationResults>> observeCoverResults() {
        return mObservableCoverResults;
    }

    public void fetchResults(String id){
        mProgressObservable.setValue(true);
        mTrackResults = mResultsCache.load(id);
        mObservableTrackResults.setValue(mTrackResults);
        mProgressObservable.setValue(false);
    }

    public void fetchCoverResults(String id){
        mProgressObservable.setValue(true);
        mCoverResults = mCoverResultsCache.load(id);
        mObservableCoverResults.setValue(mCoverResults);
        mProgressObservable.setValue(false);
    }

    public Identifier.IdentificationResults getCoverResult(int position) {
        return mCoverResults.get(position);
    }

    public Identifier.IdentificationResults getTrackResult(int position) {
        return mTrackResults.get(position);
    }
}

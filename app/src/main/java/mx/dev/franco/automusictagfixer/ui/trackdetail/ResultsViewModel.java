package mx.dev.franco.automusictagfixer.ui.trackdetail;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.identifier.Identifier;
import mx.dev.franco.automusictagfixer.identifier.Result;
import mx.dev.franco.automusictagfixer.persistence.cache.IdentificationResultsCache;

public class ResultsViewModel extends AndroidViewModel {
    protected MutableLiveData<Boolean> mProgressObservable;
    private IdentificationResultsCache mIdentificationResultsCache;
    private MutableLiveData<List<? extends Identifier.IdentificationResults>> mTrackObservableResults = new MutableLiveData<>();
    private MutableLiveData<List<? extends Identifier.IdentificationResults>> mCoverObservableResults = new MutableLiveData<>();

    @Inject
    public ResultsViewModel(@NonNull Application application,
                            @NonNull IdentificationResultsCache resultsCache) {
        super(application);
        mIdentificationResultsCache = resultsCache;
        mProgressObservable = new MutableLiveData<>();
    }

    public LiveData<Boolean> observeProgress() {
        return mProgressObservable;
    }

    public LiveData<List<? extends Identifier.IdentificationResults>> observeTrackResults() {
        return mTrackObservableResults;
    }

    public LiveData<List<? extends Identifier.IdentificationResults>> observeCoverResults() {
        return mCoverObservableResults;
    }

    public void fetchResults(String id){
        mProgressObservable.setValue(true);
        List<? extends Identifier.IdentificationResults> results = mIdentificationResultsCache.load(id);
        mTrackObservableResults.setValue(results);
        mProgressObservable.setValue(false);

    }

    public void fetchCoverResults(String id){
        mProgressObservable.setValue(true);
        List<? extends Identifier.IdentificationResults> results = mIdentificationResultsCache.load(id);
        List<? extends Identifier.IdentificationResults> filteredResults = getOnlyCovers(results);
        mProgressObservable.setValue(false);
        mCoverObservableResults.setValue(filteredResults);
    }

    public String getTitle(String id) {
        List<? extends Identifier.IdentificationResults> results = mIdentificationResultsCache.load(id);
        if (results == null) {
            return null;
        }
        return ((Result)results.get(0)).getTitle();
    }

    public Identifier.IdentificationResults getCoverResult(int position) {
        return mCoverObservableResults.getValue().get(position);
    }

    public Identifier.IdentificationResults getTrackResult(int position) {
        return mTrackObservableResults.getValue().get(position);
    }

    private List<? extends Identifier.IdentificationResults> getOnlyCovers(List<? extends Identifier.IdentificationResults> results) {
        List<Identifier.IdentificationResults> filteredIdentificationResults = new ArrayList<>();
        for (Identifier.IdentificationResults identificationResults : results) {
            Result result = (Result) identificationResults;
            if (result.getCoverArt() != null) {
                filteredIdentificationResults.add(result);
            }
        }
        return filteredIdentificationResults;
    }
}

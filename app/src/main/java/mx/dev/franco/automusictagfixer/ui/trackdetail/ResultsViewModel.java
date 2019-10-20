package mx.dev.franco.automusictagfixer.ui.trackdetail;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.identifier.Identifier;
import mx.dev.franco.automusictagfixer.interfaces.Cache;
import mx.dev.franco.automusictagfixer.persistence.cache.DownloadedTrackDataCacheImpl;
import mx.dev.franco.automusictagfixer.ui.SingleLiveEvent;

public class ResultsViewModel extends AndroidViewModel {
    protected MutableLiveData<Boolean> mProgressObservable;
    protected MutableLiveData<List<Identifier.IdentificationResults>> mObservableResults;
    //The cache where are stored temporally the identification results.
    private Cache<String, List<Identifier.IdentificationResults>> mResultsCache;

    @Inject
    public ResultsViewModel(@NonNull Application application,
                            @NonNull DownloadedTrackDataCacheImpl cache) {
        super(application);
        mResultsCache = cache;
        mProgressObservable = new MutableLiveData<>();
        mObservableResults = new SingleLiveEvent<>();
    }

    public LiveData<Boolean> observeProgress() {
        return mProgressObservable;
    }

    public LiveData<List<Identifier.IdentificationResults>> observeResults() {
        return mObservableResults;
    }

    public void fetchResults(String id){
        mProgressObservable.setValue(true);
        List<Identifier.IdentificationResults> results = mResultsCache.load(id);
        mObservableResults.setValue(results);
        mProgressObservable.setValue(false);
    }
}

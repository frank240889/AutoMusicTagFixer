package mx.dev.franco.automusictagfixer.ui.trackdetail;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.gracenote.gnsdk.GnImageSize;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.identifier.Identifier;
import mx.dev.franco.automusictagfixer.identifier.Result;
import mx.dev.franco.automusictagfixer.interfaces.Cache;
import mx.dev.franco.automusictagfixer.persistence.cache.DownloadedTrackDataCacheImpl;
import mx.dev.franco.automusictagfixer.ui.SingleLiveEvent;

public class ResultsViewModel extends AndroidViewModel {
    protected MutableLiveData<Boolean> mProgressObservable;
    protected MutableLiveData<List<Identifier.IdentificationResults>> mObservableResults;
    //The cache where are stored temporally the identification results.
    private Cache<String, List<Identifier.IdentificationResults>> mResultsCache;
    private List<Identifier.IdentificationResults> mResults;

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
        mResults = mResultsCache.load(id);
        mObservableResults.setValue(mResults);
        mProgressObservable.setValue(false);
    }

    public String getImageUrl(int position) {
        Result result  = (Result) mResults.get(position);
        Map<GnImageSize, String> covers = result.getCovers();
        Set<Map.Entry<GnImageSize, String>> entries = covers.entrySet();
        String gnImage = null;
        for(Map.Entry<GnImageSize, String> entry : entries) {
            if(entry.getValue() != null) {
                gnImage = entry.getValue();
                break;
            }
        }
        return gnImage;
    }
}

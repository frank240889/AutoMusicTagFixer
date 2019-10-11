package mx.dev.franco.automusictagfixer.UI.results;

import android.app.Application;
import android.support.annotation.NonNull;

import java.util.List;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.UI.track_detail.ResultsViewModel;
import mx.dev.franco.automusictagfixer.identifier.Identifier;
import mx.dev.franco.automusictagfixer.interfaces.Cache;

/**
 * This class load the results for the identified track.
 */
public class TagsResultsViewModel extends ResultsViewModel {
    private Cache<String, List<Identifier.IdentificationResults>> mCache;

    @Inject
    public TagsResultsViewModel(@NonNull Application application,
                                Cache<String, List<Identifier.IdentificationResults>> cache) {
        super(application);
        mCache = cache;
    }

    @Override
    public void fetchResults(String id) {
        mProgressObservable.setValue(true);
        List<Identifier.IdentificationResults> identificationResults = mCache.load(id);
        mObservableResults.setValue(identificationResults);
        mProgressObservable.setValue(false);
    }

    @Override
    protected void onCleared() {
        mCache.deleteAll();
        mCache = null;
    }
}

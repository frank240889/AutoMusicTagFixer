package mx.dev.franco.automusictagfixer.UI;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import java.util.List;

import mx.dev.franco.automusictagfixer.identifier.Identifier;

public abstract class ResultsViewModel extends AndroidViewModel {
    protected MutableLiveData<Boolean> mProgressObservable;
    protected MutableLiveData<List<Identifier.IdentificationResults>> mObservableResults;

    public ResultsViewModel(@NonNull Application application) {
        super(application);
        mProgressObservable = new MutableLiveData<>();
    }

    public LiveData<Boolean> observeProgress() {
        return mProgressObservable;
    }

    public LiveData<List<Identifier.IdentificationResults>> observeResults() {
        return mObservableResults;
    }

    public abstract void fetchResults(String id);
}

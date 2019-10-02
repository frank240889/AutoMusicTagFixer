package mx.dev.franco.automusictagfixer.UI.track_detail;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

public class TrackDetailViewModel extends AndroidViewModel {
    public MutableLiveData<String> mTitle;
    public TrackDetailViewModel(@NonNull Application application) {
        super(application);
        mTitle = new MutableLiveData<>();
    }
    
}

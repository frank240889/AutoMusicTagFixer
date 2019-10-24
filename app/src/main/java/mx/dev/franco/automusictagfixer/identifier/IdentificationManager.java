package mx.dev.franco.automusictagfixer.identifier;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.identifier.Identifier.IdentificationListener;
import mx.dev.franco.automusictagfixer.identifier.Identifier.IdentificationStatus;
import mx.dev.franco.automusictagfixer.interfaces.Cache;
import mx.dev.franco.automusictagfixer.persistence.cache.DownloadedTrackDataCacheImpl;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.ui.SingleLiveEvent;

public class IdentificationManager {

    private MutableLiveData<Boolean> mLoadingStateLiveData;
    private SingleLiveEvent<IdentificationStatus> mOnSuccessIdentificationLiveData;
    private SingleLiveEvent<IdentificationStatus> mOnFailIdentificationLiveData;
    private Identifier<Track, List<Identifier.IdentificationResults>> mIdentifier;
    private Cache<String, List<Identifier.IdentificationResults>> mResultsCache;
    private boolean mIdentifying = false;
    private GnApiService mApiService;
    private Context mContext;

    @Inject
    public IdentificationManager(@NonNull DownloadedTrackDataCacheImpl cache,
                                 IdentifierFactory identifierFactory,
                                 GnApiService gnApiService,
                                 Context context){
        mApiService = gnApiService;
        mResultsCache = cache;
        mContext = context;
        mIdentifier = identifierFactory.create(IdentifierFactory.FINGERPRINT_IDENTIFIER);
        mOnSuccessIdentificationLiveData = new SingleLiveEvent<>();
        mOnFailIdentificationLiveData = new SingleLiveEvent<>();
        mLoadingStateLiveData = new MutableLiveData<>();
    }

    /**
     * Livedata to inform a task is in progress.
     * @return A livedata with boolean value.
     */
    public LiveData<Boolean> observeLoadingState() {
        return mLoadingStateLiveData;
    }

    public LiveData<IdentificationStatus> observeSuccessIdentification() {
        return mOnSuccessIdentificationLiveData;
    }

    public LiveData<IdentificationStatus> observeFailIdentification() {
        return mOnFailIdentificationLiveData;
    }

    public void startIdentification(Track track) {
        if(!mApiService.isApiInitialized()) {
            Intent intent = new Intent(mContext, ApiInitializerService.class);
            mContext.startService(intent);
            String msg = mContext.getString(R.string.initializing_recognition_api);
            mOnFailIdentificationLiveData.setValue(new IdentificationStatus(Identifier.IdentificationState.IDENTIFICATION_ERROR, msg));
        }
        else if(mApiService.isApiInitializing()) {
            String msg = mContext.getString(R.string.initializing_recognition_api);
            mOnFailIdentificationLiveData.setValue(new IdentificationStatus(Identifier.IdentificationState.IDENTIFICATION_ERROR, msg));
        }
        else {
            if(mIdentifying)
                return;

            mIdentifier.registerCallback(new IdentificationListener<List<Identifier.IdentificationResults>, Track>() {
                @Override
                public void onIdentificationStart(Track file) {
                    mLoadingStateLiveData.setValue(true);
                    mIdentifying = true;
                }

                @Override
                public void onIdentificationFinished(List<Identifier.IdentificationResults> result, Track file) {
                    mLoadingStateLiveData.setValue(false);
                    mResultsCache.add(file.getMediaStoreId()+"", result);
                    String msg = mContext.getString(R.string.complete_identification);
                    mOnSuccessIdentificationLiveData.setValue(new IdentificationStatus(Identifier.IdentificationState.IDENTIFICATION_FINISHED, msg));
                    mIdentifying = false;
                }

                @Override
                public void onIdentificationError(Track file, String error) {
                    mLoadingStateLiveData.setValue(false);
                    mOnFailIdentificationLiveData.setValue(new IdentificationStatus(Identifier.IdentificationState.IDENTIFICATION_ERROR, error));
                    mIdentifying = false;
                }

                @Override
                public void onIdentificationCancelled(Track file) {
                    mLoadingStateLiveData.setValue(false);
                    String msg = mContext.getString(R.string.identification_cancelled);
                    mOnFailIdentificationLiveData.setValue(new IdentificationStatus(Identifier.IdentificationState.IDENTIFICATION_CANCELLED, msg));
                    mIdentifying = false;
                }

                @Override
                public void onIdentificationNotFound(Track file) {
                    mLoadingStateLiveData.setValue(false);
                    String msg = mContext.getString(R.string.no_found_tags);
                    mOnFailIdentificationLiveData.setValue(new IdentificationStatus(Identifier.IdentificationState.IDENTIFICATION_NOT_FOUND, msg));
                    mIdentifying = false;
                }
            });
            mIdentifier.identify(track);
        }
    }

    public void cancelIdentification() {
        if(mIdentifier != null && mIdentifying)
            mIdentifier.cancel();
    }

    public List<Identifier.IdentificationResults> getResult(String id) {
        return mResultsCache.load(id);
    }
}

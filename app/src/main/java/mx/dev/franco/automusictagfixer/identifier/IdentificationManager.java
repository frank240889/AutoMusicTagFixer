package mx.dev.franco.automusictagfixer.identifier;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.common.Action;
import mx.dev.franco.automusictagfixer.interfaces.Cache;
import mx.dev.franco.automusictagfixer.persistence.cache.DownloadedTrackDataCacheImpl;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.ui.SingleLiveEvent;
import mx.dev.franco.automusictagfixer.utilities.ActionableMessage;
import mx.dev.franco.automusictagfixer.utilities.Resource;

public class IdentificationManager {

    private MutableLiveData<Boolean> mLoadingStateLiveData;
    private SingleLiveEvent<Resource<ActionableMessage>> mOnActionableMessage;
    private Identifier<Track, List<Identifier.IdentificationResults>> mIdentifier;
    private Cache<String, List<Identifier.IdentificationResults>> mResultsCache;
    private IdentifierFactory identifierFactory;
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
        this.identifierFactory = identifierFactory;
        mIdentifier = identifierFactory.create(IdentifierFactory.FINGERPRINT_IDENTIFIER);
        mOnActionableMessage = new SingleLiveEvent<>();
        mLoadingStateLiveData = new MutableLiveData<>();
    }

    /**
     * Livedata to inform a task is in progress.
     * @return A livedata with boolean value.
     */
    public LiveData<Boolean> observeLoadingState() {
        return mLoadingStateLiveData;
    }

    public LiveData<Resource<ActionableMessage>> observeActionableMessage() {
        return mOnActionableMessage;
    }

    public void startIdentification(Track track) {
        if(!mApiService.isApiInitialized()) {
            Intent intent = new Intent(mContext, ApiInitializerService.class);
            mContext.startService(intent);
            mOnActionableMessage.setValue(Resource.error(new ActionableMessage(Action.RETRY_ON_ERROR, R.string.initializing_recognition_api)));
        }
        else if(mApiService.isApiInitializing()) {
            mOnActionableMessage.setValue(Resource.error(new ActionableMessage(Action.RETRY_ON_ERROR, R.string.initializing_recognition_api)));
        }
        else {
            if(mIdentifying)
                return;

            mIdentifier.registerCallback(new Identifier.IdentificationListener<List<Identifier.IdentificationResults>, Track>() {
                @Override
                public void onIdentificationStart(Track file) {
                    mLoadingStateLiveData.setValue(true);
                    mIdentifying = true;
                }

                @Override
                public void onIdentificationFinished(List<Identifier.IdentificationResults> result, Track file) {
                    mLoadingStateLiveData.setValue(false);
                    mResultsCache.add(file.getMediaStoreId()+"", result);
                    mOnActionableMessage.setValue(Resource.success(new ActionableMessage(Action.SUCCESS_IDENTIFICATION, null)));
                    mIdentifying = false;
                }

                @Override
                public void onIdentificationError(Track file, String error) {
                    mLoadingStateLiveData.setValue(false);
                    mOnActionableMessage.setValue(Resource.error(new ActionableMessage(Action.RETRY_ON_ERROR, error)));
                    mIdentifying = false;
                }

                @Override
                public void onIdentificationCancelled(Track file) {
                    mLoadingStateLiveData.setValue(false);
                    mOnActionableMessage.setValue(Resource.cancelled(new ActionableMessage(Action.CANCELLED_IDENTIFICATION, R.string.identification_cancelled)));
                    mIdentifying = false;
                }

                @Override
                public void onIdentificationNotFound(Track file) {
                    mLoadingStateLiveData.setValue(false);
                    mOnActionableMessage.setValue(Resource.error(new ActionableMessage(Action.RETRY_IDENTIFICATION, R.string.no_found_tags)));
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

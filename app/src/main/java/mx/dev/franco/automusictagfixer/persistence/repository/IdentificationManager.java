package mx.dev.franco.automusictagfixer.persistence.repository;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import java.util.List;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.UI.SingleLiveEvent;
import mx.dev.franco.automusictagfixer.common.Action;
import mx.dev.franco.automusictagfixer.identifier.Identifier;
import mx.dev.franco.automusictagfixer.interfaces.Cache;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.utilities.ActionableMessage;
import mx.dev.franco.automusictagfixer.utilities.Resource;

public class IdentificationManager {

    private MutableLiveData<Boolean> mLoadingStateLiveData = new MutableLiveData<>();
    private SingleLiveEvent<Resource<ActionableMessage>> mOnActionableMessage;
    private Identifier<Track, List<Identifier.IdentificationResults>> mIdentifier;
    private Cache<String, List<Identifier.IdentificationResults>> mResultsCache;

    @Inject
    public IdentificationManager(@NonNull Cache<String, List<Identifier.IdentificationResults>> cache){
        mResultsCache = cache;
        mOnActionableMessage = new SingleLiveEvent<>();
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
        mIdentifier.registerCallback(new Identifier.IdentificationListener<List<Identifier.IdentificationResults>, Track>() {
            @Override
            public void onIdentificationStart(Track file) {
                mLoadingStateLiveData.setValue(true);
            }

            @Override
            public void onIdentificationFinished(List<Identifier.IdentificationResults> result, Track file) {
                mLoadingStateLiveData.setValue(false);
                mResultsCache.add(file.getMediaStoreId()+"", result);
                mOnActionableMessage.setValue(Resource.success(new ActionableMessage(Action.SUCCESS_IDENTIFICATION, null)));
            }

            @Override
            public void onIdentificationError(Track file, String error) {
                mLoadingStateLiveData.setValue(false);
                mOnActionableMessage.setValue(Resource.error(new ActionableMessage(Action.RETRY_ON_ERROR, error)));
            }

            @Override
            public void onIdentificationCancelled(Track file) {
                mLoadingStateLiveData.setValue(false);
                mOnActionableMessage.setValue(Resource.cancelled(new ActionableMessage(Action.CANCELLED_IDENTIFICATION, R.string.identification_cancelled)));
            }

            @Override
            public void onIdentificationNotFound(Track file) {
                mLoadingStateLiveData.setValue(false);
                mOnActionableMessage.setValue(Resource.error(new ActionableMessage(Action.RETRY_IDENTIFICATION, R.string.no_found_tags)));
            }
        });
        mIdentifier.identify(track);
    }

    public void cancelIdentification() {
        if(mIdentifier != null)
            mIdentifier.cancel();
    }
}

package mx.dev.franco.automusictagfixer.identifier;

import android.content.Context;
import android.content.Intent;
import android.util.ArrayMap;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.identifier.Identifier.IdentificationListener;
import mx.dev.franco.automusictagfixer.identifier.Identifier.IdentificationStatus;
import mx.dev.franco.automusictagfixer.interfaces.Cache;
import mx.dev.franco.automusictagfixer.persistence.cache.IdentificationResultsCache;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.ui.SingleLiveEvent;

/**
 * @author Franco Castillo
 * @version 1.0
 * @since 1.0
 * Instances of this classes have the responsibility to orchestrate the process of identification
 * and make the results available through a shared storage to other components.
 */
public class IdentificationManager {

    /**
     * Reports the state of this manager.
     */
    private MutableLiveData<Boolean> mLoadingStateLiveData;
    /**
     * Triggers a error identification event.
     */
    private SingleLiveEvent<IdentificationStatus> mOnFailIdentificationLiveData;
    /**
     * The component that really performs the identification.
     */
    private Identifier<Map<String, String>, List<Identifier.IdentificationResults>> mIdentifier;
    /**
     * A temporal storage to put the results.
     */
    private Cache<String, List<Identifier.IdentificationResults>> mResultsCache;
    /**
     * Flag to set the state of this manager.
     */
    private boolean mIdentifying = false;
    /**
     * The API that performs the identification process.
     */
    private GnApiService mApiService;
    private Context mContext;
    private SingleLiveEvent<String> mObservableMessage;
    private SingleLiveEvent<List<Identifier.IdentificationResults>> mObservableResults;

    @Inject
    public IdentificationManager(@NonNull IdentificationResultsCache cache,
                                 @NonNull IdentifierFactory identifierFactory,
                                 @NonNull GnApiService gnApiService,
                                 @NonNull Context context){
        mApiService = gnApiService;
        mContext = context;
        mResultsCache = cache;
        mIdentifier = identifierFactory.create(IdentifierFactory.FINGERPRINT_IDENTIFIER);

        mOnFailIdentificationLiveData = new SingleLiveEvent<>();
        mLoadingStateLiveData = new SingleLiveEvent<>();

        mObservableMessage = new SingleLiveEvent<>();
        mObservableResults = new SingleLiveEvent<>();
    }

    /**
     * Returns the state of this manager.
     * @return A livedata with boolean value.
     */
    public LiveData<Boolean> observeLoadingState() {
        return mLoadingStateLiveData;
    }

    public LiveData<List<Identifier.IdentificationResults>> observeResults() {
        return mObservableResults;
    }

    public LiveData<IdentificationStatus> observeFailIdentification() {
        return mOnFailIdentificationLiveData;
    }

    public LiveData<String> observeMessage() {
        return mObservableMessage;
    }

    /**
     * Starts the identification process.
     * @param track The track to identify.
     */
    public void startIdentification(Track track) {
        if(mIdentifying)
            return;

        Map<String, String> data = new ArrayMap<>();
        data.put(Identifier.Field.FILENAME.name(), track.getPath());
        data.put(Identifier.Field.TITLE.name(), track.getTitle());
        data.put(Identifier.Field.ARTIST.name(), track.getArtist());
        data.put(Identifier.Field.ALBUM.name(), track.getAlbum());

        // Check if API is available and tries to initialize it if does not.
        if(!mApiService.isApiInitialized()) {
            Intent intent = new Intent(mContext, ApiInitializerService.class);
            mContext.startService(intent);
            String msg = mContext.getString(R.string.initializing_recognition_api);
            mObservableMessage.setValue(msg);
        }
        // Sends a message to indicate API is initializing.
        else if(mApiService.isApiInitializing()) {
            String msg = mContext.getString(R.string.initializing_recognition_api);
            mObservableMessage.setValue(msg);
        }
        else {
            mIdentifier.registerCallback(new IdentificationListener<List<Identifier.IdentificationResults>>() {
                @Override
                public void onIdentificationStart() {
                    mLoadingStateLiveData.setValue(true);
                    mObservableMessage.setValue(mContext.getString(R.string.identifying));
                    mIdentifying = true;
                }

                @Override
                public void onIdentificationFinished(List<Identifier.IdentificationResults> result) {
                    mResultsCache.add(track.getMediaStoreId()+"", result);
                    mObservableResults.setValue(result);
                    mLoadingStateLiveData.setValue(false);
                }

                @Override
                public void onIdentificationError(Throwable e) {
                    mLoadingStateLiveData.setValue(false);
                    mObservableMessage.setValue(e.getMessage());
                    mIdentifying = false;
                }

                @Override
                public void onIdentificationCancelled() {
                    mLoadingStateLiveData.setValue(false);
                    String msg = mContext.getString(R.string.identification_cancelled);
                    mObservableMessage.setValue(msg);
                    mIdentifying = false;
                }

                @Override
                public void onIdentificationNotFound() {
                    mLoadingStateLiveData.setValue(false);
                    String msg = mContext.getString(R.string.no_found_tags);
                    mObservableMessage.setValue(msg);
                    mIdentifying = false;
                }
            });
            mIdentifier.identify(data);
        }
    }

    public void cancelIdentification() {
        if(mIdentifier != null && mIdentifying)
            mIdentifier.cancel();
    }
}

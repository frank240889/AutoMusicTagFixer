package mx.dev.franco.automusictagfixer.identifier;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import java.util.List;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.identifier.Identifier.IdentificationListener;
import mx.dev.franco.automusictagfixer.identifier.Identifier.IdentificationStatus;
import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.interfaces.Cache;
import mx.dev.franco.automusictagfixer.persistence.cache.CoverResultsCache;
import mx.dev.franco.automusictagfixer.persistence.cache.TrackResultsCache;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.ui.SingleLiveEvent;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;

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
    private SingleLiveEvent<Boolean> mLoadingStateLiveData;
    /**
     * Triggers a success identification event.
     */
    private SingleLiveEvent<IdentificationStatus> mOnSuccessIdentificationLiveData;
    /**
     * Triggers a error identification event.
     */
    private SingleLiveEvent<IdentificationStatus> mOnFailIdentificationLiveData;
    /**
     * The component that really performs the identification.
     */
    private Identifier<Track, List<Identifier.IdentificationResults>> mIdentifier;
    /**
     * A temporal storage to put the cover results.
     */
    private Cache<String, List<CoverIdentificationResult>> mCoverCache;
    /**
     * A temporal storage to put the results.
     */
    private Cache<String, List<TrackIdentificationResult>> mTrackCache;
    /**
     * Flag to set the state of this manager.
     */
    private boolean mIdentifying = false;
    /**
     * The API that performs the identification process.
     */
    private GnApiService mApiService;
    private Context mContext;
    /**
     * The id of track being identified.
     */
    private String mTrackId;
    /**
     * Process the identification results converting them to a standard list of objects.
     */
    private ResultCreator mResultCreator;

    @Inject
    public IdentificationManager(@NonNull CoverResultsCache coverResultsCache,
                                 @NonNull TrackResultsCache trackResultsCache,
                                 @NonNull IdentifierFactory identifierFactory,
                                 @NonNull GnApiService gnApiService,
                                 @NonNull Context context){
        mApiService = gnApiService;
        mContext = context;
        mCoverCache = coverResultsCache;
        mTrackCache = trackResultsCache;
        mIdentifier = identifierFactory.create(IdentifierFactory.FINGERPRINT_IDENTIFIER);
        mOnSuccessIdentificationLiveData = new SingleLiveEvent<>();
        mOnFailIdentificationLiveData = new SingleLiveEvent<>();
        mLoadingStateLiveData = new SingleLiveEvent<>();
    }

    /**
     * Returns the state of this manager.
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

    /**
     * Starts the identification process.
     * @param track The track to identify.
     */
    public void startIdentification(Track track) {
        mTrackId = track.getMediaStoreId() + "";
        // Check if API is available and tries to initialize it if does not.
        if(!mApiService.isApiInitialized()) {
            Intent intent = new Intent(mContext, ApiInitializerService.class);
            mContext.startService(intent);
            String msg = mContext.getString(R.string.initializing_recognition_api);
            mOnFailIdentificationLiveData.setValue(new IdentificationStatus(Identifier.IdentificationState.IDENTIFICATION_ERROR, msg));
        }
        // Sends a message to indicate API is initializing.
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
                    processResults(result);
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

    private void processResults(List<? extends Identifier.IdentificationResults> result) {
        mResultCreator = new ResultCreator(new AsyncOperation<Void, Void, Void, Void>() {
            @Override
            public void onAsyncOperationStarted(Void params) {
                mLoadingStateLiveData.setValue(true);
            }

            @Override
            public void onAsyncOperationFinished(Void result) {
                String msg = mContext.getString(R.string.complete_identification);
                mOnSuccessIdentificationLiveData.
                        setValue(new IdentificationStatus(Identifier.IdentificationState.IDENTIFICATION_FINISHED, msg));
                mLoadingStateLiveData.setValue(false);
                mIdentifying = false;
            }

            @Override
            public void onAsyncOperationCancelled(Void cancellation) {
                mLoadingStateLiveData.setValue(false);
                String msg = mContext.getString(R.string.identification_cancelled);
                mOnFailIdentificationLiveData.setValue(new IdentificationStatus(Identifier.IdentificationState.IDENTIFICATION_CANCELLED, msg));
                mTrackCache.deleteAll();
                mCoverCache.deleteAll();
                mIdentifying = false;
            }
        },mContext, mTrackId, mCoverCache, mTrackCache);

        mResultCreator.executeOnExecutor(AutoMusicTagFixer.getExecutorService(), result);
    }

    public void cancelIdentification() {
        if(mIdentifier != null && mIdentifying)
            mIdentifier.cancel();
        if(mResultCreator != null && (mResultCreator.getStatus() == AsyncTask.Status.PENDING ||
                mResultCreator.getStatus() == AsyncTask.Status.RUNNING)) {
            mResultCreator.cancel(true);
            mResultCreator = null;
        }
    }

    public TrackIdentificationResult getTrackResult(String trackId, String resultId) {
        List<TrackIdentificationResult> resultList = getTrackListResult(trackId);
        return (TrackIdentificationResult) findResult(resultList, resultId);
    }

    public CoverIdentificationResult getCoverResult(String trackId, String coverId) {
        List<CoverIdentificationResult> resultList = getCoverListResult(trackId);
        return (CoverIdentificationResult) findResult(resultList, coverId);
    }

    public List<TrackIdentificationResult> getTrackListResult(String trackId) {
        return mTrackCache.load(trackId);
    }

    public List<CoverIdentificationResult> getCoverListResult(String trackId) {
        return mCoverCache.load(trackId);
    }

    public void clearResults() {
        mTrackCache.deleteAll();
        mCoverCache.deleteAll();
        mOnSuccessIdentificationLiveData.call();
        mOnFailIdentificationLiveData.call();
        //mLoadingStateLiveData.call();
    }

    private Identifier.IdentificationResults findResult(List<? extends Identifier.IdentificationResults> resultList, String idToSearch) {
        return AndroidUtils.findId(resultList, idToSearch);
    }
}

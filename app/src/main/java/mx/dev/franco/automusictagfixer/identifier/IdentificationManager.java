package mx.dev.franco.automusictagfixer.identifier;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.ArrayMap;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.gracenote.gnsdk.GnImageSize;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.common.AutoMusicTagFixerException;
import mx.dev.franco.automusictagfixer.common.ConnectionChecker;
import mx.dev.franco.automusictagfixer.common.ErrorCode;
import mx.dev.franco.automusictagfixer.identifier.Identifier.IdentificationListener;
import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.interfaces.Cache;
import mx.dev.franco.automusictagfixer.interfaces.Cancelable;
import mx.dev.franco.automusictagfixer.persistence.cache.IdentificationResultsCache;
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
public class IdentificationManager implements Cancelable<Void> {
    public static final int ALL_TAGS = 0;
    public static final int ONLY_COVER = 1;

    private boolean mVerifyConnection = true;

    /**
     * Dispatcher of loading event.
     */
    private final SingleLiveEvent<IdentificationEvent> mObservableLoadingMessage;
    private int mIdentificationType = ALL_TAGS;
    /**
     * The component that performs the identification against the API.
     */
    private Identifier<Map<String, String>, List<? extends Identifier.IdentificationResults>> mIdentifier;
    /**
     * A temporal storage to put the results.
     */
    private Cache<String, List<? extends Identifier.IdentificationResults>> mResultsCache;
    /**
     * Flag to set the state of this manager.
     */
    private boolean mIdentifying = false;
    private boolean mCheckingConnection = false;
    /**
     * The API that performs the identification process.
     */
    private GnApiService mApiService;
    private Context mContext;
    /**
     * A dispatcher to trigger a simple message.
     */
    private SingleLiveEvent<String> mObservableMessage;
    /**
     * A dispatcher to trigger the identification type when the identification is successful.
     */
    private SingleLiveEvent<Integer> mSuccessIdentification;

    @Inject
    public IdentificationManager(@NonNull IdentificationResultsCache cache,
                                 @NonNull IdentifierFactory identifierFactory,
                                 @NonNull GnApiService gnApiService,
                                 @NonNull Context context){

        mApiService = gnApiService;
        mContext = context;
        mResultsCache = cache;
        mIdentifier = identifierFactory.create(IdentifierFactory.FINGERPRINT_IDENTIFIER);

        mObservableMessage = new SingleLiveEvent<>();
        mObservableLoadingMessage = new SingleLiveEvent<>();
        mSuccessIdentification = new SingleLiveEvent<>();
    }

    public LiveData<Integer> observeSuccessIdentification() {
        return mSuccessIdentification;
    }

    public LiveData<String> observeMessage() {
        return mObservableMessage;
    }

    public LiveData<IdentificationEvent> observeIdentificationEvent() {
        return mObservableLoadingMessage;
    }

    /**
     * Starts the identification process.
     * @param track The track to identify.
     */
    public void startIdentification(Track track) {
        if (mVerifyConnection) {
            if (mCheckingConnection)
                return;

            ConnectionChecker connectionChecker = new ConnectionChecker(new AsyncOperation<Void, Boolean, Void, Void>() {
                @Override
                public void onAsyncOperationStarted(Void params) {
                    mCheckingConnection = true;
                }

                @Override
                public void onAsyncOperationFinished(Boolean connected) {
                    mCheckingConnection = false;
                    if (!connected) {
                        mObservableMessage.setValue(mContext.getString(R.string.connect_to_internet));
                    }
                    else {
                        identify(track);
                    }
                }
            });
            connectionChecker.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mContext);
        }
        else {
            identify(track);
        }
    }

    public IdentificationManager addCallback(IdentificationListener<List<? extends Identifier.IdentificationResults>> callback) {
        mIdentifier.registerCallback(callback);
        return this;
    }

    /**
     * Performs the identification process, checking first if there are results available in cache.
     * @param track The track to identify.
     */
    private void identify(Track track) {
        if(mIdentifying)
            return;

        List<? extends Identifier.IdentificationResults> identificationResults = mResultsCache.load(track.getMediaStoreId()+"");
        if (identificationResults != null) {
            processResults(identificationResults, mIdentificationType);
        }
        else {
            Map<String, String> data = new ArrayMap<>();
            data.put(Identifier.Field.FILENAME.name(), track.getPath());
            data.put(Identifier.Field.TITLE.name(), track.getTitle());
            data.put(Identifier.Field.ARTIST.name(), track.getArtist());
            data.put(Identifier.Field.ALBUM.name(), track.getAlbum());

            // Check if API is available and tries to initialize it if does not.
            if (!mApiService.isApiInitialized()) {
                Intent intent = new Intent(mContext, ApiInitializerService.class);
                mContext.startService(intent);
                String msg = mContext.getString(R.string.initializing_recognition_api);
                mObservableMessage.setValue(msg);
            }
            // Sends a message to indicate API is initializing.
            else if (mApiService.isApiInitializing()) {
                String msg = mContext.getString(R.string.initializing_recognition_api);
                mObservableMessage.setValue(msg);
            }
            else {
                mIdentifier.registerCallback(new IdentificationListener<List<? extends Identifier.IdentificationResults>>() {
                    @Override
                    public void onIdentificationStart() {
                        mIdentifying = true;
                        mObservableLoadingMessage.setValue(new IdentificationEvent(mIdentifying,
                                mContext.getString(R.string.identifying), mContext.getString(R.string.cancel)));
                    }

                    @Override
                    public void onIdentificationFinished(List<? extends Identifier.IdentificationResults> result) {
                        mIdentifying = false;
                        List<Result> newList = prepareResults(result);
                        mResultsCache.add(track.getMediaStoreId()+"", newList);
                        processResults(result, mIdentificationType);
                        mObservableLoadingMessage.setValue(new IdentificationEvent(mIdentifying,
                                null, null));
                    }

                    @Override
                    public void onIdentificationError(Throwable e) {
                        mIdentifying = false;
                        mObservableLoadingMessage.setValue(new IdentificationEvent(mIdentifying,
                                null, null));
                        AutoMusicTagFixerException exception = (AutoMusicTagFixerException) e;
                        if (exception.getExceptionCode() == ErrorCode.RECOGNITION_ERROR)
                            mObservableMessage.setValue(mContext.getString(R.string.identification_error));
                        else
                            mObservableMessage.setValue(exception.getMessage());
                    }

                    @Override
                    public void onIdentificationCancelled() {
                        mIdentifying = false;
                        mObservableLoadingMessage.setValue(new IdentificationEvent(mIdentifying,
                                null, null));
                        String msg = mContext.getString(R.string.identification_cancelled);
                        mObservableMessage.setValue(msg);
                    }

                    @Override
                    public void onIdentificationNotFound() {
                        mIdentifying = false;
                        mObservableLoadingMessage.setValue(new IdentificationEvent(mIdentifying,
                                null, null));
                        String msg = mContext.getString(R.string.no_found_tags);
                        mObservableMessage.setValue(msg);
                    }
                });
                mIdentifier.identify(data);
            }
        }
    }

    /**
     * Creates a result for every cover art found, because sometimes a single result can have
     * various cover arts of different sizes, so it creates a copy of such information for covers of
     * different sizes.
     * @param results A list of results objects.
     * @return A list of result objects.
     */
    public static List<Result> prepareResults(List<? extends Identifier.IdentificationResults> results) {
        List<Result> resultList = new ArrayList<>();
        for (Identifier.IdentificationResults identificationResult : results) {
            Result result = (Result) identificationResult;
            List<CoverArt> coverArts = (List<CoverArt>) result.getField(Identifier.Field.COVER_ART.name());
            if (coverArts != null && coverArts.size() > 0) {
                for (CoverArt coverArt : coverArts) {
                    Result r = (Result) result.clone();
                    r.setId(UUID.randomUUID().toString());
                    r.getCoverArt().setSize(coverArt.getSize());
                    r.getCoverArt().setUrl(coverArt.getUrl());
                    r.getCoverArt().setDimension(coverArt.getDimension());
                    resultList.add(r);
                }
            }
            else {
                result.setId(UUID.randomUUID().toString());
                resultList.add(result);
            }

        }
        return resultList;
    }

    /**
     * Check what type of identification was performed to return the appropriate response.
     * @param result The result list to process.
     */
    private void processResults(List<? extends Identifier.IdentificationResults> result, int mIdentificationType) {
        boolean hasCovers = findCovers(result);

        if (mIdentificationType == IdentificationManager.ALL_TAGS ||
                (mIdentificationType == IdentificationManager.ONLY_COVER && hasCovers)) {

            mSuccessIdentification.setValue(mIdentificationType);
        }
        else {
            String msg = mContext.getString(R.string.no_found_tags);
            mObservableMessage.setValue(msg);
        }
    }

    @Override
    public void cancel() {
        if (mIdentifier != null)
            mIdentifier.cancel();
    }

    public IdentificationManager verifyConnection(boolean verifyConnection) {
        mVerifyConnection = verifyConnection;
        return this;
    }

    public IdentificationManager setIdentificationType(int identificationType) {
        mIdentificationType = identificationType;
        return this;
    }

    public static boolean findCovers(List<? extends Identifier.IdentificationResults> results) {
        for (Identifier.IdentificationResults r : results) {
            Result result = (Result) r;
            if (result.getCoverArt() != null)
                return true;
        }

        return false;
    }

    public static Result findBestResult(List<? extends Identifier.IdentificationResults> results, Track track, SharedPreferences sharedPreferences) {

        Result bestResult = null;
        if (results == null || results.isEmpty())
            return bestResult;

        if (results.size() == 1)
            return (Result) results.get(0);

        String dimension = sharedPreferences.getString("key_size_album_art", "kImageSize1080");

        CoverArt coverArt = null;
        if (dimension.equals(GnImageSize.kImageSizeThumbnail.name())) {
            coverArt = findSmallestSize(results);
        }
        else if (dimension.equals(GnImageSize.kImageSizeSmall.name())) {
            for (int i = results.size() - 1 ; i >= 0 ; i--) {
                Result result = (Result) results.get(i);
                if (result.getCoverArt() != null &&
                        (result.getCoverArt().getDimension().equals(dimension) ||
                                result.getCoverArt().getDimension().equals(GnImageSize.kImageSize110.name()) ||
                                result.getCoverArt().getDimension().equals(GnImageSize.kImageSize170.name()))) {
                    coverArt = result.getCoverArt();
                    break;
                }
            }
        }
        else if (dimension.equals(GnImageSize.kImageSizeMedium.name())) {
            for (int i = results.size() - 1 ; i >= 0 ; i--) {
                Result result = (Result) results.get(i);
                if (result.getCoverArt() != null &&
                        (result.getCoverArt().getDimension().equals(dimension) ||
                                result.getCoverArt().getDimension().equals(GnImageSize.kImageSize220.name()) ||
                                result.getCoverArt().getDimension().equals(GnImageSize.kImageSize300.name()) ||
                                result.getCoverArt().getDimension().equals(GnImageSize.kImageSize450.name()))) {
                    coverArt = result.getCoverArt();
                    break;
                }
            }
        }
        else if (dimension.equals(GnImageSize.kImageSize720.name())) {
            for (int i = results.size() - 1 ; i >= 0 ; i--) {
                Result result = (Result) results.get(i);
                if (result.getCoverArt() != null &&
                        (result.getCoverArt().getDimension().equals(dimension) ||
                                result.getCoverArt().getDimension().equals(GnImageSize.kImageSizeLarge.name()))) {
                    coverArt = result.getCoverArt();
                    break;
                }
            }
        }
        else if (dimension.equals(GnImageSize.kImageSize1080.name())) {
            for (int i = results.size() - 1 ; i >= 0 ; i--) {
                Result result = (Result) results.get(i);
                if (result.getCoverArt() != null &&
                        (result.getCoverArt().getDimension().equals(dimension) ||
                                result.getCoverArt().getDimension().equals(GnImageSize.kImageSizeXLarge.name()))) {
                    coverArt = result.getCoverArt();
                    break;
                }
            }
        }
        else if (dimension.equals(GnImageSize.kImageSizeXLarge.name())) {
            coverArt = findBiggestSize(results);
        }
        else {
            coverArt = null;
        }


        String title = track.getTitle() != null ? AndroidUtils.replaceChars(track.getTitle().toUpperCase().trim()) : "";
        String artist = track.getArtist() != null ? AndroidUtils.replaceChars(track.getArtist().toUpperCase().trim()) : "";

        for (Identifier.IdentificationResults r : results) {
            Result result = (Result) r;
            if (title.equals(result.getTitle().toUpperCase()) &&
                    artist.equals(result.getArtist().toUpperCase())) {

                bestResult = result;
                bestResult.setCoverArt(coverArt);
                return bestResult;
            }
        }

        for (Identifier.IdentificationResults r : results) {
            Result result = (Result) r;
            if (title.equals(result.getTitle().toUpperCase()) &&
                    track.getArtist().equals(result.getArtist().toUpperCase())) {
                bestResult = result;
                bestResult.setCoverArt(coverArt);
                return bestResult;
            }
        }

        for (Identifier.IdentificationResults r : results) {
            Result result = (Result) r;
            if (track.getTitle().equals(result.getTitle().toUpperCase())) {
                bestResult = result;
                bestResult.setCoverArt(coverArt);
                return bestResult;
            }
        }


        bestResult = (Result) results.get(0);
        bestResult.setCoverArt(coverArt);

        return bestResult;
    }

    @NonNull
    public static CoverArt findSmallestSize(List<? extends Identifier.IdentificationResults> results) {
        List<CoverArt> coverArts = getCovers(results);
        CoverArt pivot = coverArts.get(0);
        for (int c = 1 ; c < coverArts.size() ; c++) {
            pivot = compareAndSelectSmallest(pivot, coverArts.get(c));
        }
        return pivot;
    }

    private static CoverArt compareAndSelectSmallest(CoverArt currentSmallestSize, CoverArt nextCover) {
        int currentSmallestValueSize = GnUtils.getValue(currentSmallestSize.getDimension());
        int nextValue = GnUtils.getValue(nextCover.getDimension());
        CoverArt smallestCoverArt;
        if (currentSmallestValueSize <= nextValue) {
            smallestCoverArt = currentSmallestSize;
        }
        else {
            smallestCoverArt = nextCover;
        }
        return smallestCoverArt;
    }

    @NonNull
    public static CoverArt findBiggestSize(List<? extends Identifier.IdentificationResults> results) {
        List<CoverArt> coverArts = getCovers(results);
        CoverArt pivot = coverArts.get(0);
        for (int c = coverArts.size() - 1 ; c >= 0 ; c--) {
            pivot = compareAndSelectBiggest(pivot, coverArts.get(c));
        }
        return pivot;
    }

    private static CoverArt compareAndSelectBiggest(CoverArt currentBiggestSize, CoverArt nextCover) {
        int currentBiggestValueSize = GnUtils.getValue(currentBiggestSize.getDimension());
        int nextValue = GnUtils.getValue(nextCover.getDimension());
        CoverArt bigeestCoverArt;
        if (currentBiggestValueSize >= nextValue) {
            bigeestCoverArt = currentBiggestSize;
        }
        else {
            bigeestCoverArt = nextCover;
        }
        return bigeestCoverArt;
    }

    private static List<CoverArt> getCovers(List<? extends Identifier.IdentificationResults> results) {
        List<CoverArt> covers = new ArrayList<>();
        for (Identifier.IdentificationResults r : results) {
            Result result = (Result) r;
            covers.add(result.getCoverArt());
        }
        return covers;
    }

    public static class IdentificationEvent {
        private boolean identifying;
        private String message;
        private String titleAction;

        public IdentificationEvent(boolean identifying, String message, String titleAction) {
            this.identifying = identifying;
            this.message = message;
            this.titleAction = titleAction;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getTitleAction() {
            return titleAction;
        }

        public void setTitleAction(String titleAction) {
            this.titleAction = titleAction;
        }

        public boolean isIdentifying() {
            return identifying;
        }

        public void setIdentifying(boolean identifying) {
            this.identifying = identifying;
        }
    }
}

package mx.dev.franco.automusictagfixer.ui.trackdetail;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.ArrayMap;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.gracenote.gnsdk.GnException;

import org.jaudiotagger.tag.FieldKey;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.common.Action;
import mx.dev.franco.automusictagfixer.filemanager.FileManager;
import mx.dev.franco.automusictagfixer.filemanager.ImageFileSaver;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger.StringUtilities;
import mx.dev.franco.automusictagfixer.fixer.CorrectionParams;
import mx.dev.franco.automusictagfixer.identifier.GnUtils;
import mx.dev.franco.automusictagfixer.identifier.Identifier;
import mx.dev.franco.automusictagfixer.identifier.Result;
import mx.dev.franco.automusictagfixer.persistence.cache.IdentificationResultsCache;
import mx.dev.franco.automusictagfixer.persistence.mediastore.MediaStoreManager;
import mx.dev.franco.automusictagfixer.persistence.repository.TrackManager;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.ui.SingleLiveEvent;
import mx.dev.franco.automusictagfixer.utilities.ActionableMessage;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.Message;
import mx.dev.franco.automusictagfixer.utilities.Resource;

public class TrackDetailViewModel extends AndroidViewModel {

    private final MediatorLiveData<Track> mTrackLoader = new MediatorLiveData<>();
    //Two-way bind livedata objects.
    public MutableLiveData<String> title = new MutableLiveData<>();
    public MutableLiveData<String> artist = new MutableLiveData<>();
    public MutableLiveData<String> album = new MutableLiveData<>();
    public MutableLiveData<String> number = new MutableLiveData<>();
    public MutableLiveData<String> year = new MutableLiveData<>();
    public MutableLiveData<String> genre = new MutableLiveData<>();
    public MutableLiveData<byte[]> cover = new MutableLiveData<>();
    public MutableLiveData<String> imageSize = new MutableLiveData<>();
    public SingleLiveEvent<Boolean> isStoredInSD = new SingleLiveEvent<>();

    //One way bind livedata objects.
    public MutableLiveData<String> filesize = new MutableLiveData<>();
    public MutableLiveData<String> channels = new MutableLiveData<>();
    public MutableLiveData<String> type = new MutableLiveData<>();
    public MutableLiveData<String> resolution = new MutableLiveData<>();
    public MutableLiveData<String> frequency = new MutableLiveData<>();
    public MutableLiveData<String> bitrate = new MutableLiveData<>();
    public MutableLiveData<String> length = new MutableLiveData<>();
    public MutableLiveData<String> absolutePath = new MutableLiveData<>();
    public MutableLiveData<String> filename = new MutableLiveData<>();

    //MediatorLiveData to observe loading state of multiple sources.
    private MediatorLiveData<Boolean> mLoadingStateMerger = new MediatorLiveData<>();
    private MediatorLiveData<String> mMessageMerger = new MediatorLiveData<>();

    private SingleLiveEvent<ValidationWrapper> mInputsInvalidLiveData = new SingleLiveEvent<>();
    private SingleLiveEvent<Void> mLiveConfirmationDeleteCover = new SingleLiveEvent<>();

    private MediatorLiveData<AudioTagger.AudioFields> mResultReading = new MediatorLiveData<>();
    private MutableLiveData<Void> mLiveSuccessReading = new MutableLiveData<>();
    private MediatorLiveData<Map<FieldKey, Object>> mResultWriting = new MediatorLiveData<>();


    private TrackManager mTrackManager;
    private AudioTagger.AudioFields mAudioFields;

    private FileManager mFileManager;
    private MediaStoreManager mMediaStoreManager;
    private IdentificationResultsCache mResultsCache;

    private LiveData<ActionableMessage> mResultFileSaving;
    private MutableLiveData<Integer> mLiveInformativeMessage = new MutableLiveData<>();
    private int mInitialAction = Constants.CorrectionActions.VIEW_INFO;
    private Handler mHandler;

    @Inject
    public TrackDetailViewModel(@NonNull Application application,
                                @NonNull TrackManager trackManager,
                                @Nonnull FileManager fileManager,
                                @Nonnull MediaStoreManager mediaStoreManager,
                                @NonNull IdentificationResultsCache resultsCache) {
        super(application);
        mHandler = new Handler(Looper.getMainLooper());
        mResultsCache = resultsCache;
        mTrackManager = trackManager;
        mFileManager = fileManager;
        mMediaStoreManager = mediaStoreManager;

        //Merge state loading into one live data to observe.
        mLoadingStateMerger.addSource(mTrackManager.observeLoadingState(), aBoolean -> {
            mLoadingStateMerger.setValue(aBoolean);
        });
        mLoadingStateMerger.addSource(mFileManager.observeLoadingState(), aBoolean -> {
            mLoadingStateMerger.setValue(aBoolean);
        });
        mLoadingStateMerger.addSource(mMediaStoreManager.observeLoadingState(), aBoolean -> {
            mLoadingStateMerger.setValue(aBoolean);
        });

        //Merge messages into one live data to observe.
        //LiveData<Integer> mediaStoreManagerMessage = mediaStoreManager.observeMediaStoreResult();
        //LiveData<Integer> fileManagerMessage = fileManager.observeResultFileSaving();

        mMessageMerger.addSource(mTrackManager.observeMessage(), integer -> mLiveInformativeMessage.setValue(integer));

        mResultFileSaving = getCoverSavingResult();

        mResultReading.addSource(mTrackManager.observeReadingResult(), this::onReadingResult);
        mResultWriting.addSource(mTrackManager.observeWritingResult(), this::onWritingResult);

        mTrackLoader.addSource(mTrackManager.observeTrack(), track ->
                mTrackManager.readAudioFile(track));
    }

    /**
     * Load the  information for current track.
     * @param trackId The id of track to load.
     */
    public void loadInfoTrack(int trackId) {
        mLiveInformativeMessage.setValue(R.string.loading_data_track);
        mTrackManager.getDetails(trackId);
    }

    /**
     * Livedata to inform to UI the progress of a task.
     * @return A live data object holding a boolean value.
     */
    public LiveData<Boolean> observeLoadingState() {
        return mLoadingStateMerger;
    }
    /**
     * Livedata to inform if track could be loaded.
     * @return Livedata holding a {@link Message} object or null.
     */
    public LiveData<Void> observeReadingResult() {
        return mLiveSuccessReading;
    }

    public LiveData<Void> observeAudioData() {
        return Transformations.map(mResultReading, input -> null);
    }

    public LiveData<Void> observeWritingFinishedEvent() {
        return Transformations.map(mResultWriting, input -> null);
    }


    public LiveData<ValidationWrapper> observeInvalidInputsValidation() {
        return mInputsInvalidLiveData;
    }

    public LiveData<String> onMessage() {
        return mMessageMerger;
    }

    public LiveData<Track> observeTrackLoaded() {
        return mTrackLoader;
    }

    public LiveData<Void> observeConfirmationRemoveCover() {
        return mLiveConfirmationDeleteCover;
    }

    public LiveData<ActionableMessage> observeCoverSavingResult() {
        return mResultFileSaving;
    }

    public LiveData<Integer> observeLoadingMessage() {
        return mLiveInformativeMessage;
    }

    public LiveData<Boolean> observeIsStoredInSD() {
        return isStoredInSD;
    }

    private void onReadingResult(AudioTagger.AudioFields audioFields) {

        if(audioFields.getCode() != AudioTagger.SUCCESS) {
            mLiveInformativeMessage.setValue(R.string.could_not_read_file);
            isStoredInSD.setValue(false);
        }
        else {
            mAudioFields = audioFields;
            setEditableInfo(audioFields);
            setNoEditableInfo(audioFields);
            setFixedInfo(audioFields);
            mLiveSuccessReading.setValue(null);
            if (mInitialAction == Constants.CorrectionActions.SEMI_AUTOMATIC) {
                mInitialAction = -1;
                mTrackLoader.setValue(getCurrentTrack());
            }
            if (audioFields.isStoredInSD()) {
                isStoredInSD.setValue(true);
            }
            else {
                isStoredInSD.setValue(false);
            }
        }
    }

    private void onWritingResult(AudioTagger.AudioTaggerResult<Map<FieldKey, Object>> writingResult) {
        mResultWriting.setValue(writingResult.getData());
        mLiveInformativeMessage.setValue(R.string.changes_applied);
        boolean deleteCoverFromCache = (writingResult.getTaskExecuted() != AudioTagger.MODE_RENAME_FILE)
                || writingResult.getData().containsKey(FieldKey.COVER_ART);
        if (deleteCoverFromCache)
            mResultsCache.delete(getCurrentTrack().getMediaStoreId()+"");
    }

    private LiveData<ActionableMessage> getCoverSavingResult() {
        LiveData<Resource<String>> resultSaving = mFileManager.observeResultFileSaving();
        return Transformations.map(resultSaving, input -> {
            ActionableMessage actionableMessage = new ActionableMessage();
            if(input.status == Resource.Status.SUCCESS) {
                actionableMessage.setAction(Action.SEE_COVER_SAVED);
                String pathToFile = input.data;
                actionableMessage.setDetails(pathToFile);
                actionableMessage.setMessage(getApplication().getString(R.string.cover_saved));
                actionableMessage.setAction(Action.WATCH_IMAGE);
                mMediaStoreManager.addFileToMediaStore(input.data, null);

            }
            else {
                actionableMessage.setAction(Action.SEE_DETAILS_COVER_NOT_SAVED);
                actionableMessage.setIdResourceMessage(R.string.cover_not_saved);
            }

            return actionableMessage;
        });
    }

    /**
     * Change the cover without asking to user once a valid image is picked up.
     * @param imageWrapper
     */
    public void fastCoverChange(ImageWrapper imageWrapper) {
        if (imageWrapper.height <= ImageWrapper.MAX_HEIGHT && imageWrapper.width <= ImageWrapper.MAX_WIDTH) {
            CorrectionParams correctionParams = new CorrectionParams();
            correctionParams.setCorrectionMode(AudioTagger.MODE_ADD_COVER);
            correctionParams.setTarget(mTrackManager.getCurrentTrack().getPath());
            correctionParams.setRenameFile(false);
            Map<FieldKey, Object> tags = new ArrayMap<>();
            Thread thread = new Thread(() -> {
                mLoadingStateMerger.postValue(true);
                byte[] data = AndroidUtils.generateCover(imageWrapper.bitmap);
                tags.put(FieldKey.COVER_ART, data);
                mLoadingStateMerger.postValue(false);
                correctionParams.setTags(tags);
                if(imageWrapper.requestCode == TrackDetailFragment.INTENT_GET_AND_UPDATE_FROM_GALLERY) {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(() ->
                            mTrackManager.performCorrection(correctionParams));
                }
                else {
                    cover.postValue(data);
                }
            });
            thread.start();

        } else {
            mLiveInformativeMessage.setValue(R.string.image_too_big);
        }
    }

    /**
     * Remove the cover.
     */
    public void confirmRemoveCover() {
        if(mAudioFields.getCover() != null) {
            mLoadingStateMerger.setValue(true);
            mLiveInformativeMessage.setValue(R.string.removing_cover);
            CorrectionParams correctionParams = new CorrectionParams();
            correctionParams.setTarget(getCurrentTrack().getPath());
            correctionParams.setCorrectionMode(AudioTagger.MODE_REMOVE_COVER);
            mTrackManager.performCorrection(correctionParams);
        }
        else {
            mLiveInformativeMessage.setValue(R.string.does_not_exist_cover);
        }

    }

    /**
     * Executes the correction of current track.
     * @param correctionParams The params required to correct current track.
     */
    public void performCorrection(CorrectionParams correctionParams) {
        mLoadingStateMerger.setValue(true);
        mLiveInformativeMessage.setValue(R.string.applying_tags);
        correctionParams.setTarget(getCurrentTrack().getPath());

        if(correctionParams.getTagsSource() == Constants.MANUAL) {
            performManualCorrection(correctionParams);
        }
        else {
            int codeRequest = correctionParams.getCorrectionMode();

            switch (codeRequest) {
                case AudioTagger.MODE_ADD_COVER:
                    performAddCoverCorrection(correctionParams);
                    break;
                case AudioTagger.MODE_OVERWRITE_ALL_TAGS:
                case AudioTagger.MODE_WRITE_ONLY_MISSING:
                    performSemiautomaticCorrection(correctionParams);
                    break;
            }
        }
    }

    public void renameFile(CorrectionParams correctionParams) {
        correctionParams.setTarget(getCurrentTrack().getPath());
        mTrackManager.performCorrection(correctionParams);
    }

    public void saveAsImageFileFrom(String id) {
        Result result = findResult(id);
        String newFilename = AndroidUtils.generateNameWithDate(mAudioFields.getFileName());
        mLiveInformativeMessage.setValue(R.string.saving_cover);
        mFileManager.saveFile(result, newFilename);
    }

    public void extractCover() {
        byte[] cover = this.cover.getValue();
        if(cover != null) {
            String imageName = null;
            if(title.getValue() != null && !title.getValue().isEmpty()) {
                imageName = StringUtilities.sanitizeString(title.getValue()).trim().replace(" ","_");
            }
            else {
                imageName = ImageFileSaver.GENERIC_NAME;
            }
            mLiveInformativeMessage.setValue(R.string.saving_cover);
            mFileManager.saveFile(cover, AndroidUtils.generateNameWithDate(imageName));

        }
        else {
            mLiveInformativeMessage.setValue(R.string.does_not_exist_cover);
        }
    }

    public Track getCurrentTrack() {
        return mTrackManager.getCurrentTrack();
    }

    public void setInitialAction(int action) {
        mInitialAction = action;
    }

    public void restorePreviousValues() {
        setEditableInfo(mAudioFields);
    }

    public void removeCover() {
        if(this.cover.getValue() != null) {
            mLiveConfirmationDeleteCover.setValue(null);
        }
        else {
            mLiveInformativeMessage.setValue(R.string.does_not_exist_cover);
        }
    }

    private void performAddCoverCorrection(CorrectionParams correctionParam) {
        Result result = findResult(correctionParam.getCoverId());
        applyCoverFromRemote(result, correctionParam);
    }

    private void applyCoverFromRemote(Result result, CorrectionParams correctionParam) {
        Thread thread = new Thread(() -> {
            try {
                boolean hasInternet = AndroidUtils.isConnected(getApplication());

                if (!hasInternet) {
                    mLoadingStateMerger.postValue(false);
                    mLiveInformativeMessage.postValue(R.string.connect_to_internet);
                }
                else {
                    final byte[] coverData = GnUtils.fetchGnCover(result.getCoverArt().getUrl(),
                            getApplication());

                    if (coverData == null) {
                        mLoadingStateMerger.postValue(false);
                        mLiveInformativeMessage.postValue(R.string.error_fetching_image);
                    }
                    else {
                        mHandler.post(() -> {
                            Map<FieldKey, Object> tags = new ArrayMap<>();
                            tags.put(FieldKey.COVER_ART, coverData);
                            correctionParam.setTags(tags);
                            mTrackManager.performCorrection(correctionParam);
                        });
                    }
                }
            } catch (GnException e) {
                e.printStackTrace();
                mLoadingStateMerger.setValue(false);
                mLiveInformativeMessage.postValue(R.string.error_fetching_image);
            }
        });
        thread.start();
    }

    private void performSemiautomaticCorrection(CorrectionParams correctionParams) {
        mLoadingStateMerger.setValue(true);
        final Result result = findResult(correctionParams.getTrackId());
        String url = result.getCoverArt() != null ? result.getCoverArt().getUrl() : null;
        Thread thread = new Thread(() -> {
            byte[] data = null;
            if (url != null) {
                try {
                    data = GnUtils.fetchGnCover(url, getApplication());
                } catch (GnException e) {
                    e.printStackTrace();
                }
            }
            AndroidUtils.createInputParams(result.getTitle(),
                    result.getArtist(),
                    result.getAlbum(),
                    result.getGenre(),
                    result.getTrackNumber(),
                    result.getTrackYear(),
                    data,
                    correctionParams);

            mHandler.post(() -> mTrackManager.performCorrection(correctionParams));
        });
        thread.start();

    }

    private void performManualCorrection(CorrectionParams correctionParams){
        ValidationWrapper validationWrapper = inputsValid();

        if(validationWrapper == null) {
            String title = this.title.getValue();
            String artist = this.artist.getValue();
            String album = this.album.getValue();
            String trackYear = this.year.getValue();
            String trackNumber = this.number.getValue();
            String genre = this.genre.getValue();
            byte[] cover = this.cover.getValue();

            if(title != null && !title.isEmpty())
                title = AudioTagger.StringUtilities.trimString(title);
            if(artist != null && !artist.isEmpty())
                artist = AudioTagger.StringUtilities.trimString(artist);
            if(album != null && !album.isEmpty())
                album = AudioTagger.StringUtilities.trimString(album);
            if(trackYear != null && !trackYear.isEmpty())
                trackYear = AudioTagger.StringUtilities.trimString(trackYear);
            if(trackNumber != null && !trackNumber.isEmpty())
                trackNumber = AudioTagger.StringUtilities.trimString(trackNumber);
            if(genre != null && !genre.isEmpty())
                genre = AudioTagger.StringUtilities.trimString(genre);

            AndroidUtils.createInputParams(title,
                    artist,album, genre, trackNumber, trackYear, cover, correctionParams);
            correctionParams.setCorrectionMode(AudioTagger.MODE_OVERWRITE_ALL_TAGS);
            mTrackManager.performCorrection(correctionParams);
        }
        else {
            validationWrapper.setMessage(R.string.empty_tag);
            mInputsInvalidLiveData.setValue(validationWrapper);
        }
    }

    private void setNoEditableInfo(AudioTagger.AudioFields audioFields) {
        filesize.setValue(audioFields.getFileSize());
        absolutePath.setValue(audioFields.getPath().concat("/").concat(audioFields.getFileName()));
        String coverSize = audioFields.getImageSize() != null ?
                audioFields.getImageSize() + " " + getApplication().getString(R.string.pixels) :
                getApplication().getString(R.string.missing_cover);
        imageSize.setValue(coverSize);
        filename.setValue(audioFields.getFileName());
    }

    private void setFixedInfo(AudioTagger.AudioFields audioFields) {
        channels.setValue(audioFields.getChannels());
        type.setValue(audioFields.getMimeType());
        resolution.setValue(audioFields.getResolution());
        frequency.setValue(audioFields.getFrequency());
        bitrate.setValue(audioFields.getBitrate());
        length.setValue(audioFields.getDuration());
    }

    private void setEditableInfo(AudioTagger.AudioFields audioFields) {
        title.setValue(audioFields.getTitle());
        artist.setValue(audioFields.getArtist());
        album.setValue(audioFields.getAlbum());
        number.setValue(audioFields.getTrackNumber());
        year.setValue(audioFields.getTrackYear());
        genre.setValue(audioFields.getGenre());
        cover.setValue(audioFields.getCover());
    }

    private ValidationWrapper inputsValid(){
        String title = this.title.getValue();
        String artist = this.artist.getValue();
        String album = this.album.getValue();
        String trackYear = this.year.getValue();
        String trackNumber = this.number.getValue();
        String genre = this.genre.getValue();

        int field = -1;
        if(title != null ){
            field = R.id.track_name_details;
            if(AudioTagger.StringUtilities.isFieldEmpty(title)) {
                return new ValidationWrapper(field, R.string.empty_tag);
            }
            if(AndroidUtils.isTooLong(field, title)) {
                return new ValidationWrapper(field, R.string.tag_too_long);
            }
        }

        if(artist != null){
            field = R.id.artist_name_details;
            if(AudioTagger.StringUtilities.isFieldEmpty(artist)) {
                return new ValidationWrapper(field, R.string.empty_tag);
            }
            if(AndroidUtils.isTooLong(field, artist)) {
                return new ValidationWrapper(field, R.string.tag_too_long);
            }
        }

        if(album != null){
            field = R.id.album_name_details;
            if(AudioTagger.StringUtilities.isFieldEmpty(album)) {
                return new ValidationWrapper(field, R.string.empty_tag);
            }
            if(AndroidUtils.isTooLong(field, album)) {
                return new ValidationWrapper(field, R.string.tag_too_long);
            }
        }

        if(trackYear != null){
            field = R.id.track_year;
            if(AudioTagger.StringUtilities.isFieldEmpty(trackYear)) {
                return new ValidationWrapper(field, R.string.empty_tag);
            }
            if(AndroidUtils.isTooLong(field, trackYear)) {
                return new ValidationWrapper(field, R.string.tag_too_long);
            }
        }

        if(trackNumber != null){
            field = R.id.track_number;
            if(AudioTagger.StringUtilities.isFieldEmpty(trackNumber)) {
                return new ValidationWrapper(field, R.string.empty_tag);
            }
            if(AndroidUtils.isTooLong(field, trackNumber)) {
                return new ValidationWrapper(field, R.string.tag_too_long);
            }
        }

        if(genre != null){
            field = R.id.track_genre;
            if(AudioTagger.StringUtilities.isFieldEmpty(genre)) {
                return new ValidationWrapper(field, R.string.empty_tag);
            }
            if(AndroidUtils.isTooLong(field, genre)) {
                return new ValidationWrapper(field, R.string.tag_too_long);
            }
        }

        return null;
    }

    @Override
    protected void onCleared() {
        mFileManager.onCleared();
    }

    private Result findResult(String id) {
        List<? extends Identifier.IdentificationResults> results =
                mResultsCache.load(getCurrentTrack().getMediaStoreId()+"");

        for (Identifier.IdentificationResults identificationResult : results) {
            if (identificationResult.getId().equals(id))
                return (Result) identificationResult;
        }

        return null;
    }
}

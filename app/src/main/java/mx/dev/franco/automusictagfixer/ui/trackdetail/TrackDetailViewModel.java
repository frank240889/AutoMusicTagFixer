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

import org.jaudiotagger.tag.FieldKey;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.common.Action;
import mx.dev.franco.automusictagfixer.covermanager.CoverManager;
import mx.dev.franco.automusictagfixer.filemanager.FileManager;
import mx.dev.franco.automusictagfixer.filemanager.ImageFileSaver;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger.StringUtilities;
import mx.dev.franco.automusictagfixer.fixer.MetadataReaderResult;
import mx.dev.franco.automusictagfixer.fixer.MetadataWriterResult;
import mx.dev.franco.automusictagfixer.identifier.CoverIdentificationResult;
import mx.dev.franco.automusictagfixer.identifier.IdentificationManager;
import mx.dev.franco.automusictagfixer.identifier.IdentificationParams;
import mx.dev.franco.automusictagfixer.identifier.Identifier;
import mx.dev.franco.automusictagfixer.identifier.TrackIdentificationResult;
import mx.dev.franco.automusictagfixer.persistence.mediastore.MediaStoreManager;
import mx.dev.franco.automusictagfixer.persistence.mediastore.MediaStoreResult;
import mx.dev.franco.automusictagfixer.persistence.repository.DataTrackManager;
import mx.dev.franco.automusictagfixer.persistence.repository.TrackRepository;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.ui.SingleLiveEvent;
import mx.dev.franco.automusictagfixer.utilities.ActionableMessage;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.Message;
import mx.dev.franco.automusictagfixer.utilities.Resource;
import mx.dev.franco.automusictagfixer.utilities.SuccessIdentification;

public class TrackDetailViewModel extends AndroidViewModel {

    //Two-way bind livedata objects.
    public MutableLiveData<String> title;
    public MutableLiveData<String> artist;
    public MutableLiveData<String> album;
    public MutableLiveData<String> number;
    public MutableLiveData<String> year;
    public MutableLiveData<String> genre;
    public MutableLiveData<byte[]> cover;
    public MutableLiveData<String> imageSize;

    //One way bind livedata objects.
    public MutableLiveData<String> filesize;
    public MutableLiveData<String> channels;
    public MutableLiveData<String> type;
    public MutableLiveData<String> resolution;
    public MutableLiveData<String> frequency;
    public MutableLiveData<String> bitrate;
    public MutableLiveData<String> length;
    public MutableLiveData<String> absolutePath;
    public MutableLiveData<String> filename;

    //MediatorLiveData to observe loading state of multiple sources.
    private MediatorLiveData<Boolean> mStateMerger;

    private SingleLiveEvent<ValidationWrapper> mInputsInvalidLiveData = new SingleLiveEvent<>();
    private SingleLiveEvent<Message> mLiveMessage = new SingleLiveEvent<>();
    private SingleLiveEvent<ActionableMessage> mLiveActionableMessage = new SingleLiveEvent<>();
    private SingleLiveEvent<Void> mLiveConfirmationDeleteCover = new SingleLiveEvent<>();
    private SingleLiveEvent<Boolean> mCancellableTask = new SingleLiveEvent<>();

    private LiveData<Message> mResultReading;
    private LiveData<Message> mResultWriting;
    private LiveData<Message> mResultRenaming;
    private LiveData<SuccessIdentification> mResultsIdentificationLiveData;
    private SingleLiveEvent<SuccessIdentification> mCachedResultsIdentificationLiveData;
    private LiveData<Message> mFailIdentificationResults;

    private DataTrackManager mDataTrackManager;
    private IdentificationManager mIdentificationManager;

    private AudioTagger.AudioFields mAudioFields;

    private int mCorrectionMode = Constants.CorrectionActions.VIEW_INFO;
    private InputCorrectionParams mCorrectionParams;
    private IdentificationParams mIdentificationParams = new IdentificationParams();
    private FileManager mFileManager;
    private MediaStoreManager mMediaStoreManager;
    private TrackRepository mTrackRepository;
    private LiveData<ActionableMessage> mResultFileSaving;
    private LiveData<Track> mLiveDataTrack;
    private Track mTrack;
    private boolean mTriggered = false;
    private MutableLiveData<Integer> mLiveLoadingMessage;
    private LiveData<Resource<MetadataWriterResult>> mResultWriter;
    private LiveData<Resource<MetadataReaderResult>> mResultReader;

    @Inject
    public TrackDetailViewModel(@NonNull Application application,
                                @NonNull DataTrackManager dataTrackManager,
                                @NonNull IdentificationManager identificationManager,
                                @Nonnull FileManager fileManager,
                                @Nonnull MediaStoreManager mediaStoreManager,
                                @NonNull TrackRepository trackRepository) {
        super(application);

        mDataTrackManager = dataTrackManager;
        mIdentificationManager = identificationManager;
        mFileManager = fileManager;
        mMediaStoreManager = mediaStoreManager;
        mTrackRepository = trackRepository;

        mLiveLoadingMessage = new MutableLiveData<>();
        title = new MutableLiveData<>();
        artist = new MutableLiveData<>();
        album = new MutableLiveData<>();
        number = new MutableLiveData<>();
        year = new MutableLiveData<>();
        genre = new MutableLiveData<>();
        cover = new MutableLiveData<>();
        imageSize = new MutableLiveData<>();

        filename = new MutableLiveData<>();
        filesize = new MutableLiveData<>();
        channels = new MutableLiveData<>();
        type = new MutableLiveData<>();
        resolution = new MutableLiveData<>();
        frequency = new MutableLiveData<>();
        bitrate = new MutableLiveData<>();
        length = new MutableLiveData<>();
        absolutePath = new MutableLiveData<>();


        mCachedResultsIdentificationLiveData = new SingleLiveEvent<>();
        mStateMerger = new MediatorLiveData<>();

        //Merge state loading into one live data to observe.
        LiveData<Boolean> stateTrackDataRepository = mDataTrackManager.observeLoadingState();
        LiveData<Boolean> identificationManagerState = mIdentificationManager.observeLoadingState();
        LiveData<Boolean> fileSaverResultState = mFileManager.observeLoadingState();
        LiveData<Boolean> mediaStoreManagerState = mMediaStoreManager.observeLoadingState();

        mStateMerger.addSource(stateTrackDataRepository, aBoolean -> {
            mCancellableTask.setValue(false);
            mStateMerger.setValue(aBoolean);
        });

        mStateMerger.addSource(mediaStoreManagerState, aBoolean -> {
            mCancellableTask.setValue(false);
            mStateMerger.setValue(aBoolean);
        });

        mStateMerger.addSource(identificationManagerState, aBoolean -> {
            mCancellableTask.setValue(aBoolean);
            mStateMerger.setValue(aBoolean);
        });

        mStateMerger.addSource(fileSaverResultState, aBoolean -> {
            mCancellableTask.setValue(false);
            mStateMerger.setValue(aBoolean);
        });

        mResultWriter = mDataTrackManager.getResultWriter();
        mResultReader = mDataTrackManager.getResultReader();
        mResultReading = getResultReading();
        mResultWriting = getResultWriting();
        mLiveDataTrack = getDataTrack();
        mResultFileSaving = getCoverSavingResult();
        mResultRenaming = getRenamingResult();
        mResultsIdentificationLiveData = getSuccessIdentification();
        mFailIdentificationResults = getFailIdentification();
    }

    /**
     * Livedata to inform to UI the progress of a task.
     * @return A live data object holding a boolean value.
     */
    public LiveData<Boolean> observeLoadingState() {
        return mStateMerger;
    }

    public LiveData<Track> observeTrack() {
        return mLiveDataTrack;
    }

    /**
     * Livedata to inform if track could be loaded.
     * @return Livedata holding a {@link Message} object or null.
     */
    public LiveData<Message> observeReadingResult() {
        return mResultReading;
    }

    public LiveData<Message> observeWritingResult() {
        return mResultWriting;
    }

    public LiveData<Boolean> observeCancellableTaskFlag() {
        return mCancellableTask;
    }

    private LiveData<Message> getResultReading() {
        return Transformations.map(mResultReader, input -> {
            Message message = null;
            if(input.data.getFields().getCode() != AudioTagger.SUCCESS) {
                if(input.data.getFields().getError().getMessage() != null)
                    message = new Message(input.data.getFields().getError().getMessage());
                else
                    message = new Message(R.string.could_not_read_file);
            }
            else {
                mTrack = input.data.getTrack();
                mAudioFields = input.data.getFields();
                setEditableInfo(mAudioFields);
                setNoEditableInfo(mAudioFields);
                setFixedInfo(mAudioFields);
            }
            return message;
        });
    }

    public void shouldTriggerInitialIdentification() {
        if(mCorrectionMode == Constants.CorrectionActions.SEMI_AUTOMATIC && !mTriggered) {
            mTriggered = true;
            startIdentification(new IdentificationParams(IdentificationParams.ALL_TAGS));
        }
    }


    private LiveData<Message> getResultWriting(){
        //Todo: Add functionality to update correctly the data track when track is stored in SD.
        //Todo: Take in mind that updating data track of SD file will create a copy into the internal memory
        //Todo: and when correction is done, this copy is copied back into the memory SD, replacing
        //Todo: the original file, and in consequence, the MediaStoreId will not be the same.
        return Transformations.map(mResultWriter, input -> {
            Message message = null;
            if(input.data.getResultCorrection().getCode() == AudioTagger.SUCCESS) {
                CoverManager.removeCover(mTrack.getMediaStoreId()+"");
                //After applied tags check if file should be renamed.
                if(mCorrectionParams.renameFile()){
                    mDataTrackManager.updateTrack(input.data.getResultCorrection().getTagsUpdated());
                    mDataTrackManager.renameFile(mCorrectionParams);
                }
                else {
                    //Get updated tags to update the repository track item
                    if(input.data.getResultCorrection().getTagsUpdated() != null) {
                        mDataTrackManager.updateTrack(input.data.getResultCorrection().getTagsUpdated());
                        mMediaStoreManager.updateDataTrackOnMediaStore(input.data.getResultCorrection().getTagsUpdated(),
                                MediaStoreResult.UPDATE_TAGS, mTrack.getMediaStoreId());
                    }
                    else {
                        mDataTrackManager.readAudioFile(mTrack);
                        message = new Message(getApplication().getString(R.string.changes_applied));
                    }
                }
            }
            else {
                //Could not apply tags, send a message indicating that.
                message = getMessage(input.data);
            }
            return message;
        });
    }

    private LiveData<Track> getDataTrack() {
        LiveData<Track> trackLiveData = mDataTrackManager.observeTrack();
        return Transformations.map(trackLiveData, input -> {
            mTrack = input;
            mDataTrackManager.readAudioFile(input);
            return input;
        });
    }

    private Message getMessage(MetadataWriterResult data) {
        Message message;
        String err = "";
        if(data.getResultCorrection().getError() != null &&
                data.getResultCorrection().getError().getMessage() != null)
            err = data.getResultCorrection().getError().getMessage();
        String msg = getApplication().getString(R.string.message_could_not_apply_tags) + " " + err;

        if(data.getResultCorrection().getCode() == AudioTagger.COULD_NOT_GET_URI_SD_ROOT_TREE) {
            message = new ActionableMessage(Action.URI_ERROR, msg);
        }
        else {
            message = new Message(msg);
        }
        return message;
    }

    public LiveData<Message> observeRenamingResult() {
        return mResultRenaming;
    }

    private LiveData<Message> getRenamingResult() {
        LiveData<Resource<AudioTagger.ResultRename>> resultRename = mDataTrackManager.getResultRename();
        return Transformations.map(resultRename, input -> {
            Message message = null;
            if(input.status == Resource.Status.SUCCESS) {
                Map<FieldKey, Object> map = new ArrayMap<>();
                map.put(FieldKey.CUSTOM1, input.data.getNewAbsolutePath());
                mDataTrackManager.updateTrack(map);
                mMediaStoreManager.updateDataTrackOnMediaStore(map,
                        MediaStoreResult.UPDATE_RENAMED_FILE, mTrack.getMediaStoreId());
            }
            else {
                message = new Message(R.string.changes_partially_applied);
            }

            return message;
        });
    }

    /**
     * Livedata to observe for identification tasks.
     * @return a Livedata holding the result.
     */
    public LiveData<SuccessIdentification> observeSuccessIdentification() {
        return mResultsIdentificationLiveData;
    }

    private LiveData<SuccessIdentification> getSuccessIdentification() {
        LiveData<Identifier.IdentificationStatus> resultIdentification = mIdentificationManager.observeSuccessIdentification();
        return Transformations.map(resultIdentification, input ->
                new SuccessIdentification(mIdentificationParams.getIdentificationType(),
                        mTrack.getMediaStoreId() + ""));
    }

    /**
     * Livedata to observe for identification tasks.
     * @return a Livedata holding the result.
     */
    public LiveData<SuccessIdentification> observeCachedIdentification() {
        return mCachedResultsIdentificationLiveData;
    }

    /**
     * Livedata to observe for identification tasks.
     * @return a Livedata holding the result.
     */
    public LiveData<Message> observeFailIdentification() {
        return mFailIdentificationResults;
    }

    private LiveData<Message> getFailIdentification() {
        LiveData<Identifier.IdentificationStatus> resultIdentification = mIdentificationManager.observeFailIdentification();
        return Transformations.map(resultIdentification,
                input -> new ActionableMessage(Action.MANUAL_CORRECTION,input.getMessage()));
    }

    public LiveData<ActionableMessage> observeCoverSavingResult() {
        return mResultFileSaving;
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
                if (AudioTagger.StorageHelper.getInstance(getApplication()).isStoredInSD(input.data) ) {
                    mMediaStoreManager.addFileToMediaStore(input.data, (path, uri) -> {
                        mTrackRepository.delete(mTrack);
                        mMediaStoreManager.rescan();
                    });
                }
            }
            else {
                actionableMessage.setAction(Action.SEE_DETAILS_COVER_NOT_SAVED);
                actionableMessage.setIdResourceMessage(R.string.cover_not_saved);
            }

            return actionableMessage;
        });
    }

    public LiveData<Message> observeMessage(){
        return mLiveMessage;
    }

    public LiveData<ActionableMessage> observeActionableMessage() {
        return mLiveActionableMessage;
    }


    public LiveData<ValidationWrapper> observeInvalidInputsValidation() {
        return mInputsInvalidLiveData;
    }

    public LiveData<Void> observeConfirmationRemoveCover() {
        return mLiveConfirmationDeleteCover;
    }

    public void setInitialAction(int correctionMode) {
        mCorrectionMode = correctionMode;
    }

    /**
     * Load the  information for current track.
     * @param trackId The id of track to load.
     */
    public void loadInfoTrack(int trackId) {
        mLiveLoadingMessage.setValue(R.string.loading_data_track);
        mDataTrackManager.getDetails(trackId);
    }

    /**
     * Change the cover without asking to user once a valid image is picked up.
     * @param imageWrapper
     */
    public void fastCoverChange(ImageWrapper imageWrapper) {
        if (imageWrapper.height <= 1080 && imageWrapper.width <= 1080) {
            mCorrectionParams = new InputCorrectionParams();
            mCorrectionParams.setCorrectionMode(Constants.MANUAL);
            mCorrectionParams.setCodeRequest(AudioTagger.MODE_ADD_COVER);
            mCorrectionParams.setTargetFile(mTrack.getPath());
            mCorrectionParams.setRenameFile(false);
            Map<FieldKey, Object> tags = new ArrayMap<>();
            Thread thread = new Thread(() -> {
                mStateMerger.postValue(true);
                byte[] data = AndroidUtils.generateCover(imageWrapper.bitmap);
                tags.put(FieldKey.COVER_ART, data);
                mStateMerger.postValue(false);
                mCorrectionParams.setFields(tags);
                if(imageWrapper.requestCode == TrackDetailFragment.INTENT_GET_AND_UPDATE_FROM_GALLERY) {
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(() ->
                            mDataTrackManager.performCorrection(mCorrectionParams));
                }
                else {
                    cover.postValue(data);
                }
            });
            thread.start();

        } else {
            Message message = new Message(R.string.image_too_big);
            mLiveMessage.setValue(message);
        }
    }

    /**
     * Remove the cover.
     */
    public void confirmRemoveCover() {
        if(mAudioFields.getCover() != null) {
            mLiveLoadingMessage.setValue(R.string.removing_cover);
            mCorrectionParams = new CoverCorrectionParams();
            mCorrectionParams.setTargetFile(mTrack.getPath());
            mCorrectionParams.setCodeRequest(AudioTagger.MODE_REMOVE_COVER);
            mDataTrackManager.removeCover(mCorrectionParams);
        }
        else {
            Message message = new Message(R.string.does_not_exist_cover);
            mLiveMessage.setValue(message);
        }

    }

    /**
     * Cancel the identification in progress.
     */
    public void cancelIdentification() {
        mIdentificationManager.cancelIdentification();
    }

    /**
     * Executes the correction of current track.
     * @param correctionParams The params required to correct current track.
     */
    public void performCorrection(InputCorrectionParams correctionParams) {
        mLiveLoadingMessage.setValue(R.string.applying_tags);
        mCorrectionParams = correctionParams;
        mCorrectionParams.setTargetFile(mTrack.getPath());
        if(mCorrectionParams.getCorrectionMode() == Constants.MANUAL) {
            createAndValidateInputParams(mCorrectionParams);
        }
        else {
            int codeRequest = mCorrectionParams.getCorrectionMode();

            switch (codeRequest) {
                case AudioTagger.MODE_ADD_COVER:
                    processAddCover(mCorrectionParams);
                    break;
                case AudioTagger.MODE_OVERWRITE_ALL_TAGS:
                case AudioTagger.MODE_WRITE_ONLY_MISSING:
                    processApplyTags(mCorrectionParams);
                    if(!"-1".equals(mCorrectionParams.getCoverId()))
                        processAddCover(mCorrectionParams);
                    break;
            }
            mDataTrackManager.performCorrection(mCorrectionParams);
        }
    }

    private void processAddCover(InputCorrectionParams correctionParam) {
        String coverId = correctionParam.getCoverId();
        CoverIdentificationResult result = mIdentificationManager.getCoverResult(mTrack.getMediaStoreId()+"", coverId);
        AndroidUtils.createCoverInputParams(result, correctionParam);
    }

    private void processApplyTags(InputCorrectionParams correctionParams) {
        String trackId = correctionParams.getTrackId();
        TrackIdentificationResult result = mIdentificationManager.getTrackResult(mTrack.getMediaStoreId()+"", trackId);
        AndroidUtils.createInputParams(result, correctionParams);
    }

    private void createAndValidateInputParams(InputCorrectionParams correctionParams){
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
            correctionParams.setTargetFile(mTrack.getPath());
            correctionParams.setCodeRequest(AudioTagger.MODE_OVERWRITE_ALL_TAGS);
            mDataTrackManager.performCorrection(correctionParams);
        }
        else {
            validationWrapper.setMessage(new Message(getApplication().getString(R.string.empty_tag)));
            mInputsInvalidLiveData.setValue(validationWrapper);
        }
    }

    public void saveAsImageFileFrom(CoverCorrectionParams correctionParams) {
        Date date = new Date();
        DateFormat now = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String newFilename = mAudioFields.getFileName().trim().replaceAll(" ","_") + "_" +now.format(date);
        mFileManager.saveFile(correctionParams.getCoverId(), mTrack.getMediaStoreId()+"",  newFilename);
    }

    public void restorePreviousValues() {
        setEditableInfo(mAudioFields);
    }

    /**
     * Identifies the track.
     * @param identificationParams Input parameters that could be useful for this identification.
     */
    public void startIdentification(IdentificationParams identificationParams) {
        mIdentificationParams = identificationParams;
        mLiveLoadingMessage.setValue(R.string.identifying);
        if(mIdentificationParams.getIdentificationType() == IdentificationParams.ONLY_COVER) {
            if(mIdentificationManager.getCoverListResult(mTrack.getMediaStoreId()+"") != null &&
                    mIdentificationManager.getCoverListResult(mTrack.getMediaStoreId()+"").size() > 0) {
                mCachedResultsIdentificationLiveData.setValue(new SuccessIdentification(mIdentificationParams.getIdentificationType(),
                        mTrack.getMediaStoreId() + ""));
            }
            else {
                mIdentificationManager.startIdentification(mTrack);
            }
        }
        else {
            if(mIdentificationManager.getTrackListResult(mTrack.getMediaStoreId()+"") != null &&
                mIdentificationManager.getTrackListResult(mTrack.getMediaStoreId()+"").size() > 0) {
                mCachedResultsIdentificationLiveData.setValue(new SuccessIdentification(mIdentificationParams.getIdentificationType(),
                        mTrack.getMediaStoreId() + ""));
            } else {
                    mIdentificationManager.startIdentification(mTrack);
            }
        }
    }

    public void removeCover() {
        if(mAudioFields.getCover() != null) {
            mLiveLoadingMessage.setValue(R.string.removing_cover);
            mLiveConfirmationDeleteCover.setValue(null);
        }
        else {
            mLiveMessage.setValue(new Message(R.string.does_not_exist_cover));
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
                return new ValidationWrapper(field, new Message(R.string.empty_tag));
            }
            if(AndroidUtils.isTooLong(field, title)) {
                return new ValidationWrapper(field, new Message(R.string.tag_too_long));
            }
        }

        if(artist != null){
            field = R.id.artist_name_details;
            if(AudioTagger.StringUtilities.isFieldEmpty(artist)) {
                return new ValidationWrapper(field, new Message(R.string.empty_tag));
            }
            if(AndroidUtils.isTooLong(field, artist)) {
                return new ValidationWrapper(field, new Message(R.string.tag_too_long));
            }
        }

        if(album != null){
            field = R.id.album_name_details;
            if(AudioTagger.StringUtilities.isFieldEmpty(album)) {
                return new ValidationWrapper(field, new Message(R.string.empty_tag));
            }
            if(AndroidUtils.isTooLong(field, album)) {
                return new ValidationWrapper(field, new Message(R.string.tag_too_long));
            }
        }

        if(trackYear != null){
            field = R.id.track_year;
            if(AudioTagger.StringUtilities.isFieldEmpty(trackYear)) {
                return new ValidationWrapper(field, new Message(R.string.empty_tag));
            }
            if(AndroidUtils.isTooLong(field, trackYear)) {
                return new ValidationWrapper(field, new Message(R.string.tag_too_long));
            }
        }

        if(trackNumber != null){
            field = R.id.track_number;
            if(AudioTagger.StringUtilities.isFieldEmpty(trackNumber)) {
                return new ValidationWrapper(field, new Message(R.string.empty_tag));
            }
            if(AndroidUtils.isTooLong(field, trackNumber)) {
                return new ValidationWrapper(field, new Message(R.string.tag_too_long));
            }
        }

        if(genre != null){
            field = R.id.track_genre;
            if(AudioTagger.StringUtilities.isFieldEmpty(genre)) {
                return new ValidationWrapper(field, new Message(R.string.empty_tag));
            }
            if(AndroidUtils.isTooLong(field, genre)) {
                return new ValidationWrapper(field, new Message(R.string.tag_too_long));
            }
        }

        return null;
    }

    @Override
    protected void onCleared() {
        mIdentificationManager.cancelIdentification();
        mDataTrackManager.onCleared();
        mIdentificationManager.clearResults();
        mFileManager.onCleared();
        mMediaStoreManager.onCleared();
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
            mLiveLoadingMessage.setValue(R.string.saving_cover);
            mFileManager.saveFile(cover, imageName);

        }
        else {
            mLiveMessage.setValue(new Message(R.string.does_not_exist_cover));
        }
    }

    public void cancelTasks() {
        mIdentificationManager.cancelIdentification();
    }

    public LiveData<Integer> observeLoadingMessage() {
        return mLiveLoadingMessage;
    }
}

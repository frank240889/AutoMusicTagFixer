package mx.dev.franco.automusictagfixer.ui.trackdetail;

import android.app.Application;
import android.arch.core.util.Function;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.ArrayMap;

import org.jaudiotagger.tag.FieldKey;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;
import mx.dev.franco.automusictagfixer.fixer.MetadataReaderResult;
import mx.dev.franco.automusictagfixer.fixer.MetadataWriterResult;
import mx.dev.franco.automusictagfixer.identifier.IdentificationManager;
import mx.dev.franco.automusictagfixer.identifier.IdentificationParams;
import mx.dev.franco.automusictagfixer.identifier.Identifier;
import mx.dev.franco.automusictagfixer.identifier.Result;
import mx.dev.franco.automusictagfixer.persistence.repository.DataTrackRepository;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.ui.SingleLiveEvent;
import mx.dev.franco.automusictagfixer.utilities.ActionableMessage;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.IdentificationType;
import mx.dev.franco.automusictagfixer.utilities.Message;
import mx.dev.franco.automusictagfixer.utilities.Resource;

import static mx.dev.franco.automusictagfixer.common.Action.NONE;
import static mx.dev.franco.automusictagfixer.common.Action.SUCCESS_IDENTIFICATION;

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

    private LiveData<Message> mResultReading;
    private LiveData<ActionableMessage> mResultWriting;
    private LiveData<Message> mResultRenaming;
    private LiveData<IdentificationType> mResultsIdentificationLiveData;

    private DataTrackRepository mDataTrackRepository;
    private IdentificationManager mIdentificationManager;

    private AudioTagger.AudioFields mAudioFields;

    private int mCorrectionMode = Constants.CorrectionActions.VIEW_INFO;
    private InputCorrectionParams mCorrectionParams;
    private IdentificationParams mIdentificationParams;

    @Inject
    public TrackDetailViewModel(@NonNull Application application,
                                @NonNull DataTrackRepository dataTrackRepository,
                                @NonNull IdentificationManager identificationManager) {
        super(application);
        mDataTrackRepository = dataTrackRepository;
        mIdentificationManager = identificationManager;

        title = new MutableLiveData<>();
        artist = new MutableLiveData<>();
        album = new MutableLiveData<>();
        number = new MutableLiveData<>();
        year = new MutableLiveData<>();
        genre = new MutableLiveData<>();
        cover = new MutableLiveData<>();
        imageSize = new MediatorLiveData<>();

        filename = new MutableLiveData<>();
        filesize = new MutableLiveData<>();
        channels = new MutableLiveData<>();
        type = new MutableLiveData<>();
        resolution = new MutableLiveData<>();
        frequency = new MutableLiveData<>();
        bitrate = new MutableLiveData<>();
        length = new MutableLiveData<>();
        absolutePath = new MutableLiveData<>();

        mStateMerger = new MediatorLiveData<>();

        //Merge state loading into one live data to observe.
        LiveData<Boolean> stateTrackDataRepository = mDataTrackRepository.observeLoadingState();
        LiveData<Boolean> identificationRepositoryState = mIdentificationManager.observeLoadingState();
        mStateMerger.addSource(stateTrackDataRepository, aBoolean ->
                mStateMerger.setValue(aBoolean));

        mStateMerger.addSource(identificationRepositoryState, aBoolean ->
                mStateMerger.setValue(aBoolean));
    }

    /**
     * Livedata to inform to UI the progress of a task.
     * @return A live data object holding a boolean value.
     */
    public LiveData<Boolean> observeLoadingState() {
        return mStateMerger;
    }

    /**
     * Livedata to inform if track could be loaded.
     * @return Livedata holding a {@link Message} object or null.
     */
    public LiveData<Message> observeReadingResult() {
        LiveData<Resource<MetadataReaderResult>> resultReader = mDataTrackRepository.getResultReader();
        mResultReading = Transformations.map(resultReader, input -> {
            Message message = null;
            if(input.data.getFields().getCode() != AudioTagger.SUCCESS) {
                message = new Message(R.string.could_not_read_file);
                message.setDetails(input.data.getFields().getError().getMessage());
            }
            else {
                mAudioFields = input.data.getFields();
                setEditableInfo(mAudioFields);
                setNoEditableInfo(mAudioFields);
                setFixedInfo(mAudioFields);
            }
            return message;
        });
        return mResultReading;
    }

    public LiveData<ActionableMessage> observeWritingResult() {
        LiveData<Resource<MetadataWriterResult>> resultWriter = mDataTrackRepository.getResultWriter();
        mResultWriting = Transformations.map(resultWriter, new Function<Resource<MetadataWriterResult>, ActionableMessage>() {
            @Override
            public ActionableMessage apply(Resource<MetadataWriterResult> input) {
                ActionableMessage message = null;
                if(input.data.getResultCorrection().getCode() == AudioTagger.SUCCESS) {
                    if(mCorrectionParams.renameFile()){
                        mDataTrackRepository.renameFile(mCorrectionParams);
                    }
                    else {
                        message = new ActionableMessage(NONE, R.string.changes_applied);
                    }
                }
                else {
                    message = new ActionableMessage(NONE,R.string.could_not_correct_file,
                            input.data.getResultCorrection().getError().getMessage());
                }
                return message;
            }
        });
        return mResultWriting;
    }

    public LiveData<Message> observeRenamingResult() {
        LiveData<Resource<AudioTagger.ResultRename>> resultRename = mDataTrackRepository.getResultRename();
        mResultRenaming = Transformations.map(resultRename, input -> {
            Message message;
            if(input.status == Resource.Status.SUCCESS) {
                message = new Message(R.string.changes_applied);
            }
            else {
                message = new Message(R.string.changes_partially_applied);
            }
            return message;
        });
        return mResultRenaming;
    }

    /**
     * Livedata to observe for identification tasks.
     * @return a Livedata holding the result.
     */
    public LiveData<IdentificationType> observeResultIdentification() {
        LiveData<Resource<ActionableMessage>> resultIdentification = mIdentificationManager.observeActionableMessage();
        mResultsIdentificationLiveData = Transformations.map(resultIdentification, input -> {

            if(input.data.getAction() == SUCCESS_IDENTIFICATION) {
                IdentificationType identificationType = new IdentificationType();
                identificationType.setAction(SUCCESS_IDENTIFICATION);
                identificationType.setIdentificationType(mIdentificationParams.getIdentificationType());
                return identificationType;
            }
            else {
                if(input.status == Resource.Status.ERROR || input.status == Resource.Status.CANCELLED)  {
                    IdentificationType identificationType = new IdentificationType();
                    identificationType.setAction(NONE);
                    identificationType.setIdentificationType(mIdentificationParams.getIdentificationType());
                    mLiveActionableMessage.setValue(identificationType);
                }
            }

            return null;
        });
        return mResultsIdentificationLiveData;
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
        mDataTrackRepository.loadDataTrack(trackId);
    }

    /**
     * Change the cover without asking to user once a valid image is picked up.
     * @param imageWrapper
     */
    public void fastCoverChange(ImageWrapper imageWrapper) {
        if (imageWrapper.height <= 2000 || imageWrapper.width <= 2000) {
            InputCorrectionParams inputCorrectionParams = new InputCorrectionParams();
            inputCorrectionParams.setCorrectionMode(Constants.MANUAL);
            inputCorrectionParams.setCodeRequest(AudioTagger.MODE_ADD_COVER);
            Map<FieldKey, Object> tags = new ArrayMap<>();
            tags.put(FieldKey.COVER_ART, AndroidUtils.generateCover(imageWrapper.bitmap));
            mDataTrackRepository.performCorrection(inputCorrectionParams);

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
            InputCorrectionParams inputParams = new InputCorrectionParams();
            inputParams.setCodeRequest(AudioTagger.MODE_REMOVE_COVER);
            inputParams.setFields(new ArrayMap<>());
            mDataTrackRepository.performCorrection(inputParams);
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
        mCorrectionParams = correctionParams;
        if(mCorrectionParams.getCorrectionMode() == Constants.MANUAL) {
            createAndValidateInputParams(mCorrectionParams);
        }
        else {
            //Get results from cache and make the correction.
            List<Identifier.IdentificationResults> results = mIdentificationManager.getResult(getTrack().getMediaStoreId()+"");
            int id = Integer.parseInt(((SemiAutoCorrectionParams)correctionParams).getPosition());
            Result result = (Result) results.get(id);
            AndroidUtils.createInputParams(result, correctionParams);
            mDataTrackRepository.performCorrection(correctionParams);
        }
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
            correctionParams.setTargetFile(getTrack().getPath());
            correctionParams.setCodeRequest(AudioTagger.MODE_OVERWRITE_ALL_TAGS);
            mDataTrackRepository.performCorrection(correctionParams);
        }
        else {
            mInputsInvalidLiveData.setValue(validationWrapper);
        }
    }

    public void saveAsImageFileFrom(int cached) {

    }

    public void restorePreviousValues() {

    }

    /**
     * Identifies the track.
     * @param identificationParams Input parameters that could be useful for this identification.
     */
    public void startIdentification(IdentificationParams identificationParams) {
        mIdentificationParams = identificationParams;
        mIdentificationManager.startIdentification(mDataTrackRepository.getTrack());
    }

    public void openInExternalApp(Context applicationContext) {

    }

    public void removeCover() {
        if(mAudioFields.getCover() != null) {

        }
        else {

        }
    }

    public Track getTrack() {
        return mDataTrackRepository.getTrack();
    }

    private void setNoEditableInfo(AudioTagger.AudioFields audioFields) {
        filesize.setValue(audioFields.getFileSize());
        absolutePath.setValue(audioFields.getPath());
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
        if(title != null){
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
        mDataTrackRepository.onCleared();
    }

    public void saveChanges() {

    }
}

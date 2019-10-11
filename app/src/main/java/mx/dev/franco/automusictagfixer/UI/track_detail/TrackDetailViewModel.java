package mx.dev.franco.automusictagfixer.UI.track_detail;

import android.app.Application;
import android.arch.core.util.Function;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.content.Context;
import android.support.annotation.NonNull;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.UI.SingleLiveEvent;
import mx.dev.franco.automusictagfixer.fixer.AudioMetadataTagger;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;
import mx.dev.franco.automusictagfixer.fixer.MetadataReaderResult;
import mx.dev.franco.automusictagfixer.fixer.MetadataWriterResult;
import mx.dev.franco.automusictagfixer.identifier.IdentificationParams;
import mx.dev.franco.automusictagfixer.persistence.repository.DataTrackRepository;
import mx.dev.franco.automusictagfixer.persistence.repository.IdentificationManager;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.utilities.ActionableMessage;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.IdentificationType;
import mx.dev.franco.automusictagfixer.utilities.Message;
import mx.dev.franco.automusictagfixer.utilities.Resource;

import static mx.dev.franco.automusictagfixer.common.Action.NONE;

public class TrackDetailViewModel extends AndroidViewModel {

    //Two-way binded livedata objects.
    public MutableLiveData<String> title;
    public MutableLiveData<String> artist;
    public MutableLiveData<String> album;
    public MutableLiveData<String> number;
    public MutableLiveData<String> year;
    public MutableLiveData<String> genre;
    public MutableLiveData<byte[]> cover;

    //One way binded livedata objects.
    public MutableLiveData<String> filesize;
    public MutableLiveData<String> channels;
    public MutableLiveData<String> type;
    public MutableLiveData<String> resolution;
    public MutableLiveData<String> frequency;
    public MutableLiveData<String> bitrate;
    public MutableLiveData<String> length;
    public MutableLiveData<String> absolutePath;

    //MediatorLiveData to observe loading state of multiple sources.
    private MediatorLiveData<Boolean> mStateMerger;

    private SingleLiveEvent<ValidationWrapper> mInputsInvalidLiveData = new SingleLiveEvent<>();
    private SingleLiveEvent<Message> mLiveMessage = new SingleLiveEvent<>();
    private SingleLiveEvent<ActionableMessage> mLiveActionableMessage = new SingleLiveEvent<>();

    private LiveData<Message> mResultReading;
    private LiveData<ActionableMessage> mResultWriting;
    private LiveData<Message> mResultRenaming;
    private LiveData<IdentificationType> mResultsIdentificationLiveData;

    private DataTrackRepository mDataTrackRepository;
    private IdentificationManager mIdentificationManager;

    private AudioTagger.AudioFields mAudioFields;

    private int mCorrectionMode = Constants.CorrectionActions.VIEW_INFO;
    private ManualCorrectionParams mCorrectionParams;
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

    public LiveData<Boolean> observeLoadingState() {
        return mStateMerger;
    }

    public LiveData<Message> observeReadingResult() {
        LiveData<Resource<MetadataReaderResult>> resultReader = mDataTrackRepository.getResultReader();
        mResultReading = Transformations.map(resultReader, input -> {
            Message message = null;
            if(input.data.getFields().getCode() != AudioTagger.SUCCESS) {
                message = new Message(R.string.cannot_read_file);
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
            IdentificationType identificationType = new IdentificationType();
            identificationType.setIdentificationType(mIdentificationParams.getIdentificationType());
            return identificationType;
        })
        return mResultsIdentificationLiveData;
    }

    public LiveData<Message> observeMessage(){
        return mLiveMessage;
    }

    public LiveData<ActionableMessage> observeActionableMessage() {
        return mLiveActionableMessage;
    }


    public LiveData<ValidationWrapper> observeInputsValidation() {
        return mInputsInvalidLiveData;
    }

    public void setInitialAction(int correctionMode) {
        mCorrectionMode = correctionMode;
    }

    public void loadInfoTrack(int trackId) {
        mDataTrackRepository.loadDataTrack(trackId);
    }

    public void validateImageSize(ImageWrapper imageWrapper) {
        if (imageWrapper.height <= 2000 || imageWrapper.width <= 2000) {
            performCorrection();

        } else {
            Message message = new Message(R.string.image_too_big);
            mLiveMessage.setValue(message);
        }
    }

    public void confirmRemoveCover() {
        mDataTrackRepository.removeCover();
    }

    public void cancelIdentification() {
        mIdentificationManager.cancelIdentification();
    }

    public void performCorrection(AudioMetadataTagger.InputParams correctionParams) {
        mCorrectionParams = (ManualCorrectionParams) correctionParams;
        if(correctionParams == null) {
            performCorrection();
        }
        else {
            mDataTrackRepository.performCorrection(correctionParams);
        }
    }

    private void performCorrection(){
        boolean inputsValid = inputsValid();
        if(inputsValid) {
            String title = this.title.getValue();
            String artist = this.artist.getValue();
            String album = this.album.getValue();
            String trackYear = this.year.getValue();
            String trackNumber = this.number.getValue();
            String genre = this.genre.getValue();

            title = AudioTagger.StringUtilities.trimString(title);
            artist = AudioTagger.StringUtilities.trimString(artist);
            album = AudioTagger.StringUtilities.trimString(album);
            trackYear = AudioTagger.StringUtilities.trimString(trackYear);
            trackNumber = AudioTagger.StringUtilities.trimString(trackNumber);
            genre = AudioTagger.StringUtilities.trimString(genre);
            byte[] cover = this.cover.getValue();



            AudioMetadataTagger.InputParams inputParams = AndroidUtils.createInputParams(title,
                    artist,album, genre, trackNumber, trackYear, cover);
            inputParams.setTargetFile(getTrack().getPath());
            inputParams.setCodeRequest(AudioTagger.MODE_OVERWRITE_ALL_TAGS);
            mDataTrackRepository.performCorrection(inputParams);
        }

    }

    public void saveAsImageFileFrom(int cached) {

    }

    public void enableEditMode() {

    }

    public void restorePreviousValues() {

    }

    public void startIdentification(IdentificationParams identificationParams) {
        mIdentificationParams = identificationParams;
        mIdentificationManager.startIdentification(mDataTrackRepository.getTrack());
    }

    public void validateInputData() {

    }

    public void openInExternalApp(Context applicationContext) {

    }

    public void removeCover() {

    }

    public Track getTrack() {
        return mDataTrackRepository.getTrack();
    }

    private void setNoEditableInfo(AudioTagger.AudioFields audioFields) {
        filesize.setValue(audioFields.getFileSize());
        absolutePath.setValue(audioFields.getPath());
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

    private boolean inputsValid(){
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
                mInputsInvalidLiveData.setValue(new ValidationWrapper(field, new Message(R.string.empty_tag)));
                return false;
            }
            if(AndroidUtils.isTooLong(field, title)) {
                mInputsInvalidLiveData.setValue(new ValidationWrapper(field, new Message(R.string.tag_too_long)));
                return false;
            }
            title = AudioTagger.StringUtilities.trimString(title);
        }

        if(artist != null){
            field = R.id.artist_name_details;
            if(AudioTagger.StringUtilities.isFieldEmpty(artist)) {
                mInputsInvalidLiveData.setValue(new ValidationWrapper(field, new Message(R.string.empty_tag)));
                return false;
            }
            if(AndroidUtils.isTooLong(field, artist)) {
                mInputsInvalidLiveData.setValue(new ValidationWrapper(field, new Message(R.string.tag_too_long)));
                return false;
            }
            artist = AudioTagger.StringUtilities.trimString(artist);
        }

        if(album != null){
            field = R.id.album_name_details;
            if(AudioTagger.StringUtilities.isFieldEmpty(album)) {
                mInputsInvalidLiveData.setValue(new ValidationWrapper(field, new Message(R.string.empty_tag)));
                return false;
            }
            if(AndroidUtils.isTooLong(field, album)) {
                mInputsInvalidLiveData.setValue(new ValidationWrapper(field, new Message(R.string.tag_too_long)));
                return false;
            }
            album = AudioTagger.StringUtilities.trimString(album);
        }

        if(trackYear != null){
            field = R.id.track_year;
            if(AudioTagger.StringUtilities.isFieldEmpty(trackYear)) {
                mInputsInvalidLiveData.setValue(new ValidationWrapper(field, new Message(R.string.empty_tag)));
                return false;
            }
            if(AndroidUtils.isTooLong(field, trackYear)) {
                mInputsInvalidLiveData.setValue(new ValidationWrapper(field, new Message(R.string.tag_too_long)));
                return false;
            }
            trackYear = AudioTagger.StringUtilities.trimString(trackYear);
        }

        if(trackNumber != null){
            field = R.id.track_number;
            if(AudioTagger.StringUtilities.isFieldEmpty(trackNumber)) {
                mInputsInvalidLiveData.setValue(new ValidationWrapper(field, new Message(R.string.empty_tag)));
                return false;
            }
            if(AndroidUtils.isTooLong(field, trackNumber)) {
                mInputsInvalidLiveData.setValue(new ValidationWrapper(field, new Message(R.string.tag_too_long)));
                return false;
            }
            trackNumber = AudioTagger.StringUtilities.trimString(trackNumber);
        }

        if(genre != null){
            field = R.id.track_genre;
            if(AudioTagger.StringUtilities.isFieldEmpty(genre)) {
                mInputsInvalidLiveData.setValue(new ValidationWrapper(field, new Message(R.string.empty_tag)));
                return false;
            }
            if(AndroidUtils.isTooLong(field, genre)) {
                mInputsInvalidLiveData.setValue(new ValidationWrapper(field, new Message(R.string.tag_too_long)));
                return false;
            }
            genre = AudioTagger.StringUtilities.trimString(genre);
        }

        return true;
    }

    @Override
    protected void onCleared() {
        mDataTrackRepository.onCleared();
    }
}

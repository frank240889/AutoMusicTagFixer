package mx.dev.franco.automusictagfixer.UI.track_detail;

import android.app.Application;
import android.arch.core.util.Function;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.content.Context;
import android.support.annotation.NonNull;

import java.util.List;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.UI.SingleLiveEvent;
import mx.dev.franco.automusictagfixer.fixer.AudioMetadataTagger;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;
import mx.dev.franco.automusictagfixer.fixer.MetadataReaderResult;
import mx.dev.franco.automusictagfixer.identifier.Identifier;
import mx.dev.franco.automusictagfixer.identifier.Result;
import mx.dev.franco.automusictagfixer.interfaces.Cache;
import mx.dev.franco.automusictagfixer.persistence.cache.DownloadedTrackDataCacheImpl;
import mx.dev.franco.automusictagfixer.persistence.repository.DataTrackRepository;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.utilities.ActionableMessage;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.Message;
import mx.dev.franco.automusictagfixer.utilities.Resource;

public class TrackDetailViewModel extends AndroidViewModel {
    public MutableLiveData<String> title;
    public MutableLiveData<String> artist;
    public MutableLiveData<String> album;
    public MutableLiveData<String> number;
    public MutableLiveData<String> year;
    public MutableLiveData<String> genre;
    public MutableLiveData<byte[]> cover;


    public MutableLiveData<String> filesize;
    public MutableLiveData<String> channels;
    public MutableLiveData<String> type;
    public MutableLiveData<String> resolution;
    public MutableLiveData<String> frequency;
    public MutableLiveData<String> bitrate;
    public MutableLiveData<String> length;
    public MutableLiveData<String> absolutePath;

    private MutableLiveData<Boolean> mLoadingState;

    private SingleLiveEvent<Message> mLiveMessage = new SingleLiveEvent<>();
    private SingleLiveEvent<ActionableMessage> mLiveActionableMessage = new SingleLiveEvent<>();
    private LiveData<Message> mResultReading;
    private SingleLiveEvent<String> mResultsIdentificationLiveData = new SingleLiveEvent<>();
    private SingleLiveEvent<ValidationWrapper> mInputsInvalidLiveData = new SingleLiveEvent<>();

    private Identifier<Track, List<Identifier.IdentificationResults>> mIdentifier;
    private DataTrackRepository mDataTrackRepository;
    private int mTrackId;
    private Track mTrack;
    private AudioTagger.AudioFields mAudioFields;
    private Cache<String, List<Identifier.IdentificationResults>> mResultsCache;

    private int mCorrectionMode = Constants.CorrectionActions.VIEW_INFO;

    @Inject
    public TrackDetailViewModel(@NonNull Application application,
                                DataTrackRepository dataTrackRepository,
                                DownloadedTrackDataCacheImpl cache) {
        super(application);
        mResultsCache = cache;
        mDataTrackRepository = dataTrackRepository;

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

        mLoadingState = mDataTrackRepository.observeLoadingState();

    }

    public LiveData<Message> observeReadingResult() {
        LiveData<Resource<MetadataReaderResult>> resultReader = mDataTrackRepository.getResultReader();
        mResultReading = Transformations.map(resultReader, new Function<Resource<MetadataReaderResult>, Message>() {
            @Override
            public Message apply(Resource<MetadataReaderResult> input) {
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
            }
        });
        return mResultReading;
    }

    public LiveData<Message> observeMessage(){
        return mLiveMessage;
    }

    public LiveData<ActionableMessage> observeActionableMessage() {
        return mLiveActionableMessage;
    }

    public LiveData<String> observeResultIdentification() {
        return mResultsIdentificationLiveData;
    }

    public LiveData<ValidationWrapper> observeInputsValidation() {
        return mInputsInvalidLiveData;
    }


    public void setInitialAction(int correctionMode) {
        mCorrectionMode = correctionMode;
    }

    public void loadInfoTrack(int trackId) {
        mTrackId = trackId;
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
        mDataTrackRepository.removeCover(mTrackId);
    }

    public void cancelIdentification() {

    }

    void performCorrection(ImageWrapper imageWrapper) {

    }

    public void performCorrection(AudioMetadataTagger.InputParams correctionParams) {
        if(correctionParams == null) {
            performCorrection();
        }
        else {
            List<Identifier.IdentificationResults> results = mResultsCache.load(getTrack().getMediaStoreId()+"");
            int id = Integer.parseInt(((CorrectionParams)correctionParams).getId());
            Result result = (Result) results.get(id);
            AudioMetadataTagger.InputParams inputParams = AndroidUtils.createInputParams(result);
            mDataTrackRepository.fixTrack(inputParams);
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

            mDataTrackRepository.fixTrack(inputParams);
        }

    }

    public void saveAsImageFileFrom(int cached) {

    }

    public void enableEditMode() {

    }

    public void restorePreviousValues() {

    }

    public void startIdentification() {
        mIdentifier.registerCallback(new Identifier.IdentificationListener<List<Identifier.IdentificationResults>, Track>() {
            @Override
            public void onIdentificationStart(Track file) {
                mLoadingState.setValue(true);
            }

            @Override
            public void onIdentificationFinished(List<Identifier.IdentificationResults> result, Track file) {
                mLoadingState.setValue(false);
                mResultsCache.add(file.getMediaStoreId()+"", result);
            }

            @Override
            public void onIdentificationError(Track file, String error) {
                mLoadingState.setValue(false);
                mLiveMessage.setValue(new Message(error));
            }

            @Override
            public void onIdentificationCancelled(Track file) {
                mLoadingState.setValue(false);
                mLiveMessage.setValue(new Message(R.string.identification_cancelled));
            }

            @Override
            public void onIdentificationNotFound(Track file) {
                mLoadingState.setValue(false);
                mLiveMessage.setValue(new Message(R.string.no_found_tags));
            }
        });
        mIdentifier.identify(mTrack);
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
        mResultsCache.deleteAll();
        mResultsCache = null;
    }
}

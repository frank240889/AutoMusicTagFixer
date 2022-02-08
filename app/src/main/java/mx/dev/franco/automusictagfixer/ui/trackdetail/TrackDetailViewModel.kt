package mx.dev.franco.automusictagfixer.ui.trackdetail

import android.app.Application
import android.util.ArrayMap
import android.util.Log
import androidx.lifecycle.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.dev.franco.automusictagfixer.R
import mx.dev.franco.automusictagfixer.common.Action
import mx.dev.franco.automusictagfixer.covermanager.CoverLoader
import mx.dev.franco.automusictagfixer.filemanager.ImageFileSaver
import mx.dev.franco.automusictagfixer.fixer.AudioTagger
import mx.dev.franco.automusictagfixer.fixer.AudioTagger.*
import mx.dev.franco.automusictagfixer.fixer.CorrectionParams
import mx.dev.franco.automusictagfixer.persistence.mediastore.MediaStoreAccess
import mx.dev.franco.automusictagfixer.persistence.repository.TrackManager
import mx.dev.franco.automusictagfixer.persistence.room.Track
import mx.dev.franco.automusictagfixer.ui.SingleLiveEvent
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils
import mx.dev.franco.automusictagfixer.utilities.Constants
import mx.dev.franco.automusictagfixer.utilities.SnackbarMessage
import mx.dev.franco.automusictagfixer.utilities.resource_manager.ResourceManager
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.TagException
import java.io.IOException
import javax.inject.Inject
import kotlin.collections.set


class TrackDetailViewModel
@Inject
constructor(
    private val trackManager: TrackManager,
    private val mediaStoreAccess: MediaStoreAccess,
    private val audioTagger: AudioTagger,
    private val resourceManager: ResourceManager,
    application: Application
) : AndroidViewModel(application) {
    private val mTrackLoader = MediatorLiveData<Track>()

    //Two-way bind livedata objects.

    var title = MutableLiveData<String?>()

    var artist = MutableLiveData<String>()

    var album = MutableLiveData<String>()

    var number = MutableLiveData<String>()

    var year = MutableLiveData<String>()

    var genre = MutableLiveData<String>()
    val cover: MutableLiveData<ByteArray?> by lazy { MutableLiveData<ByteArray?>() }
    var imageSize = MutableLiveData<String>()
    var isStoredInSD = SingleLiveEvent<Boolean>()

    //One way bind livedata objects.

    var filesize = MutableLiveData<String>()

    var channels = MutableLiveData<String>()

    var type = MutableLiveData<String>()

    var resolution = MutableLiveData<String>()

    var frequency = MutableLiveData<String>()

    val bitrate = MutableLiveData<String>()

    val length = MutableLiveData<String>()

    val absolutePath = MutableLiveData<String>()
    val filename: MutableLiveData<String> by lazy { MutableLiveData<String>() }
    private val mLiveInformativeMessage: SingleLiveEvent<Int> = SingleLiveEvent()

    //MediatorLiveData to observe loading state of multiple sources.
    private val busy: MutableLiveData<Boolean> by lazy { MutableLiveData<Boolean>() }

    private val snackbarMessage: MutableLiveData<SnackbarMessage<String>> by lazy {
        MutableLiveData<SnackbarMessage<String>>()
    }

    private val mMessageMerger = MediatorLiveData<String>()
    private val mInputsInvalidLiveData = MutableLiveData<ValidationWrapper>()
    private val mLiveConfirmationDeleteCover = SingleLiveEvent<Void>()
    private val mResultReading = MediatorLiveData<AudioFields>()
    private val mLiveSuccessReading = MutableLiveData<Unit>()
    private val mResultWriting = MediatorLiveData<Map<FieldKey, Any>>()
    private var mAudioFields: AudioFields? = null
    private var mInitialAction = Constants.CorrectionActions.VIEW_INFO



    init {
        /*mResultWriting.addSource(this.trackManager.observeWritingResult()) { writingResult: AudioTaggerResult<Map<FieldKey, Any>> ->
            onWritingResult(
                writingResult
            )
        }*/
    }

    /**
     * Load the  information for current track.
     * @param trackId The id of track to load.
     */
    fun loadInfoTrack(trackId: Int) {
        busy.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val track = trackManager.getTrack(trackId)
             val trackData = try {
                audioTagger.readFile(track.path) as AudioFields
            } catch (e: ReadOnlyFileException) {
                e.printStackTrace()
                AudioFields(COULD_NOT_READ_TAGS, e)
            } catch (e: CannotReadException) {
                e.printStackTrace()
                AudioFields(COULD_NOT_READ_TAGS, e)
            } catch (e: TagException) {
                e.printStackTrace()
                AudioFields(COULD_NOT_READ_TAGS, e)
            } catch (e: InvalidAudioFrameException) {
                e.printStackTrace()
                AudioFields(COULD_NOT_READ_TAGS, e)
            } catch (e: IOException) {
                e.printStackTrace()
                AudioFields(COULD_NOT_READ_TAGS, e)
            }

            withContext(Dispatchers.Main) {
                onReadingResult(trackData)
                busy.value = false
            }
        }
    }

    /**
     * Livedata to inform to UI the progress of a task.
     * @return A live data object holding a boolean value.
     */
    fun observeLoadingState() = busy as LiveData<Boolean>

    fun observeReadingResult(): LiveData<Unit> {
        return mLiveSuccessReading
    }

    fun observeAudioData(): LiveData<Void> {
        return Transformations.map(mResultReading) { input: AudioFields? -> null }
    }

    fun observeWritingFinishedEvent(): LiveData<Void> {
        return Transformations.map(mResultWriting) { input: Map<FieldKey, Any>? -> null }
    }

    fun observeInvalidInputsValidation(): LiveData<ValidationWrapper> {
        return mInputsInvalidLiveData
    }

    fun onMessage(): LiveData<String> {
        return mMessageMerger
    }

    fun observeConfirmationRemoveCover(): LiveData<Void> {
        return mLiveConfirmationDeleteCover as LiveData<Void>
    }

    fun observeCoverSavingResult() = snackbarMessage as LiveData<SnackbarMessage<String>>


    fun observeLoadingMessage()= mLiveInformativeMessage as LiveData<Int>

    fun observeIsStoredInSD() = isStoredInSD as LiveData<Boolean>

    private fun onReadingResult(audioFields: AudioFields) {
        if (audioFields.code != AudioTagger.SUCCESS) {
            mLiveInformativeMessage.value = R.string.could_not_read_file
            isStoredInSD.setValue(false)
        } else {
            mAudioFields = audioFields
            setEditableInfo(audioFields)
            setNoEditableInfo(audioFields)
            setFixedInfo(audioFields)
            Log.e(javaClass.name, "mLiveSuccessReading.setValue")
            mLiveSuccessReading.value = Unit
            if (audioFields.isStoredInSD) {
                isStoredInSD.setValue(true)
            } else {
                isStoredInSD.setValue(false)
            }
        }
    }

    /*fun observeErrorWriting(): LiveData<SnackbarMessage<*>> {
        return Transformations.map(mTrackManager.observeErrorWriting()) { input: AudioTaggerResult<Map<FieldKey, Any>> ->
            getMessageErrorWriting(
                input
            )
        }
    }*/

    /*private fun getMessageErrorWriting(input: AudioTaggerResult<Map<FieldKey, Any>>): SnackbarMessage<*> {
        val builder: SnackbarMessage.Builder<*> = SnackbarMessage.Builder<Any?>(
            getApplication()
        )
        val errorCode = input.code
        if (errorCode == AudioTagger.COULD_NOT_GET_URI_SD_ROOT_TREE) {
            builder.action(Action.URI_ERROR).body(R.string.message_uri_tree_not_set)
                .mainActionText(R.string.details).dismissible(false)
        } else {
            val idStringResource: Int
            idStringResource = when (errorCode) {
                AudioTagger.COULD_NOT_REMOVE_COVER, AudioTagger.COULD_NOT_APPLY_COVER -> R.string.message_could_not_apply_cover
                AudioTagger.COULD_NOT_COPY_BACK_TO_ORIGINAL_LOCATION, AudioTagger.COULD_NOT_CREATE_AUDIOFILE, AudioTagger.COULD_NOT_CREATE_TEMP_FILE, AudioTagger.COULD_NOT_REMOVE_OLD_ID3_VERSION, AudioTagger.COULD_NOT_RENAME_FILE, AudioTagger.COULD_NOT_WRITE_TAGS, AudioTagger.COULD_NOT_APPLY_TAGS -> R.string.message_could_not_apply_tags
                else -> R.string.message_could_not_apply_tags
            }
            builder.action(Action.NONE)
            builder.body(idStringResource)
        }
        return builder.build()
    }*/

    private fun onWritingResult(writingResult: AudioTaggerResult<Map<FieldKey, Any>>) {
        mResultWriting.value = writingResult.data
        mLiveInformativeMessage.value = R.string.changes_applied
        val deleteCoverFromCache =
            writingResult.taskExecuted == AudioTagger.MODE_OVERWRITE_ALL_TAGS ||
                    writingResult.taskExecuted == MODE_ADD_COVER ||
                    writingResult.taskExecuted == MODE_REMOVE_COVER
        if (deleteCoverFromCache) CoverLoader.removeCover(currentTrack.mediaStoreId.toString() + "")
    }

    /**
     * Change the cover without asking to user once a valid image is picked up.
     * @param imageWrapper
     */
    fun fastCoverChange(imageWrapper: ImageWrapper) {
        if (imageWrapper.height <= ImageWrapper.MAX_HEIGHT && imageWrapper.width <= ImageWrapper.MAX_WIDTH) {
            val correctionParams = CorrectionParams()
            correctionParams.correctionMode = MODE_ADD_COVER
            correctionParams.target = trackManager.currentTrack!!.path
            correctionParams.setRenameFile(false)
            val tags: MutableMap<FieldKey, Any> = ArrayMap()
            viewModelScope.launch(Dispatchers.IO) {
                busy.postValue(true)
                val data = AndroidUtils.generateCover(imageWrapper.bitmap)
                tags[FieldKey.COVER_ART] = data
                busy.postValue(false)
                correctionParams.tags = tags
                if (imageWrapper.requestCode == TrackDetailFragment.INTENT_GET_AND_UPDATE_FROM_GALLERY) {
                    val audioTaggerResult = trackManager.performCorrection(correctionParams)
                } else {
                    cover.postValue(data)
                }
            }
        } else {
            mLiveInformativeMessage.setValue(R.string.image_too_big)
        }
    }

    /**
     * Remove the cover.
     */
    fun confirmRemoveCover() {
        if (mAudioFields!!.cover != null) {
            busy.value = true
            mLiveInformativeMessage.value = R.string.removing_cover
            val correctionParams = CorrectionParams()
            correctionParams.target = currentTrack.path
            correctionParams.correctionMode = MODE_REMOVE_COVER
            //trackManager.performCorrection(correctionParams)
        } else {
            mLiveInformativeMessage.setValue(R.string.does_not_exist_cover)
        }
    }

    /**
     * Executes the correction of current track.
     * @param correctionParams The params required to correct current track.
     */
    fun performCorrection(correctionParams: CorrectionParams) {
        busy.value = true
        correctionParams.target = currentTrack.path
        if (correctionParams.tagsSource == Constants.MANUAL) {
            performManualCorrection(correctionParams)
        } else {
            val codeRequest = correctionParams.correctionMode
            mLiveInformativeMessage.setValue(R.string.applying_tags)
            when (codeRequest) {
                MODE_ADD_COVER -> performAddCoverCorrection(correctionParams)
                AudioTagger.MODE_OVERWRITE_ALL_TAGS, AudioTagger.MODE_WRITE_ONLY_MISSING -> {
                }
            }
        }
    }

    fun renameFile(correctionParams: CorrectionParams) {
        correctionParams.target = currentTrack.path
        //trackManager.performCorrection(correctionParams)
    }

    fun saveAsImageFileFrom(id: String?) {
        val newFilename = AndroidUtils.generateNameWithDate(mAudioFields!!.fileName)
        mLiveInformativeMessage.setValue(R.string.saving_cover)
    }

    fun extractCover() {
        val cover = cover.value
        if (cover != null) {
            var imageName: String? = null
            imageName = if (title.value != null && !title.value!!.isEmpty()) {
                StringUtilities.sanitizeString(title.value).trim { it <= ' ' }.replace(" ", "_")
            } else {
                ImageFileSaver.GENERIC_NAME
            }
            mLiveInformativeMessage.value = R.string.saving_cover
            val builder: SnackbarMessage.Builder<String> =
                SnackbarMessage.Builder(getApplication())

            viewModelScope.launch(Dispatchers.IO) {
                val s = ImageFileSaver.saveImageFile(cover, AndroidUtils.generateNameWithDate(imageName))

                withContext(Dispatchers.Main) {

                    if(ImageFileSaver.INPUT_OUTPUT_ERROR == s || ImageFileSaver.NULL_DATA == s
                        || ImageFileSaver.NO_EXTERNAL_STORAGE_WRITABLE == s
                    ) {
                        builder.body(R.string.cover_not_saved);
                        mediaStoreAccess.addFileToMediaStore(s, null);
                    }
                    else {
                        builder
                            .action(Action.WATCH_IMAGE).mainActionText(R.string.see_image)
                            .duration(Snackbar.LENGTH_LONG)
                            .data(s)
                            .body(R.string.cover_saved)
                    }
                    snackbarMessage.value = builder.build()
                }
            }
        } else {
            mLiveInformativeMessage.setValue(R.string.does_not_exist_cover)
        }
    }

    val currentTrack: Track
        get() = trackManager.currentTrack!!

    fun setInitialAction(action: Int) {
        mInitialAction = action
    }

    fun restorePreviousValues() {
        setEditableInfo(mAudioFields)
    }

    fun removeCover() {
        if (cover.value != null) {
            mLiveConfirmationDeleteCover.setValue(null)
        } else {
            mLiveInformativeMessage.setValue(R.string.does_not_exist_cover)
        }
    }

    private fun performAddCoverCorrection(correctionParam: CorrectionParams) {
        //Result result = findResult(correctionParam.getCoverId());
        //applyCoverFromRemote(result, correctionParam);
    }

    /*private void applyCoverFromRemote(Result result, CorrectionParams correctionParam) {
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
    }*/
    /*private void performSemiautomaticCorrection(CorrectionParams correctionParams) {
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

    }*/
    private fun performManualCorrection(correctionParams: CorrectionParams) {
        val validationWrapper = inputsValid()
        if (validationWrapper == null) {
            var title = title.value
            var artist = artist.value
            var album = album.value
            var trackYear = year.value
            var trackNumber = number.value
            var genre = genre.value
            val cover = cover.value
            if (title != null && !title.isEmpty()) title = StringUtilities.trimString(title)
            if (artist != null && !artist.isEmpty()) artist = StringUtilities.trimString(artist)
            if (album != null && !album.isEmpty()) album = StringUtilities.trimString(album)
            if (trackYear != null && !trackYear.isEmpty()) trackYear =
                StringUtilities.trimString(trackYear)
            if (trackNumber != null && !trackNumber.isEmpty()) trackNumber =
                StringUtilities.trimString(trackNumber)
            if (genre != null && !genre.isEmpty()) genre = StringUtilities.trimString(genre)
            AndroidUtils.createInputParams(
                title,
                artist, album, genre, trackNumber, trackYear, cover, correctionParams
            )
            correctionParams.correctionMode = AudioTagger.MODE_OVERWRITE_ALL_TAGS
            mLiveInformativeMessage.setValue(R.string.applying_tags)
            //trackManager.performCorrection(correctionParams)
        } else {
            busy.setValue(false)
            validationWrapper.message = R.string.empty_tag
            mInputsInvalidLiveData.setValue(validationWrapper)
        }
    }

    private fun setNoEditableInfo(audioFields: AudioFields) {
        filesize.value = audioFields.fileSize
        absolutePath.value = audioFields.path + "/" + audioFields.fileName
        val coverSize = if (audioFields.imageSize != null)
            audioFields.imageSize + " " + resourceManager.getString(R.string.pixels)
        else
            resourceManager.getString(R.string.missing_cover)
        imageSize.value = coverSize
        filename.value = audioFields.fileName
    }

    private fun setFixedInfo(audioFields: AudioFields) {
        channels.value = audioFields.channels
        type.value = audioFields.mimeType
        resolution.value = audioFields.resolution
        frequency.value = audioFields.frequency
        bitrate.value = audioFields.bitrate
        length.value = audioFields.duration
    }

    private fun setEditableInfo(audioFields: AudioFields?) {
        title.value = audioFields!!.title
        artist.value = audioFields.artist
        album.value = audioFields.album
        number.value = audioFields.trackNumber
        year.value = audioFields.trackYear
        genre.value = audioFields.genre
        cover.value = audioFields.cover
    }

    private fun inputsValid(): ValidationWrapper? {
        val title = title.value
        val artist = artist.value
        val album = album.value
        val trackYear = year.value
        val trackNumber = number.value
        val genre = genre.value
        var field = -1
        if (title != null) {
            field = R.id.track_name_details
            if (StringUtilities.isFieldEmpty(title)) {
                return ValidationWrapper(field, R.string.empty_tag)
            }
            if (AndroidUtils.isTooLong(field, title)) {
                return ValidationWrapper(field, R.string.tag_too_long)
            }
        }
        if (artist != null) {
            field = R.id.artist_name_details
            if (StringUtilities.isFieldEmpty(artist)) {
                return ValidationWrapper(field, R.string.empty_tag)
            }
            if (AndroidUtils.isTooLong(field, artist)) {
                return ValidationWrapper(field, R.string.tag_too_long)
            }
        }
        if (album != null) {
            field = R.id.album_name_details
            if (StringUtilities.isFieldEmpty(album)) {
                return ValidationWrapper(field, R.string.empty_tag)
            }
            if (AndroidUtils.isTooLong(field, album)) {
                return ValidationWrapper(field, R.string.tag_too_long)
            }
        }
        if (trackYear != null) {
            field = R.id.track_year
            if (StringUtilities.isFieldEmpty(trackYear)) {
                return ValidationWrapper(field, R.string.empty_tag)
            }
            if (AndroidUtils.isTooLong(field, trackYear)) {
                return ValidationWrapper(field, R.string.tag_too_long)
            }
        }
        if (trackNumber != null) {
            field = R.id.track_number
            if (StringUtilities.isFieldEmpty(trackNumber)) {
                return ValidationWrapper(field, R.string.empty_tag)
            }
            if (AndroidUtils.isTooLong(field, trackNumber)) {
                return ValidationWrapper(field, R.string.tag_too_long)
            }
        }
        if (genre != null) {
            field = R.id.track_genre
            if (StringUtilities.isFieldEmpty(genre)) {
                return ValidationWrapper(field, R.string.empty_tag)
            }
            if (AndroidUtils.isTooLong(field, genre)) {
                return ValidationWrapper(field, R.string.tag_too_long)
            }
        }
        return null
    }
}
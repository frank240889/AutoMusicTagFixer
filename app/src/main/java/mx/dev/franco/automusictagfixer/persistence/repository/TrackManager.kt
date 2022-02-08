package mx.dev.franco.automusictagfixer.persistence.repository

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import mx.dev.franco.automusictagfixer.AutoMusicTagFixer.Companion.executorService
import mx.dev.franco.automusictagfixer.covermanager.CoverLoader
import mx.dev.franco.automusictagfixer.fixer.AudioTagger
import mx.dev.franco.automusictagfixer.fixer.AudioTagger.*
import mx.dev.franco.automusictagfixer.fixer.CorrectionParams
import mx.dev.franco.automusictagfixer.fixer.TrackReader
import mx.dev.franco.automusictagfixer.fixer.TrackWriter
import mx.dev.franco.automusictagfixer.persistence.mediastore.MediaStoreHelper
import mx.dev.franco.automusictagfixer.persistence.repository.AsyncOperation.TrackUpdater
import mx.dev.franco.automusictagfixer.persistence.room.Track
import mx.dev.franco.automusictagfixer.persistence.room.TrackDAO
import mx.dev.franco.automusictagfixer.persistence.room.TrackRoomDatabase
import mx.dev.franco.automusictagfixer.ui.SingleLiveEvent
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.TagException
import java.io.IOException
import javax.inject.Inject

class TrackManager
/**
 * Inject all dependencies into constructor.
 * @param audioTagger Interface to rename audio files and read/write their metadata.
 * @param trackRoomDatabase Database where is stored the base info of tracks.
 */
@Inject
constructor(//The database where is temporally stored the information about tracks.
    private val mTrackRoomDatabase: TrackRoomDatabase,
    private val mAudioTagger: AudioTagger,
    private val context: Context
) {
    //Live data objects that only dispatch a state change when its method "setVlue()" is called explicitly.
    private val mReaderResult = MutableLiveData<AudioFields>()
    private val mWriterResult = SingleLiveEvent<AudioTaggerResult<Map<FieldKey, Any>>>()
    private val mWriterError = SingleLiveEvent<AudioTaggerResult<Map<FieldKey, Any>>>()
    private val mLiveMessage = SingleLiveEvent<Int>()
    private val mMediatorLiveDataTrack = MediatorLiveData<Track?>()

    //Live data to inform the progress of task.
    private val mLoadingStateLiveData: SingleLiveEvent<Boolean> = SingleLiveEvent()

    //The cache where are stored temporally the identification results.
    private var mTrackReader: TrackReader? = null
    private var mTrackWriter: TrackWriter? = null

    suspend fun getTrack(id: Int) = mTrackRoomDatabase.trackDao().getTrack(id)


    fun observeTrack(): LiveData<Track?> {
        return mMediatorLiveDataTrack
    }

    fun observeLoadingState(): LiveData<Boolean> {
        return mLoadingStateLiveData as LiveData<Boolean>
    }

    fun observeReadingResult(): LiveData<AudioFields> {
        return mReaderResult
    }

    fun observeWritingResult(): LiveData<AudioTaggerResult<Map<FieldKey, Any>>> {
        return mWriterResult as LiveData<AudioTaggerResult<Map<FieldKey, Any>>>
    }

    fun observeErrorWriting(): LiveData<AudioTaggerResult<Map<FieldKey, Any>>> {
        return mWriterError as LiveData<AudioTaggerResult<Map<FieldKey, Any>>>
    }

    fun observeMessage(): LiveData<Int> {
        return mLiveMessage as LiveData<Int>
    }

    fun readAudioFile(track: Track) {
        /*mTrackReader = TrackReader(
            mAudioTagger,
            object : AsyncOperation<Void?, AudioTaggerResult<*>?, Void?, AudioTaggerResult<*>?> {
                override fun onAsyncOperationStarted(params: Void) {
                    mLoadingStateLiveData.value = true
                }

                override fun onAsyncOperationFinished(result: AudioTaggerResult<*>) {
                    mLoadingStateLiveData.value = false
                    mReaderResult.value = result as AudioFields
                }

                override fun onAsyncOperationError(error: AudioTaggerResult<*>) {
                    mLoadingStateLiveData.value = false
                    mReaderResult.value = error as AudioFields
                }
            })
        mTrackReader!!.executeOnExecutor(Executors.newSingleThreadExecutor(), track.path)*/
    }

    /**
     * Exposes a public method to make the correction.
     * @param correctionParams The params required by [AudioTagger]
     */
    suspend fun performCorrection(correctionParams: CorrectionParams): ResultCorrection? {
        correctionParams.mediaStoreId = currentTrack!!.mediaStoreId.toString()


        var resultCorrection: ResultCorrection? = null

        // Get the type of correction

        // Get the type of correction
        if (correctionParams.correctionMode == MODE_OVERWRITE_ALL_TAGS ||
            correctionParams.correctionMode == MODE_WRITE_ONLY_MISSING
        ) {

            // Apply the tags
            try {
                resultCorrection = mAudioTagger.saveTags(
                    correctionParams.target,
                    correctionParams.tags,
                    correctionParams.correctionMode
                ) as ResultCorrection
                // Check if writing is success and then check if file must be renamed.
                if (resultCorrection.code == SUCCESS) {
                    resultCorrection.taskExecuted = correctionParams.correctionMode
                    var filePath: String = correctionParams.target
                    if (correctionParams.renameFile()) {
                        val newName: String? = mAudioTagger.renameFile(
                            correctionParams.target,
                            correctionParams.newName
                        )
                        if (newName != null) {
                            resultCorrection.resultRename = newName
                            filePath = newName
                        } else {
                            resultCorrection.code = TAGS_APPLIED_BUT_NOT_RENAMED_FILE
                        }
                    }

                    // Check if is stored in SD and update the ID of media store wuth the id of the original file, because
                    // every time a file in SD is corrected, although is the same file, it will have a different ID, and the
                    // Android system will recognize as another file, meaning that when media store is scanned again,
                    // it will add the same file to the list, only with different ID; from the perspective of Android OS,
                    // is a different file because have different ID, but from the perspective of user, is the same file
                    if (resultCorrection!!.isStoredInSD) {
                        val mediaStoreId: String = correctionParams.getMediaStoreId()
                        val finalFilePath = filePath
                        MediaScannerConnection.scanFile(
                            context,
                            arrayOf(filePath),
                            arrayOf(MimeTypeMap.getFileExtensionFromUrl(filePath))
                        ) { path: String?, uri: Uri? ->
                            val newMediaStoreId =
                                MediaStoreHelper.getIdOfURI(context, finalFilePath)
                            MediaStoreHelper.swapMediaStoreId(
                                context,
                                mediaStoreId,
                                newMediaStoreId
                            )
                        }
                    }
                }
            } catch (e: IOException) {
                resultCorrection = ResultCorrection(COULD_NOT_APPLY_TAGS, null)
                resultCorrection.error = e
                return resultCorrection
            } catch (e: ReadOnlyFileException) {
                resultCorrection = ResultCorrection(COULD_NOT_APPLY_TAGS, null)
                resultCorrection.error = e
                return resultCorrection
            } catch (e: CannotReadException) {
                resultCorrection = ResultCorrection(COULD_NOT_APPLY_TAGS, null)
                resultCorrection.error = e
                return resultCorrection
            } catch (e: TagException) {
                resultCorrection = ResultCorrection(COULD_NOT_APPLY_TAGS, null)
                resultCorrection.error = e
                return resultCorrection
            } catch (e: InvalidAudioFrameException) {
                resultCorrection = ResultCorrection(COULD_NOT_APPLY_TAGS, null)
                resultCorrection.error = e
                return resultCorrection
            }
        } else if (correctionParams.correctionMode == MODE_RENAME_FILE) {
            val newName: String = mAudioTagger.renameFile(
                correctionParams.target,
                correctionParams.newName
            )
            resultCorrection = ResultCorrection()
            resultCorrection.code = SUCCESS
            resultCorrection.resultRename = newName
            resultCorrection.taskExecuted = correctionParams.getCorrectionMode()
        } else {
            val tags: Map<FieldKey, Any> = correctionParams.tags
            val cover = if (tags != null) tags[FieldKey.COVER_ART] as ByteArray? else null
            try {
                resultCorrection = mAudioTagger.applyCover(cover, correctionParams.target)
                resultCorrection.taskExecuted = correctionParams.correctionMode
                if (resultCorrection.code == SUCCESS) {
                    if (resultCorrection.isStoredInSD) {
                        val filePath: String = correctionParams.target
                        val mediaStoreId: String = correctionParams.mediaStoreId
                        MediaScannerConnection.scanFile(
                            context,
                            arrayOf(filePath),
                            arrayOf(MimeTypeMap.getFileExtensionFromUrl(filePath))
                        ) { path: String?, uri: Uri? ->
                            val newMediaStoreId =
                                MediaStoreHelper.getIdOfURI(context, filePath)
                            MediaStoreHelper.swapMediaStoreId(
                                context,
                                mediaStoreId,
                                newMediaStoreId
                            )
                        }
                    }
                }
            } catch (e: ReadOnlyFileException) {
                e.printStackTrace()
                resultCorrection = ResultCorrection(COULD_NOT_APPLY_TAGS, null)
                resultCorrection.error = e
            } catch (e: IOException) {
                e.printStackTrace()
                resultCorrection = ResultCorrection(COULD_NOT_APPLY_TAGS, null)
                resultCorrection.error = e
            } catch (e: TagException) {
                e.printStackTrace()
                resultCorrection = ResultCorrection(COULD_NOT_APPLY_TAGS, null)
                resultCorrection.error = e
            } catch (e: InvalidAudioFrameException) {
                e.printStackTrace()
                resultCorrection = ResultCorrection(COULD_NOT_APPLY_TAGS, null)
                resultCorrection.error = e
            } catch (e: CannotReadException) {
                e.printStackTrace()
                resultCorrection = ResultCorrection(COULD_NOT_APPLY_TAGS, null)
                resultCorrection.error = e
            }
        }
        return resultCorrection
    }

    val currentTrack: Track?
        get() = mMediatorLiveDataTrack.value

    fun updateTrack(
        result: AudioTaggerResult<Map<FieldKey, Any>>,
        track: Track?,
        trackDAO: TrackDAO?
    ) {
        val tags = result.data
        if (tags != null) {
            val title = tags[FieldKey.TITLE] as String?
            val artist = tags[FieldKey.ARTIST] as String?
            val album = tags[FieldKey.ALBUM] as String?
            if (title != null && !title.isEmpty()) {
                track!!.title = title
            }
            if (artist != null && !artist.isEmpty()) {
                track!!.artist = artist
            }
            if (album != null && !album.isEmpty()) {
                track!!.album = album
            }
        }
        val path = (result as ResultCorrection).resultRename
        if (path != null && path != "") {
            CoverLoader.removeCover(track!!.mediaStoreId.toString())
            track.path = path
        }
        track!!.setChecked(0)
        track.setProcessing(0)
        track.version = track.version + 1
        TrackUpdater(trackDAO).executeOnExecutor(executorService, track)
    }
}
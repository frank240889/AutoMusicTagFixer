package mx.dev.franco.automusictagfixer.fixer;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.AsyncTask;
import android.webkit.MimeTypeMap;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;

import java.io.IOException;
import java.util.Map;

import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.persistence.mediastore.MediaStoreHelper;

/**
 * @author Franco Castillo
 * @version 1.0
 * Instances of this class are in charge of writing the tags to the file, out of the main thread.
 */
public class TrackWriter extends AsyncTask<Context, Void, AudioTagger.AudioTaggerResult<Map<FieldKey, Object>>> {
    private AsyncOperation<Void, AudioTagger.AudioTaggerResult<Map<FieldKey, Object>>, Void,
            AudioTagger.AudioTaggerResult<Map<FieldKey, Object>>> mCallback;
    private AudioTagger mAudioTagger;
    private CorrectionParams mCorrectionParams;

    public TrackWriter(AsyncOperation<Void, AudioTagger.AudioTaggerResult<Map<FieldKey, Object>>, Void,
            AudioTagger.AudioTaggerResult<Map<FieldKey, Object>>> callback,
                       AudioTagger audioTagger,
                       CorrectionParams correctionParams) {

        mCallback = callback;
        mAudioTagger  = audioTagger;
        mCorrectionParams = correctionParams;

    }

    @Override
    protected void onPreExecute() {
        if (mCallback != null)
            mCallback.onAsyncOperationStarted(null);
    }

    @Override
    protected AudioTagger.AudioTaggerResult<Map<FieldKey, Object>> doInBackground(Context... contexts) {
        AudioTagger.ResultCorrection resultCorrection = null;

        // Get the type of correction
        if (mCorrectionParams.getCorrectionMode() == AudioTagger.MODE_OVERWRITE_ALL_TAGS ||
            mCorrectionParams.getCorrectionMode() == AudioTagger.MODE_WRITE_ONLY_MISSING) {

            // Apply the tags
            try {
                resultCorrection = (AudioTagger.ResultCorrection) mAudioTagger.saveTags(
                        mCorrectionParams.getTarget(),
                        mCorrectionParams.getTags(),
                        mCorrectionParams.getCorrectionMode());
                // Check if writing is success and then check if file must be renamed.
                if (resultCorrection.getCode() == AudioTagger.SUCCESS) {
                    resultCorrection.setTaskExecuted(mCorrectionParams.getCorrectionMode());


                    String filePath = mCorrectionParams.getTarget();

                    if (mCorrectionParams.renameFile()) {
                        String newName = mAudioTagger.renameFile(mCorrectionParams.getTarget(), mCorrectionParams.getNewName());
                        if (newName != null) {
                            resultCorrection.setResultRename(newName);
                            filePath = newName;
                        }
                        else {
                            resultCorrection.setCode(AudioTagger.TAGS_APPLIED_BUT_NOT_RENAMED_FILE);
                        }
                    }

                    // Check if is stored in SD and update the ID of media store wuth the id of the original file, because
                    // every time a file in SD is corrected, although is the same file, it will have a different ID, and the
                    // Android system will recognize as another file, meaning that when media store is scanned again,
                    // it will add the same file to the list, only with different ID; from the perspective of Android OS,
                    // is a different file because have different ID, but from the perspective of user, is the same file
                    if (resultCorrection.isStoredInSD()) {

                        String mediaStoreId = mCorrectionParams.getMediaStoreId();
                        String finalFilePath = filePath;
                        MediaScannerConnection.scanFile(
                                contexts[0],
                                new String[]{filePath},
                                new String[]{MimeTypeMap.getFileExtensionFromUrl(filePath)},
                                (path, uri) -> {
                                    String newMediaStoreId = MediaStoreHelper.getIdOfURI(contexts[0], finalFilePath);
                                    MediaStoreHelper.swapMediaStoreId(contexts[0], mediaStoreId, newMediaStoreId);
                                });
                    }
                }
            }
            catch (IOException | ReadOnlyFileException | CannotReadException | TagException | InvalidAudioFrameException e) {
                resultCorrection = new AudioTagger.ResultCorrection(AudioTagger.COULD_NOT_APPLY_TAGS, null);
                resultCorrection.setError(e);
                return resultCorrection;
            }

        }
        // Rename file.
        else if (mCorrectionParams.getCorrectionMode() == AudioTagger.MODE_RENAME_FILE) {
            String newName = mAudioTagger.renameFile(mCorrectionParams.getTarget(), mCorrectionParams.getNewName());
            resultCorrection = new AudioTagger.ResultCorrection();
            resultCorrection.setCode(AudioTagger.SUCCESS);
            resultCorrection.setResultRename(newName);
            resultCorrection.setTaskExecuted(mCorrectionParams.getCorrectionMode());
        }
        // Add or remove cover.
        else {
            Map<FieldKey, Object> tags = mCorrectionParams.getTags();

            byte[] cover = tags != null ? (byte[]) tags.get(FieldKey.COVER_ART) : null;

            try {
                resultCorrection = mAudioTagger.applyCover(cover, mCorrectionParams.getTarget());
                resultCorrection.setTaskExecuted(mCorrectionParams.getCorrectionMode());
                if (resultCorrection.getCode() == AudioTagger.SUCCESS) {
                    if (resultCorrection.isStoredInSD()) {
                        String filePath = mCorrectionParams.getTarget();
                        String mediaStoreId = mCorrectionParams.getMediaStoreId();
                        MediaScannerConnection.scanFile(
                                contexts[0],
                                new String[]{filePath},
                                new String[]{MimeTypeMap.getFileExtensionFromUrl(filePath)},
                                (path, uri) -> {
                                    String newMediaStoreId = MediaStoreHelper.getIdOfURI(contexts[0], filePath);
                                    MediaStoreHelper.swapMediaStoreId(contexts[0], mediaStoreId, newMediaStoreId);
                                });
                    }
                }
            } catch (ReadOnlyFileException | IOException | TagException | InvalidAudioFrameException | CannotReadException e) {
                e.printStackTrace();
                resultCorrection = new AudioTagger.ResultCorrection(AudioTagger.COULD_NOT_APPLY_TAGS, null);
                resultCorrection.setError(e);
            }
        }
        return resultCorrection;
    }

    @Override
    protected void onPostExecute(AudioTagger.AudioTaggerResult<Map<FieldKey, Object>> audioTaggerResult) {
        if(mCallback != null) {
            if(audioTaggerResult.getCode() != AudioTagger.SUCCESS) {
                mCallback.onAsyncOperationError(audioTaggerResult);
            }
            else {
                mCallback.onAsyncOperationFinished(audioTaggerResult);
            }
        }
        mCallback = null;
        mAudioTagger = null;
        mCorrectionParams = null;
    }
}

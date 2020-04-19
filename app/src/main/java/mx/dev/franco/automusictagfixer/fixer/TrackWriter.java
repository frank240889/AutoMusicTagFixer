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

        if (mCorrectionParams.getCorrectionMode() == AudioTagger.MODE_OVERWRITE_ALL_TAGS ||
            mCorrectionParams.getCorrectionMode() == AudioTagger.MODE_WRITE_ONLY_MISSING) {

            try {
                resultCorrection = (AudioTagger.ResultCorrection) mAudioTagger.saveTags(
                        mCorrectionParams.getTarget(),
                        mCorrectionParams.getTags(),
                        mCorrectionParams.getCorrectionMode());

                if (resultCorrection.getCode() == AudioTagger.SUCCESS) {
                    resultCorrection.setTaskExecuted(mCorrectionParams.getCorrectionMode());
                    if (resultCorrection.isStoredInSD()) {
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
                    else {
                        if (mCorrectionParams.renameFile()) {
                            String newName = mAudioTagger.renameFile(mCorrectionParams.getTarget(), mCorrectionParams.getNewName());
                            resultCorrection.setResultRename(newName);
                        }
                    }
                }
            }
            catch (IOException | ReadOnlyFileException | CannotReadException | TagException | InvalidAudioFrameException e) {
                resultCorrection = new AudioTagger.ResultCorrection(AudioTagger.COULD_NOT_APPLY_TAGS, null);
                resultCorrection.setError(e);
                return resultCorrection;
            }

        }
        else if (mCorrectionParams.getCorrectionMode() == AudioTagger.MODE_RENAME_FILE) {
            String newName = mAudioTagger.renameFile(mCorrectionParams.getTarget(), mCorrectionParams.getNewName());
            resultCorrection = new AudioTagger.ResultCorrection();
            resultCorrection.setCode(AudioTagger.SUCCESS);
            resultCorrection.setResultRename(newName);
            resultCorrection.setTaskExecuted(mCorrectionParams.getCorrectionMode());
        }
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

package mx.dev.franco.automusictagfixer.fixer;

import android.content.Context;

import com.gracenote.gnsdk.GnAssetFetch;
import com.gracenote.gnsdk.GnException;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;

import java.io.IOException;

import mx.dev.franco.automusictagfixer.identifier.GnApiService;
import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;

public class MetadataWriter extends AbstractMetadataFixer<Context, Void, AudioTagger.ResultCorrection> {
    private AsyncOperation<Track, MetadataWriterResult, Track, MetadataWriterResult> mCallback;
    private AudioMetadataTagger.InputParams mInputParams;
    public MetadataWriter(AsyncOperation<Track, MetadataWriterResult, Track, MetadataWriterResult> callback,
                          AudioMetadataTagger fileTagger,
                          AudioMetadataTagger.InputParams inputParams, Track track) {
        super(fileTagger, track);
        mCallback = callback;
        mInputParams = inputParams;
    }

    @Override
    protected void onPreExecute() {
        if(mCallback != null)
            mCallback.onAsyncOperationStarted(track);
    }

    @Override
    protected AudioTagger.ResultCorrection doInBackground(Context... contexts) {
        AudioTagger.ResultCorrection resultCorrection = null;
        if(mInputParams.getCodeRequest() == AudioTagger.MODE_REMOVE_COVER) {
            try {
                resultCorrection = mFileTagger.writeMetadata(mInputParams);
                String description = AndroidUtils.AudioTaggerErrorDescription.getErrorMessage(contexts[0], resultCorrection.getCode());
                resultCorrection.setError(new Exception(description));
            } catch (IOException | ReadOnlyFileException | CannotReadException | TagException | InvalidAudioFrameException e) {
                resultCorrection = new AudioTagger.ResultCorrection(AudioTagger.COULD_NOT_APPLY_TAGS, null);
                resultCorrection.setError(e);
                return resultCorrection;
            }
        }
        else {
            Object coverData = mInputParams.getFields().get(FieldKey.COVER_ART);
            //Check if exist cover as URL and fetch its data, then replace it in the same key.
            if(coverData instanceof String) {
                String url = (String) coverData;
                if(!url.isEmpty()) {
                    try {
                        byte[] cover = new GnAssetFetch(GnApiService.getInstance(contexts[0]).getGnUser(), url).data();
                        mInputParams.getFields().put(FieldKey.COVER_ART, cover);
                        resultCorrection = mFileTagger.writeMetadata(mInputParams);
                    } catch (IOException |
                            ReadOnlyFileException |
                            CannotReadException |
                            TagException |
                            InvalidAudioFrameException |
                            GnException e) {
                        e.printStackTrace();
                        resultCorrection.setError(e);
                    }
                }
            }
            else {
                try {
                    resultCorrection = mFileTagger.writeMetadata(mInputParams);
                    String description = AndroidUtils.AudioTaggerErrorDescription.getErrorMessage(contexts[0], resultCorrection.getCode());
                    resultCorrection.setError(new Exception(description));
                } catch (IOException |
                        ReadOnlyFileException |
                        CannotReadException |
                        TagException |
                        InvalidAudioFrameException e) {
                    e.printStackTrace();
                    resultCorrection = new AudioTagger.ResultCorrection(AudioTagger.COULD_NOT_APPLY_TAGS, null);
                    resultCorrection.setError(e);
                }
            }
        }



        return resultCorrection;
    }

    @Override
    protected void onPostExecute(AudioTagger.ResultCorrection resultCorrection) {
        if(mCallback != null) {
            MetadataWriterResult result = new MetadataWriterResult(track, resultCorrection);
            if(resultCorrection.getCode() != AudioTagger.SUCCESS) {
                mCallback.onAsyncOperationError(result);
            }
            else {
                mCallback.onAsyncOperationFinished(result);
            }
        }
    }

    @Override
    protected void onCancelled(AudioTagger.ResultCorrection resultCorrection) {
        onCancelled();
        if(mCallback != null)
            mCallback.onAsyncOperationCancelled(track);
    }
}

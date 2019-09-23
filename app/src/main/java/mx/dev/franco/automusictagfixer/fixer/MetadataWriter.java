package mx.dev.franco.automusictagfixer.fixer;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;

import java.io.IOException;

import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.persistence.room.Track;

public class MetadataWriter extends AbstractMetadataFixer<Void, Void, AudioTagger.ResultCorrection> {
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
    protected AudioTagger.ResultCorrection doInBackground(Void... voids) {
        try {
            return mFileTagger.writeMetadata(mInputParams);
        } catch (IOException|
                ReadOnlyFileException|
                CannotReadException|
                TagException|
                InvalidAudioFrameException e) {
            e.printStackTrace();
        }

        AudioTagger.ResultCorrection resultCorrection = new AudioTagger.ResultCorrection();

        resultCorrection.setCode(AudioTagger.COULD_NOT_WRITE_TAGS);

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

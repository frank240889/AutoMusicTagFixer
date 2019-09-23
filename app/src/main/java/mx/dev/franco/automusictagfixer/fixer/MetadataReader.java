package mx.dev.franco.automusictagfixer.fixer;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;

import java.io.IOException;

import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.persistence.room.Track;

public class MetadataReader extends AbstractMetadataFixer<Void, Void, AudioTagger.AudioFields> {
    private Throwable mError;
    private AsyncOperation<Track, MetadataReaderResult, Track, MetadataReaderResult> mCallback;
    private AudioMetadataTagger.InputParams mInputParams;
    public MetadataReader(AsyncOperation<Track, MetadataReaderResult, Track, MetadataReaderResult> callback, AudioMetadataTagger fileTagger,
                          AudioMetadataTagger.InputParams inputParams, Track track) {
        super(fileTagger,track);
        mCallback = callback;
        mInputParams = inputParams;
    }

    @Override
    protected void onPreExecute() {
        if(mCallback != null)
            mCallback.onAsyncOperationStarted(track);
    }

    @Override
    protected AudioTagger.AudioFields doInBackground(Void... voids) {
        try {
            return mFileTagger.readMetadata(track.getPath());
        } catch (IOException|
                ReadOnlyFileException|
                CannotReadException|
                TagException|
                InvalidAudioFrameException e) {
            e.printStackTrace();
            return new AudioTagger.AudioFields(AudioTagger.COULD_NOT_READ_TAGS);
        }
    }

    @Override
    protected void onPostExecute(AudioTagger.AudioFields audioFields) {
        if(mCallback != null) {
            MetadataReaderResult readerResult = new MetadataReaderResult(track, audioFields);
            if(audioFields.getCode() != AudioTagger.SUCCESS) {
                mCallback.onAsyncOperationError(readerResult);
            }
            else {
                mCallback.onAsyncOperationFinished(readerResult);
            }
        }
    }

    @Override
    protected void onCancelled(AudioTagger.AudioFields audioFields) {
        onCancelled();
        if(mCallback != null)
            mCallback.onAsyncOperationCancelled(track);
    }
}

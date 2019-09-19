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
    private AsyncOperation<Track, AudioTagger.AudioFields, Track, MetadataWriter.Error> mCallback;
    private AudioMetadataTagger.InputParams mInputParams;
    public MetadataReader(AsyncOperation<Track, AudioTagger.AudioFields, Track, MetadataWriter.Error> callback, AudioMetadataTagger fileTagger,
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

            mError = e;
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(AudioTagger.AudioFields audioFields) {
        if(mCallback != null) {
            if(audioFields == null && mError != null) {
                MetadataWriter.Error error = new MetadataWriter.Error(track, mError.toString(), -1);
                mCallback.onAsyncOperationError(error);
            }
            else {
                mCallback.onAsyncOperationFinished(audioFields);
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

package mx.dev.franco.automusictagfixer.fixer;

import android.support.annotation.NonNull;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;

import java.io.IOException;

import javax.annotation.Nullable;

import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.persistence.room.Track;

public class MetadataReader extends AbstractMetadataFixer<Void, Void, AudioTagger.AudioFields> {
    private AsyncOperation<Track, MetadataReaderResult, Track, MetadataReaderResult> mCallback;
    public MetadataReader(@Nullable AsyncOperation<Track, MetadataReaderResult, Track, MetadataReaderResult> callback,
                          @NonNull AudioMetadataTagger fileTagger,
                          @NonNull Track track) {
        super(fileTagger,track);
        mCallback = callback;
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
            AudioTagger.AudioFields result =
                    new AudioTagger.AudioFields(AudioTagger.COULD_NOT_READ_TAGS);
            result.setError(e);
            return result;
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

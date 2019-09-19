package mx.dev.franco.automusictagfixer.fixer;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;

import java.io.IOException;

import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.persistence.room.Track;

public class MetadataWriter extends AbstractMetadataFixer<Void, Void, AudioTagger.ResultCorrection> {
    private AsyncOperation<Track, MetadataFixerResult, Track, Error> mCallback;
    private AudioMetadataTagger.InputParams mInputParams;
    public MetadataWriter(AsyncOperation<Track, MetadataFixerResult, Track, Error> callback, AudioMetadataTagger fileTagger,
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
        Throwable error = null;
        try {
            return mFileTagger.writeMetadata(mInputParams);
        } catch (IOException|
                ReadOnlyFileException|
                CannotReadException|
                TagException|
                InvalidAudioFrameException e) {

            error = e;
            e.printStackTrace();
        }

        AudioTagger.ResultCorrection resultCorrection = new AudioTagger.ResultCorrection();

        resultCorrection.error = error;

        return resultCorrection;
    }

    @Override
    protected void onPostExecute(AudioTagger.ResultCorrection resultCorrection) {
        if(mCallback != null) {
            if(resultCorrection.error != null || resultCorrection.code != AudioTagger.SUCCESS) {
                Error error = new Error(track, resultCorrection.error.toString(), resultCorrection.code);
                mCallback.onAsyncOperationError(error);
            }
            else {
                mCallback.onAsyncOperationFinished(new MetadataFixerResult(track, resultCorrection));
            }
        }
    }

    @Override
    protected void onCancelled(AudioTagger.ResultCorrection resultCorrection) {
        onCancelled();
        if(mCallback != null)
            mCallback.onAsyncOperationCancelled(track);
    }



    public static class MetadataFixerResult {
        private Track track;
        private AudioTagger.ResultCorrection resultCorrection;

        public MetadataFixerResult() {
        }

        public MetadataFixerResult(Track track, AudioTagger.ResultCorrection resultCorrection) {
            this();
            this.track = track;
            this.resultCorrection = resultCorrection;
        }

        public Track getTrack() {
            return track;
        }

        public void setTrack(Track track) {
            this.track = track;
        }

        public AudioTagger.ResultCorrection getResultCorrection() {
            return resultCorrection;
        }

        public void setResultCorrection(AudioTagger.ResultCorrection resultCorrection) {
            this.resultCorrection = resultCorrection;
        }
    }


    public static class Error {
        private Track track;
        private String error;
        private int e;

        public Error() {}

        public Error(Track track, String error, int e) {
            this();
            this.track = track;
            this.error = error;
            this.e = e;
        }

        public Track getTrack() {
            return track;
        }

        public void setTrack(Track track) {
            this.track = track;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public int getE() {
            return e;
        }

        public void setE(int e) {
            this.e = e;
        }
    }
}

package mx.dev.franco.automusictagfixer.fixer;

import android.os.AsyncTask;

import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.interfaces.FileManager;
import mx.dev.franco.automusictagfixer.persistence.room.Track;

public class MetadataFixer extends AsyncTask<Void, Void, AudioTagger.ResultCorrection> {
    private AsyncOperation<Track, Result, Track, Error> mCallback;
    private FileManager<FileTagger.InputParams, AudioTagger.TrackDataItem, AudioTagger.ResultCorrection> mFileTagger;
    private FileTagger.InputParams mInputParams;
    private Track track;

    public MetadataFixer(AsyncOperation<Track, Result, Track, Error> callback, FileTagger fileTagger,
                         FileTagger.InputParams inputParams, Track track) {

        mCallback = callback;
        mInputParams = inputParams;
        mFileTagger = fileTagger;
        this.track = track;
    }

    @Override
    protected void onPreExecute() {
        if(mCallback != null)
            mCallback.onAsyncOperationStarted(track);
    }

    @Override
    protected AudioTagger.ResultCorrection doInBackground(Void... voids) {
        return mFileTagger.writeFile(mInputParams);
    }

    @Override
    protected void onPostExecute(AudioTagger.ResultCorrection resultCorrection) {
        if(mCallback != null)
            mCallback.onAsyncOperationFinished(new Result(track, resultCorrection));
    }

    @Override
    protected void onCancelled(AudioTagger.ResultCorrection resultCorrection) {
        onCancelled();
        if(mCallback != null)
            mCallback.onAsyncOperationCancelled(track);
    }

    public static class Result {
        private Track track;
        private AudioTagger.ResultCorrection resultCorrection;

        public Result() {
        }

        public Result(Track track, AudioTagger.ResultCorrection resultCorrection) {
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

        public Error() {}

        public Error(Track track, String error) {
            this();
            this.track = track;
            this.error = error;
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
    }
}

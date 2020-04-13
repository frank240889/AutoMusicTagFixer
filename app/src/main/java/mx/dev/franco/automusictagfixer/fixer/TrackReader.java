package mx.dev.franco.automusictagfixer.fixer;

import android.os.AsyncTask;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;

import java.io.IOException;

import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;

public class TrackReader extends AsyncTask<String, Void, AudioTagger.AudioFields> {
    private AudioTagger mAudioTagger;
    private AsyncOperation<Void, AudioTagger.AudioTaggerResult, Void, AudioTagger.AudioTaggerResult> mCallback;

    public TrackReader(AudioTagger audioTagger,
                       AsyncOperation<Void, AudioTagger.AudioTaggerResult, Void, AudioTagger.AudioTaggerResult> callback) {
        mAudioTagger = audioTagger;
        mCallback = callback;
    }

    @Override
    protected AudioTagger.AudioFields doInBackground(String... strings) {
        try {
            return (AudioTagger.AudioFields) mAudioTagger.readFile(strings[0]);
        } catch (ReadOnlyFileException |
                CannotReadException |
                TagException |
                InvalidAudioFrameException | IOException e) {
            e.printStackTrace();
            return new AudioTagger.AudioFields(AudioTagger.COULD_NOT_READ_TAGS, e);
        }
    }

    @Override
    protected void onPreExecute() {
        if (mCallback != null)
            mCallback.onAsyncOperationStarted(null);
    }

    @Override
    protected void onPostExecute(AudioTagger.AudioFields audioTaggerResult) {
        if (mCallback != null) {
            if (audioTaggerResult.getCode() == AudioTagger.SUCCESS) {
                mCallback.onAsyncOperationFinished(audioTaggerResult);
            }
            else {
                mCallback.onAsyncOperationError(audioTaggerResult);
            }
        }
    }
}

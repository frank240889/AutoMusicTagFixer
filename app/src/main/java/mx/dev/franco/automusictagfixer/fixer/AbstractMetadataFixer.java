package mx.dev.franco.automusictagfixer.fixer;

import android.os.AsyncTask;

import mx.dev.franco.automusictagfixer.interfaces.AudioMetadataManager;
import mx.dev.franco.automusictagfixer.persistence.room.Track;

public abstract class AbstractMetadataFixer<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {
    protected AudioMetadataManager<AudioMetadataTagger.InputParams, AudioTagger.AudioFields,
            AudioTagger.ResultCorrection> mFileTagger;

    protected Track track;

    public AbstractMetadataFixer(AudioMetadataTagger fileTagger, Track track) {
        mFileTagger = fileTagger;
        this.track = track;
    }
}

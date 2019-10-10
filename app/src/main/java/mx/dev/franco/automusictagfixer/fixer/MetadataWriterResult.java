package mx.dev.franco.automusictagfixer.fixer;

import mx.dev.franco.automusictagfixer.persistence.room.Track;

public class MetadataWriterResult {
    private Track track;
    private AudioTagger.ResultCorrection resultCorrection;

    public MetadataWriterResult() {
    }

    public MetadataWriterResult(Track track, AudioTagger.ResultCorrection resultCorrection) {
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

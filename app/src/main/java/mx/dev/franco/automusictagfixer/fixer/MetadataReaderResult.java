package mx.dev.franco.automusictagfixer.fixer;

import mx.dev.franco.automusictagfixer.persistence.room.Track;

public class MetadataReaderResult {
    private Track track;
    private AudioTagger.AudioTaggerResult resultCorrection;

    public MetadataReaderResult() {
    }

    public MetadataReaderResult(Track track, AudioTagger.AudioTaggerResult resultCorrection) {
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

    public AudioTagger.AudioTaggerResult getResultCorrection() {
        return resultCorrection;
    }

    public void setResultCorrection(AudioTagger.AudioTaggerResult resultCorrection) {
        this.resultCorrection = resultCorrection;
    }
}

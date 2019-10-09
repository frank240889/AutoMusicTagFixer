package mx.dev.franco.automusictagfixer.fixer;

import mx.dev.franco.automusictagfixer.persistence.room.Track;

public class MetadataReaderResult {
    private Track track;
    private AudioTagger.AudioFields audioFields;

    public MetadataReaderResult() {
    }

    public MetadataReaderResult(Track track, AudioTagger.AudioFields audioFields) {
        this();
        this.track = track;
        this.audioFields = audioFields;
    }

    public Track getTrack() {
        return track;
    }

    public void setTrack(Track track) {
        this.track = track;
    }

    public AudioTagger.AudioFields getFields() {
        return audioFields;
    }

    public void setFields(AudioTagger.AudioFields resultCorrection) {
        this.audioFields = resultCorrection;
    }
}

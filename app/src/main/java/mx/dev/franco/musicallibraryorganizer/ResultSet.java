package mx.dev.franco.musicallibraryorganizer;

/**
 * Created by franco on 12/05/17.
 */

public final class ResultSet {
    private String name = "";
    private String artist = "";
    private String album = "";
    private String imageUrl = "";
    private String track = "";
    private String year = "";
    private String genre = "";
    private AudioItem audioItem = null;

    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    String getArtist() {
        return artist;
    }

    void setArtist(String artist) {
        this.artist = artist;
    }

    String getAlbum() {
        return album;
    }

    void setAlbum(String album) {
        this.album = album;
    }

    String getImageUrl() {
        return imageUrl;
    }

    void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    String getTrack() {
        return track;
    }

    void setTrack(String track) {
        this.track = track;
    }

    String getYear() {
        return year;
    }

    void setYear(String year) {
        this.year = year;
    }

    String getGenre() {
        return genre;
    }

    void setGenre(String genre) {
        this.genre = genre;
    }

    AudioItem getAudioItem() {
        return audioItem;
    }

    void setAudioItem(AudioItem audioItem) {
        this.audioItem = audioItem;
    }
}

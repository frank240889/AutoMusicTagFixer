package mx.dev.franco.automusictagfixer.identifier;

public class TrackIdentificationResult extends Identifier.IdentificationResults {
    private String title = "";
    private String artist = "";
    private String album = "";
    private String trackNumber = "";
    private String trackYear = "";
    private String genre = "";

    public TrackIdentificationResult() {}

    public TrackIdentificationResult(String title, String artist, String album, String trackNumber, String trackYear, String genre) {
        this();
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.trackNumber = trackNumber;
        this.trackYear = trackYear;
        this.genre = genre;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getTrackNumber() {
        return trackNumber;
    }

    public void setTrackNumber(String trackNumber) {
        this.trackNumber = trackNumber;
    }

    public String getTrackYear() {
        return trackYear;
    }

    public void setTrackYear(String trackYear) {
        this.trackYear = trackYear;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }
}

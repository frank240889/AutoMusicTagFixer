package mx.dev.franco.automusictagfixer.identifier;

import java.util.ArrayList;
import java.util.List;

public class Result implements Identifier.IdentificationResults {
    private String title;
    private String artist;
    private String album;
    private String trackNumber;
    private String trackYear;
    private String genre;
    private List<byte[]> covers = new ArrayList<>();

    public Result() {
    }

    public Result(String title, String artist, String album, String trackNumber, String trackYear, String genre, List<byte[]> covers) {
        this();
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.trackNumber = trackNumber;
        this.trackYear = trackYear;
        this.genre = genre;
        this.covers.addAll(covers);
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

    public List<byte[]> getCovers() {
        return covers;
    }

    public void addCover(byte[] cover) {
        this.covers.add(cover);
    }
}

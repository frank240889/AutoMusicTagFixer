package mx.dev.franco.automusictagfixer.identifier;

import android.util.ArrayMap;

import androidx.annotation.NonNull;

import com.gracenote.gnsdk.GnImageSize;

import java.util.LinkedHashMap;
import java.util.Map;

public class Result extends Identifier.IdentificationResults implements Cloneable {
    private String title = "";
    private String artist = "";
    private String album = "";
    private String trackNumber = "";
    private String trackYear = "";
    private String genre = "";
    private CoverArt coverArt;
    private Map<GnImageSize, String> covers = new LinkedHashMap<>();
    private Map<String, Object> mData = new ArrayMap<>();

    public Result() {}

    public Result(String id, String title, String artist, String album, String trackNumber,
                  String trackYear, String genre, CoverArt coverArt, Map<GnImageSize, String> covers) {
        this();
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.trackNumber = trackNumber;
        this.trackYear = trackYear;
        this.genre = genre;
        this.coverArt = coverArt;
        this.covers.putAll(covers);
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

    public Map<GnImageSize, String> getCovers() {
        return covers;
    }

    public CoverArt getCoverArt() {
        return coverArt;
    }

    public void setCoverArt(CoverArt coverArt) {
        this.coverArt = coverArt;
    }

    public void addCover(GnImageSize size, String url) {
        this.covers.put(size, url);
    }

    public void addField(String fieldName, Object value) {
        mData.put(fieldName, value);
    }

    public Object getField(String fieldName) {
        return mData.get(fieldName);
    }

    @NonNull
    @Override
    protected Object clone() {
        return new Result(getId(), getTitle(), getArtist(), getAlbum(), getTrackNumber(), getTrackYear(), getGenre(), new CoverArt(), getCovers());
    }
}

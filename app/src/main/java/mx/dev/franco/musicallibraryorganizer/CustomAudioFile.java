package mx.dev.franco.musicallibraryorganizer;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import java.io.File;
import java.net.URI;

/**
 * Created by franco on 26/03/17.
 */

public final class CustomAudioFile extends File {
    private String album = "";
    private String albumArtist = "";
    private String artist = "";
    private String author = "";
    private String bitrate = "";
    private String trackNumber = "";
    private String composer = "";
    private String date = "";
    private String discNumber = "";
    private String duration = "";
    private String genre = "";
    private String title = "";
    private String writer = "";
    private String year = "";
    private byte[] albumArt;
    private Bitmap decodedAlbumArt;
    private int lengthAlbumArt;
    private String status = "";
    private int position = 0;
    static final String FILE_STATUS_BAD = "FAIL" ;
    static final String FILE_STATUS_OK = "OK";

    public CustomAudioFile(@NonNull String pathname) {
        super(pathname);
    }

    public CustomAudioFile(String parent, @NonNull String child) {
        super(parent, child);
    }

    public CustomAudioFile(File parent, @NonNull String child) {
        super(parent, child);
    }

    public CustomAudioFile(@NonNull URI uri) {
        super(uri);
    }

    @Override
    public CustomAudioFile[] listFiles() {
        File[] files = super.listFiles();
        if(files == null || files.length == 0){
            return null;
        }

        CustomAudioFile[] customAudioFile = new CustomAudioFile[files.length];
        for(int i=0 ; i<files.length;i++){
            customAudioFile[i] = new CustomAudioFile(files[i].getAbsolutePath());
        }

        return customAudioFile;
    }

    public String getStatus() {
        return this.status;
    }

    public CustomAudioFile setStatus(String status) {
        this.status = status;
        return this;
    }

    public int getPosition() {
        return this.position;
    }

    public CustomAudioFile setPosition(int position) {
        this.position = position;
        return this;
    }

    public byte[] getAlbumArt() {
        return albumArt;
    }

    public CustomAudioFile setAlbumArt(byte[] albumArt) {
        this.albumArt = albumArt;
        this.lengthAlbumArt = albumArt.length;
        return this;
    }

    public Bitmap getDecodedAlbumArt() {
        return decodedAlbumArt;
    }

    public void setDecodedAlbumArt(Bitmap decodedAlbumArt) {
        this.decodedAlbumArt = decodedAlbumArt;
    }

    public String getAlbum() {
        return album;
    }

    public CustomAudioFile setAlbum(String album) {
        this.album = album;
        return this;
    }

    public String getAlbumArtist() {
        return albumArtist;
    }

    public CustomAudioFile setAlbumArtist(String albumArtist) {
        this.albumArtist = albumArtist;
        return this;
    }

    public String getArtist() {
        return artist;
    }

    public CustomAudioFile setArtist(String artist) {
        this.artist = artist;
        return this;
    }

    public String getAuthor() {
        return author;
    }

    public CustomAudioFile setAuthor(String author) {
        this.author = author;
        return this;
    }

    public String getBitrate() {
        return bitrate;
    }

    public CustomAudioFile setBitrate(String bitrate) {
        this.bitrate = bitrate;
        return this;
    }

    public String getTrackNumber() {
        return trackNumber;
    }

    public CustomAudioFile setTrackNumber(String trackNumber) {
        this.trackNumber = trackNumber;
        return this;
    }

    public String getComposer() {
        return composer;
    }

    public CustomAudioFile setComposer(String composer) {
        this.composer = composer;
        return this;
    }

    public String getDate() {
        return date;
    }

    public CustomAudioFile setDate(String date) {
        this.date = date;
        return this;
    }

    public String getDiscNumber() {
        return discNumber;
    }

    public CustomAudioFile setDiscNumber(String discNumber) {
        this.discNumber = discNumber;
        return this;
    }

    public String getDuration() {
        return duration;
    }

    public CustomAudioFile setDuration(String duration) {
        this.duration = duration;
        return this;
    }

    public String getGenre() {
        return genre;
    }

    public CustomAudioFile setGenre(String genre) {
        this.genre = genre;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public CustomAudioFile setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getWriter() {
        return writer;
    }

    public CustomAudioFile setWriter(String writer) {
        this.writer = writer;
        return this;
    }

    public String getYear() {
        return year;
    }

    public CustomAudioFile setYear(String year) {
        this.year = year;
        return this;
    }

    public int getByteLengthAlbum(){
        return this.lengthAlbumArt;
    }

}

package mx.dev.franco.automusictagfixer.persistence.room;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "track_table")
public class Track {

    @PrimaryKey()
    @ColumnInfo(name = "mediastore_id")
    private int mMediaStoreId;

    @NonNull
    @ColumnInfo(name = "title")
    private String mTitle;

    @NonNull
    @ColumnInfo(name = "artist")
    private String mArtist;

    @NonNull
    @ColumnInfo(name = "album")
    private String mAlbum;

    @NonNull
    @ColumnInfo(name = "_data")
    private String mPath;

    @ColumnInfo(name = "selected")
    private int mChecked = 0;

    @ColumnInfo(name = "state")
    private int mState = TrackState.NO_TAGS_SEARCHED_YET;

    @ColumnInfo(name = "processing")
    private int mProcessing = 0;

    @ColumnInfo(name = "version")
    private int mVersion = 0;

    public Track(String title, String artist, String album, String path, int version){
        mTitle = title;
        mArtist = artist;
        mAlbum = album;
        mPath = path;
        mVersion = version;
    }

    public Track(Track track) {
        this(track.getTitle(), track.getArtist(), track.getAlbum(), track.getPath(), track.getVersion());
    }

    public int getMediaStoreId(){
        return mMediaStoreId;
    }

    public void setMediaStoreId(int mediaStoreId){
        mMediaStoreId = mediaStoreId;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(@NonNull String mTitle) {
        this.mTitle = mTitle;
    }

    public String getArtist() {
        return mArtist;
    }

    public void setArtist(@NonNull String mArtist) {
        this.mArtist = mArtist;
    }

    public String getAlbum() {
        return mAlbum;
    }

    public void setAlbum(@NonNull String mAlbum) {
        this.mAlbum = mAlbum;
    }

    public String getPath() {
        return mPath;
    }

    public void setPath(@NonNull String mPath) {
        this.mPath = mPath;
    }

    public int checked(){
        return mChecked;
    }

    public void setChecked(int isChecked){
        mChecked = isChecked;
    }

    public int getState(){
        return mState;
    }

    public void setState(int state){
        mState = state;
    }

    public int processing(){
        return mProcessing;
    }

    public void setProcessing(int isProcessing){
        mProcessing = isProcessing;
    }

    public int getVersion() {
        return mVersion;
    }

    public void setVersion(int version) {
        mVersion = version;
    }
}

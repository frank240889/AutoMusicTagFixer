package mx.dev.franco.musicallibraryorganizer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

import com.gracenote.gnsdk.GnAudioFile;

import java.io.File;

/**
 * Created by franco on 14/04/17.
 */

public final class AudioItem implements Parcelable{
    private Context context;
    private long id;
    private String album = "";
    private String artist = "";
    private String title = "";
    private int status = 0;
    private int position = -1;
    private String humanReadableDuration = "";
    private int duration = -1;
    private String newAbsolutePath = "";
    private String fileName = "";
    private boolean isSelected = false;
    private boolean isChecked = false;
    private boolean isProcessing = false;
    private boolean isPlayingAudio = false;
    private boolean isVisible = true;
    private byte[] coverArt = null;
    private boolean addedRecently = true;
    private String path = "";
    private float size = 0;
    private String trackNumber = "0";
    private String trackYear = "0";
    private String genre = "";
    public static final int FILE_STATUS_NO_PROCESSED = 0;
    public static final int FILE_STATUS_OK = 1;
    public static final int FILE_STATUS_INCOMPLETE = 2;
    public static final int FILE_STATUS_BAD = -1;
    static final int FILE_STATUS_EDIT_BY_USER = 3;
    static final int FILE_STATUS_DOES_NOT_EXIST = 4;
    public static final float KILOBYTE = 1048576;

    public AudioItem(){

    }

    public AudioItem(Parcel in){
        this();
        readFromParcel(in);
    }

    public void setContext(Context context){
        this.context = context.getApplicationContext();
    }

    public Context getContext(){
        return this.context;
    }

    public void clearContext(){
        if(this.context != null){
            this.context = null;
        }
    }

    public String getArtist() {
        return artist;
    }

    public AudioItem setArtist(String artist) {
        this.artist = artist;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public AudioItem setTitle(String title) {
        this.title = title;
        return this;
    }

    public int getStatus() {
        return status;
    }

    public AudioItem setStatus(int status) {
        this.status = status;
        return this;
    }

    public int getPosition() {
        return position;
    }

    public AudioItem setPosition(int position) {
        this.position = position;
        return this;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public AudioItem setSelected(boolean selected) {
        isSelected = selected;
        return this;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public AudioItem setChecked(boolean checked) {
        isChecked = checked;
        return this;
    }

    public boolean isProcessing() {
        return isProcessing;
    }

    public AudioItem setProcessing(boolean processing) {
        isProcessing = processing;
        return this;
    }

    public boolean isPlayingAudio() {
        return isPlayingAudio;
    }

    public AudioItem setPlayingAudio(boolean playingAudio) {
        isPlayingAudio = playingAudio;
        return this;
    }

    public String getNewAbsolutePath() {
        return newAbsolutePath;
    }

    public AudioItem setNewAbsolutePath(String newAbsolutePath) {
        this.newAbsolutePath = newAbsolutePath;
        return this;
    }

    public String getAlbum() {
        return album;
    }

    public AudioItem setAlbum(String album) {
        this.album = album;
        return this;
    }

    public String getHumanReadableDuration() {
        return humanReadableDuration;
    }

    public AudioItem setHumanReadableDuration(String humanReadableDuration) {
        this.humanReadableDuration = humanReadableDuration;
        return this;
    }

    public String getFileName() {
        return fileName;
    }

    public AudioItem setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public long getId() {
        return id;
    }

    public AudioItem setId(long id) {
        this.id = id;
        return this;
    }

    public static String getHumanReadableDuration(int duration){
        int totalSeconds = duration/1000;
        int minutes = 0;
        int seconds = 0;
        String readableDuration = "00:00";
        minutes = (int) Math.floor(totalSeconds / 60);
        seconds = totalSeconds%60;
        readableDuration = minutes + "\'" + (seconds<10?("0"+seconds):seconds) + "\"";
        return readableDuration;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public AudioItem setVisible(boolean visible) {
        isVisible = visible;
        return this;
    }

    public float getSize() {
        return size;
    }

    public AudioItem setSize(float size) {
        this.size = size;
        return this;
    }

    public String getConvertedFileSize(){
        if(getSize() > 0){
            String str = String.valueOf(getSize()).substring(0,4);
            str += "mb";
            return str;
        }
        return "Tamaño desconocido";
    }

    public int getDuration() {
        return duration;
    }

    public AudioItem setDuration(int duration) {
        this.duration = duration;
        return this;
    }


    public String getPath() {
        return path;
    }

    public AudioItem setPath(String path) {
        this.path = path;
        return this;
    }

    public byte[] getCoverArt() {
        return coverArt;
    }

    public AudioItem setCoverArt(byte[] coverArt) {
        this.coverArt = coverArt;
        return this;
    }

    public Drawable getStatusDrawable(){
        int status = getStatus();
        Drawable drawable = null;
        switch (status){
            case AudioItem.FILE_STATUS_OK:
            case AudioItem.FILE_STATUS_EDIT_BY_USER:
                drawable = context.getResources().getDrawable(R.drawable.ic_star_white_24px,null);
                break;
            case AudioItem.FILE_STATUS_INCOMPLETE:
                drawable = context.getResources().getDrawable(R.drawable.ic_star_half_white_24px,null);
                break;
            case AudioItem.FILE_STATUS_BAD:
                drawable = context.getResources().getDrawable(R.drawable.ic_error_outline_white_24px,null);
                break;
            default:
                drawable = context.getResources().getDrawable(R.drawable.ic_star_border_white_24px,null);
                break;
        }

        return drawable;
    }

    public String getStatusText(){
        int status = getStatus();
        String msg = "";
        switch (status){
            case AudioItem.FILE_STATUS_OK:
                msg = context.getResources().getString(R.string.file_status_ok);
                break;
            case AudioItem.FILE_STATUS_INCOMPLETE:
                msg = context.getResources().getString(R.string.file_status_incomplete);
                break;
            case AudioItem.FILE_STATUS_BAD:
                msg = context.getResources().getString(R.string.file_status_bad);
                break;
            case AudioItem.FILE_STATUS_EDIT_BY_USER:
                msg = context.getResources().getString(R.string.file_status_edit_by_user);
                break;
            default:
                msg = context.getResources().getString(R.string.file_status_no_processed);
                break;
        }

        return msg;
    }

    public AudioItem setStatusText(String statusText){
        return this;
    }

    public boolean isAddedRecently() {
        return addedRecently;
    }

    public AudioItem setAddedRecently(boolean addedRecently) {
        this.addedRecently = addedRecently;
        return this;
    }

    public String getStringImageSize(){
        if(getCoverArt() != null && getCoverArt().length > 0) {
            Bitmap bitmapDrawable = BitmapFactory.decodeByteArray(getCoverArt(), 0, getCoverArt().length);
            return "h" + bitmapDrawable.getHeight() + ", w" + bitmapDrawable.getWidth() + " (px)";
        }
        return "Añadir carátula";
    }

    public Bitmap getBitmapCover(){
        return getCoverArt() != null ? BitmapFactory.decodeByteArray(getCoverArt(), 0, getCoverArt().length):null;
    }

    /**
     * This method returns the relative path,
     * t = 4 because further on is the
     * understable path of file, for example, absolute
     * path from any file may be "/storage/emulated/0/music/rock/song.mp3",
     * so this method returns only "/music/rock"
     * @param str
     * @return
     */
    public static String getRelativePath(String str){
        String[] parts = str.split("/");
        String relativePath = "";
        for (int t = 4 ; t < parts.length ; t++){
            relativePath += "/"+parts[t];
        }
        return relativePath;
    }

    //Only for use when retrieved data for a single track and be sent to DetailsTrackDialogActivity;
    //used for caching values retrieved and don't send again the query to Gracenote API,
   /************************************************************/
    public String getTrackNumber() {
        return trackNumber;
    }

    public AudioItem setTrackNumber(String trackNumber) {
        this.trackNumber = trackNumber;
        return this;
    }

    public String getTrackYear() {
        return trackYear;
    }

    public AudioItem setTrackYear(String trackYear) {
        this.trackYear = trackYear;
        return this;
    }

    public String getGenre() {
        return genre;
    }

    public AudioItem setGenre(String genre) {
        this.genre = genre;
        return this;
    }
/*************************************************************/
    public String[] getExtraData(){
        String[] extraData = new String[3];
        try {
            GnAudioFile gnAudioFile = new GnAudioFile(new File(getNewAbsolutePath()));
            gnAudioFile.sourceInit();


            float freq = ((float) gnAudioFile.samplesPerSecond() / 1000f);
            extraData[0] = freq + " Khz";

            long res = gnAudioFile.sampleSizeInBits();
            extraData[1] = res + " bits";

            long cha = gnAudioFile.numberOfChannels();
            extraData[2] = cha == 1 ? "Mono" : (cha == 2 ? "Estéreo" : "Surround");

            gnAudioFile.sourceClose();
            gnAudioFile = null;
        }
        catch (Exception e){
            e.printStackTrace();
        }finally {
            return extraData;
        }

    }

    public static String getBitrate(String bitrate) {
        String convertedBitrate = "Bitrate desconocido";
        if (bitrate != null || bitrate != ""){
            try {
                int bitrateInt = Integer.parseInt(bitrate);
                int bitrateInKb = bitrateInt / 1000;
                convertedBitrate = bitrateInKb + " Kbps";
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        return convertedBitrate;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getTitle());
        dest.writeString(getArtist());
        dest.writeString(getAlbum());
        dest.writeString(getGenre());
        dest.writeString(getTrackNumber());
        dest.writeString(getTrackYear());
        dest.writeByteArray(getCoverArt());
    }

    private void readFromParcel(Parcel in){
        setTitle(in.readString());
        setArtist(in.readString());
        setAlbum(in.readString());
        setGenre(in.readString());
        setTrackNumber(in.readString());
        setTrackYear(in.readString());
        setCoverArt(in.createByteArray());
    }

    public static final Parcelable.Creator<AudioItem> CREATOR = new Parcelable.Creator<AudioItem>() {
        public AudioItem createFromParcel(Parcel in) {
            return new AudioItem(in);
        }

        public AudioItem[] newArray(int size) {
            return new AudioItem[size];
        }
    };
}

package mx.dev.franco.musicallibraryorganizer.list;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.gracenote.gnsdk.GnAudioFile;

import java.io.File;

import mx.dev.franco.musicallibraryorganizer.utilities.StringUtilities;

/**
 * Created by franco on 14/04/17.
 */

public final class AudioItem implements Parcelable{
    public static final int FILE_STATUS_NO_PROCESSED = 0;
    public static final int FILE_STATUS_OK = 1;
    public static final int FILE_STATUS_INCOMPLETE = 2;
    public static final int FILE_STATUS_BAD = -1;
    public static final int FILE_STATUS_EDIT_BY_USER = 3;
    public static final int FILE_ERROR_READ = 4;
    public static final float KILOBYTE = 1048576;

    private long id = -1;
    private String title = "";
    private String artist = "";
    private String album = "";
    private String absolutePath = "";
    private int status = FILE_STATUS_NO_PROCESSED;
    private int position = -1;

    private boolean isSelected = false;
    private boolean isChecked = false;
    private boolean isProcessing = false;
    private boolean isPlayingAudio = false;

    //Need for trackid from DetailsTrackActivity
    private byte[] coverArt = null;
    private String trackNumber = "";
    private String trackYear = "";
    private String genre = "";


    public AudioItem(){

    }

    public AudioItem(Parcel in){
        this();
        readFromParcel(in);
    }

    public long getId() {
        return id;
    }

    public AudioItem setId(long id) {
        this.id = id;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public AudioItem setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getArtist() {
        return artist;
    }

    public AudioItem setArtist(String artist) {
        this.artist = artist;
        return this;
    }

    public String getAlbum() {
        return album;
    }

    public AudioItem setAlbum(String album) {
        this.album = album;
        return this;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public AudioItem setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
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

    //Only for use when retrieved data for a single track and is sent to TrackDetailsActivity;
    //used for caching values retrieved and don't send again the query to Gracenote API,
    /************************************************************/
    public byte[] getCoverArt() {
        return coverArt;
    }

    public AudioItem setCoverArt(byte[] coverArt) {
        this.coverArt = coverArt;
        return this;
    }

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


    public static String getHumanReadableDuration(String duration){
        if(duration == null || duration.isEmpty())
            return "0";
        int d = Integer.parseInt(duration);
        int totalSeconds = d;
        int minutes = 0;
        int seconds = 0;
        String readableDuration = "\'" + "00" +  "\"" + "00";
        minutes = (int) Math.floor(totalSeconds / 60);
        seconds = totalSeconds%60;
        readableDuration = minutes + "\'" + (seconds<10?("0"+seconds):seconds) + "\"";
        return readableDuration;
    }

    public static String getFileSize(long size){
        if(size <= 0)
            return "0 mb";

        float s = size / KILOBYTE;
        String str = String.valueOf(s);
        int l = str.length();
        String readableSize = "";
        if(l > 4)
            readableSize = str.substring(0,4);
        else
            readableSize =str.substring(0,3);
        readableSize += " mb";

        return readableSize;
    }

    public static String getStringImageSize(byte[] cover){
        Log.d("cover art is null",(cover == null)+"");
        String msg;
        if(cover != null && cover.length > 0) {
            Log.d("cover art is null",(cover == null)+"_ " + cover.length);
            try {
                Bitmap bitmapDrawable = BitmapFactory.decodeByteArray(cover, 0, cover.length);
                msg = bitmapDrawable.getHeight() + " * " + bitmapDrawable.getWidth() + " pixeles";
                return msg;
            }
            catch (Exception e){
                msg = "Sin carátula";
            }
        }
        return "Sin carátula";
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

    /**
     * Returns only the last part of absolute path,
     * it means, the file name
     * @param absolutePath
     * @return
     */
    public static String getFilename(String absolutePath){
        String[] parts = absolutePath.split("/");
        return parts[parts.length-1];
    }

    /**
     * This method helps to rename the file,
     * in case is set from options in settings activity
     * @param currentPath
     * @return String[]
     */
    public static String renameFile(String currentPath, String title, String artistName){

        boolean couldBeRenamed = false;
        File currentFile = new File(currentPath), renamedFile;
        String newParentPath = currentFile.getParent();

        String newFilename = StringUtilities.sanitizeFilename(title) + ".mp3";
        String newAbsolutePath= newParentPath + "/" + newFilename;
        renamedFile = new File(newAbsolutePath);
        if(!renamedFile.exists()) {
            couldBeRenamed = currentFile.renameTo(renamedFile);
        }else {
            //if artist tag was found
            if(!artistName.equals("")) {
                newFilename = title + "(" + artistName + ")" + ".mp3";
            }
            else{
                newFilename = title +"("+ (int)Math.floor((Math.random()*10)+ 1) +")"+".mp3";
            }
            newAbsolutePath = newParentPath + "/" + newFilename;
            renamedFile = new File(newAbsolutePath);
            couldBeRenamed = currentFile.renameTo(renamedFile);
        }

        Log.d("MANUAL_RENAMED",couldBeRenamed+"");

        return newAbsolutePath;
    }


    public String[] getExtraData(){
        String[] extraData = new String[3];
        try {
            GnAudioFile gnAudioFile = new GnAudioFile(new File(getAbsolutePath()));
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

    public static String getFrequency(String freq){
        if(freq == null || freq.isEmpty())
            return "0 Khz";

        float f = Float.parseFloat(freq);
        float f2 = f / 1000f;
        return f2+ " Khz";
    }

    public static String getResolution(int res){
        return res + " bits";
    }

    public static String getBitrate(String bitrate) {

        if (bitrate != null && !bitrate.equals("") && !bitrate.equals("0") ){
           // int bitrateInt = Integer.parseInt(bitrate);
            return bitrate + " Kbps";
        }
        return "0 Kbps";
    }

    /**
     * This method check the integrity of file
     *
     * @param absolutePath
     * @return false in case it cannot be read
     */

    public static boolean checkFileIntegrity(String absolutePath){
        File file = new File(absolutePath);
        return file.exists() && file.length() > 0 && file.canRead();
    }

    public static String getMimeType(String absolutePath){
        String ext = getExtension(absolutePath);
        if(ext == null)
            return null;
        //get type depending on extension
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
    }

    public static String getExtension(String absolutePath){
        if(absolutePath == null || absolutePath.isEmpty())
            return null;

        File file = new File(absolutePath);
        //get file extension
        return file.getName().substring(file.getName().lastIndexOf(".") + 1);
    }



    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(getId());
        dest.writeString(getTitle());
        dest.writeString(getArtist());
        dest.writeString(getAlbum());
        dest.writeString(getAbsolutePath());
        dest.writeInt(getStatus());
        dest.writeString(getGenre());
        dest.writeString(getTrackNumber());
        dest.writeString(getTrackYear());
        dest.writeByteArray(getCoverArt());
        dest.writeInt(getPosition());
    }

    private void readFromParcel(Parcel in){
        setId(in.readLong());
        setTitle(in.readString());
        setArtist(in.readString());
        setAlbum(in.readString());
        setAbsolutePath(in.readString());
        setStatus(in.readInt());
        setGenre(in.readString());
        setTrackNumber(in.readString());
        setTrackYear(in.readString());
        setCoverArt(in.createByteArray());
        setPosition(getPosition());
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

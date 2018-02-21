package mx.dev.franco.automusictagfixer.list;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;

import mx.dev.franco.automusictagfixer.utilities.StringUtilities;

/**
 * Created by franco on 14/04/17.
 */

public final class AudioItem implements Parcelable{

    //Status of correction of items
    public static final int STATUS_NO_TAGS_SEARCHED_YET = 0;
    public static final int STATUS_ALL_TAGS_FOUND = 1;
    public static final int STATUS_ALL_TAGS_NOT_FOUND = 2;
    public static final int STATUS_NO_TAGS_FOUND = -1;
    public static final int STATUS_TAGS_EDITED_BY_USER = 3;
    public static final int FILE_ERROR_READ = 4;
    public static final int STATUS_TAGS_CORRECTED_BY_SEMIAUTOMATIC_MODE = 5;

    public static final float KILOBYTE = 1048576;

    //Default values
    private long id = -1;
    private String title = "";
    private String artist = "";
    private String album = "";
    private String absolutePath = "";
    private int status = STATUS_NO_TAGS_SEARCHED_YET;
    private int position = -1;

    //States of items
    private boolean isChecked = false;
    private boolean isProcessing = false;
    private boolean hasNewValues = false;

    //Fields that caches trackid results
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

    public AudioItem setHasNewValues(boolean hasNewValues){
        this.hasNewValues = hasNewValues;
        return this;
    }

    public boolean getHasNewValues(){
        return this.hasNewValues;
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


    /**
     * Formats duration to human readable
     * string
     * @param duration duration in seconds
     * @return formatted string duration
     */
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

    /**
     * Gets file size in megabytes
     * @param size File size
     * @return formatted file size string
     */
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

    /**
     * Gets image dimensions information
     * @param cover Cover art data
     * @return formatted string image size
     */
    public static String getStringImageSize(byte[] cover){
        String msg;
        if(cover != null && cover.length > 0) {
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
        int limit = 0;
        if(str.contains("emulated")){
            limit = 3;
        }
        else if(str.contains("storage/extSdCard")){
            limit = 2;
        }

        String[] parts = str.split("/");
        String relativePath = "";
        for (int t = parts.length - 1 ; t > limit ; --t){
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
        if(absolutePath == null || absolutePath.isEmpty())
            return "";

        String[] parts = absolutePath.split("/");
        return parts[parts.length-1];
    }

    /**
     * Rename the file,
     * in case is set from options in Settings activity
     * @param currentPath
     * @return String[]
     */
    public static String renameFile(String currentPath, String title, String artistName){

        if(!checkFileIntegrity(currentPath)){
            Log.d("integrity ", ""+checkFileIntegrity(currentPath));
            return null;
        }

        boolean success =false;
        File currentFile = new File(currentPath), renamedFile;
        String newParentPath = currentFile.getParent();

        String newFilename = StringUtilities.sanitizeFilename(title) + "." + getExtension(currentPath);
        String newAbsolutePath= newParentPath + "/" + newFilename;
        renamedFile = new File(newAbsolutePath);
        if(!renamedFile.exists()) {
            //currentFile.renameTo(renamedFile);
            success = DocumentFile.fromFile(currentFile).renameTo(renamedFile.getName());
        }else {
            //if artist tag was found
            if(!artistName.equals("")) {
                newFilename = title + "(" + artistName + ")" + "." + getExtension(currentPath);
            }
            else{
                newFilename = title +"("+ (int)Math.floor((Math.random()*10)+ 1) +")"+ "." + getExtension(currentPath);
            }
            newAbsolutePath = newParentPath + "/" + newFilename;
            renamedFile = new File(newAbsolutePath);
            //currentFile.renameTo(renamedFile);
            success = DocumentFile.fromFile(currentFile).renameTo(renamedFile.getName());
        }

        Log.d("reanmed",success+"");
        return newAbsolutePath;
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

    public static boolean checkFileIntegrity(DocumentFile file){
        return file.exists() && file.length() > 0 && file.canRead();
    }

    public static String getMimeType(String absolutePath){
        String ext = getExtension(absolutePath);
        if(ext == null)
            return null;
        //get type depending on extension
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
    }

    public static String getMimeType(File file){
        String ext = getExtension(file);
        if(ext == null)
            return null;
        //get type depending on extension
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
    }

    public static String getExtension(String absolutePath){
        if(absolutePath == null || absolutePath.isEmpty())
            return null;

        File file = new File(absolutePath);
        //get file extension, extension must be in lowercase
        return file.getName().substring(file.getName().lastIndexOf(".") + 1).toLowerCase();
    }

    public static String getExtension(File file){
        if(file == null)
            return null;
        //get file extension, extension must be in lowercase
        return file.getName().substring(file.getName().lastIndexOf(".") + 1).toLowerCase();
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

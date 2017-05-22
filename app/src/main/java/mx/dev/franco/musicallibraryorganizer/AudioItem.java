package mx.dev.franco.musicallibraryorganizer;

/**
 * Created by franco on 14/04/17.
 */

final class AudioItem {
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
    private boolean isProcessing = false;
    private boolean isPlayingAudio = false;
    private boolean isVisible = true;
    private float size = 0;
    static final int FILE_STATUS_NO_PROCESSED = 0;
    static final int FILE_STATUS_OK = 1;
    static final int FILE_STATUS_INCOMPLETE = 2;
    static final int FILE_STATUS_BAD = -1;
    static final int FILE_STATUS_EDIT_BY_USER = 3;
    static final int FILE_STATUS_DOES_NOT_EXIST = 4;

    AudioItem(){

    }

    String getArtist() {
        return artist;
    }

    AudioItem setArtist(String artist) {
        this.artist = artist;
        return this;
    }

    String getTitle() {
        return title;
    }

    AudioItem setTitle(String title) {
        this.title = title;
        return this;
    }

    int getStatus() {
        return status;
    }

    AudioItem setStatus(int status) {
        this.status = status;
        return this;
    }

    int getPosition() {
        return position;
    }

    AudioItem setPosition(int position) {
        this.position = position;
        return this;
    }

    boolean isSelected() {
        return isSelected;
    }

    AudioItem setSelected(boolean selected) {
        isSelected = selected;
        return this;
    }

    boolean isProcessing() {
        return isProcessing;
    }

    AudioItem setProcessing(boolean processing) {
        isProcessing = processing;
        return this;
    }

    boolean isPlayingAudio() {
        return isPlayingAudio;
    }

    AudioItem setPlayingAudio(boolean playingAudio) {
        isPlayingAudio = playingAudio;
        return this;
    }

    String getNewAbsolutePath() {
        return newAbsolutePath;
    }

    AudioItem setNewAbsolutePath(String newAbsolutePath) {
        this.newAbsolutePath = newAbsolutePath;
        return this;
    }

    String getAlbum() {
        return album;
    }

    AudioItem setAlbum(String album) {
        this.album = album;
        return this;
    }

    String getHumanReadableDuration() {
        return humanReadableDuration;
    }

    AudioItem setHumanReadableDuration(String humanReadableDuration) {
        this.humanReadableDuration = humanReadableDuration;
        return this;
    }

    String getFileName() {
        return fileName;
    }

    AudioItem setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    long getId() {
        return id;
    }

    AudioItem setId(long id) {
        this.id = id;
        return this;
    }

    static String convertSpeed(String speed){
        if(speed == null || speed.equals("")){
            return "0";
        }

        return String.valueOf(Integer.parseInt(speed) / 1000);
    }

    static String getChannelsMode(String numberChannels){
        if(numberChannels == null || numberChannels.equals("")){
            return "0";
        }

        return Integer.valueOf(numberChannels) == 1? "Mono":"Estéreo";
    }

    static String getHumanReadableDuration(int duration){
        int totalSeconds = duration/1000;
        int minutes = 0;
        int seconds = 0;
        String readableDuration = "00:00";
        minutes = (int) Math.floor(totalSeconds / 60);
        seconds = totalSeconds%60;
        readableDuration = minutes + ":" + (seconds<10?("0"+seconds):seconds);
        return readableDuration;
    }

    boolean isVisible() {
        return isVisible;
    }

    AudioItem setVisible(boolean visible) {
        isVisible = visible;
        return this;
    }

    float getSize() {
        return size;
    }

    AudioItem setSize(float size) {
        this.size = size;
        return this;
    }

    int getDuration() {
        return duration;
    }

    AudioItem setDuration(int duration) {
        this.duration = duration;
        return this;
    }
}

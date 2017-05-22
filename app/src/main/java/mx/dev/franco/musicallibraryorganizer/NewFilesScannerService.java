package mx.dev.franco.musicallibraryorganizer;

import android.app.Notification;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import wseemann.media.FFmpegMediaMetadataRetriever;

/**
 * Created by franco on 18/04/17.
 */

public class NewFilesScannerService extends Service {
    final static String TAG_SERVICE = NewFilesScannerService.class.getName();
    final IBinder serviceBinder = new BinderService();
    ArrayList<String> listOfPaths = new ArrayList<String>();
    private FFmpegMediaMetadataRetriever mediaMetadataRetriever;
    private MediaExtractor mediaExtractor;
    private MediaFormat mediaFormat;
    private DataTrackDbHelper dataTrackDbHelper;
    private ArrayAdapter<AudioItem> mFilesAdapter;
    int updateType;
    SelectFolderActivity selectFolderActivity;

    @Override
    public void onCreate(){
        super.onCreate();
        startForeground(R.string.app_name, new Notification());
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        super.onStartCommand(intent,flags,startId);
        Toast.makeText(getApplicationContext(), "service starting", Toast.LENGTH_SHORT).show();
        scanForChangedFiles(DetectorChangesFiles.UPDATE_FROM_SERVICE_SCANNER_ON_APP_START);
        return START_NOT_STICKY;
    }



    class BinderService extends Binder{
        NewFilesScannerService getService(){
            return NewFilesScannerService.this;
        }
    }
    /**
     * This method is going to check if there have been
     * any file change, such as new, deleted or moved file
     * from its current location and then select path of those that changed,
     * with the purpose of
     * updating the list of audiotracks in main activity.
     * @return true if there are any change in files, false otherwise
     */
    protected void scanForChangedFiles(int updateRequestType){
        updateType = updateRequestType;
        if(SplashActivity.existDatabase){
            dataTrackDbHelper = new DataTrackDbHelper(getApplicationContext());
            File rootPath = new File("/storage/emulated/0/Music/TestMusic/");//Environment.getExternalStorageDirectory();
            getFiles(rootPath);
            //stopSelf();
            Log.d(TAG_SERVICE, "scanForChangedFiles");

        }
    }

    protected void getFiles(File dir) {
        File listFile[] = (File[])dir.listFiles();
        if (listFile != null && listFile.length > 0) {
            for (int i = 0; i < listFile.length; i++) {
                if (listFile[i].isDirectory()) {
                    getFiles(listFile[i]);
                } else {
                    if (listFile[i].getName().endsWith(".mp3")) {
                        boolean existInDatabase = dataTrackDbHelper.existInDatabase(listFile[i].getAbsolutePath());
                        if(!existInDatabase){
                            Log.d("EXIST IN_DB",existInDatabase+"");
                            //setPaths(listFile[i].getAbsolutePath());
                            addModifiedToDatabase(listFile[i].getAbsolutePath());
                        }
                    }
                }

            }
        }
    }


    private void setPaths(String s){
        listOfPaths.add(s);
    }

    private void addModifiedToDatabase(String pathTrack){
        mediaMetadataRetriever = new FFmpegMediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(pathTrack);
        mediaExtractor = new MediaExtractor();
        File tempFile = new File(pathTrack);
        try {
            mediaExtractor.setDataSource(pathTrack);
            mediaFormat = mediaExtractor.getTrackFormat(0);
        } catch (IOException e) {
            e.printStackTrace();
        }


        String filename = tempFile.getName();
        String path = tempFile.getParent();
        String fullPath = path + "/" + filename;

        String title = mediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_TITLE) != null ?
                mediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_TITLE):"";
        String artist = mediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ARTIST) != null ?
                mediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ARTIST):"";
        String album = mediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ALBUM) != null ?
                mediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ALBUM):"";
        String albumArtist = mediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ALBUM_ARTIST) != null ?
                mediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ALBUM_ARTIST):"";
        String author = mediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ARTIST) != null ?
                mediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ARTIST):"";
        String composer = mediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_COMPOSER) != null ?
                mediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_COMPOSER):"";
        String writer = mediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_COMPOSER) != null ?
                mediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_COMPOSER):"";
        String trackNumber = mediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_TRACK) != null?
                mediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_TRACK):"0";
        String discNumber = mediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DISC) != null ?
                mediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DISC):"";
        String year = mediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DATE) != null ?
                mediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DATE):"0";
        String duration = mediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION) != null ?
                mediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_DURATION):"0";
        String genre = mediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_GENRE) != null ?
                mediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_GENRE):"";
        String fileType = mediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_AUDIO_CODEC) != null ?
                mediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_AUDIO_CODEC):"";
        //String resolution = mediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ENCODER) != null ?
        //        mediaMetadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_):"0";

        int bitrate = mediaFormat != null ? mediaFormat.getInteger(MediaFormat.KEY_BIT_RATE):0;
        int channels =  mediaFormat !=null ? mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT):0;
        int samplingRate = mediaFormat != null ? mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE):0;
        //int resolution = mediaFormat != null ? mediaFormat.getInteger(KEY_):0;

        ContentValues values = new ContentValues();

        values.put(TrackContract.TrackData.COLUMN_NAME_TITLE, title );
        values.put(TrackContract.TrackData.COLUMN_NAME_ARTIST, artist);
        values.put(TrackContract.TrackData.COLUMN_NAME_ALBUM, album);
        values.put(TrackContract.TrackData.COLUMN_NAME_ALBUM_ARTIST, albumArtist);
        values.put(TrackContract.TrackData.COLUMN_NAME_AUTHOR, author);
        values.put(TrackContract.TrackData.COLUMN_NAME_COMPOSER, composer);
        values.put(TrackContract.TrackData.COLUMN_NAME_WRITER, writer);
        values.put(TrackContract.TrackData.COLUMN_NAME_TRACK_NUMBER, trackNumber);
        values.put(TrackContract.TrackData.COLUMN_NAME_DISC_NUMBER, discNumber);
        values.put(TrackContract.TrackData.COLUMN_NAME_YEAR, year);
        values.put(TrackContract.TrackData.COLUMN_NAME_DURATION, Integer.parseInt(duration));
        values.put(TrackContract.TrackData.COLUMN_NAME_GENRE, genre);
        values.put(TrackContract.TrackData.COLUMN_NAME_FILE_TYPE, fileType);
        //values.put(TrackContract.TracKData.COLUMN_NAME_RESOLUTION, Integer.parseInt(resolution));
        values.put(TrackContract.TrackData.COLUMN_NAME_SAMPLING_RATE, samplingRate);
        values.put(TrackContract.TrackData.COLUMN_NAME_BITRATE, bitrate);
        values.put(TrackContract.TrackData.COLUMN_NAME_CHANNELS, channels);
        values.put(TrackContract.TrackData.COLUMN_NAME_ORIGINAL_FILENAME, filename);
        values.put(TrackContract.TrackData.COLUMN_NAME_ORIGINAL_PATH, path);
        values.put(TrackContract.TrackData.COLUMN_NAME_ORIGINAL_FULL_PATH, fullPath);
        values.put(TrackContract.TrackData.COLUMN_NAME_CURRENT_FILENAME, filename);
        values.put(TrackContract.TrackData.COLUMN_NAME_CURRENT_PATH, path);
        values.put(TrackContract.TrackData.COLUMN_NAME_CURRENT_FULL_PATH, fullPath);
        values.put(TrackContract.TrackData.COLUMN_NAME_FILE_SIZE, tempFile.length());
        values.put(TrackContract.TrackData.COLUMN_NAME_STATUS, AudioItem.FILE_STATUS_NO_PROCESSED);
        long _id = dataTrackDbHelper.saveFileData(values, TrackContract.TrackData.TABLE_NAME);

        if(updateType == DetectorChangesFiles.UPDATE_FROM_FILE_OBSERVER_CHANGES) {
            final AudioItem audioItem = new AudioItem();
            audioItem.setTitle(title).setArtist(artist).setAlbum(album).setHumanReadableDuration(AudioItem.getHumanReadableDuration(Integer.parseInt(duration))).setFileName(new File(pathTrack).getName()).setPosition(mFilesAdapter.getCount() - 1).setId(_id).setNewAbsolutePath(fullPath).setStatus(AudioItem.FILE_STATUS_NO_PROCESSED);
            selectFolderActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mFilesAdapter.add(audioItem);
                    mFilesAdapter.notifyDataSetChanged();
                }
            });
        }

        mediaFormat = null;
        mediaExtractor.release();
        mediaExtractor = null;
        mediaMetadataRetriever.release();
        mediaMetadataRetriever = null;
        values.clear();
        values = null;
    }
    /**
     *
     * @return array of paths of files that
     * have been changed
     */
    public ArrayList<String> getChangedFiles(){
        return listOfPaths;
    }

    public void setParameters(ArrayAdapter<AudioItem> filesAdapter, SelectFolderActivity selectFolderActivity1){
        selectFolderActivity = selectFolderActivity1;
        this.mFilesAdapter = filesAdapter;
    }


}

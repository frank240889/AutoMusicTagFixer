package mx.dev.franco.musicallibraryorganizer.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.gracenote.gnsdk.GnAssetFetch;
import com.gracenote.gnsdk.GnAudioFile;
import com.gracenote.gnsdk.GnContent;
import com.gracenote.gnsdk.GnDataLevel;
import com.gracenote.gnsdk.GnError;
import com.gracenote.gnsdk.GnException;
import com.gracenote.gnsdk.GnImageSize;
import com.gracenote.gnsdk.GnLookupData;
import com.gracenote.gnsdk.GnMusicIdFile;
import com.gracenote.gnsdk.GnMusicIdFileCallbackStatus;
import com.gracenote.gnsdk.GnMusicIdFileInfo;
import com.gracenote.gnsdk.GnMusicIdFileInfoManager;
import com.gracenote.gnsdk.GnMusicIdFileProcessType;
import com.gracenote.gnsdk.GnMusicIdFileResponseType;
import com.gracenote.gnsdk.GnResponseAlbums;
import com.gracenote.gnsdk.GnResponseDataMatches;
import com.gracenote.gnsdk.GnStatus;
import com.gracenote.gnsdk.IGnCancellable;
import com.gracenote.gnsdk.IGnMusicIdFileEvents;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.images.AndroidArtwork;
import org.jaudiotagger.tag.images.Artwork;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import mx.dev.franco.musicallibraryorganizer.MainActivity;
import mx.dev.franco.musicallibraryorganizer.R;
import mx.dev.franco.musicallibraryorganizer.SelectedOptions;
import mx.dev.franco.musicallibraryorganizer.SplashActivity;
import mx.dev.franco.musicallibraryorganizer.database.DataTrackDbHelper;
import mx.dev.franco.musicallibraryorganizer.database.TrackContract;
import mx.dev.franco.musicallibraryorganizer.list.AudioItem;

import static mx.dev.franco.musicallibraryorganizer.services.GnService.appString;

/**
 * Created by franco on 17/08/17.
 */

public class FixerTrackService extends Service {
    public static final String MANUAL_MODE = "manual_mode" ;
    public static final String MEDIASTORE_ID = "mediastore_id";
    public static final String ACTION_SHOW_NOTIFICATION = "action_show_notification";
    public static final String ACTION_NOT_FOUND = "action_not_found";
    private static String TAG = FixerTrackService.class.getName();
    //response actions in order to receivers can distinguish the response and handle correctly
    public static final String ACTION_DONE = "action_done";
    public static final String ACTION_DONE_DETAILS = "action_done";
    public static final String ACTION_CANCEL = "action_cancel";
    public static final String ACTION_FAIL = "action_fail";
    public static final String ACTION_COMPLETE_TASK = "action_complete_task";
    public static final String ACTION_SET_AUDIOITEM_PROCESSING = "action_set_audioitem_processing";
    public static final String SINGLE_TRACK = "single_track";
    public static final String FROM_EDIT_MODE = "from_edit_mode";
    public static final String AUDIO_ITEM = "audio_item";
    //global reference to the object being processed
    private volatile AudioItem currentAudioItem = null;
    //Necessary objects to interact with Gracenote API
    private GnMusicIdFileInfoManager gnMusicIdFileInfoManager = null;
    private GnMusicIdFileInfo gnMusicIdFileInfo = null;
    private GnMusicIdFile gnMusicIdFile = null;
    private MusicIdFileEvents musicIdFileEvents = null;
    //Reference to the intent sent from activities
    private Intent intent;
    //Connection to database
    private DataTrackDbHelper dataTrackDbHelper;
    //Reference o broadcast manager that sends the intents to receivers
    private LocalBroadcastManager localBroadcastManager;
    //Intent that contaitn the response sent from broadcast manager
    private Intent broadcastResponseIntent;
    //A list to maintain references to actual objects being processed in case
    //it needs to cancel the task
    public static List<GnMusicIdFile> gnMusicIdFileList = new ArrayList<>();

    //set this flag to TRUE if we touch an element from list, means that only one track is queued to correct
    private boolean singleTrack = false;
    //Initial value, if exist audio item_list, this will override this initial value
    private long currentId = - 1;
    //set this flag to TRUE if we send this intent from DetailsTrackDialogActitivy class
    private boolean fromEditMode = false;
    private boolean downloadCover = false;

    private String currentPath = "";

    private Cursor selectedItems = null;
    private int countSelectedItems = 0;
    private int mode = -1;
    private int numberOfItems = 0;
    private NotificationManager mNM;
    private HandlerThread thread;
    private Looper looper;
    private MyHandler handler;


    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public FixerTrackService() {
        super();
        Log.d(TAG, "constructor");

    }

    @Override
    public void onCreate(){
        super.onCreate();
        Log.d(TAG, "onCreate");
        dataTrackDbHelper = DataTrackDbHelper.getInstance(getApplicationContext());
        localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());

        //Object to handle callbacks from Gracenote API
        musicIdFileEvents = new MusicIdFileEvents(this);
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        thread = new HandlerThread(TAG, Process.THREAD_PRIORITY_FOREGROUND);
        thread.start();
        looper = thread.getLooper();
        handler = new MyHandler(looper);

    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId){
        super.onStartCommand(intent,flags,startId);
        Log.d(TAG,"onStartCommand");

        if( intent != null){
            Log.d(TAG,(intent==null)+"");
            Message msg = handler.obtainMessage();
            msg.arg1 = startId;
            msg.obj = intent;
            handler.sendMessage(msg);
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        //return null if is not binded service
        return null;
    }

    @Override
    public void onDestroy(){
        stopForeground(true);
        super.onDestroy();
    }

    private void startNotification() {
        Intent notificationIntent = new Intent(this,MainActivity.class);
        notificationIntent.setAction(MainActivity.MAIN_ACTION);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                R.mipmap.ic_launcher);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setAutoCancel(true)
                .setColor(ContextCompat.getColor(getApplicationContext(),R.color.grey_800))
                .setTicker(getString(R.string.fixing_task))
                .setContentText(getString(R.string.fixing_task))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();

        startForeground(R.string.app_name,notification);

    }
    private void saveStateProcessing(){
        SharedPreferences sharedPreferences = getSharedPreferences(SplashActivity.APP_SHARED_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(MainActivity.IS_PROCESSING_TASK, MainActivity.isProcessingTask);
        editor.apply();
    }
    private void onHandleIntent(Intent intent){
        String action = intent.getAction();
        Log.d(TAG,((intent == null) ? 0 : action)+"");
        if(action != null && action.equals(ACTION_SHOW_NOTIFICATION)){
            startNotification();
            return;
        }
        else if(action != null && action.equals(ACTION_CANCEL)){
            Log.d("action",ACTION_CANCEL);
            cancelGnMusicIdFileProcessing();
            return;
        }
        else if(!intent.getBooleanExtra(FROM_EDIT_MODE, false)) {
            //intent comes from list,
            selectedItems = dataTrackDbHelper.getAllSelected();
            countSelectedItems = selectedItems == null ? 0 : selectedItems.getCount();
            if(countSelectedItems > 0){
                if(numberOfItems == 1) {
                    //Log.d("1", "1");
                    singleTrack = true;
                    fromEditMode = false;
                    doTrackIdSingleTrack();
                }
                else {
                    //Log.d("3","3");
                    singleTrack = false;
                    fromEditMode = false;
                    doTrackIdMultipleTracks();
                }
            }
            else {
                //Log.d("2",countSelectedItems+"");
                singleTrack = true;
                fromEditMode = false;
                currentAudioItem = intent.getParcelableExtra(AUDIO_ITEM);
                //Log.d("fromEditMode && sing",currentAudioItem.getAbsolutePath()+"");
                doTrackIdSingleTrack();
            }

        }
        else {
            //Log.d(FROM_EDIT_MODE,SINGLE_TRACK);
            currentAudioItem = intent.getParcelableExtra(AUDIO_ITEM);
            singleTrack = true;
            fromEditMode = true;
            doTrackIdSingleTrack();
        }


        Intent intent1 = new Intent();
        intent1.setAction(ACTION_COMPLETE_TASK);
        LocalBroadcastManager localBroadcastManager1 = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastManager1.sendBroadcastSync(intent1);
        Log.d("action_sent",intent1.getAction());
        if(musicIdFileEvents != null)
            musicIdFileEvents.clearValues();

        //stopSelf();

        MainActivity.isProcessingTask = false;
        saveStateProcessing();
    }

    private synchronized void doTrackIdSingleTrack(){
        String title = "";
        String artist = "";
        String album = "";
        String fullPath = "";
        int status = -1;

        if((!fromEditMode && singleTrack && numberOfItems == 0)){
            currentId = currentAudioItem.getId();
            title = currentAudioItem.getTitle();
            artist = currentAudioItem.getArtist();
            album = currentAudioItem.getAlbum();
            fullPath = currentAudioItem.getAbsolutePath();

            ContentValues contentValues = new ContentValues();
            contentValues.put(TrackContract.TrackData.IS_PROCESSING,true);
            dataTrackDbHelper.updateData(currentId,contentValues);
            contentValues.clear();
            contentValues = null;

        }
        else if(fromEditMode && singleTrack){
            currentId = currentAudioItem.getId();
            title = currentAudioItem.getTitle();
            artist = currentAudioItem.getArtist();
            album = currentAudioItem.getAlbum();
            fullPath = currentAudioItem.getAbsolutePath();
        }


        try {

            //Log.d("path",fullPath);
            //Create the object that enables us do "trackid"
            gnMusicIdFile = new GnMusicIdFile(GnService.gnUser, musicIdFileEvents);
            //set option to get type of response
            gnMusicIdFile.options().lookupData(GnLookupData.kLookupDataContent, true);
            //we add the reference to this object in case we need to cancel
            gnMusicIdFileList.add(gnMusicIdFile);
            //get the FileInfoManager
            gnMusicIdFileInfoManager = gnMusicIdFile.fileInfos();

            //add new resource and all possible metadata to increase the accuracy of identify
            gnMusicIdFileInfo = gnMusicIdFileInfoManager.add(fullPath);
            gnMusicIdFileInfo.albumArtist(artist);
            gnMusicIdFileInfo.albumTitle(album);
            gnMusicIdFileInfo.trackTitle(title);
            gnMusicIdFileInfo.fileName(fullPath);

            gnMusicIdFile.waitForComplete();
            //gnMusicIdFile.waitForComplete(10000);

            //do the recognition, kQueryReturnSingle returns only the most accurate result!!!
            gnMusicIdFile.doTrackId(GnMusicIdFileProcessType.kQueryReturnSingle, GnMusicIdFileResponseType.kResponseAlbums);

        } catch (GnException e) {
            e.printStackTrace();
        }
        finally {

        }


    }

    private synchronized void doTrackIdMultipleTracks(){
        String title = "";
        String artist = "";
        String album = "";
        String fullPath = "";
        int status = -1;

        while (selectedItems.moveToNext() ){
            currentAudioItem = new AudioItem();
            currentId = selectedItems.getLong(selectedItems.getColumnIndexOrThrow(TrackContract.TrackData.MEDIASTORE_ID));
            title = selectedItems.getString(selectedItems.getColumnIndexOrThrow(TrackContract.TrackData.TITLE));
            artist = selectedItems.getString(selectedItems.getColumnIndexOrThrow(TrackContract.TrackData.ARTIST));
            album = selectedItems.getString(selectedItems.getColumnIndexOrThrow(TrackContract.TrackData.ALBUM));
            fullPath = selectedItems.getString(selectedItems.getColumnIndexOrThrow(TrackContract.TrackData.DATA));
            status = selectedItems.getInt(selectedItems.getColumnIndexOrThrow(TrackContract.TrackData.STATUS));

            currentAudioItem.setId(currentId).
                    setTitle(title).
                    setArtist(artist).
                    setAlbum(album).
                    setAbsolutePath(fullPath).
                    setStatus(status);

            Intent intent = new Intent();
            intent.setAction(ACTION_SET_AUDIOITEM_PROCESSING);
            intent.putExtra(MEDIASTORE_ID,currentAudioItem.getId());
            localBroadcastManager.sendBroadcastSync(intent);
            Log.d("action_sent",intent.getAction());

            ContentValues contentValues = new ContentValues();
            contentValues.put(TrackContract.TrackData.IS_PROCESSING,true);
            dataTrackDbHelper.updateData(currentId,contentValues);
            contentValues.clear();
            contentValues = null;
            //Log.d(TAG, "dotrackidmultiple");


            String fileName = currentAudioItem.getAbsolutePath();
            File source = new File(fileName);

            try {

                gnMusicIdFile = new GnMusicIdFile(GnService.gnUser, musicIdFileEvents);
                gnMusicIdFile.options().lookupData(GnLookupData.kLookupDataContent, true);
                gnMusicIdFileList.add(gnMusicIdFile);
                gnMusicIdFileInfoManager = gnMusicIdFile.fileInfos();
                gnMusicIdFileInfo = gnMusicIdFileInfoManager.add(source.getAbsolutePath());
                gnMusicIdFileInfo.albumArtist(currentAudioItem.getArtist());
                gnMusicIdFileInfo.trackTitle(currentAudioItem.getTitle());
                gnMusicIdFileInfo.fileName(source.getAbsolutePath());


                gnMusicIdFile.waitForComplete();
                //gnMusicIdFile.waitForComplete(10000);
                gnMusicIdFile.doTrackId(GnMusicIdFileProcessType.kQueryReturnSingle, GnMusicIdFileResponseType.kResponseAlbums);

            } catch (GnException e) {
                e.printStackTrace();
            }
        }
    }

    public void cancelGnMusicIdFileProcessing(){
        Log.d(TAG, "stopping service");
        Iterator<GnMusicIdFile> iterator = gnMusicIdFileList.iterator();

        while (iterator.hasNext()){
            iterator.next().cancel();
        }

        if(currentAudioItem != null){
            ContentValues contentValues = new ContentValues();
            contentValues.put(TrackContract.TrackData.IS_PROCESSING, false);
            contentValues.put(TrackContract.TrackData.IS_SELECTED, false);
            dataTrackDbHelper.updateData(currentAudioItem.getId(), contentValues);
            Intent intent = new Intent();
            intent.setAction(ACTION_CANCEL);
            intent.putExtra(AUDIO_ITEM,currentAudioItem);
            localBroadcastManager.sendBroadcastSync(intent);
            Log.d(TAG, "cancel currentAudioItem");
        }
        stopSelf();
        MainActivity.isProcessingTask = false;
        saveStateProcessing();
        Log.d(TAG, "stopped service");
    }

    /**
     * Instances of this class
     * handles the response of doTrackId method
     */


    private class MusicIdFileEvents implements IGnMusicIdFileEvents {
        private HashMap<String,String> gnStatusToDisplay;
        private String newName = "";
        private String artistName = "";
        private String albumName = "";
        private String trackNumber = "";
        private String year = "";
        private String genre = "";
        private String imageUrl = "";
        private AudioFile audioFile = null;
        private Tag tag = null;
        private AudioHeader audioHeader = null;

        public MusicIdFileEvents(Context context){
            //Put the status of downloaded info
            gnStatusToDisplay = new HashMap<>();
            gnStatusToDisplay.put("kMusicIdFileCallbackStatusProcessingBegin",context.getResources().getString(R.string.begin_processing));
            gnStatusToDisplay.put("kMusicIdFileCallbackStatusFileInfoQuery",context.getResources().getString(R.string.querying_info_));
            gnStatusToDisplay.put("kMusicIdFileCallbackStatusProcessingComplete",context.getResources().getString(R.string.complete_identification));
        }

        @Override
        public void musicIdFileStatusEvent(GnMusicIdFileInfo gnMusicIdFileInfo, GnMusicIdFileCallbackStatus gnMusicIdFileCallbackStatus, long l, long l1, IGnCancellable iGnCancellable) {
                try {
                    String status = gnMusicIdFileCallbackStatus.toString();
                    if (gnStatusToDisplay.containsKey(status)) {
                        if(status.equals("kMusicIdFileCallbackStatusProcessingComplete")){
                            //report to receivers that task were done
                            /*broadcastResponseIntent.setAction(ACTION_DONE);
                            Log.d("action_Sent",broadcastResponseIntent.getAction());
                            broadcastResponseIntent.putExtra("id", currentId);
                            broadcastResponseIntent.putExtra("singleTrack",singleTrack);
                            if(singleTrack)
                                broadcastResponseIntent.putExtra("status",currentAudioItem.getStatus());

                            boolean sent = localBroadcastManager.sendBroadcast(broadcastResponseIntent);
                            Log.d("sent_match",sent+"");


                            //now yes finally, set current audio item_list as deselected
                            contentValues.clear();

                            if(gnMusicIdFile != null)
                                gnMusicIdFileList.remove(gnMusicIdFile);*/
                        }

                        String filename = gnMusicIdFileInfo.identifier();
                        /*if (filename != null) {
                            status = gnStatusToDisplay.get(status) + ": " + currentAudioItem.getAbsolutePath();

                        }*/

                        //Log.d("FILE STATUS EVENT", status);

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(appString, "error in retrieving musicIdFileStatus");
                }

        }

        @Override
        public void gatherFingerprint(GnMusicIdFileInfo fileInfo, long l, long l1, IGnCancellable iGnCancellable) {
            //Log.d("gatherFingerprint", "gatherFingerprint");

            //Callback to inform that fingerprint was retrieved from audiotrack.
                try {
                    if (GnAudioFile.isFileFormatSupported(fileInfo.fileName())) {
                        fileInfo.fingerprintFromSource(new GnAudioFile(new File(fileInfo.fileName())));
                    }

                }
                catch (GnException e) {
                    if (!GnError.isErrorEqual(e.errorCode(), GnError.GNSDKERR_Aborted)) {
                        Log.e(appString, "error in fingerprinting file: " + e.errorAPI() + ", " + e.errorModule() + ", " + e.errorDescription());

                        if(!fromEditMode){
                            ContentValues contentValues = new ContentValues();
                            contentValues.put(TrackContract.TrackData.STATUS, AudioItem.FILE_STATUS_BAD);
                            dataTrackDbHelper.updateData(currentAudioItem.getId(), contentValues);
                        }
                        currentAudioItem.setStatus(AudioItem.FILE_STATUS_BAD);
                        currentAudioItem.setChecked(false);
                        currentAudioItem.setProcessing(false);



                        Intent localIntent = new Intent();
                        localIntent.setAction(ACTION_FAIL);
                        localIntent.putExtra(AUDIO_ITEM,currentAudioItem);
                        localBroadcastManager.sendBroadcastSync(localIntent);
                    }
                }

        }
        //This method is not need because we provide some metadata before
        @Override
        public void gatherMetadata(GnMusicIdFileInfo gnMusicIdFileInfo, long l, long l1, IGnCancellable iGnCancellable) {
            Log.d("gatherMetadata", "gatherMetadata");
        }

        @Override
        public void musicIdFileAlbumResult(GnResponseAlbums gnResponseAlbums, long l, long l1, IGnCancellable iGnCancellable) {

            try {
                newName = gnResponseAlbums.albums().at(0).next().trackMatched().title().display();
            } catch (GnException e) {
                e.printStackTrace();
                newName = "";
            }

            try {
                artistName = gnResponseAlbums.albums().at(0).next().artist().name().display();
            } catch (GnException e) {
                e.printStackTrace();
                artistName = "";
            }
            try {
                albumName = gnResponseAlbums.albums().at(0).next().title().display();
            } catch (GnException e) {
                e.printStackTrace();
                albumName = "";
            }

            try {
                //If is selected "No descargar imagen"
                if (SelectedOptions.ALBUM_ART_SIZE == null) {
                    imageUrl = "";
                }
                //If is selected "De mejor calidad disponible"
                else if (SelectedOptions.ALBUM_ART_SIZE == GnImageSize.kImageSizeXLarge) {
                    GnContent gnContent = gnResponseAlbums.albums().at(0).next().coverArt();
                    GnImageSize[] values = GnImageSize.values();
                    for (int sizes = values.length - 1; sizes >= 0; --sizes) {
                        String url = gnContent.asset(values[sizes]).url();
                        if (!gnContent.asset(values[sizes]).url().equals("")) {
                            imageUrl = url;
                            break;
                        }
                    }
                }
                //If is selected "De menor calidad disponible"
                else if (SelectedOptions.ALBUM_ART_SIZE == GnImageSize.kImageSizeThumbnail) {
                    GnContent gnContent = gnResponseAlbums.albums().at(0).next().coverArt();
                    GnImageSize[] values = GnImageSize.values();
                    for (int sizes = 0; sizes < values.length ; sizes++) {
                        String url = gnContent.asset(values[sizes]).url();
                        if (!gnContent.asset(values[sizes]).url().equals("")) {
                            imageUrl = url;
                            break;
                        }
                    }
                }
                //get "de baja calidad, de media calidad", "de alta calidad" or "de la mejor calidad"
                else {
                    imageUrl = gnResponseAlbums.albums().at(0).next().coverArt().asset(SelectedOptions.ALBUM_ART_SIZE).url();
                }

            } catch (GnException e) {
                e.printStackTrace();
                imageUrl = "";
            }

            try {
                trackNumber = gnResponseAlbums.albums().at(0).next().trackMatchNumber() + "";
            } catch (GnException e) {
                e.printStackTrace();
                trackNumber = "";
            }
            try {
                year = gnResponseAlbums.albums().at(0).next().year();
            } catch (GnException e) {
                e.printStackTrace();
                year = "";
            }
            try {
                genre = gnResponseAlbums.albums().at(0).next().genre(GnDataLevel.kDataLevel_3);
            } catch (GnException e) {
                e.printStackTrace();
                genre = "";
            }

            /*Log.d("TITLE_BG", newName);
            Log.d("ARTIST_BG", artistName);
            Log.d("ALBUM_BG",albumName);
            Log.d("ALBUM_ART_BG", imageUrl);
            Log.d("NUMBER_BG", trackNumber);
            Log.d("YEAR_BG", year);
            Log.d("GENRE_BG", genre);*/

            //was found song name?
            boolean dataTitle = !newName.equals("");
            //was found artist?
            boolean dataArtist = !artistName.equals("");
            //was found album?
            boolean dataAlbum = !albumName.equals("");
            //was found cover art?
            boolean dataImage = !imageUrl.equals("");
            //was found track number?
            boolean dataTrackNumber = !trackNumber.equals("");
            //was found track year?
            boolean dataYear = !year.equals("");
            //was found genre?
            boolean dataGenre = !genre.equals("");

            //Is it a request from MainActivity?
            if(!fromEditMode) {
                ContentValues contentValues = new ContentValues();
                //lets try to open the file to edit meta tags.
                try {
                    audioFile = AudioFileIO.read(new File(currentAudioItem.getAbsolutePath()));
                    audioHeader = audioFile.getAudioHeader();
                    tag = audioFile.getTag();

                    //SET THE VALUE FOR EVERY TAG

                    if (dataTitle) {
                        //set the value to update our DB
                        contentValues.put(TrackContract.TrackData.TITLE, newName);
                        //set the value to update the item list
                        currentAudioItem.setTitle(newName);
                        //set the value to update the audio file
                        tag.setField(FieldKey.TITLE, newName);
                    }

                    if (dataArtist) {
                        tag.setField(FieldKey.ARTIST, artistName);
                        currentAudioItem.setArtist(artistName);
                        contentValues.put(TrackContract.TrackData.ARTIST, artistName);
                    }

                    if (dataAlbum) {
                        tag.setField(FieldKey.ALBUM, albumName);
                        currentAudioItem.setAlbum(albumName);
                        contentValues.put(TrackContract.TrackData.ALBUM, albumName);
                    }

                    //FOR NEXT TAGS ONLY UPDATE THE AUDIO FILE, NOT ITEM LIST NEITHER OUR DATABASE
                    //BECAUSE WE DON'T STORE THAT VALUES IN OUR DATABASE

                    if (dataImage) {
                        try {
                            byte[] imgData = new GnAssetFetch(GnService.gnUser, imageUrl).data();
                            Artwork artwork = new AndroidArtwork();
                            artwork.setBinaryData(imgData);
                            tag.setField(artwork);
                        }
                        catch (GnException e){
                            e.printStackTrace();
                        }
                     }

                    if (dataTrackNumber) {
                        tag.setField(FieldKey.TRACK, trackNumber);
                    }

                    if (dataYear) {
                        tag.setField(FieldKey.YEAR, year);
                    }

                    if (dataGenre) {
                        tag.setField(FieldKey.GENRE, genre);
                    }

                    //Update the audio file meta tags
                    audioFile.commit();

                    //If some info missed, mark as INCOMPLETE this song
                    if (!dataTitle || !dataArtist || !dataAlbum || !dataImage || !dataTrackNumber || !dataYear || !dataGenre) {
                        contentValues.put(TrackContract.TrackData.STATUS, AudioItem.FILE_STATUS_INCOMPLETE);
                        currentAudioItem.setStatus(AudioItem.FILE_STATUS_INCOMPLETE);
                    }
                    //All info for this song was found!!!
                    else {
                        contentValues.put(TrackContract.TrackData.STATUS, AudioItem.FILE_STATUS_OK);
                        currentAudioItem.setStatus(AudioItem.FILE_STATUS_OK);
                    }

                    if (SelectedOptions.AUTOMATIC_CHANGE_FILENAME) {

                        String newAbsolutePath = AudioItem.renameFile(currentAudioItem.getAbsolutePath(), newName, artistName);

                        //Lets inform to system that one file has change its name
                        ContentValues localContentValues = new ContentValues();
                        localContentValues.put(MediaStore.MediaColumns.DATA, newAbsolutePath);
                        String selection = MediaStore.MediaColumns.DATA + "= ?";
                        String selectionArgs[] = {currentAudioItem.getAbsolutePath()}; //old path
                        boolean successMediaStore = getContentResolver().
                                update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                        localContentValues,
                                        selection,
                                        selectionArgs) == 1;
                        localContentValues.clear();
                        localContentValues = null;

                        currentAudioItem.setAbsolutePath(newAbsolutePath);
                        contentValues.put(TrackContract.TrackData.DATA, newAbsolutePath); //new path
                        Log.d("success renaming", successMediaStore+" and renaming");
                    }

                    //Finally update our database
                    contentValues.put(TrackContract.TrackData.IS_PROCESSING,false);
                    contentValues.put(TrackContract.TrackData.IS_SELECTED, false);
                    int updated = dataTrackDbHelper.updateData(currentAudioItem.getId(), contentValues);

                    //Report to receivers the status
                    Intent intent = new Intent();
                    intent.setAction(ACTION_DONE);
                    intent.putExtra(AUDIO_ITEM,currentAudioItem);
                    localBroadcastManager.sendBroadcastSync(intent);
                    Log.d("DB updated",updated+"");
                    contentValues.clear();
                    contentValues = null;

                }
                catch (CannotWriteException | IOException | InvalidAudioFrameException | TagException | ReadOnlyFileException | CannotReadException e) {
                    e.printStackTrace();

                    currentAudioItem.setStatus(AudioItem.FILE_STATUS_BAD);
                    currentAudioItem.setChecked(false);
                    currentAudioItem.setProcessing(false);

                    contentValues.put(TrackContract.TrackData.STATUS, AudioItem.FILE_STATUS_BAD);
                    dataTrackDbHelper.updateData(currentAudioItem.getId(), contentValues);

                    Intent localIntent = new Intent();
                    localIntent.setAction(ACTION_FAIL);
                    localIntent.putExtra(AUDIO_ITEM,currentAudioItem);
                    localBroadcastManager.sendBroadcastSync(localIntent);
                }
            }
            //if intent was made from TrackDetailsActivity
            //don't modify the track, only retrieve data and
            //send back to activity, the user has to decide
            //if the information is correct or not, besides,
            //for UX, is not convenient to interfere with what the user
            //is doing
            else {

                    if (dataTitle) {
                        currentAudioItem.setTitle(newName);
                    }

                    if (dataArtist) {
                        currentAudioItem.setArtist(artistName);
                    }

                    if (dataAlbum) {
                        currentAudioItem.setAlbum(albumName);
                    }

                    if (dataImage) {
                        try {
                            byte[] imgData = new GnAssetFetch(GnService.gnUser, imageUrl).data();
                            currentAudioItem.setCoverArt(imgData);
                        } catch (GnException e) {
                            e.printStackTrace();
                            currentAudioItem.setCoverArt(null);
                        }

                    }

                    if (dataTrackNumber) {
                        currentAudioItem.setTrackNumber(trackNumber);
                    }

                    if (dataYear) {
                        currentAudioItem.setTrackYear(year);
                    }

                    if (dataGenre) {
                        currentAudioItem.setGenre(genre);
                    }

                    Intent localIntent = new Intent();
                    localIntent.setAction(ACTION_DONE_DETAILS);
                    localIntent.putExtra(AUDIO_ITEM,currentAudioItem);
                    localBroadcastManager.sendBroadcastSync(localIntent);
            }

            clearValues();

        }

        @Override
        public void musicIdFileMatchResult(GnResponseDataMatches gnResponseDataMatches, long l, long l1, IGnCancellable iGnCancellable) {
            //Not used because we selected GnMusicIdFileResponseType.kResponseAlbums as response type
            Log.d("musicIdFileMatchResult", "musicIdFileMatchResult");
        }

        @Override
        public void musicIdFileResultNotFound(GnMusicIdFileInfo gnMusicIdFileInfo, long l, long l1, IGnCancellable iGnCancellable) {
            currentAudioItem.setStatus(AudioItem.FILE_STATUS_BAD);
            currentAudioItem.setChecked(false);
            currentAudioItem.setProcessing(false);
            Intent localIntent = new Intent();
            localIntent.setAction(ACTION_NOT_FOUND);
            localIntent.putExtra(AUDIO_ITEM,currentAudioItem);
            localBroadcastManager.sendBroadcastSync(localIntent);

            if(!fromEditMode) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(TrackContract.TrackData.STATUS, AudioItem.FILE_STATUS_BAD);
                dataTrackDbHelper.updateData(currentAudioItem.getId(), contentValues);
            }


            Log.d("not_found_action",localIntent.getAction());

            try {
                Log.d("Result_Not_Found",gnMusicIdFileInfo.status().name());
            }
            catch (GnException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void musicIdFileComplete(GnError gnError) {
            Log.d("musicIdFileComplete",gnError.errorDescription());
        }

        @Override
        public void statusEvent(GnStatus gnStatus, long l, long l1, long l2, IGnCancellable iGnCancellable) {
            Log.d("gnStatus",gnStatus.name());
        }

        public void clearValues(){
            if(gnMusicIdFile != null)
                gnMusicIdFileList.remove(gnMusicIdFile);

            newName = "";
            artistName = "";
            albumName = "";
            trackNumber = "";
            year = "";
            genre = "";
            imageUrl = "";
            currentId = -1;
            currentAudioItem = null;
        }
    }

    private class MyHandler extends Handler{
        public MyHandler(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(Message msg){
            //int startId = msg.arg1;
            Intent intent = (Intent) msg.obj;

            MainActivity.isProcessingTask = true;
            saveStateProcessing();
            onHandleIntent(intent);

            stopSelf();
            //Log.i(TAG, stopped+" startId: " + startId);
        }


    }


}



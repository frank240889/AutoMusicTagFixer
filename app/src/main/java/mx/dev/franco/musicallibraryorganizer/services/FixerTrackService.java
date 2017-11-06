package mx.dev.franco.musicallibraryorganizer.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
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
import org.jaudiotagger.tag.KeyNotFoundException;
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
import mx.dev.franco.musicallibraryorganizer.Settings;
import mx.dev.franco.musicallibraryorganizer.database.DataTrackDbHelper;
import mx.dev.franco.musicallibraryorganizer.database.TrackContract;
import mx.dev.franco.musicallibraryorganizer.list.AudioItem;
import mx.dev.franco.musicallibraryorganizer.utilities.Constants;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static mx.dev.franco.musicallibraryorganizer.services.GnService.appString;

/**
 * Created by franco on 17/08/17.
 */

public class FixerTrackService extends Service {
    public static String CLASS_NAME = FixerTrackService.class.getName();
    //Initial value, if exist audio item_list, this will override this initial value
    private long mCurrentId = - 1;
    //Necessary objects to interact with Gracenote API
    private GnMusicIdFileInfoManager mGnMusicIdFileInfoManager = null;
    private GnMusicIdFile mGnMusicIdFile = null;
    private MusicIdFileEvents mMusicIdFileEvents = null;
    //Connection to database
    private DataTrackDbHelper mDataTrackDbHelper;
    //Reference o broadcast manager that sends the intents to receivers
    private LocalBroadcastManager mLocalBroadcastManager;
    //A list to maintain references to actual objects being processed in case
    //it needs to cancel the task
    public static List<GnMusicIdFile> sGnMusicIdFileList;

    //set this flag to TRUE if we send this intent from DetailsTrackDialogActitivy class
    private boolean mFromEditMode = false;

    private Cursor mSelectedItems = null;
    private int mNumberSelected = 0;
    private NotificationManager mNM;
    private Notification notification;
    private HandlerThread mThread;
    private Looper mLooper;
    private MyHandler mHandler;
    private boolean finishTaskByUser = true;
    private boolean mStartOrUpdateForegroundNotification = false;
    private boolean mShowNotification = false;


    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public FixerTrackService() {
        super();
        //Log.d(CLASS_NAME, "constructor");

    }

    @Override
    public void onCreate(){
        super.onCreate();
        //Log.d(CLASS_NAME, "onCreate");
        mDataTrackDbHelper = DataTrackDbHelper.getInstance(getApplicationContext());
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);

        //Object to handle callbacks from Gracenote API
        mMusicIdFileEvents = new MusicIdFileEvents();
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        sGnMusicIdFileList = new ArrayList<>();

        mThread = new HandlerThread(CLASS_NAME, THREAD_PRIORITY_BACKGROUND);
        mThread.start();
        mLooper = mThread.getLooper();
        mHandler = new MyHandler(mLooper);
    }


    @Override
    public int onStartCommand(final Intent intent, int flags, int startId){
        super.onStartCommand(intent,flags,startId);
        //Log.d(CLASS_NAME,"onStartCommand");
        new Thread(new Runnable() {
            @Override
            public void run() {
                onHandleIntent(intent);
            }
        }).start();


        /*Message msg = mHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        mHandler.sendMessage(msg);*/
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
        stopTrackId();
        stopForeground(true);
        if(sGnMusicIdFileList != null) {
            sGnMusicIdFileList.remove(mGnMusicIdFile);
            sGnMusicIdFileList.clear();
            sGnMusicIdFileList = null;
        }

        Log.d("onDestroy","releasing resources...");
        super.onDestroy();
    }

    private void startNotification(String msg) {
        Intent notificationIntent = new Intent(this,MainActivity.class);
        notificationIntent.setAction(MainActivity.ACTION_OPEN_MAIN_ACTIVITY);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Intent stopTaskIntent = new Intent(this, FixerTrackService.class);
        stopTaskIntent.setAction(Constants.Actions.ACTION_CANCEL_TASK);
        PendingIntent pendingStopIntent = PendingIntent.getService(this, 0, stopTaskIntent, 0);

        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                R.mipmap.ic_launcher);
        notification = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setAutoCancel(true)
                .setColor(ContextCompat.getColor(getApplicationContext(),R.color.grey_800))
                .setTicker(getString(R.string.fixing_task))
                .setContentText(msg != null ? msg : getString(R.string.fixing_task))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .addAction(R.drawable.ic_help_black_24dp,"Detener", pendingStopIntent)
                .build();

        startForeground(R.string.app_name,notification);

    }

    private void onHandleIntent(Intent intent){


        /*if(intent.getAction() != null && intent.getAction().equals(Constants.Actions.ACTION_CANCEL_TASK)){
            stopSelf();
            return;
        }*/

        //if intent comes from touch an item, or from TrackDetailsActivity
        //if not, default value is -1
        mCurrentId = intent.getLongExtra(Constants.MEDIASTORE_ID, -1);
        //get selected items directly from DB, if item is not equal than -1,
        //it means that cursor returned of this query will contain only
        //one row, if mCurrentId is equal than -1, so will get
        //a cursor with more than one row
        mSelectedItems = mDataTrackDbHelper.getAllSelected(mCurrentId);
        mNumberSelected = mSelectedItems == null ? 0 : mSelectedItems.getCount();
        //if intent comes from TrackDetailsActivity means that
        //mFromEditMode will be true, default value is false
        mFromEditMode = intent.getBooleanExtra(Constants.Activities.FROM_EDIT_MODE, false);
        mShowNotification = intent.getBooleanExtra(Constants.Actions.ACTION_SHOW_NOTIFICATION,false);
        mStartOrUpdateForegroundNotification = !mFromEditMode && mShowNotification;

        if(mStartOrUpdateForegroundNotification){
            startNotification(null);
        }
        createGnFiles();
    }

    private void createGnFiles(){
        long id = -1;
        String title = "";
        String artist = "";
        String album = "";
        String fullPath = "";

        //track id objects provide identification services
        try {
            mGnMusicIdFile = new GnMusicIdFile(GnService.gnUser, mMusicIdFileEvents);
            mGnMusicIdFile.options().lookupData(GnLookupData.kLookupDataContent, true);
            mGnMusicIdFile.options().batchSize(1);
            mGnMusicIdFileInfoManager = mGnMusicIdFile.fileInfos();
            sGnMusicIdFileList.add(mGnMusicIdFile);

        } catch (GnException e) {
            e.printStackTrace();
        }

        //create and add files to a queue for processing them
        while (mSelectedItems.moveToNext()){
            id = mSelectedItems.getLong(mSelectedItems.getColumnIndexOrThrow(TrackContract.TrackData.MEDIASTORE_ID));
            title = mSelectedItems.getString(mSelectedItems.getColumnIndexOrThrow(TrackContract.TrackData.TITLE));
            artist = mSelectedItems.getString(mSelectedItems.getColumnIndexOrThrow(TrackContract.TrackData.ARTIST));
            album = mSelectedItems.getString(mSelectedItems.getColumnIndexOrThrow(TrackContract.TrackData.ALBUM));
            fullPath = mSelectedItems.getString(mSelectedItems.getColumnIndexOrThrow(TrackContract.TrackData.DATA));

            try {
                //Log.d("file",fullPath);
                GnMusicIdFileInfo gnMusicIdFileInfo = mGnMusicIdFileInfoManager.add(fullPath);
                gnMusicIdFileInfo.fileName(fullPath);
                gnMusicIdFileInfo.mediaId(String.valueOf(id));
                gnMusicIdFileInfo.trackTitle(title);
                gnMusicIdFileInfo.trackArtist(artist);
                gnMusicIdFileInfo.albumTitle(album);

            } catch (GnException e) {
                e.printStackTrace();
            }
            finally {
                id = -1;
                title = "";
                artist = "";
                album = "";
                fullPath = "";
            }

        }
        mSelectedItems.close();
        mSelectedItems = null;

        startTrackId();
    }

    public void startTrackId(){
        //Log.d("starting track id", "staring");
        try {
            mGnMusicIdFile.doTrackId(GnMusicIdFileProcessType.kQueryReturnSingle,GnMusicIdFileResponseType.kResponseAlbums);
        } catch (GnException e) {
            e.printStackTrace();
        }
    }

    public void stopTrackId(){
        //check if task were cancelled by user,
        //default value is true, in case that
        //ACTION_COMPLETE_TASK be reached, this value will
        //be false, meaning that is not
        //necessary cancel anything.
        if(finishTaskByUser){
            if(sGnMusicIdFileList != null) {
                Iterator<GnMusicIdFile> iterator = sGnMusicIdFileList.iterator();
                while (iterator.hasNext()) {
                    Log.d("cancel gn", "cancelling...");
                    iterator.next().cancel();
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Instances of this class
     * handles the response of doTrackId method
     */


    private class MusicIdFileEvents implements IGnMusicIdFileEvents {
        private HashMap<String,String> mGnStatusToDisplay;
        private String mPath = "";
        private long mId = -1;
        private AudioFile mAudioFile = null;
        private Tag mTag = null;
        private boolean mLastFile = false;
        private AudioHeader mAudioHeader = null;

        public MusicIdFileEvents(){
            //Put the status of downloaded info
            mGnStatusToDisplay = new HashMap<>();
            mGnStatusToDisplay.put("kMusicIdFileCallbackStatusProcessingBegin",Constants.State.BEGIN_PROCESSING_MSG);
            mGnStatusToDisplay.put("kMusicIdFileCallbackStatusFileInfoQuery",Constants.State.QUERYING_INFO_MSG);
            mGnStatusToDisplay.put("kMusicIdFileCallbackStatusProcessingComplete",Constants.State.COMPLETE_IDENTIFICATION_MSG);
            mGnStatusToDisplay.put("kMusicIdFileCallbackStatusError",Constants.State.STATUS_ERROR_MSG);
            mGnStatusToDisplay.put("kMusicIdFileCallbackStatusProcessingError",Constants.State.STATUS_PROCESSING_ERROR_MSG);
        }

        @Override
        public void musicIdFileStatusEvent(GnMusicIdFileInfo gnMusicIdFileInfo, GnMusicIdFileCallbackStatus gnMusicIdFileCallbackStatus, long currentFile, long totalFiles, IGnCancellable iGnCancellable) {
            String status = gnMusicIdFileCallbackStatus.toString();
            mLastFile = currentFile == totalFiles;
            if(mStartOrUpdateForegroundNotification) {
                startNotification("Corrigiendo " + currentFile + " canción de " + totalFiles);
            }
            Log.d("CallbackStatus", status);
            if(iGnCancellable.isCancelled()){
                onCancelTask(gnMusicIdFileInfo);
                return;
            }

            if (mGnStatusToDisplay.containsKey(status)) {
                if(status.equals("kMusicIdFileCallbackStatusProcessingBegin")){
                    try {
                        mPath = gnMusicIdFileInfo.fileName();
                        mId = Long.parseLong(gnMusicIdFileInfo.mediaId());
                        //update UI to show progress bar in current item list only in Main activity
                        if (!mFromEditMode){
                            Intent intentUpdateUI = new Intent();
                            intentUpdateUI.setAction(Constants.Actions.ACTION_SET_AUDIOITEM_PROCESSING);
                            intentUpdateUI.putExtra(Constants.MEDIASTORE_ID, mId);
                            mLocalBroadcastManager.sendBroadcastSync(intentUpdateUI);
                            ContentValues processingValue = new ContentValues();
                            processingValue.put(TrackContract.TrackData.IS_PROCESSING, true);
                            mDataTrackDbHelper.updateData(mId, processingValue);
                        }
                    }catch (GnException e){
                        e.printStackTrace();
                    }
                }
                else if(status.equals("kMusicIdFileCallbackStatusError") || status.equals("kMusicIdFileCallbackStatusProcessingError")){
                    Log.d("error while processing","try again");
                    onCancelTask(gnMusicIdFileInfo);
                }


            }


        }
            @Override
        public void gatherFingerprint(GnMusicIdFileInfo fileInfo, long l, long l1, IGnCancellable iGnCancellable) {
            Log.d("gatherFingerprint", "gatherFingerprint");
            //Callback to inform that fingerprint was retrieved from audiotrack.
                try {
                    if (GnAudioFile.isFileFormatSupported(fileInfo.fileName())) {
                        fileInfo.fingerprintFromSource(new GnAudioFile(new File(fileInfo.fileName())));
                    }

                }
                catch (GnException e) {
                    if (!GnError.isErrorEqual(e.errorCode(), GnError.GNSDKERR_Aborted)) {
                        Log.e(appString, "error in fingerprinting file: " + e.errorAPI() + ", " + e.errorModule() + ", " + e.errorDescription());

                    }
                }

        }
        //This method is not need because we provide some metadata before
        @Override
        public void gatherMetadata(GnMusicIdFileInfo gnMusicIdFileInfo, long currentFile, long totalFiles, IGnCancellable iGnCancellable) {
            Log.d("gatherMetadata", "gatherMetadata");
        }

        @Override
        public void musicIdFileAlbumResult(GnResponseAlbums gnResponseAlbums, long currentAlbum, long totalAlbums, IGnCancellable iGnCancellable) {
            String title = "";
            String artist = "";
            String album = "";
            String cover = "";
            String number = "";
            String year = "";
            String genre = "";

            try {
                title = gnResponseAlbums.albums().at(0).next().trackMatched().title().display();
            } catch (GnException e) {
                e.printStackTrace();
                title = "";
            }

            try {
                if(!gnResponseAlbums.albums().at(0).next().trackMatched().artist().name().display().isEmpty())
                    artist = gnResponseAlbums.albums().at(0).next().trackMatched().artist().name().display();
                else
                    artist = gnResponseAlbums.albums().at(0).next().artist().name().display();

            } catch (GnException e) {
                e.printStackTrace();
                artist = "";
            }
            try {
                album = gnResponseAlbums.albums().at(0).next().title().display();
            } catch (GnException e) {
                e.printStackTrace();
                album = "";
            }

            try {
                //If is selected "No descargar imagen"
                //don't retrieve the url from the cover
                if (Settings.SETTING_SIZE_ALBUM_ART == null) {
                    cover = "";
                }
                //If is selected "De mejor calidad disponible"
                //iterate from higher to lower quality and select the first higher found.
                else if (Settings.SETTING_SIZE_ALBUM_ART == GnImageSize.kImageSizeXLarge) {
                    GnContent gnContent = gnResponseAlbums.albums().at(0).next().coverArt();
                    GnImageSize[] values = GnImageSize.values();
                    for (int sizes = values.length - 1; sizes >= 0; --sizes) {
                        String url = gnContent.asset(values[sizes]).url();
                        if (!gnContent.asset(values[sizes]).url().equals("")) {
                            cover = url;
                            break;
                        }
                    }
                }
                //If is selected "De menor calidad disponible"
                //iterate from lower to higher quality and select the first lower found.
                else if (Settings.SETTING_SIZE_ALBUM_ART == GnImageSize.kImageSizeThumbnail) {
                    GnContent gnContent = gnResponseAlbums.albums().at(0).next().coverArt();
                    GnImageSize[] values = GnImageSize.values();
                    for (int sizes = 0; sizes < values.length ; sizes++) {
                        String url = gnContent.asset(values[sizes]).url();
                        if (!gnContent.asset(values[sizes]).url().equals("")) {
                            cover = url;
                            break;
                        }
                    }
                }
                //get the first found in any of those predefined sizes:
                //"de baja calidad", "de media calidad", "de alta calidad", "de muy alta calidad"
                else {
                    GnContent gnContent = gnResponseAlbums.albums().at(0).next().coverArt();
                    cover = gnContent.asset(Settings.SETTING_SIZE_ALBUM_ART).url();
                }

            } catch (GnException e) {
                e.printStackTrace();
                cover = "";
            }

            try {
                number = gnResponseAlbums.albums().at(0).next().trackMatchNumber() + "";
            } catch (GnException e) {
                e.printStackTrace();
                number = "";
            }
            try {
                if(!gnResponseAlbums.albums().at(0).next().trackMatched().year().isEmpty())
                    year = gnResponseAlbums.albums().at(0).next().trackMatched().year();
                else
                    year = gnResponseAlbums.albums().at(0).next().year();
            } catch (GnException e) {
                e.printStackTrace();
                year = "";
            }
            try {
                //search for first genre of or related for current song
                if(!gnResponseAlbums.albums().at(0).next().trackMatched().genre(GnDataLevel.kDataLevel_3).isEmpty())
                    genre = gnResponseAlbums.albums().at(0).next().trackMatched().genre(GnDataLevel.kDataLevel_3);
                else if(!gnResponseAlbums.albums().at(0).next().trackMatched().genre(GnDataLevel.kDataLevel_2).isEmpty())
                    genre = gnResponseAlbums.albums().at(0).next().trackMatched().genre(GnDataLevel.kDataLevel_2);
                else if(!gnResponseAlbums.albums().at(0).next().trackMatched().genre(GnDataLevel.kDataLevel_1).isEmpty())
                    genre = gnResponseAlbums.albums().at(0).next().trackMatched().genre(GnDataLevel.kDataLevel_1);
                else if(!gnResponseAlbums.albums().at(0).next().genre(GnDataLevel.kDataLevel_3).isEmpty())
                    genre = gnResponseAlbums.albums().at(0).next().genre(GnDataLevel.kDataLevel_3);
                else if(!gnResponseAlbums.albums().at(0).next().genre(GnDataLevel.kDataLevel_2).isEmpty())
                    genre = gnResponseAlbums.albums().at(0).next().genre(GnDataLevel.kDataLevel_2);
                else if(!gnResponseAlbums.albums().at(0).next().genre(GnDataLevel.kDataLevel_1).isEmpty())
                    genre = gnResponseAlbums.albums().at(0).next().genre(GnDataLevel.kDataLevel_1);
            } catch (GnException e) {
                e.printStackTrace();
                genre = "";
            }

            Log.d("TITLE_BG", title);
            Log.d("ARTIST_BG", artist);
            Log.d("ALBUM_BG", album);
            Log.d("ALBUM_ART_BG", cover);
            Log.d("NUMBER_BG", number);
            Log.d("YEAR_BG", year);
            Log.d("GENRE_BG", genre);

            setNewAudioTags(title, artist, album, cover, number, year, genre);
        }

        private void setNewAudioTags(String... tags){

            //was found song name?
            boolean dataTitle = !tags[0].equals("");
            //was found artist?
            boolean dataArtist = !tags[1].equals("");
            //was found album?
            boolean dataAlbum = !tags[2].equals("");
            //was found cover art?
            boolean dataImage = !tags[3].equals("");
            //was found track number?
            boolean dataTrackNumber = !tags[4].equals("");
            //was found track mNewYear?
            boolean dataYear = !tags[5].equals("");
            //was found mNewGenre?
            boolean dataGenre = !tags[6].equals("");

            //Is it a request from MainActivity?

            if(!mFromEditMode) {
                ContentValues newTags = new ContentValues();
                AudioItem audioItem = new AudioItem();
                //lets try to open the file to edit meta tags.
                try {
                    mAudioFile = AudioFileIO.read(new File(mPath));

                    audioItem.setAbsolutePath(mPath);
                    audioItem.setTitle(tags[0]);
                    audioItem.setArtist(tags[1]);
                    audioItem.setAlbum(tags[2]);
                    audioItem.setId(mId);
                    mAudioHeader = mAudioFile.getAudioHeader();
                    mTag = mAudioFile.getTag();

                    //SET THE VALUE FOR EVERY CLASS_NAME

                    if (dataTitle) {
                        //set the value to update our DB
                        newTags.put(TrackContract.TrackData.TITLE, tags[0]);
                        //set the value to update the item list
                        audioItem.setTitle(tags[0]);
                        //set the value to update the audio file
                        mTag.setField(FieldKey.TITLE, tags[0]);
                    }

                    if (dataArtist) {
                        newTags.put(TrackContract.TrackData.ARTIST, tags[1]);
                        audioItem.setArtist(tags[1]);
                        mTag.setField(FieldKey.ARTIST, tags[1]);
                    }

                    if (dataAlbum) {
                        newTags.put(TrackContract.TrackData.ALBUM, tags[2]);
                        audioItem.setAlbum(tags[2]);
                        mTag.setField(FieldKey.ALBUM, tags[2]);
                    }
                    Log.d("fromeditmode",tags[3]);
                    //FOR NEXT TAGS ONLY UPDATE THE AUDIO FILE, NOT ITEM LIST NEITHER OUR DATABASE
                    //BECAUSE WE DON'T STORE THOSE VALUES

                    if (dataImage) {
                        try {
                            byte[] imgData = new GnAssetFetch(GnService.gnUser, tags[3]).data();
                            Artwork artwork = new AndroidArtwork();
                            artwork.setBinaryData(imgData);
                            //if(mTag.getFirstArtwork() != null)
                                //mTag.deleteArtworkField();
                            //mTag.setField(artwork);
                        }
                        catch (GnException | KeyNotFoundException e){
                            e.printStackTrace();
                        }
                    }

                    if (dataTrackNumber) {
                        mTag.setField(FieldKey.TRACK, tags[4]);
                    }

                    if (dataYear) {
                        mTag.setField(FieldKey.YEAR, tags[5]);
                    }

                    if (dataGenre) {
                        mTag.setField(FieldKey.GENRE, tags[6]);
                    }

                    //Update the audio file meta tags
                    mAudioFile.commit();

                    //If some info is missed, mark as INCOMPLETE this song
                    if (!dataTitle || !dataArtist || !dataAlbum || !dataImage || !dataTrackNumber || !dataYear || !dataGenre) {
                        newTags.put(TrackContract.TrackData.STATUS, AudioItem.FILE_STATUS_INCOMPLETE);
                        audioItem.setStatus(AudioItem.FILE_STATUS_INCOMPLETE);
                    }
                    //All info for this song was found!!!
                    else {
                        newTags.put(TrackContract.TrackData.STATUS, AudioItem.FILE_STATUS_OK);
                        audioItem.setStatus(AudioItem.FILE_STATUS_OK);
                    }

                    //Rename file if is activated in settings
                    if (Settings.SETTING_RENAME_FILE_AUTOMATIC_MODE) {
                        String newAbsolutePath = AudioItem.renameFile(audioItem.getAbsolutePath(), tags[0], tags[1]);

                        //Lets inform to system that one file has change its name
                        ContentValues newValuesToMediaStore = new ContentValues();
                        newValuesToMediaStore.put(MediaStore.MediaColumns.DATA, newAbsolutePath);
                        String selection = MediaStore.MediaColumns.DATA + "= ?";
                        String selectionArgs[] = {audioItem.getAbsolutePath()}; //old path
                        boolean successMediaStore = getContentResolver().
                                update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                        newValuesToMediaStore,
                                        selection,
                                        selectionArgs) == 1;
                        newValuesToMediaStore.clear();
                        newValuesToMediaStore = null;

                        audioItem.setAbsolutePath(newAbsolutePath);
                        newTags.put(TrackContract.TrackData.DATA, newAbsolutePath); //new path
                        Log.d("success renaming", successMediaStore+" and renaming");
                    }
                    Log.d("mDataTrackDbHelper",(mDataTrackDbHelper == null)+"");
                    //Finally update our database
                    newTags.put(TrackContract.TrackData.IS_SELECTED, false);
                    newTags.put(TrackContract.TrackData.IS_PROCESSING, false);
                    mDataTrackDbHelper.updateData(audioItem.getId(), newTags);

                    //Report to receivers the status
                    Intent intentActionDone = new Intent();
                    intentActionDone.setAction(Constants.Actions.ACTION_DONE);
                    intentActionDone.putExtra(Constants.AUDIO_ITEM, audioItem);
                    mLocalBroadcastManager.sendBroadcastSync(intentActionDone);

                }
                catch (CannotWriteException | IOException | InvalidAudioFrameException | TagException | ReadOnlyFileException | CannotReadException e) {
                    e.printStackTrace();
                    audioItem.setStatus(AudioItem.FILE_ERROR_READ);
                    newTags.put(TrackContract.TrackData.STATUS, AudioItem.FILE_ERROR_READ);
                    newTags.put(TrackContract.TrackData.IS_SELECTED, false);
                    newTags.put(TrackContract.TrackData.IS_PROCESSING, false);

                    mDataTrackDbHelper.updateData(audioItem.getId(), newTags);

                    Intent intentActionDone = new Intent();
                    intentActionDone.setAction(Constants.Actions.ACTION_FAIL);
                    intentActionDone.putExtra(Constants.AUDIO_ITEM, audioItem);
                    mLocalBroadcastManager.sendBroadcastSync(intentActionDone);
                }
                newTags.clear();
                newTags = null;
            }
            //if intent was made from TrackDetailsActivity
            //don't modify the track, only retrieve data and
            //send back to activity, the user has to decide
            //if apply the found tags, besides,
            //for UX, is not convenient to interfere with what the user
            //is doing
            else {
                AudioItem audioItem = new AudioItem();
                audioItem.setId(mId);
                //Wrap the found new tags(if are availables) in an AudioItem object.
                if (dataTitle) {
                    audioItem.setTitle(tags[0]);
                }

                if (dataArtist) {
                    audioItem.setArtist(tags[1]);
                }

                if (dataAlbum) {
                    audioItem.setAlbum(tags[2]);
                }

                if (dataImage) {
                    try {
                        byte[] imgData = new GnAssetFetch(GnService.gnUser, tags[3]).data();
                        audioItem.setCoverArt(imgData);
                    } catch (GnException e) {
                        e.printStackTrace();
                    }

                }

                if (dataTrackNumber) {
                    audioItem.setTrackNumber(tags[4]);
                }

                if (dataYear) {
                    audioItem.setTrackYear(tags[5]);
                }

                if (dataGenre) {
                    audioItem.setGenre(tags[6]);
                }

                //send back the results
                Intent intentActionDoneDetails = new Intent();
                intentActionDoneDetails.setAction(Constants.Actions.ACTION_DONE_DETAILS);
                intentActionDoneDetails.putExtra(Constants.AUDIO_ITEM, audioItem);
                mLocalBroadcastManager.sendBroadcastSync(intentActionDoneDetails);

            }
            //if is the last file stop the service
            if(mLastFile){
                //Inform only to UI of MainActivity that task has completed;
                //if intent was made from TrackDetailsActivity is not necessary inform this action
                if(!mFromEditMode){
                    Intent intentCompleteTask = new Intent();
                    intentCompleteTask.setAction(Constants.Actions.ACTION_COMPLETE_TASK);
                    mLocalBroadcastManager.sendBroadcastSync(intentCompleteTask);
                    Log.d("action_sent",intentCompleteTask.getAction());
                }
                finishTaskByUser = false;
                stopSelf();
            }

            clearValues();
        }

        @Override
        public void musicIdFileMatchResult(GnResponseDataMatches gnResponseDataMatches, long l, long l1, IGnCancellable iGnCancellable) {
            //Not used because we selected GnMusicIdFileResponseType.kResponseAlbums as response type
            Log.d("musicIdFileMatchResult", "musicIdFileMatchResult");

            String title;
            String artist;
            String album;
            String cover = "";
            String number;
            String year;
            String genre;

            try {
                title = gnResponseDataMatches.dataMatches().at(0).next().getAsAlbum().trackMatched().title().display();
            } catch (GnException e) {
                e.printStackTrace();
                title = "";
            }

            try {
                artist = gnResponseDataMatches.dataMatches().at(0).next().getAsAlbum().trackMatched().artist().name().display();
            } catch (GnException e) {
                e.printStackTrace();
                artist = "";
            }
            try {
                album = gnResponseDataMatches.dataMatches().at(0).next().getAsAlbum().title().display();
            } catch (GnException e) {
                e.printStackTrace();
                album = "";
            }

            try {
                //If is selected "No descargar imagen"
                if (Settings.SETTING_SIZE_ALBUM_ART == null) {
                    cover = "";
                }
                //If is selected "De mejor calidad disponible"
                else if (Settings.SETTING_SIZE_ALBUM_ART == GnImageSize.kImageSizeXLarge) {
                    GnContent gnContent = gnResponseDataMatches.dataMatches().at(0).next().getAsAlbum().coverArt();
                    GnImageSize[] values = GnImageSize.values();
                    for (int sizes = values.length - 1; sizes >= 0; --sizes) {
                        String url = gnContent.asset(values[sizes]).url();
                        if (!gnContent.asset(values[sizes]).url().equals("")) {
                            cover = url;
                            break;
                        }
                    }
                }
                //If is selected "De menor calidad disponible"
                else if (Settings.SETTING_SIZE_ALBUM_ART == GnImageSize.kImageSizeThumbnail) {
                    GnContent gnContent = gnResponseDataMatches.dataMatches().at(0).next().getAsAlbum().coverArt();
                    GnImageSize[] values = GnImageSize.values();
                    for (int sizes = 0; sizes < values.length ; sizes++) {
                        String url = gnContent.asset(values[sizes]).url();
                        if (!gnContent.asset(values[sizes]).url().equals("")) {
                            cover = url;
                            break;
                        }
                    }
                }
                //get "de baja calidad, de media calidad", "de alta calidad" or "de la mejor calidad"
                else {
                    cover = gnResponseDataMatches.dataMatches().at(0).next().getAsAlbum().coverArt().asset(Settings.SETTING_SIZE_ALBUM_ART).url();
                }

            } catch (GnException e) {
                e.printStackTrace();
                cover = "";
            }

            try {
                number = gnResponseDataMatches.dataMatches().at(0).next().getAsAlbum().trackMatched().trackNumber();
            } catch (GnException e) {
                e.printStackTrace();
                number = "";
            }
            try {
                year = gnResponseDataMatches.dataMatches().at(0).next().getAsAlbum().trackMatched().year();
            } catch (GnException e) {
                e.printStackTrace();
                year = "";
            }
            try {
                genre = gnResponseDataMatches.dataMatches().at(0).next().getAsAlbum().genre(GnDataLevel.kDataLevel_3);
            } catch (GnException e) {
                e.printStackTrace();
                genre = "";
            }

            /*Log.d("TITLE_BG", mNewTitle);
            Log.d("ARTIST_BG", mNewArtist);
            Log.d("ALBUM_BG", mNewAlbum);
            Log.d("ALBUM_ART_BG", mNewCover);
            Log.d("NUMBER_BG", mNewTrackNumber);
            Log.d("YEAR_BG", mNewYear);
            Log.d("GENRE_BG", mNewGenre);*/

            setNewAudioTags(title, artist, album, cover, number, year, genre);


        }

        @Override
        public void musicIdFileResultNotFound(GnMusicIdFileInfo gnMusicIdFileInfo, long l, long l1, IGnCancellable iGnCancellable) {
            //this callback is triggered when no results, instead of musicIdFileAlbumResult or musicIdFileMatchResult

            try {
                Log.d("Result_Not_Found",gnMusicIdFileInfo.status().name());
            } catch (GnException e) {
                e.printStackTrace();
            }

            //check first is user has not cancelled the task
            if(iGnCancellable.isCancelled()){
                onCancelTask(gnMusicIdFileInfo);
                return;
            }


            if(mFromEditMode){
                Intent intentActionNotFound = new Intent();
                intentActionNotFound.setAction(Constants.Actions.ACTION_NOT_FOUND);
                mLocalBroadcastManager.sendBroadcastSync(intentActionNotFound);
            }
            else {
                ContentValues contentValues = new ContentValues();
                contentValues.put(TrackContract.TrackData.STATUS, AudioItem.FILE_STATUS_BAD);
                contentValues.put(TrackContract.TrackData.IS_PROCESSING, false);
                contentValues.put(TrackContract.TrackData.IS_SELECTED, false);
                Intent intentActionNotFound = new Intent();
                intentActionNotFound.setAction(Constants.Actions.ACTION_NOT_FOUND);
                intentActionNotFound.putExtra(Constants.MEDIASTORE_ID, mId);
                Log.d("media store not found",mId+"");
                mDataTrackDbHelper.updateData(mId, contentValues);
                mLocalBroadcastManager.sendBroadcastSync(intentActionNotFound);
            }


            Log.d("mLastFile mFromEditMode",mLastFile + "_" + mFromEditMode );
            if(mLastFile){
                //is last file? inform it to UI when user is on MainActivity only
                if(!mFromEditMode) {
                    finishTaskByUser = false;
                    Intent intentCompleteTask = new Intent();
                    intentCompleteTask.setAction(Constants.Actions.ACTION_COMPLETE_TASK);
                    mLocalBroadcastManager.sendBroadcastSync(intentCompleteTask);
                }

                stopSelf();
            }

            clearValues();
        }

        @Override
        public void musicIdFileComplete(GnError gnError) {
            Log.d("musicIdFileComplete","complete");
        }

        @Override
        public void statusEvent(GnStatus gnStatus, long percentComplete, long bytesTotalSent, long bytesTotalReceived, IGnCancellable iGnCancellable) {
            Log.d("gnStatus","gnStatus");
        }

        private void onCancelTask(GnMusicIdFileInfo fileInfo){
            //if(!mFromEditMode) {
                try {
                    Intent intentActionDone = new Intent();
                    intentActionDone.putExtra(Constants.MEDIASTORE_ID, Long.parseLong(fileInfo.mediaId()));
                    intentActionDone.setAction(Constants.Actions.ACTION_CANCEL_TASK);
                    boolean sent = mLocalBroadcastManager.sendBroadcast(intentActionDone);
                    Log.d("was_Sent",sent+"");
                } catch (GnException e1) {
                    e1.printStackTrace();
                }
            //}
            stopSelf();
        }




        public void clearValues(){
            mId= -1;
            mAudioFile = null;
            mTag = null;
            mAudioHeader = null;
        }
    }

    private class MyHandler extends Handler{
        public MyHandler(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(Message msg){
            //Log.d("message_received","received");
            int startId = msg.arg1;
            Intent intent = (Intent) msg.obj;
            onHandleIntent(intent);
            Log.i(CLASS_NAME, "startId: " + startId);
        }
    }


}



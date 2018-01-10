package mx.dev.franco.automusictagfixer.services;

import android.app.Notification;
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
import android.os.Process;
import android.preference.PreferenceManager;
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
import com.gracenote.gnsdk.GnLanguage;
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
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.id3.ID3v24Tag;
import org.jaudiotagger.tag.images.AndroidArtwork;
import org.jaudiotagger.tag.images.Artwork;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import mx.dev.franco.automusictagfixer.MainActivity;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.database.DataTrackDbHelper;
import mx.dev.franco.automusictagfixer.database.TrackContract;
import mx.dev.franco.automusictagfixer.list.AudioItem;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.Settings;

import static mx.dev.franco.automusictagfixer.services.GnService.sAppString;

/**
 * Created by franco on 17/08/17.
 */

public class FixerTrackService extends Service implements IGnMusicIdFileEvents {
    public static String CLASS_NAME = FixerTrackService.class.getName();
    //Initial value, if exist audio item_list, this will override this initial value
    private long mCurrentId = - 1;
    //Necessary objects to interact with Gracenote API
    private GnMusicIdFileInfoManager mGnMusicIdFileInfoManager = null;
    private GnMusicIdFile mGnMusicIdFile = null;
    //Connection to database
    private DataTrackDbHelper mDataTrackDbHelper;
    //Reference o broadcast manager that sends the intents to receivers
    private LocalBroadcastManager mLocalBroadcastManager;
    //A list to maintain references to actual objects being processed in case
    //it needs to cancel the task
    private List<GnMusicIdFile> mGnMusicIdFileList;

    //set this flag to TRUE if we send this intent from DetailsTrackDialogActitivy class
    private boolean mFromEditMode = false;

    //Cursor that stores the result of making a query
    private Cursor mSelectedItems = null;
    //Number of elements in cursor
    private int mNumberSelected = 0;
    //Notification on status bar
    private Notification notification;
    //indicates if user has cancel or not current correction task
    private static volatile boolean sFinishTaskByUser = true;
    //save value extracted from intent, and indicates
    //it should put this service in foreground or not
    private boolean mShowNotification = false;
    //show notification only when request comes from MainActivity
    private boolean mStartOrUpdateForegroundNotification = false;
    private String mErrorMessage = null;

    private HashMap<String,String> mGnStatusToDisplay;
    private String mPath = "";
    private long mId = -1;
    private boolean mLastFile = false;
    private HandlerThread mThread;
    private Looper mLooper;
    private MyHandler mHandler;
    private volatile int mStopService = Constants.StopsReasons.CONTINUE_TASK;


    /**
     * Creates a Service.  Invoked by your subclass's constructor.
     */
    public FixerTrackService() {
        super();
        Log.d(CLASS_NAME, "Constructor");
    }

    @Override
    public void onCreate(){
        super.onCreate();
        //Log.d(CLASS_NAME, "onCreate");
        mDataTrackDbHelper = DataTrackDbHelper.getInstance(getApplicationContext());
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());

        mGnMusicIdFileList = new ArrayList<>();

        //Create the HandlerThread to handle the correction task in another thread and
        //give it low priority
        mThread = new HandlerThread(CLASS_NAME, Process.THREAD_PRIORITY_BACKGROUND);
        mThread.start();
        mLooper = mThread.getLooper();
        mHandler = new MyHandler(mLooper,this);

        //These status are name events received from Gracenote server to
        //report status of track id task
        mGnStatusToDisplay = new HashMap<>();
        mGnStatusToDisplay.put(Constants.State.BEGIN_PROCESSING,Constants.State.BEGIN_PROCESSING_MSG);
        mGnStatusToDisplay.put(Constants.State.QUERYING_INFO,Constants.State.QUERYING_INFO_MSG);
        mGnStatusToDisplay.put(Constants.State.COMPLETE_IDENTIFICATION,Constants.State.COMPLETE_IDENTIFICATION_MSG);
        mGnStatusToDisplay.put(Constants.State.STATUS_ERROR,Constants.State.STATUS_ERROR_MSG);
        mGnStatusToDisplay.put(Constants.State.STATUS_PROCESSING_ERROR,Constants.State.STATUS_PROCESSING_ERROR_MSG);
        Settings.SETTING_OVERWRITE_ALL_TAGS_AUTOMATIC_MODE = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("key_overwrite_all_tags_automatic_mode", true);
    }

    /**
     * This callback runs when service starts to running
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(final Intent intent, int flags, int startId){
        super.onStartCommand(intent,flags,startId);
        //Log.d(CLASS_NAME,"onStartCommand");

        //check if stop request was received, default is continue task
        if(intent.getAction() != null && intent.getAction().equals(Constants.Actions.ACTION_STOP_SERVICE)) {
            mStopService = intent.getIntExtra(Constants.Actions.ACTION_STOP_SERVICE, Constants.StopsReasons.CONTINUE_TASK);
            //Log.d("mStopService", mStopService + "");
        }


        //Service is stopped from notification "Detener" button
        if(mStopService != Constants.StopsReasons.CONTINUE_TASK){
            stopSelf();
            return START_NOT_STICKY;
        }

        //if value is true, then the service will be able to run in background,
        //and a correction won't stop when app closes, but when you explicitly
        //stop the task by pressing stop button or task finishes
        mShowNotification = intent.getBooleanExtra(Constants.Actions.ACTION_SHOW_NOTIFICATION,false);

        //show notification only when request comes from MainActivity
        mStartOrUpdateForegroundNotification = !mFromEditMode && mShowNotification;

        //starts or update notification progress
        if(mStartOrUpdateForegroundNotification){
            startNotification(null, null, null);
        }
        else {
            stopForeground(true);
        }



        Message msg = mHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        mHandler.sendMessage(msg);
        //Don't  re start the service if system kills it
        return START_NOT_STICKY;
    }

    /**
     * This callback is called when service is binded
     * to an activity
     * @param intent Intent object
     * @return
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        //return null if is not bind service
        return null;
    }

    /**
     * Last callback received when this service is destroyed
     */
    @Override
    public void onDestroy(){
        //cancel current track id if is executing
        stopTrackId();
        //remove this service from foreground and close the notification
        stopForeground(true);
        //clear queue of GnMusicIdFiles were processed
        if(mGnMusicIdFileList != null) {
            mGnMusicIdFileList.remove(mGnMusicIdFile);
            mGnMusicIdFileList.clear();
            mGnMusicIdFileList = null;
        }

        mThread = null;

        //remove pending messages, means that intents
        //sent while was processing will be cancelled
        Message msg = mHandler.obtainMessage();
        mHandler.removeMessages(msg.what, msg.obj);
        mHandler = null;

        System.gc();
        Log.d("onDestroy","releasing resources...");
        super.onDestroy();
    }

    /**
     * Starts a notification and set
     * this service as foreground service,
     * allowing run no matter if app closes
     * @param contentText the content text o notification
     * @param contentTitle the title of notification
     * @param status the status string to show in notification
     */
    private void startNotification(String contentText, String contentTitle, String status) {

        Intent notificationIntent = new Intent(this,MainActivity.class);
        notificationIntent.setAction(Constants.ACTION_OPEN_MAIN_ACTIVITY);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Intent stopTaskIntent = new Intent(this, FixerTrackService.class);
        stopTaskIntent.setAction(Constants.Actions.ACTION_CANCEL_TASK);
        PendingIntent pendingStopIntent = PendingIntent.getService(this, 0, stopTaskIntent, 0);

        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                R.mipmap.ic_launcher);
        notification = new NotificationCompat.Builder(this)
                .setContentTitle(contentTitle != null ? contentTitle : "")
                .setAutoCancel(true)
                .setColor(ContextCompat.getColor(getApplicationContext(),R.color.grey_800))
                .setTicker(getString(R.string.app_name))
                .setSubText(contentText != null ? contentText : getString(R.string.fixing_task))
                .setContentText(status != null ? status : "")
                .setSmallIcon(R.drawable.ic_stat_name)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .addAction(R.drawable.ic_stat_name,getString(R.string.stop), pendingStopIntent)
                .build();

        startForeground(R.string.app_name,notification);

    }

    /**
     * Handles the intent received
     * @param intent the intent received
     */
    private void onHandleIntent(Intent intent){

        //if intent comes from touching an item, or from TrackDetailsActivity
        //get the ID received, if not, default value is -1
        mCurrentId = intent.getLongExtra(Constants.MEDIA_STORE_ID, -1);
        //get selected items directly from DB, if ID is not -1,
        //it means that one id has been sent and
        //cursor returned from this query will contain only
        //one row, if mCurrentId is equal than -1, means that no id was provided
        //and it will get a cursor with more than one row
        mSelectedItems = mDataTrackDbHelper.getAllSelected(mCurrentId, MainActivity.Sort.setDefaultOrder());
        mNumberSelected = mSelectedItems == null ? 0 : mSelectedItems.getCount();
        //if intent comes from TrackDetailsActivity means that
        //mFromEditMode will be true, default value is false
        mFromEditMode = intent.getBooleanExtra(Constants.Activities.FROM_EDIT_MODE, false);

        createGnFiles();
    }

    /**
     * Creates and prepares gnobjects
     * that can be processed by GNSDK
     */

    private void createGnFiles(){
        long id = -1;
        String title = "";
        String artist = "";
        String album = "";
        String fullPath = "";

        //Before start task check if is not cancelled
        if(mStopService != Constants.StopsReasons.CONTINUE_TASK){
            stopSelf();
            return;
        }

        //GnMusicIdFile object provides identification service
        try {
            mGnMusicIdFile = new GnMusicIdFile(GnService.sGnUser, this);
            //set options of track id process
            mGnMusicIdFile.options().lookupData(GnLookupData.kLookupDataContent, true);
            mGnMusicIdFile.options().preferResultLanguage(GnLanguage.kLanguageSpanish);
            //queue will be processed one by one
            mGnMusicIdFile.options().batchSize(1);
            //get the fileInfoManager
            mGnMusicIdFileInfoManager = mGnMusicIdFile.fileInfos();
            mGnMusicIdFile.waitForComplete();
            //hold reference to queue in case we need cancel the
            //identification
            mGnMusicIdFileList.add(mGnMusicIdFile);

        } catch (GnException e) {
            e.printStackTrace();
        }

        //create and add files to a queue for process them
        while (mSelectedItems.moveToNext()){
            id = mSelectedItems.getLong(mSelectedItems.getColumnIndexOrThrow(TrackContract.TrackData.MEDIASTORE_ID));
            title = mSelectedItems.getString(mSelectedItems.getColumnIndexOrThrow(TrackContract.TrackData.TITLE));
            artist = mSelectedItems.getString(mSelectedItems.getColumnIndexOrThrow(TrackContract.TrackData.ARTIST));
            album = mSelectedItems.getString(mSelectedItems.getColumnIndexOrThrow(TrackContract.TrackData.ALBUM));
            fullPath = mSelectedItems.getString(mSelectedItems.getColumnIndexOrThrow(TrackContract.TrackData.DATA));

            try {
                //add all info available for more accurate results
                GnMusicIdFileInfo gnMusicIdFileInfo = mGnMusicIdFileInfoManager.add(fullPath);
                gnMusicIdFileInfo.fileName(fullPath);
                gnMusicIdFileInfo.mediaId(String.valueOf(id));
                gnMusicIdFileInfo.trackTitle(title);
                gnMusicIdFileInfo.trackArtist(artist);
                gnMusicIdFileInfo.albumTitle(album);

            } catch (GnException e) {
                e.printStackTrace();

                //Service is stopped from notification "Detener" button
                mStopService = Constants.StopsReasons.ERROR_TASK;
                mErrorMessage = e.getMessage();

                stopSelf();
                return;
            }
            finally {
                //reset values in every iteration
                id = -1;
                title = "";
                artist = "";
                album = "";
                fullPath = "";
            }

        }

        //close cursor to release resources
        mSelectedItems.close();
        mSelectedItems = null;

        //starts track id processing
        startTrackId();
    }

    /**
     * Starts the correction task
     */
    public void startTrackId(){
        //Before start task check if is not cancelled
        if(mStopService != Constants.StopsReasons.CONTINUE_TASK){
            stopSelf();
            return;
        }

        try {
            //use different methods depending on number of selected songs to be corrected
            if (mNumberSelected == 1) {
                mGnMusicIdFile.doTrackId(GnMusicIdFileProcessType.kQueryReturnSingle, GnMusicIdFileResponseType.kResponseAlbums);
            } else if (mNumberSelected >= 2) {
                mGnMusicIdFile.doLibraryId(GnMusicIdFileResponseType.kResponseAlbums);
            }
        } catch (GnException e) {
            e.printStackTrace();
            mStopService = Constants.StopsReasons.ERROR_TASK;
            mErrorMessage = e.getMessage();
            stopSelf();
            return;
        }
    }

    /**
     * Stops track id process when user
     * cancel or lost internet connection
     */
    public void  stopTrackId(){
        if (mGnMusicIdFileList != null) {
            Iterator<GnMusicIdFile> iterator = mGnMusicIdFileList.iterator();

            while (iterator.hasNext()) {
                iterator.next().cancel();
            }

        }

        //check cause of cancel task
        if(mStopService != Constants.StopsReasons.NORMAL_TERMINATION_TASK) {

            //Save state to database
            ContentValues contentValues = new ContentValues();
            contentValues.put(TrackContract.TrackData.IS_PROCESSING, false);
            int items = mDataTrackDbHelper.updateData(contentValues, TrackContract.TrackData.IS_PROCESSING, true);
            Log.d("items updated", items + "");

            Intent intentAction = new Intent();

            switch (mStopService){
                case Constants.StopsReasons.USER_CANCEL_TASK:
                        Log.d("cancel gn", "cancelling...");
                        intentAction.setAction(Constants.Actions.ACTION_CANCEL_TASK);
                    break;
                case Constants.StopsReasons.ERROR_TASK:
                        Log.d("cancel gn", "cancelling...api error");
                        intentAction.setAction(Constants.Actions.ACTION_ERROR);
                        intentAction.putExtra(Constants.GnServiceActions.API_ERROR, mErrorMessage);
                    break;
                case Constants.StopsReasons.LOST_CONNECTION_TASK:
                        Log.d("cancel gn", "cancelling...lost connection");
                        intentAction.setAction(Constants.Actions.ACTION_CONNECTION_LOST);
                    break;
                    default:
                        Log.d("cancel gn", "cancelling...");
                        intentAction.setAction(Constants.Actions.ACTION_CANCEL_TASK);
                        break;
            }

            //broadcast action to interested receivers
            mLocalBroadcastManager.sendBroadcastSync(intentAction);
        }
        //stop thread and looper
        mThread.quitSafely();
        mLooper.quitSafely();
    }

    @Override
    public void musicIdFileStatusEvent(GnMusicIdFileInfo gnMusicIdFileInfo, GnMusicIdFileCallbackStatus gnMusicIdFileCallbackStatus, long currentFile, long totalFiles, IGnCancellable iGnCancellable) {
        //Retrieve current status of current tracked id song
        String status = gnMusicIdFileCallbackStatus.toString();
        //Check if this is the last file being processed
        mLastFile = currentFile == totalFiles;
        //Log.d("CallbackStatus", gnMusicIdFileCallbackStatus.toString());

        if(mStopService != Constants.StopsReasons.CONTINUE_TASK){
            stopSelf();
            return;
        }

        //check the current state
        if (mGnStatusToDisplay.containsKey(status)) {
            //report status to notification
            if (mStartOrUpdateForegroundNotification) {
                try {
                    startNotification(currentFile + " de " + totalFiles, AudioItem.getFilename(gnMusicIdFileInfo.fileName()), mGnStatusToDisplay.get(status));
                }
                catch(GnException e){
                    e.printStackTrace();
                }
            }

            if(status.equals(Constants.State.BEGIN_PROCESSING)){

                //update status of current song
                try {
                    mPath = gnMusicIdFileInfo.fileName();
                    mId = Long.parseLong(gnMusicIdFileInfo.mediaId());
                    //update UI to show progress bar in current item list (only in MainActivity)
                    if (!mFromEditMode){
                        Intent intentUpdateUI = new Intent();
                        intentUpdateUI.setAction(Constants.Actions.ACTION_SET_AUDIOITEM_PROCESSING);
                        intentUpdateUI.putExtra(Constants.MEDIA_STORE_ID, mId);
                        mLocalBroadcastManager.sendBroadcastSync(intentUpdateUI);
                        //save state to current song in our database because
                        //if user closes the app and then reopen it, the current
                        //song if is still processing will show the progress bar
                        //and user experience will be consistent
                        ContentValues processingValue = new ContentValues();
                        processingValue.put(TrackContract.TrackData.IS_PROCESSING, true);
                        mDataTrackDbHelper.updateData(mId, processingValue);
                    }
                }catch (GnException e){
                    e.printStackTrace();
                }
            }
            else if(status.equals(Constants.State.STATUS_ERROR) || status.equals(Constants.State.STATUS_PROCESSING_ERROR)){
                Log.d("error while processing","try again");
                Intent intentUpdateUI = new Intent();
                intentUpdateUI.setAction(Constants.Actions.ACTION_SET_AUDIOITEM_PROCESSING);
                intentUpdateUI.putExtra(Constants.MEDIA_STORE_ID, mId);
                mLocalBroadcastManager.sendBroadcastSync(intentUpdateUI);
                ContentValues processingValue = new ContentValues();
                processingValue.put(TrackContract.TrackData.IS_PROCESSING, false);

                mDataTrackDbHelper.updateData(mId, processingValue);
            }


        }
    }

    @Override
    public void gatherFingerprint(GnMusicIdFileInfo fileInfo, long l, long l1, IGnCancellable iGnCancellable) {
        //Log.d("gatherFingerprint", "gatherFingerprint");

        //internet connection has lost?
        if(mStopService != Constants.StopsReasons.CONTINUE_TASK){
            stopSelf();
            return;
        }

        //Callback to inform that fingerprint was retrieved from audiotrack.
        try {
            //Verify that this song still exist in memory
            if(!AudioItem.checkFileIntegrity(fileInfo.fileName())){
                throw new FileNotFoundException("File does not exist anymore.");
            }

            if (GnAudioFile.isFileFormatSupported(fileInfo.fileName())) {
                fileInfo.fingerprintFromSource(new GnAudioFile(new File(fileInfo.fileName())));
            }

        }
        catch (GnException e) {
            if (!GnError.isErrorEqual(e.errorCode(), GnError.GNSDKERR_Aborted)) {
                Log.e(sAppString, "error in fingerprinting file: " + e.errorAPI() + ", " + e.errorModule() + ", " + e.errorDescription());

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void gatherMetadata(GnMusicIdFileInfo gnMusicIdFileInfo, long l, long l1, IGnCancellable iGnCancellable) {
        //Log.d("cancelled8",iGnCancellable.isCancelled()+"");
        //Log.d("gatherMetadata", "gatherMetadata");
    }

    @Override
    public void musicIdFileAlbumResult(GnResponseAlbums gnResponseAlbums, long l, long l1, IGnCancellable iGnCancellable) {
        //This callback is executed if results were found to current song
        if(mStopService != Constants.StopsReasons.CONTINUE_TASK){
            stopSelf();
            return;
        }

        String title = "";
        String artist = "";
        String album = "";
        String cover = "";
        String number = "";
        String year = "";
        String genre = "";

        //retrieve title results found
        try {
            title = gnResponseAlbums.albums().at(0).next().trackMatched().title().display();
        } catch (GnException e) {
            e.printStackTrace();
            title = "";
        }

        try {
            //get artist name of song if exist
            //otherwise get artist album
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
            //iterate from higher to lower quality and select the first higher quality found.
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
            //iterate from lower to higher quality and select the first lower quality found.
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
            //"De baja calidad", "De media calidad", "De alta calidad", "De muy alta calidad"
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
            //Get the first level found of genre, first from track matched if exist, if not, then from album found.

            //The Gracenote Genre System contains more than 2200 genres from around the world.
            //To make this list easier to manage and give more display options for client applications,
            //the Gracenote Genre System groups these genres into a relationship hierarchy.
            //Most hierarchies consists of three levels: level-1. level-2, and level-3. For example:
            //Level-1
                /*Rock
                    //Level-2
                    Heavy Metal
                        //Level-3
                        Grindcore
                        Black Metal
                        Death Metal
                    //Level-2
                    50's Rock
                        //Level-3
                        Doo Wop
                        Rockabilly
                        Early Rock & Roll
                 */
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

        setNewAudioTags(title, artist, album, cover, number, year, genre);
    }

    @Override
    public void musicIdFileMatchResult(GnResponseDataMatches gnResponseDataMatches, long l, long l1, IGnCancellable iGnCancellable) {

    }

    @Override
    public void musicIdFileResultNotFound(GnMusicIdFileInfo gnMusicIdFileInfo, long l, long l1, IGnCancellable iGnCancellable) {
        //this callback is triggered when no results, instead of musicIdFileAlbumResult or musicIdFileMatchResult

        if(mStopService != Constants.StopsReasons.CONTINUE_TASK){
            stopSelf();
            return;
        }
            //when no results were found, if task was started by
            //TrackDetailsActivity, only notify with action ACTION_NOT_FOUND
            //to show to user a snackbar indicating this
            if (mFromEditMode) {
                Intent intentActionNotFound = new Intent();
                intentActionNotFound.setAction(Constants.Actions.ACTION_NOT_FOUND);
                mLocalBroadcastManager.sendBroadcastSync(intentActionNotFound);
            }

        //if task was started by MainActivity, notify the same but
        //besides, update the UI and save the state to current song into our database
        else {
            ContentValues contentValues = new ContentValues();
            contentValues.put(TrackContract.TrackData.STATUS, AudioItem.STATUS_NO_TAGS_FOUND);
            contentValues.put(TrackContract.TrackData.IS_PROCESSING, false);
            contentValues.put(TrackContract.TrackData.IS_SELECTED, false);
            mDataTrackDbHelper.updateData(mId, contentValues);

            Intent intentActionNotFound = new Intent();
            intentActionNotFound.setAction(Constants.Actions.ACTION_NOT_FOUND);
            intentActionNotFound.putExtra(Constants.MEDIA_STORE_ID, mId);
            //Log.d("media store not found", mId + "");
            mLocalBroadcastManager.sendBroadcastSync(intentActionNotFound);
        }

        //if is the last file stop the service
        if (mLastFile) {
            //Update the UI only to MainActivity if task has completed;
            //if task was started from TrackDetailsActivity is not necessary inform this action
            mStopService = Constants.StopsReasons.NORMAL_TERMINATION_TASK;
            if (!mFromEditMode) {

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
        //triggered when all files were processed by doTrackId method,
        Log.d("musicIdFileComplete","complete");
    }

    @Override
    public void statusEvent(GnStatus gnStatus, long percentComplete, long bytesTotalSent, long bytesTotalReceived, IGnCancellable iGnCancellable) {
        Log.d("gnStatus","gnStatus");
    }

    private synchronized void setNewAudioTags(String... tags){

        if(mStopService != Constants.StopsReasons.CONTINUE_TASK){
            stopSelf();
            return;
        }

        //check what data has been found
        boolean dataTitle = !tags[0].isEmpty();
        boolean dataArtist = !tags[1].isEmpty();
        boolean dataAlbum = !tags[2].isEmpty();
        boolean dataImage = !tags[3].isEmpty();
        boolean dataTrackNumber = !tags[4].isEmpty();
        boolean dataYear = !tags[5].isEmpty();
        boolean dataGenre = !tags[6].isEmpty();

        //Is it a request from MainActivity?
        if(!mFromEditMode) {
            ContentValues newTags = new ContentValues();
            AudioItem audioItem = new AudioItem();
            Tag tag = null;
            AudioFile audioTaggerFile = null;
            String mimeType = AudioItem.getMimeType(mPath);
            boolean isMp3 = (mimeType.equals("audio/mpeg_mp3") || mimeType.equals("audio/mpeg")) && AudioItem.getExtension(mPath).equals("mp3");

            //because this audioitem is sent to main activity
            //set firstly its id and path(in case the file be renamed, we get the path from this audioitem)
            //and wrap new found tags(if are availables) into this AudioItem object.
            audioItem.setAbsolutePath(mPath);
            audioItem.setId(mId);

            //lets try to open the file to edit meta tags.
            try {
                //we checked the mime type and extension because if file is MP3, it can contain
                //ID3v1, ID3v2 or both tags versions, and there are considerable differences between these,
                //for example v1 doesn't have cover art support, so it cannot call deleteArtworkField nor setField(artwork)
                //because this would cause an error and the app would crash,
                //because of that for MP3 files is necessary convert the v1 to v2 first for standardize tags version.

                if(isMp3){
                    audioTaggerFile = AudioFileIO.read(new File(mPath));
                    if(((MP3File)audioTaggerFile).hasID3v1Tag() && !((MP3File) audioTaggerFile).hasID3v2Tag()){
                        //create new version of ID3v2
                        ID3v24Tag id3v24Tag = new ID3v24Tag( ((MP3File)audioTaggerFile).getID3v1Tag() );
                        audioTaggerFile.setTag(id3v24Tag);
                        tag = ((MP3File) audioTaggerFile).getID3v2TagAsv24();
                        //Log.d("converted_tag","converted_tag");
                    }
                    else {
                        //already has ID3v2 tag version
                        if(((MP3File) audioTaggerFile).hasID3v2Tag()) {
                            tag = ((MP3File) audioTaggerFile).getID3v2Tag();
                            //Log.d("get_v24_tag","get_v24_tag");
                        }
                        //Has no tags? create a new one, but no save until
                        //user apply changes
                        else {
                            ID3v24Tag id3v24Tag = new ID3v24Tag();
                            ((MP3File) audioTaggerFile).setID3v2Tag(id3v24Tag);
                            tag = ((MP3File) audioTaggerFile).getID3v2TagAsv24();
                            //Log.d("create_v24_tag","create_v24_tag");
                        }
                    }

                }
                //any other audio file supported
                else {
                    audioTaggerFile = AudioFileIO.read(new File(mPath));
                    tag = audioTaggerFile.getTag() == null ? audioTaggerFile.createDefaultTag() : audioTaggerFile.getTag();
                }



                //If this option is enabled, all existent tags in audio file will be
                //replace instead news.
                if(Settings.SETTING_OVERWRITE_ALL_TAGS_AUTOMATIC_MODE) {
                    //For every tag set value if were found
                    if (dataTitle) {
                        //set value to update our DB
                        newTags.put(TrackContract.TrackData.TITLE, tags[0]);
                        //set value to update the item list
                        audioItem.setTitle(tags[0]);
                        //set value to update the file
                        tag.setField(FieldKey.TITLE, tags[0]);
                    }

                    if (dataArtist) {
                        newTags.put(TrackContract.TrackData.ARTIST, tags[1]);
                        audioItem.setArtist(tags[1]);
                        tag.setField(FieldKey.ARTIST, tags[1]);
                    }

                    if (dataAlbum) {
                        newTags.put(TrackContract.TrackData.ALBUM, tags[2]);
                        audioItem.setAlbum(tags[2]);
                        tag.setField(FieldKey.ALBUM, tags[2]);
                    }

                    //FOR NEXT TAGS ONLY UPDATE THE AUDIO FILE,
                    //NOT ITEM LIST NOR OUR DATABASE
                    //BECAUSE WE DON'T STORE THOSE VALUES

                    if (dataImage) {
                        try {
                            byte[] imgData = new GnAssetFetch(GnService.sGnUser, tags[3]).data();
                            Artwork artwork = new AndroidArtwork();
                            artwork.setBinaryData(imgData);
                            tag.deleteArtworkField();
                            tag.setField(artwork);
                        } catch (GnException | KeyNotFoundException e) {
                            e.printStackTrace();
                        }
                    }

                    if (dataTrackNumber) {
                        tag.setField(FieldKey.TRACK, tags[4]);
                    }

                    if (dataYear) {
                        tag.setField(FieldKey.YEAR, tags[5]);
                    }

                    if (dataGenre) {
                        tag.setField(FieldKey.GENRE, tags[6]);
                    }
                    //Log.d("OverwriteAllTags", Settings.SETTING_OVERWRITE_ALL_TAGS_AUTOMATIC_MODE+"");
                }
                //Only write missing tags to audio file
                else {
                    if (dataTitle && tag.getFirst(FieldKey.TITLE).isEmpty()) {
                        //set value to update our DB
                        newTags.put(TrackContract.TrackData.TITLE, tags[0]);
                        //set value to update the item list
                        audioItem.setTitle(tags[0]);
                        //set value to update the file
                        tag.setField(FieldKey.TITLE, tags[0]);
                        //Log.d("missing","title");
                    }

                    if (dataArtist && tag.getFirst(FieldKey.ARTIST).isEmpty()) {
                        newTags.put(TrackContract.TrackData.ARTIST, tags[1]);
                        audioItem.setArtist(tags[1]);
                        tag.setField(FieldKey.ARTIST, tags[1]);
                        //Log.d("missing","artist");
                    }

                    if (dataAlbum && tag.getFirst(FieldKey.ALBUM).isEmpty()) {
                        newTags.put(TrackContract.TrackData.ALBUM, tags[2]);
                        audioItem.setAlbum(tags[2]);
                        tag.setField(FieldKey.ALBUM, tags[2]);
                        //Log.d("missing","album");
                    }

                    //FOR NEXT TAGS ONLY UPDATE THE AUDIO FILE,
                    //NOT ITEM LIST NOR OUR DATABASE
                    //BECAUSE WE DON'T STORE THOSE VALUES
                    if (dataImage && tag.getFirstArtwork() != null && tag.getFirstArtwork().getBinaryData() == null ) {
                        //Log.d("missing","cover");
                        try {
                            byte[] imgData = new GnAssetFetch(GnService.sGnUser, tags[3]).data();
                            Artwork artwork = new AndroidArtwork();
                            artwork.setBinaryData(imgData);
                            tag.deleteArtworkField();
                            tag.setField(artwork);
                        } catch (GnException | KeyNotFoundException e) {
                            e.printStackTrace();
                        }
                    }

                    if (dataTrackNumber && tag.getFirst(FieldKey.TRACK).isEmpty()) {
                        //Log.d("missing","number");
                        tag.setField(FieldKey.TRACK, tags[4]);
                    }

                    if (dataYear && tag.getFirst(FieldKey.YEAR).isEmpty()) {
                        //Log.d("missing","year");
                        tag.setField(FieldKey.YEAR, tags[5]);
                    }

                    if (dataGenre && tag.getFirst(FieldKey.GENRE).isEmpty()) {
                        //Log.d("missing","genre");
                        tag.setField(FieldKey.GENRE, tags[6]);
                    }
                    //Log.d("OverwriteAllTags", Settings.SETTING_OVERWRITE_ALL_TAGS_AUTOMATIC_MODE+"");
                }

                if(isMp3 && ((MP3File) audioTaggerFile).hasID3v1Tag()){
                    //remove old version of ID3 tags
                    //Log.d("removed ID3v1","remove ID3v1");
                    ((MP3File) audioTaggerFile).delete( ((MP3File)audioTaggerFile).getID3v1Tag() );
                }

                //Update the file meta tags, this changes in tags
                //are visible to all media players that are able to read
                //ID3 tags (most nowadays)
                audioTaggerFile.commit();
                //If some info was not found, mark its state as INCOMPLETE
                if (!dataTitle || !dataArtist || !dataAlbum || !dataImage || !dataTrackNumber || !dataYear || !dataGenre) {
                    newTags.put(TrackContract.TrackData.STATUS, AudioItem.STATUS_ALL_TAGS_NOT_FOUND);
                    audioItem.setStatus(AudioItem.STATUS_ALL_TAGS_NOT_FOUND);
                }
                //All info for this song was found, mark its state as COMPLETE!!!
                else {
                    newTags.put(TrackContract.TrackData.STATUS, AudioItem.STATUS_ALL_TAGS_FOUND);
                    audioItem.setStatus(AudioItem.STATUS_ALL_TAGS_FOUND);
                }

                //Rename file if this option is enabled in Settings
                if (Settings.SETTING_RENAME_FILE_AUTOMATIC_MODE) {
                    String newAbsolutePath = AudioItem.renameFile(audioItem.getAbsolutePath(), tags[0], tags[1]);
                    if (newAbsolutePath != null){
                        //Inform to system that one file has change its name
                        ContentValues newValuesToMediaStore = new ContentValues();
                        newValuesToMediaStore.put(MediaStore.MediaColumns.DATA, newAbsolutePath);
                        String selection = MediaStore.MediaColumns.DATA + "= ?";
                        String selectionArgs[] = {audioItem.getAbsolutePath()}; //this is the old path
                        boolean successMediaStore = getContentResolver().
                                update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                        newValuesToMediaStore,
                                        selection,
                                        selectionArgs) == 1;
                        newValuesToMediaStore.clear();
                        newValuesToMediaStore = null;

                        audioItem.setAbsolutePath(newAbsolutePath);
                        newTags.put(TrackContract.TrackData.DATA, newAbsolutePath); //new path
                        Log.d("success renaming", successMediaStore + " and renaming");
                    }
                }

                //Update our database to keep track the state of songs
                newTags.put(TrackContract.TrackData.IS_SELECTED, false);
                newTags.put(TrackContract.TrackData.IS_PROCESSING, false);
                mDataTrackDbHelper.updateData(audioItem.getId(), newTags);

                //finally report to receivers the status
                //of this file, to update the UI
                Intent intentActionDone = new Intent();
                intentActionDone.setAction(Constants.Actions.ACTION_DONE);
                intentActionDone.putExtra(Constants.AUDIO_ITEM, audioItem);
                mLocalBroadcastManager.sendBroadcastSync(intentActionDone);

            }
            catch (CannotWriteException | IOException | InvalidAudioFrameException | TagException | ReadOnlyFileException | CannotReadException e) {
                e.printStackTrace();
                //if an error has ocurred, mark this file as bad
                //update our database
                audioItem.setStatus(AudioItem.FILE_ERROR_READ);
                newTags.put(TrackContract.TrackData.STATUS, AudioItem.FILE_ERROR_READ);
                newTags.put(TrackContract.TrackData.IS_SELECTED, false);
                newTags.put(TrackContract.TrackData.IS_PROCESSING, false);
                mDataTrackDbHelper.updateData(audioItem.getId(), newTags);

                //and report to receivers that current song could
                //not be processed
                Intent intentActionDone = new Intent();
                intentActionDone.setAction(Constants.Actions.ACTION_FAIL);
                intentActionDone.putExtra(Constants.AUDIO_ITEM, audioItem);
                mLocalBroadcastManager.sendBroadcastSync(intentActionDone);
            }
            finally {
                newTags.clear();
                newTags = null;
            }

        }
        //if intent was made from TrackDetailsActivity
        //don't modify the track, only retrieve data and
        //send back to activity to show to user, he/she has to decide
        //if want to apply found tags, this is the SEMIAUTOMATIC MODE
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
                    byte[] imgData = new GnAssetFetch(GnService.sGnUser, tags[3]).data();
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

            //send back results and inform that track id for this
            //song has been successful processed
            Intent intentActionDoneDetails = new Intent();
            intentActionDoneDetails.setAction(Constants.Actions.ACTION_DONE_DETAILS);
            intentActionDoneDetails.putExtra(Constants.AUDIO_ITEM, audioItem);
            mLocalBroadcastManager.sendBroadcastSync(intentActionDoneDetails);

        }

        //is the last processed song?
        if(mLastFile){
            mStopService = Constants.StopsReasons.NORMAL_TERMINATION_TASK;

            //Inform to update the UI only to MainActivity if task has completed;
            //if task was started from TrackDetailsActivity is not necessary inform this action
            if(!mFromEditMode){
                Intent intentCompleteTask = new Intent();
                intentCompleteTask.setAction(Constants.Actions.ACTION_COMPLETE_TASK);
                mLocalBroadcastManager.sendBroadcastSync(intentCompleteTask);
            }

            //if this was the last file, lets assume that
            //user did not cancel the task and stop service
            stopSelf();
            return;
        }



        //Check if is requested to stop service
        if(mStopService != Constants.StopsReasons.CONTINUE_TASK){
            stopSelf();
            return;
        }

    }

    /**
     * Clear temporal values used to
     * hold a global reference to current song being
     * corrected
     */
    public void clearValues(){
        mId = -1;
        mPath = null;
    }

    //This Handler class handles the task of correction
    public static class MyHandler extends Handler {
        //A weak reference can avoid us memory leaks
        private WeakReference<FixerTrackService> fixerTrackServiceWeakReference;
        public MyHandler(Looper looper, FixerTrackService fixerTrackService){
            super(looper);
            fixerTrackServiceWeakReference = new WeakReference<>(fixerTrackService);
        }

        @Override
        public void handleMessage(Message msg){
            int startId = msg.arg1;
            Intent intent = (Intent) msg.obj;
            fixerTrackServiceWeakReference.get().onHandleIntent(intent);
            Log.i(CLASS_NAME, "startId: " + startId);
        }
    }

}



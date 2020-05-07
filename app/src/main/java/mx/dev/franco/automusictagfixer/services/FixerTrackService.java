package mx.dev.franco.automusictagfixer.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.ArrayMap;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import com.gracenote.gnsdk.GnException;

import org.jaudiotagger.tag.FieldKey;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import dagger.android.DispatchingAndroidInjector;
import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
import mx.dev.franco.automusictagfixer.BuildConfig;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.covermanager.CoverLoader;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;
import mx.dev.franco.automusictagfixer.fixer.CorrectionParams;
import mx.dev.franco.automusictagfixer.fixer.TrackLoader;
import mx.dev.franco.automusictagfixer.fixer.TrackWriter;
import mx.dev.franco.automusictagfixer.identifier.ApiInitializerService;
import mx.dev.franco.automusictagfixer.identifier.GnApiService;
import mx.dev.franco.automusictagfixer.identifier.GnUtils;
import mx.dev.franco.automusictagfixer.identifier.IdentificationManager;
import mx.dev.franco.automusictagfixer.identifier.Identifier;
import mx.dev.franco.automusictagfixer.identifier.IdentifierFactory;
import mx.dev.franco.automusictagfixer.identifier.Result;
import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.persistence.cache.IdentificationResultsCache;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.persistence.room.TrackDAO;
import mx.dev.franco.automusictagfixer.persistence.room.TrackRoomDatabase;
import mx.dev.franco.automusictagfixer.ui.main.MainActivity;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;

import static mx.dev.franco.automusictagfixer.identifier.IdentificationManager.prepareResults;
import static mx.dev.franco.automusictagfixer.persistence.repository.AsyncOperation.TrackUpdaterSync;

/**
 * @author franco
 * @version 2.0
 * Service that fix the metadata in background threads and is capable to run in background no matter
 * if app is closed.
 */

public class FixerTrackService extends Service {
    public static String CLASS_NAME = FixerTrackService.class.getName();

    @Inject
    DispatchingAndroidInjector<Service> serviceDispatchingAndroidInjector;

    @Inject
    IdentifierFactory mIdentifierFactory;
    @Inject
    TrackRoomDatabase mTrackRoomDatabase;
    @Inject
    GnApiService mGnApiService;
    @Inject
    AudioTagger mTagger;
    @Inject
    AudioTagger mAudioTagger;
    @Inject
    IdentificationResultsCache mIdentificationResultsCache;
    @Inject
    AbstractSharedPreferences mAbstractSharedPreferences;
    //Notification on status bar
    private Notification mNotification;
    private Identifier<Map<String, String>, List<? extends Identifier.IdentificationResults>> mIdentifier;
    private TrackLoader mTrackLoader;
    private TrackWriter mTrackWriter;
    private SharedPreferences mSharedPreferences;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private AtomicBoolean mCancelled = new AtomicBoolean(false);
    private Thread mCorrectionParamsThread;


    /**
     * Creates a Service.  Invoked by your subclass's constructor.
     */
    public FixerTrackService() {
        super();
    }

    @Override
    public void onCreate(){
        AndroidInjection.inject(this);
        super.onCreate();
        mIdentifier =  mIdentifierFactory.create(IdentifierFactory.FINGERPRINT_IDENTIFIER);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    /**
     * This callback runs when service starts running
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(final Intent intent, int flags, int startId){
        //When setting "Corrección en segundo plano" is on,
        //then the service will be able to run in background,
        //and a correction won't stop when app closes, but when you explicitly
        //stop the task by pressing stop button in main screen or notification or task finishes.
        String action = intent.getAction();
        if(action != null) {
            // Service will be stopped
            if(action.equals(Constants.Actions.ACTION_STOP_TASK) ||
                    Intent.ACTION_MEDIA_MOUNTED.equals(action) ||
                    Intent.ACTION_MEDIA_UNMOUNTED.equals(action)){
                mCancelled.set(true);
                actionStopTask();
                /*stopTasks();
                broadcastMessage(getString(R.string.task_cancelled));
                broadcastCompleteCorrection();
                stopServiceAndRemoveFromForeground();*/
            }
            //Service is running and correction task is about to begin.
            else {
                actionStartTask(false);
            }
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        this.mTrackWriter = null;
        this.mTrackLoader = null;
        this.mCorrectionParamsThread = null;
        this.mIdentifier = null;
    }

    private void actionStartTask(boolean isNextTrack) {
        if (mCancelled.get())
            return;

        mTrackLoader = new TrackLoader(new AsyncOperation<Void, Track, Void, Void>() {
            @Override
            public void onAsyncOperationFinished(Track track) {
                if (track != null) {
                    startIdentification(track);
                }
                else {
                    if (isNextTrack) {
                        broadcastMessage(getString(R.string.complete_task));
                    }
                    else {
                        broadcastMessage(getString(R.string.no_songs_to_correct));
                    }
                    broadcastCompleteCorrection();
                    stopServiceAndRemoveFromForeground();
                }
            }
        }, mTrackRoomDatabase.trackDao());
        mTrackLoader.executeOnExecutor(AutoMusicTagFixer.getExecutorService(), mAbstractSharedPreferences);
    }

    private void actionStopTask() {
        stopIdentification();
        broadcastMessage(getString(R.string.task_cancelled));
        broadcastCompleteCorrection();
        Thread thread = new Thread(() -> {
            mTrackRoomDatabase.trackDao().unprocessTracks();
            stopServiceAndRemoveFromForeground();
        });
        thread.start();
    }

    private void startIdentification(Track track){
        List<? extends Identifier.IdentificationResults> cacheResults = mIdentificationResultsCache.load(track.getMediaStoreId()+"");
        if(!canContinue()){
            broadcastMessage(getString(R.string.initializing_recognition_api));
            broadcastCompleteCorrection();
            stopServiceAndRemoveFromForeground();
        }
        else {
            if (cacheResults != null && cacheResults.size() > 0) {
                track.setProcessing(1);
                TrackUpdaterSync trackUpdaterSync = new TrackUpdaterSync(new AsyncOperation<Void, Integer, Void, Void>() {
                    @Override
                    public void onAsyncOperationFinished(Integer result) {
                        broadcastCorrectionForId(track.getMediaStoreId());
                        broadcastStartingCorrection();
                        onStartIdentification(track);
                        createCorrectionParams(cacheResults, track);
                    }
                }, mTrackRoomDatabase.trackDao());
                trackUpdaterSync.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, track);
            }
            else {
                if (mCancelled.get())
                    return;

                mIdentifier.registerCallback(new Identifier.IdentificationListener<List<? extends Identifier.IdentificationResults>>() {
                    @Override
                    public void onIdentificationStart() {
                        broadcastCorrectionForId(track.getMediaStoreId());
                        broadcastStartingCorrection();
                        onStartIdentification(track);
                    }

                    @Override
                    public void onIdentificationFinished(List<? extends Identifier.IdentificationResults> result) {
                        createCorrectionParams(result, track);
                    }

                    @Override
                    public void onIdentificationCancelled() {
                        identificationCancelled();
                        track.setChecked(0);
                        track.setProcessing(0);
                        TrackUpdaterSync trackUpdaterSync = new TrackUpdaterSync(new AsyncOperation<Void, Integer, Void, Void>() {
                            @Override
                            public void onAsyncOperationFinished(Integer result) {
                                finishTrack(track);
                                actionStartTask(true);
                            }
                        }, mTrackRoomDatabase.trackDao());
                        trackUpdaterSync.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, track);
                    }

                    @Override
                    public void onIdentificationNotFound() {
                        identificationNotFound(track);
                        track.setChecked(0);
                        track.setProcessing(0);
                        TrackUpdaterSync trackUpdaterSync = new TrackUpdaterSync(new AsyncOperation<Void, Integer, Void, Void>() {
                            @Override
                            public void onAsyncOperationFinished(Integer result) {
                                finishTrack(track);
                                actionStartTask(true);
                            }
                        }, mTrackRoomDatabase.trackDao());
                        trackUpdaterSync.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, track);
                    }

                    @Override
                    public void onIdentificationError(Throwable error) {
                        identificationError(error.getMessage(), track);
                        track.setChecked(0);
                        track.setProcessing(0);
                        TrackUpdaterSync trackUpdaterSync = new TrackUpdaterSync(new AsyncOperation<Void, Integer, Void, Void>() {
                            @Override
                            public void onAsyncOperationFinished(Integer result) {
                                finishTrack(track);
                                actionStartTask(true);
                            }
                        }, mTrackRoomDatabase.trackDao());
                        trackUpdaterSync.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, track);
                    }
                });

                if (mCancelled.get())
                    return;

                Map<String, String> map = new ArrayMap<>();
                map.put(Identifier.Field.FILENAME.name(), track.getPath());
                track.setProcessing(1);
                TrackUpdaterSync trackUpdaterSync = new TrackUpdaterSync(new AsyncOperation<Void, Integer, Void, Void>() {
                    @Override
                    public void onAsyncOperationFinished(Integer result) {
                        if (mCancelled.get())
                            return;
                        mIdentifier.identify(map);
                    }
                }, mTrackRoomDatabase.trackDao());
                trackUpdaterSync.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, track);

            }
        }
    }

    private void createCorrectionParams(List<? extends Identifier.IdentificationResults> results, Track track) {
        mCorrectionParamsThread = new Thread(() -> {

            List<Result> newList = prepareResults(results);
            Result result = IdentificationManager.findBestResult(newList,
                    track,
                    mSharedPreferences.getString("key_size_album_art", "kImageSize1080"));

            CorrectionParams correctionParams = new CorrectionParams();
            int correctionMode = mSharedPreferences.getBoolean("key_overwrite_all_tags_automatic_mode", true) ?
                    AudioTagger.MODE_OVERWRITE_ALL_TAGS : AudioTagger.MODE_WRITE_ONLY_MISSING;
            boolean renameFile = mSharedPreferences.getBoolean("key_rename_file_automatic_mode", false);
            correctionParams.setCorrectionMode(correctionMode);
            if (renameFile && result.getTitle() != null && !result.getTitle().equals("")) {
                correctionParams.setNewName(result.getTitle());
                correctionParams.setRenameFile(true);
            }
            correctionParams.setMediaStoreId(track.getMediaStoreId()+"");
            correctionParams.setTarget(track.getPath());
            String url = result.getCoverArt() != null ? result.getCoverArt().getUrl() : null;
            byte[] data = null;
            if (url != null) {
                try {
                    data = GnUtils.fetchGnCover(url, FixerTrackService.this.getApplication());
                } catch (GnException e) {
                    e.printStackTrace();
                }
            }

            AndroidUtils.createInputParams(result.getTitle(),
                    result.getArtist(),
                    result.getAlbum(),
                    result.getGenre(),
                    result.getTrackNumber(),
                    result.getTrackYear(),
                    data,
                    correctionParams);

            mHandler.post(() -> applyCorrection(correctionParams, track));
        });
        mCorrectionParamsThread.start();
    }

    private void applyCorrection(CorrectionParams correctionParams, Track track) {
        mTrackWriter = new TrackWriter(new AsyncOperation<Void,
                AudioTagger.AudioTaggerResult<Map<FieldKey, Object>>, Void, AudioTagger.AudioTaggerResult<Map<FieldKey, Object>>>() {
            @Override
            public void onAsyncOperationStarted(Void params) {
                onCorrectionStarted(track);
            }

            @Override
            public void onAsyncOperationFinished(AudioTagger.AudioTaggerResult<Map<FieldKey, Object>> result) {
                boolean deleteCoverFromCache = result.getTaskExecuted() == AudioTagger.MODE_OVERWRITE_ALL_TAGS ||
                        result.getTaskExecuted() == AudioTagger.MODE_WRITE_ONLY_MISSING
                 || result.getData().containsKey(FieldKey.COVER_ART);

                if (deleteCoverFromCache)
                    CoverLoader.removeCover(track.getMediaStoreId()+"");
                track.setChecked(0);
                track.setProcessing(0);
                updateTrack(result, track, mTrackRoomDatabase.trackDao());
            }

            @Override
            public void onAsyncOperationError(AudioTagger.AudioTaggerResult<Map<FieldKey, Object>> error) {
                String errorMessage = processCorrectionError(error);
                broadcastMessage(errorMessage);
                track.setChecked(0);
                track.setProcessing(0);
                TrackUpdaterSync trackUpdaterSync = new TrackUpdaterSync(new AsyncOperation<Void, Integer, Void, Void>() {
                    @Override
                    public void onAsyncOperationFinished(Integer result) {
                        finishTrack(track);
                        actionStartTask(true);
                    }
                }, mTrackRoomDatabase.trackDao());
                trackUpdaterSync.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, track);
            }
        }, mAudioTagger, correctionParams);
        mTrackWriter.executeOnExecutor(Executors.newSingleThreadExecutor(), getApplicationContext());
    }

    private String processCorrectionError(AudioTagger.AudioTaggerResult<Map<FieldKey, Object>> error) {
        int errorCode = error.getCode();
        String errorMessage;
        if (errorCode == AudioTagger.COULD_NOT_GET_URI_SD_ROOT_TREE) {
            errorMessage = getString(R.string.message_uri_tree_not_set);
        } else {
            int idStringResource;
            switch (errorCode) {
                case AudioTagger.COULD_NOT_REMOVE_COVER:
                case AudioTagger.COULD_NOT_APPLY_COVER:
                        errorMessage = getString(R.string.message_could_not_apply_cover);
                    break;
                case AudioTagger.COULD_NOT_COPY_BACK_TO_ORIGINAL_LOCATION:
                case AudioTagger.COULD_NOT_CREATE_AUDIOFILE:
                case AudioTagger.COULD_NOT_CREATE_TEMP_FILE:
                case AudioTagger.COULD_NOT_REMOVE_OLD_ID3_VERSION:
                case AudioTagger.COULD_NOT_RENAME_FILE:
                case AudioTagger.COULD_NOT_WRITE_TAGS:
                case AudioTagger.COULD_NOT_APPLY_TAGS:
                default:
                        errorMessage = getString(R.string.message_could_not_apply_tags);

            }
        }
        return errorMessage;
    }

    private boolean canContinue() {
        if(mGnApiService.isApiInitializing() || !mGnApiService.isApiInitialized()){
            Intent intent = new Intent(getApplicationContext(), ApiInitializerService.class);
            startService(intent);
            return false;
        }
        return true;
    }

    private void onStartIdentification(Track track) {
        startNotification(AudioTagger.getPath(track.getPath()),
                getString(R.string.correction_in_progress),
                getString(R.string.identifying), track.getMediaStoreId());
    }

    private void identificationError(String error, Track track) {
        startNotification(getString(R.string.correction_in_progress), "", error, track.getMediaStoreId() );
        broadcastMessage(error);
    }

    private void identificationNotFound(Track track) {
        startNotification(AudioTagger.getPath(track.getPath()),getString(R.string.correction_in_progress),
                getString(R.string.no_match_found), track.getMediaStoreId() );
    }

    private void finishTrack(Track track) {
        startNotification(AudioTagger.getFilename(track.getPath()),
                getString(R.string.success), "", track.getMediaStoreId() );
    }

    private void onCorrectionStarted(Track track) {
        startNotification(AudioTagger.getFilename(track.getPath()), getString(R.string.starting_correction),
                getString(R.string.applying_tags), track.getMediaStoreId() );
    }

    private void broadcastCorrectionForId(int mediaStoreId) {
        Intent intent = new Intent(Constants.Actions.START_PROCESSING_FOR);
        intent.putExtra(Constants.MEDIA_STORE_ID, mediaStoreId);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcastSync(intent);
    }

    private void updateTrack(AudioTagger.AudioTaggerResult<Map<FieldKey, Object>> result, Track track, TrackDAO trackDAO) {
        Map<FieldKey, Object> tags = result.getData();

        if (tags != null) {
            String title = (String) tags.get(FieldKey.TITLE);
            String artist = (String) tags.get(FieldKey.ARTIST);
            String album = (String) tags.get(FieldKey.ALBUM);
            if (title != null && !title.isEmpty()) {
                track.setTitle(title);
            }

            if (artist != null && !artist.isEmpty()) {
                track.setArtist(artist);
            }

            if (album != null && !album.isEmpty()) {
                track.setAlbum(album);
            }
        }

        String path = ((AudioTagger.ResultCorrection)result).getResultRename();
        if(path != null && !path.equals(""))
            track.setPath(path);

        track.setVersion(track.getVersion()+1);


        track.setChecked(0);
        track.setProcessing(0);
        TrackUpdaterSync trackUpdaterSync = new TrackUpdaterSync(new AsyncOperation<Void, Integer, Void, Void>() {
            @Override
            public void onAsyncOperationFinished(Integer result) {
                finishTrack(track);
                actionStartTask(true);
            }
        }, trackDAO);
        trackUpdaterSync.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, track);

    }

    public void identificationCancelled() {
        broadcastCompleteCorrection();
        broadcastMessage(getString(R.string.task_cancelled));
        stopServiceAndRemoveFromForeground();
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

    private void stopServiceAndRemoveFromForeground() {
        stopSelf();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            stopForeground(true);
    }

    private void stopTasks(){
        stopIdentification();
        if (mCorrectionParamsThread != null && !mCorrectionParamsThread.isInterrupted())
            mCorrectionParamsThread.interrupt();
        if (mTrackLoader != null)
            mTrackLoader.cancel(true);
        if (mTrackWriter != null)
            mTrackWriter.cancel(true);
    }

    private void broadcastStartingCorrection(){
        Intent intent = new Intent(Constants.Actions.ACTION_START_TASK);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private void broadcastCompleteCorrection(){
        Intent intent = new Intent(Constants.Actions.ACTION_COMPLETE_TASK);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private void broadcastMessage(String message) {
        Intent intent = new Intent(Constants.Actions.ACTION_BROADCAST_MESSAGE);
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }


    private void stopIdentification(){
        if(mIdentifier != null){
            mIdentifier.cancel();
        }
    }

    /**
     * Starts a notification and set
     * this service as foreground service,
     * allowing run no matter if app closes
     * @param contentText the content text o notification
     * @param title the title of notification
     * @param status the status string to show in notification
     * @param mediaStoreId
     */
    private void startNotification(String contentText, String title, String status, int mediaStoreId) {

        Intent notificationIntent = new Intent(this,MainActivity.class);
        notificationIntent.setAction(Constants.ACTION_OPEN_MAIN_ACTIVITY);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
        notificationIntent.putExtra(Constants.MEDIA_STORE_ID, mediaStoreId);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Intent stopTaskIntent = new Intent(this, FixerTrackService.class);
        stopTaskIntent.setAction(Constants.Actions.ACTION_STOP_TASK);
        PendingIntent pendingStopIntent = PendingIntent.getService(this, 0,
                stopTaskIntent, 0);

        NotificationCompat.Builder builder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String channelId = createNotificationChannel();
            builder = new NotificationCompat.Builder(this, channelId);
        }
        else {
            builder = new NotificationCompat.Builder(this, Constants.Application.FULL_QUALIFIED_NAME);
        }

        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                R.mipmap.ic_launcher);


                    mNotification = builder.setContentTitle(title != null ? title : "")
                    .setContentText(status != null ? status : "")
                    .setSubText(contentText != null ? contentText : getString(R.string.fixing_task))
                    .setAutoCancel(true)
                    //.setColor(ContextCompat.getColor(getApplicationContext(), R.color.primaryColor))
                    .setTicker(getString(R.string.app_name))
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .addAction(R.drawable.ic_stat_name, getString(R.string.stop), pendingStopIntent)
                    .build();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForeground(1, mNotification);
        }
        else {
            startForeground(R.string.app_name, mNotification);
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(){
        String channelId = BuildConfig.APPLICATION_ID + "." + FixerTrackService.CLASS_NAME;
        String channelName = FixerTrackService.CLASS_NAME;
        NotificationChannel chan = new NotificationChannel(channelId,channelName,
                NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager notificationManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(chan);
        }
        return channelId;
    }
}



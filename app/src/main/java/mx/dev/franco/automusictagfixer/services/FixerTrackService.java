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
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import mx.dev.franco.automusictagfixer.BuildConfig;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.fixer.AudioMetadataTagger;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger.ResultRename;
import mx.dev.franco.automusictagfixer.fixer.FileRenamer;
import mx.dev.franco.automusictagfixer.fixer.MetadataWriter;
import mx.dev.franco.automusictagfixer.fixer.MetadataWriterResult;
import mx.dev.franco.automusictagfixer.fixer.TrackIdLoader;
import mx.dev.franco.automusictagfixer.fixer.TrackInformationLoader;
import mx.dev.franco.automusictagfixer.identifier.GnApiService;
import mx.dev.franco.automusictagfixer.identifier.GnIdentifier;
import mx.dev.franco.automusictagfixer.identifier.Identifier;
import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.persistence.repository.TrackRepository;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.persistence.room.TrackRoomDatabase;
import mx.dev.franco.automusictagfixer.ui.MainActivity;
import mx.dev.franco.automusictagfixer.ui.trackdetail.InputCorrectionParams;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.TrackResultRename;
import mx.dev.franco.automusictagfixer.utilities.TrackUtils;
import mx.dev.franco.automusictagfixer.utilities.resource_manager.ResourceManager;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;

/**
 * @author franco
 * @version 2.0
 * Service that fix the metadata in background threads and is capable to run in background no matter
 * if app is closed.
 */

public class FixerTrackService extends Service {
    public static String CLASS_NAME = FixerTrackService.class.getName();

    @Inject
    TrackRepository mTrackRepository;
    @Inject
    TrackRoomDatabase mTrackRoomDatabase;
    @Inject
    ResourceManager mResourceManager;
    @Inject
    GnApiService mGnApiService;
    @Inject
    AudioTagger mTagger;
    @Inject
    AbstractSharedPreferences mSharedPreferences;

    //Notification on status bar
    private Notification mNotification;
    private Identifier<Track, List<Identifier.IdentificationResults>> mIdentifier;
    private MetadataWriter mMetadataWriter;
    private TrackInformationLoader mTrackInformationLoader;
    private FileRenamer mFileRenamer;
    private TrackIdLoader mTrackIdLoader;
    private List<Integer> mTrackIds = new ArrayList<>();
    private ServiceState mServiceState = ServiceState.NOT_RUNNING;
    private FixerState mFixerState;
    private Intent mRecycledIntent = new Intent();

    /**
     * Creates a Service.  Invoked by your subclass's constructor.
     */
    public FixerTrackService() {
        super();
    }

    @Override
    public void onCreate(){
        super.onCreate();
        broadcastStartingCorrection();
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
            if(action.equals(Constants.Actions.ACTION_STOP_TASK)){
                mFixerState = FixerState.CANCELLED;
                actionStopTask();
            }
            //Service is running and correction task is about to begin.
            else {
                actionStartTask();
            }
        }
        return super.onStartCommand(intent,flags,startId);
    }

    private void actionStartTask() {
        SharedPreferences sharedPreferences =
                getSharedPreferences(Constants.Application.FULL_QUALIFIED_NAME, Context.MODE_PRIVATE);
        String order = sharedPreferences.getString(Constants.SORT_KEY, null);

        mTrackIdLoader = new TrackIdLoader(new AsyncOperation<Void, List<Integer>, Void, Void>() {
            @Override
            public void onAsyncOperationStarted(Void params) {
                mFixerState = FixerState.STARTING_FETCHING_TRACKS;
                mServiceState = ServiceState.RUNNING;
            }

            @Override
            public void onAsyncOperationFinished(List<Integer> result) {
                mFixerState = FixerState.FINISHED_FETCHING_TRACKS;
                mTrackIdLoader = null;
                if(result.isEmpty()) {
                    broadcastMessage(getString(R.string.no_songs_to_correct));
                    broadcastCompleteCorrection();
                    stopServiceAndRemoveFromForeground();
                }
                else {
                    mTrackIds.addAll(result);
                    shouldContinue();
                }
            }

            @Override
            public void onAsyncOperationCancelled(Void cancellation) {
                mTrackIdLoader = null;
            }
        }, mTrackRoomDatabase);
        mTrackIdLoader.executeOnExecutor(Executors.newCachedThreadPool(), order);
    }

    private void actionStopTask() {
        if(mServiceState == ServiceState.RUNNING) {
            stopTasks();
            broadcastMessage(getString(R.string.task_cancelled));
            broadcastCompleteCorrection();
            stopServiceAndRemoveFromForeground();
        }
        else {
            broadcastCompleteCorrection();
            stopServiceAndRemoveFromForeground();
        }
    }

    private void shouldContinue(){
        if(mFixerState == FixerState.CANCELLED){
            broadcastCompleteCorrection();
            broadcastMessage(getString(R.string.identification_cancelled));
            stopServiceAndRemoveFromForeground();
        }
        else {
            if(mTrackIds != null && !mTrackIds.isEmpty()){
                startCorrection();
            }
            else {
                broadcastMessage(getString(R.string.complete_task));
                broadcastCompleteCorrection();
                stopServiceAndRemoveFromForeground();
            }
        }
    }

    private void startCorrection(){
        if(!canContinue()){
            broadcastMessage(getString(R.string.initializing_recognition_api));
            broadcastCompleteCorrection();
            stopServiceAndRemoveFromForeground();
        }
        else if(mFixerState == FixerState.CANCELLED){
            broadcastMessage(getString(R.string.task_cancelled));
            broadcastCompleteCorrection();
            stopServiceAndRemoveFromForeground();
        }
        else if (mServiceState != ServiceState.RUNNING) {//(!isRunning){
            mTrackInformationLoader = new TrackInformationLoader(new AsyncOperation<Void, List<Track>, Void, Void>() {
                @Override
                public void onAsyncOperationStarted(Void params) {
                    mFixerState = FixerState.STARTING_FETCHING_TRACK_INFO;
                }

                @Override
                public void onAsyncOperationFinished(List<Track> result) {
                    mFixerState = FixerState.FINISHED_FETCHING_TRACK_INFO;
                    processTrack(result);
                }
            }, mTrackRoomDatabase);
            mTrackInformationLoader.executeOnExecutor(Executors.newCachedThreadPool(), mTrackIds.get(0));
        }
    }

    private boolean canContinue(){
        if(mGnApiService.isApiInitializing()|| !mGnApiService.isApiInitialized()){
            mGnApiService.initializeAPI();
            return false;
        }
        return true;
    }

    public void processTrack(List<Track> data) {
        if(mFixerState == FixerState.CANCELLED){
            broadcastMessage(getString(R.string.task_cancelled));
            broadcastCompleteCorrection();
            stopServiceAndRemoveFromForeground();
        }
        else {
            if(AudioTagger.checkFileIntegrity(data.get(0).getPath())) {
                identificationError(getString(R.string.could_not_read_file), data.get(0));
            }
            else {
                mIdentifier = new GnIdentifier(mGnApiService, mSharedPreferences);
                mIdentifier.registerCallback(new Identifier.IdentificationListener<List<Identifier.IdentificationResults>, Track>() {
                    @Override
                    public void onIdentificationStart(Track file) {
                        mFixerState = FixerState.STARTING_IDENTIFYING;
                        onStartIdentification(file);
                    }

                    @Override
                    public void onIdentificationFinished(List<Identifier.IdentificationResults> result, Track file) {
                        mTrackInformationLoader = null;
                        mFixerState = FixerState.FINISHED_IDENTIFYING;
                        identificationFound(result, file);
                    }

                    @Override
                    public void onIdentificationError(Track file, String error) {
                        mFixerState = FixerState.ERROR_IDENTIFYING;
                        mTrackInformationLoader = null;
                        identificationError(error, file);
                    }

                    @Override
                    public void onIdentificationCancelled(Track file) {
                        mFixerState = FixerState.CANCELLED_IDENTIFYING;
                        mTrackInformationLoader = null;
                        identificationCancelled(file);
                    }

                    @Override
                    public void onIdentificationNotFound(Track file) {
                        mFixerState = FixerState.FINISHED_IDENTIFYING;
                        mTrackInformationLoader = null;
                        identificationNotFound(file);
                    }
                });

                mIdentifier.identify(data.get(0));
            }
        }
    }


    private void onStartIdentification(Track track) {
        //isRunning = true;
        track.setChecked(1);
        track.setProcessing(1);
        updateTrack(track);
        startNotification(TrackUtils.getPath(track.getPath()),
                getString(R.string.correction_in_progress),
                getString(R.string.identifying), track.getMediaStoreId());
        broadcastCorrectionForId(track);
    }

    private void identificationError(String error, Track track) {
        startNotification(getString(R.string.correction_in_progress), "", error, track.getMediaStoreId() );

        broadcastMessage(error);

        track.setChecked(0);
        track.setProcessing(0);
        updateTrack(track);

        mServiceState = ServiceState.NOT_RUNNING;
        //isRunning = false;

        popFirstFromList();
        shouldContinue();
    }

    private void identificationNotFound(Track track) {

        startNotification(TrackUtils.getPath(track.getPath()),getString(R.string.correction_in_progress),
                getString(R.string.no_match_found), track.getMediaStoreId() );


        track.setChecked(0);
        track.setProcessing(0);
        updateTrack(track);

        mServiceState = ServiceState.NOT_RUNNING;
        //isRunning = false;

        popFirstFromList();
        shouldContinue();
    }

    /**
     * Called if current track is identified.
     * @param results
     * @param track
     */
    public void identificationFound(List<Identifier.IdentificationResults> results, Track track) {
        startNotification(TrackUtils.getPath(track.getPath()), getString(R.string.match_found),
                getString(R.string.starting_correction), track.getMediaStoreId());

        //Recover from preferences if user wants to overwrite or write only missing tags in automatic mode.
        int task = PreferenceManager.
                getDefaultSharedPreferences(getApplicationContext()).
                getBoolean("key_overwrite_all_tags_automatic_mode", true) ?
                AudioTagger.MODE_OVERWRITE_ALL_TAGS : AudioTagger.MODE_WRITE_ONLY_MISSING;

        InputCorrectionParams inputCorrectionParams = new InputCorrectionParams();
                AndroidUtils.createInputParams(results.get(0), inputCorrectionParams);
        inputCorrectionParams.setCodeRequest(task);

        mMetadataWriter = new MetadataWriter(new AsyncOperation<Track, MetadataWriterResult, Track, MetadataWriterResult>() {
            @Override
            public void onAsyncOperationStarted(Track params) {
                mFixerState = FixerState.STARTING_FIXING;
                onCorrectionStarted(params);
            }

            @Override
            public void onAsyncOperationFinished(MetadataWriterResult result) {
                mFixerState = FixerState.FINISHED_FIXING;
                mMetadataWriter = null;
                onCorrectionCompleted(result);
            }

            @Override
            public void onAsyncOperationCancelled(Track cancellation) {
                mFixerState = FixerState.CANCELLED_FIXING;
                mMetadataWriter = null;
                onCorrectionCancelled(cancellation);
            }

            @Override
            public void onAsyncOperationError(MetadataWriterResult result1) {
                mFixerState = FixerState.ERROR_FIXING;
                mMetadataWriter = null;
                onOperationError(result1);
            }
        }, new AudioMetadataTagger(mTagger), inputCorrectionParams, track);

        mMetadataWriter.executeOnExecutor(Executors.newSingleThreadExecutor(), getApplicationContext());
    }



    public void onCorrectionCompleted(MetadataWriterResult result) {

        boolean shouldRename = PreferenceManager.
                getDefaultSharedPreferences(getApplicationContext()).
                getBoolean("key_rename_file_automatic_mode", true);

        String newName = AndroidUtils.createName(result.getTrack());

        if(shouldRename && newName != null) {
            mFileRenamer = new FileRenamer(new AsyncOperation<Track, AudioTagger.ResultRename, Track, AudioTagger.ResultRename>() {
                @Override
                public void onAsyncOperationStarted(Track params) {
                    mFixerState = FixerState.STARTING_RENAMING;
                }

                @Override
                public void onAsyncOperationCancelled(Track cancellation) {
                    mFixerState = FixerState.CANCELLED_RENAMING;
                    mFileRenamer = null;
                }

                @Override
                public void onAsyncOperationFinished(ResultRename result) {
                    mFixerState = FixerState.FINISHED_RENAMING;
                    mFileRenamer = null;
                    Track track = ((TrackResultRename)result).getTrack();
                    finishTrack(track);
                }

                @Override
                public void onAsyncOperationError(ResultRename error) {
                    mFixerState = FixerState.ERROR_RENAMING;
                    mFileRenamer = null;

                    onOperationError(error);
                }
            }, new AudioMetadataTagger(mTagger), result.getTrack(),newName);
        }
        else {
            Track track = result.getTrack();
            finishTrack(track);
        }
    }

    private void finishTrack(Track track) {
        startNotification(TrackUtils.getFilename(track.getPath()),
                getString(R.string.success), "", track.getMediaStoreId() );

        track.setChecked(0);
        track.setProcessing(0);
        updateTrack(track);

        popFirstFromList();
        mServiceState = ServiceState.NOT_RUNNING;
        //isRunning = false;
        shouldContinue();
    }

    private void onCorrectionStarted(Track track) {
        startNotification(TrackUtils.getFilename(track.getPath()), getString(R.string.starting_correction),
                getString(R.string.applying_tags), track.getMediaStoreId() );
    }

    public void onCorrectionCancelled(Track track) {
        broadcastMessage(getString(R.string.task_cancelled));
        track.setChecked(0);
        track.setProcessing(0);
        updateTrack(track);

        mServiceState = ServiceState.NOT_RUNNING;
        //isRunning = false;
        broadcastCompleteCorrection();
        stopServiceAndRemoveFromForeground();
    }

    private void onOperationError(MetadataWriterResult result) {
        String error = AndroidUtils.AudioTaggerErrorDescription.
                getErrorMessage(this, result.getResultCorrection().getCode());

        Track track = result.getTrack();

        startNotification(getString(R.string.error_in_correction), getString(R.string.error_in_correction),
                error, track.getMediaStoreId());
        broadcastMessage(error);

        track.setChecked(0);
        track.setProcessing(0);
        updateTrack(track);

        popFirstFromList();
        mServiceState = ServiceState.NOT_RUNNING;
        shouldContinue();
    }

    private void onOperationError(ResultRename resultRename) {
        String error = AndroidUtils.AudioTaggerErrorDescription.
            getErrorMessage(this, resultRename.getCode());

        Track track = ((TrackResultRename)resultRename).getTrack();

        startNotification(getString(R.string.error_in_correction), getString(R.string.error_in_correction),
            error, track.getMediaStoreId());
        broadcastMessage(error);

        track.setChecked(0);
        track.setProcessing(0);
        updateTrack(track);

        popFirstFromList();
        mServiceState = ServiceState.NOT_RUNNING;
        shouldContinue();
    }


    private void updateTrack(Track track) {
        if(mTrackRepository != null) {
            mTrackRepository.update(track);
        }
    }

    private void popFirstFromList() {
        if(mTrackIds != null && mTrackIds.size() > 0)
            mTrackIds.remove(0);
    }


    public void identificationCancelled(Track track) {
        track.setChecked(0);
        track.setProcessing(0);
        updateTrack(track);
        broadcastCompleteCorrection();
        broadcastMessage(getString(R.string.task_cancelled));
        stopServiceAndRemoveFromForeground();
        mServiceState = ServiceState.NOT_RUNNING;
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
        mTrackIds.clear();
        mTrackIds = null;
        mResourceManager = null;
        mRecycledIntent = null;
    }

    private void stopServiceAndRemoveFromForeground() {
        stopSelf();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            stopForeground(true);
    }

    private void stopTasks(){
        if(mFixerState == FixerState.STARTING_IDENTIFYING) {
            stopIdentification();
        }
        else if(mFixerState == FixerState.STARTING_FIXING) {
            stopCorrection();
        }
        else if(mFixerState == FixerState.STARTING_RENAMING) {
            stopRenaming();
        }
        else {
            stopRetrievingTrack();
        }
    }

    private void broadcastStartingCorrection(){
        mRecycledIntent.setAction(Constants.Actions.ACTION_START_TASK);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(mRecycledIntent);
    }

    private void broadcastCompleteCorrection(){
        mRecycledIntent.setAction(Constants.Actions.ACTION_COMPLETE_TASK);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(mRecycledIntent);
    }

    private void broadcastCorrectionForId(Track track) {
        mRecycledIntent.setAction(Constants.Actions.START_PROCESSING_FOR);
        mRecycledIntent.putExtra(Constants.MEDIA_STORE_ID, track.getMediaStoreId());
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcastSync(mRecycledIntent);
    }

    private void broadcastMessage(String message) {
        mRecycledIntent.setAction(Constants.Actions.ACTION_BROADCAST_MESSAGE);
        mRecycledIntent.putExtra("message", message);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(mRecycledIntent);
    }

    private void stopIdentification(){
        if(mIdentifier != null){
            mIdentifier.cancel();
        }
        mIdentifier = null;
    }

    private void stopCorrection(){
        if(mMetadataWriter != null && (mMetadataWriter.getStatus() == AsyncTask.Status.PENDING
                || mMetadataWriter.getStatus() == AsyncTask.Status.RUNNING)){
            mMetadataWriter.cancel(true);
        }
        mMetadataWriter = null;
    }

    private void stopRetrievingTrack(){
        if(mTrackInformationLoader != null && (mTrackInformationLoader.getStatus() == AsyncTask.Status.PENDING
                || mTrackInformationLoader.getStatus() == AsyncTask.Status.RUNNING)){
            mTrackInformationLoader.cancel(true);
        }
        mTrackInformationLoader = null;
    }


    private void stopRenaming() {
        if(mFileRenamer != null && (mFileRenamer.getStatus() == AsyncTask.Status.PENDING
                || mFileRenamer.getStatus() == AsyncTask.Status.RUNNING)) {
            mFileRenamer.cancel(true);
        }
        mFileRenamer = null;
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
        stopTaskIntent.setAction(Constants.Actions.ACTION_COMPLETE_TASK);
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
                    .setColor(ContextCompat.getColor(getApplicationContext(), R.color.primaryColor))
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

    private enum ServiceState {
        RUNNING,
        NOT_RUNNING
    }
}



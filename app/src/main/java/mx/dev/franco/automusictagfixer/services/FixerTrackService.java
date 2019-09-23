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
import mx.dev.franco.automusictagfixer.UI.main.MainActivity;
import mx.dev.franco.automusictagfixer.fixer.AudioMetadataTagger;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;
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
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.TrackUtils;
import mx.dev.franco.automusictagfixer.utilities.resource_manager.ResourceManager;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;

/**
 * Created by franco on 17/08/17.
 */

public class FixerTrackService extends Service {
    public static String CLASS_NAME = FixerTrackService.class.getName();
    //Notification on status bar
    private Notification mNotification;
    private Identifier<Track, List<Identifier.IdentificationResults>> mIdentifier;
    private MetadataWriter mMetadataWriter;
    private TrackInformationLoader mTrackInformationLoader;
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

    private TrackIdLoader mTrackIdLoader;
    private boolean isRunning = false;
    private List<Integer> mTrackIds = new ArrayList<>();
    private String messageFinishTask = "";
    private boolean mIsCancelled = false;
    private ServiceState mServiceState = ServiceState.IDLE;
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
            if(action.equals(Constants.Actions.ACTION_STOP_TASK)){
                mIsCancelled = true;
                if(mServiceState == ServiceState.RUNNING) {
                    stopAsyncTasks();
                    broadcastMessage(getString(R.string.task_cancelled));
                }
                else {
                    notifyFinished();
                    stopAndRemoveFromForeground();
                }
            }
            else {
                mServiceState = FixerState.STARTING;
                SharedPreferences sharedPreferences =
                        getSharedPreferences(Constants.Application.FULL_QUALIFIED_NAME, Context.MODE_PRIVATE);
                String order = sharedPreferences.getString(Constants.SORT_KEY, null);

                mTrackIdLoader = new TrackIdLoader(new AsyncOperation<Void, List<Integer>, Void, Void>() {
                    @Override
                    public void onAsyncOperationFinished(List<Integer> result) {
                        mTrackIdLoader = null;
                        if(result.isEmpty()) {
                            messageFinishTask = getString(R.string.no_songs_to_correct);
                            notifyFinished();
                            stopAndRemoveFromForeground();
                        }
                        else {
                            mTrackIds.addAll(result);
                            shouldContinue();
                        }
                    }

                }, mTrackRoomDatabase);
                mTrackIdLoader.executeOnExecutor(Executors.newSingleThreadExecutor(), order);
            }
        }
        return super.onStartCommand(intent,flags,startId);
    }

    private void startCorrection(){
        if(!canContinue()){
            messageFinishTask = getString(R.string.initializing_recognition_api);
            notifyFinished();
            stopAndRemoveFromForeground();
        }
        else if(mIsCancelled){
            messageFinishTask = getString(R.string.task_cancelled);
            notifyFinished();
            stopAndRemoveFromForeground();
        }
        else if (!isRunning){
            mTrackInformationLoader = new TrackInformationLoader(new AsyncOperation<Void, List<Track>, Void, Void>() {
                @Override
                public void onAsyncOperationFinished(List<Track> result) {
                    processTrack(result);
                }
            }, mTrackRoomDatabase);
            mTrackInformationLoader.executeOnExecutor(Executors.newSingleThreadExecutor(), mTrackIds.get(0));
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
        if(mIsCancelled){
            messageFinishTask = getString(R.string.task_cancelled);
            notifyFinished();
            stopAndRemoveFromForeground();
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
                        mServiceState = FixerState.IDENTIFYING;
                        onStartIdentification(file);
                    }

                    @Override
                    public void onIdentificationFinished(List<Identifier.IdentificationResults> result, Track file) {
                        mTrackInformationLoader = null;
                        mServiceState = FixerState.STOPPED;
                        identificationFound(result, file);
                    }

                    @Override
                    public void onIdentificationError(Track file, String error) {
                        mServiceState = FixerState.STOPPED;
                        mTrackInformationLoader = null;
                    }

                    @Override
                    public void onIdentificationCancelled(Track file) {
                        mServiceState = FixerState.STOPPED;
                        mTrackInformationLoader = null;
                    }

                    @Override
                    public void onIdentificationNotFound(Track file) {
                        mServiceState = FixerState.STOPPED;
                        mTrackInformationLoader = null;
                    }
                });

                mIdentifier.identify(data.get(0));
            }
        }
    }


    private void onStartIdentification(Track track) {
        isRunning = true;
        Intent intent = new Intent(Constants.Actions.START_PROCESSING_FOR);
        intent.putExtra(Constants.MEDIA_STORE_ID, track.getMediaStoreId());
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcastSync(intent);
        if(mTrackRepository != null) {
            track.setChecked(1);
            track.setProcessing(1);
            mTrackRepository.update(track);
        }
        startNotification(TrackUtils.getPath(track.getPath()),
                getString(R.string.correction_in_progress),
                getString(R.string.identifying), track.getMediaStoreId());
    }

    private void identificationError(String error, Track track) {
        startNotification(getString(R.string.correction_in_progress), "", error, track.getMediaStoreId() );

        Intent intent = new Intent(Constants.Actions.FINISH_TRACK_PROCESSING);
        intent.putExtra("error", error);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcastSync(intent);

        if(mTrackRepository != null) {
            track.setChecked(0);
            track.setProcessing(0);
            mTrackRepository.update(track);
        }

        isRunning = false;
        if(mTrackIds != null && mTrackIds.size()>0)
            mTrackIds.remove(0);
        shouldContinue();
    }

    private void identificationNotFound(Track track) {
        if(mIdentifier != null){
            mIdentifier = null;
        }
        startNotification(TrackUtils.getPath(track.getPath()),getString(R.string.correction_in_progress),
                getString(R.string.no_match_found), track.getMediaStoreId() );

        Intent intent = new Intent(Constants.Actions.FINISH_TRACK_PROCESSING);
        intent.putExtra(Constants.MEDIA_STORE_ID, track.getMediaStoreId());
        intent.putExtra("processing", false);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcastSync(intent);

        if(mTrackRepository != null) {
            track.setChecked(0);
            track.setProcessing(0);
            mTrackRepository.update(track);
        }
        isRunning = false;
        if(mTrackIds != null && mTrackIds.size()>0)
            mTrackIds.remove(0);
        shouldContinue();
    }

    public void identificationFound(List<Identifier.IdentificationResults> result, Track track) {
        startNotification(TrackUtils.getPath(track.getPath()), getString(R.string.match_found),
                getString(R.string.starting_correction), track.getMediaStoreId() );
        mMetadataWriter = new MetadataWriter(new AsyncOperation<Track, MetadataWriterResult, Track, MetadataWriterResult>() {
            @Override
            public void onAsyncOperationStarted(Track params) {
                onCorrectionStarted(params);
            }

            @Override
            public void onAsyncOperationFinished(MetadataWriterResult result) {
                onCorrectionCompleted(result);
                mMetadataWriter = null;
            }

            @Override
            public void onAsyncOperationCancelled(Track cancellation) {
                onCorrectionCancelled(cancellation);
                mMetadataWriter = null;
            }

            @Override
            public void onAsyncOperationError(MetadataWriterResult result1) {
                onCorrectionError(result1);
            }
        }, new AudioMetadataTagger(mTagger), new AudioMetadataTagger.InputParams(), track);

        mMetadataWriter.executeOnExecutor(Executors.newSingleThreadExecutor());
    }



    public void onCorrectionCompleted(MetadataWriterResult result) {
        Track track = result.getTrack();
        startNotification(TrackUtils.getFilename(track.getPath()), getString(R.string.success), "", track.getMediaStoreId() );

        if(mTrackRepository != null) {
            track.setChecked(0);
            track.setProcessing(0);
            mTrackRepository.update(track);
        }

        if(mTrackIds != null && mTrackIds.size()>0)
            mTrackIds.remove(0);

        isRunning = false;

        shouldContinue();
    }

    public void onCorrectionCancelled(Track track) {
        messageFinishTask = getString(R.string.task_cancelled);
        if(mTrackRepository != null) {
            track.setChecked(0);
            track.setProcessing(0);
            mTrackRepository.update(track);
        }

        isRunning = false;
        notifyFinished();
        stopAndRemoveFromForeground();
    }

    public void onCorrectionError(MetadataWriterResult result) {
        String error = AndroidUtils.AudioTaggerErrorDescription.getErrorMessage(this, result.getResultCorrection().getCode());
        Track track = result.getTrack();

        startNotification(getString(R.string.error_in_correction), getString(R.string.error_in_correction),
                error, track.getMediaStoreId());
        broadcastMessage(error);

        track.setChecked(0);
        track.setProcessing(0);

        updateTrack(track);

        popFirstFromList();

        isRunning = false;

        shouldContinue();
    }

    private void updateTrack(Track track) {
        if(mTrackRepository != null) {
            mTrackRepository.update(track);
        }
    }

    private void popFirstFromList() {
        if(mTrackIds != null && mTrackIds.size()>0)
            mTrackIds.remove(0);
    }


    public void onIdentificationCancelled(String cancelledReason, Track track) {
        messageFinishTask = getString(R.string.task_cancelled);

        if(mTrackRepository != null && track != null) {
            track.setChecked(0);
            track.setProcessing(0);
            mTrackRepository.update(track);
        }
        notifyFinished();
        stopAndRemoveFromForeground();
        isRunning = false;
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

    private void stopAndRemoveFromForeground() {
        stopSelf();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            stopForeground(true);
    }

    private void stopAsyncTasks(){
        if(mFixerState == FixerState.IDENTIFYING) {
            stopIdentification();
        }
        else if(mFixerState == FixerState.FIXING) {
            stopCorrection();
        }
        else {
            stopRetrievingTrack();
        }
    }

    private void notifyFinished(){
        broadcastCompleteCorrection();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            stopForeground(true);
    }

    private void broadcastCompleteCorrection(){
        mRecycledIntent.setAction(Constants.Actions.ACTION_COMPLETE_TASK);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(mRecycledIntent);
    }

    private void broadcastMessage(String message) {
        mRecycledIntent.setAction(Constants.Actions.ACTION_BROADCAST_MESSAGE);
        mRecycledIntent.putExtra("message", message);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(mRecycledIntent);
    }

    private void broadcastStartingCorrection(){
        mRecycledIntent.setAction(Constants.Actions.ACTION_START_TASK);
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
                    .setColor(ContextCompat.getColor(getApplicationContext(), R.color.grey_800))
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

    public void onCorrectionStarted(Track track) {
        startNotification(TrackUtils.getFilename(track.getPath()), getString(R.string.starting_correction),
                getString(R.string.applying_tags), track.getMediaStoreId() );
    }

    private void shouldContinue(){
        if(mIsCancelled){
            notifyFinished();
            stopAndRemoveFromForeground();
        }
        else {
            if(mTrackIds != null && !mTrackIds.isEmpty()){
                startCorrection();
            }
            else {
                messageFinishTask = getString(R.string.complete_task);
                notifyFinished();
                stopAndRemoveFromForeground();
            }
        }
    }

    enum FixerState {
        FETCHING_TRACKS,
        FETCHING_TRACK_INFO,
        IDENTIFYING,
        FIXING,
        RENAMING
    }

    enum ServiceState {
        IDLE,
        RUNNING,
        NOT_RUNNING
    }
}



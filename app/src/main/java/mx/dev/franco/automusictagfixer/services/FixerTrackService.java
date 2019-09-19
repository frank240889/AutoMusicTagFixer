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
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.BuildConfig;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.UI.main.MainActivity;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;
import mx.dev.franco.automusictagfixer.fixer.FileTagger;
import mx.dev.franco.automusictagfixer.fixer.FixerService;
import mx.dev.franco.automusictagfixer.fixer.IdLoader;
import mx.dev.franco.automusictagfixer.fixer.MetadataFixer;
import mx.dev.franco.automusictagfixer.fixer.TrackLoader;
import mx.dev.franco.automusictagfixer.identifier.GnApiService;
import mx.dev.franco.automusictagfixer.identifier.GnIdentifier;
import mx.dev.franco.automusictagfixer.identifier.Identifier;
import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.interfaces.InfoTrackLoader;
import mx.dev.franco.automusictagfixer.persistence.repository.TrackRepository;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.persistence.room.TrackRoomDatabase;
import mx.dev.franco.automusictagfixer.receivers.ResponseReceiver;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.Tagger;
import mx.dev.franco.automusictagfixer.utilities.TrackUtils;
import mx.dev.franco.automusictagfixer.utilities.resource_manager.ResourceManager;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;

/**
 * Created by franco on 17/08/17.
 */

public class FixerTrackService extends Service implements
        FixerService.OnCorrectionListener, InfoTrackLoader<List<Track>>
        ,ResponseReceiver.OnResponse{
    public static String CLASS_NAME = FixerTrackService.class.getName();
    //Notification on status bar
    private Notification mNotification;
    private static Identifier<Track, List<Identifier.IdentificationResults>> sIdentifier;
    private static MetadataFixer sFixer;
    private static TrackLoader sTrackDataLoader;
    @Inject
    TrackRepository mTrackRepository;
    @Inject
    TrackRoomDatabase trackRoomDatabase;
    @Inject
    ResourceManager resourceManager;
    @Inject
    GnApiService mGnApiService;
    @Inject
    AudioTagger tagger;
    @Inject
    AbstractSharedPreferences sharedPreferences;

    private static IdLoader sIdLoader;
    private boolean isRunning = false;
    private List<Integer> mIds = new ArrayList<>();
    private ResponseReceiver mReceiver;
    private String messageFinishTask = "";
    private boolean mIsCancelled = false;
    private FixerState mFixerState;


    /**
     * Creates a Service.  Invoked by your subclass's constructor.
     */
    public FixerTrackService() {
        super();
    }

    @Override
    public void onCreate(){
        super.onCreate();
        notifyStartingCorrection();
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
        //When setting "Corrección en segundo plano" is on,
        //then the service will be able to run in background,
        //and a correction won't stop when app closes, but when you explicitly
        //stop the task by pressing stop button in main screen or notification or task finishes.
        String action = intent.getAction();
        if(action != null && action.equals(Constants.Actions.ACTION_STOP_TASK)){
            messageFinishTask = getString(R.string.task_cancelled);
            mIsCancelled = true;
            if(isRunning)
                stopAsyncTasks();
            else {
                notifyFinished();
                stopSelf();
            }
        }
        else {
            SharedPreferences sharedPreferences =
                    getSharedPreferences(Constants.Application.FULL_QUALIFIED_NAME, Context.MODE_PRIVATE);
            String order = sharedPreferences.getString(Constants.SORT_KEY, null);

            sIdLoader = new IdLoader(new AsyncOperation<Void, List<Integer>, Void, Void>() {
                @Override
                public void onAsyncOperationFinished(List<Integer> result) {
                    sIdLoader = null;
                    if(result.isEmpty()) {
                        messageFinishTask = getString(R.string.no_songs_to_correct);
                        notifyFinished();
                        stopSelf();
                    }
                    else {
                        mIds.addAll(result);
                        shouldContinue();
                    }
                }

            }, trackRoomDatabase);
            sIdLoader.executeOnExecutor(Executors.newSingleThreadExecutor(), order);
        }
        return super.onStartCommand(intent,flags,startId);
    }

    private void startCorrection(){
        if(!canContinue()){
            messageFinishTask = getString(R.string.initializing_recognition_api);
            notifyFinished();
            stopSelf();
        }
        else if(mIsCancelled){
            messageFinishTask = getString(R.string.task_cancelled);
            notifyFinished();
            stopSelf();
        }
        else if (!isRunning){
            sTrackDataLoader = new TrackLoader(new AsyncOperation<Void, List<Track>, Void, Void>() {
                @Override
                public void onAsyncOperationFinished(List<Track> result) {
                    processTrack(result);
                }
            }, trackRoomDatabase);
            sTrackDataLoader.executeOnExecutor(Executors.newSingleThreadExecutor(), mIds.get(0));
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
            stopSelf();
        }
        else {
            if(AudioTagger.checkFileIntegrity(data.get(0).getPath())) {
                identificationError(getString(R.string.could_not_read_file), data.get(0));
            }
            else {
                sIdentifier = new GnIdentifier(mGnApiService, sharedPreferences);
                sIdentifier.registerCallback(new Identifier.IdentificationListener<List<Identifier.IdentificationResults>, Track>() {
                    @Override
                    public void onIdentificationStart(Track file) {
                        mFixerState = FixerState.IDENTIFYING;
                        onStartIdentification(file);
                    }

                    @Override
                    public void onIdentificationFinished(List<Identifier.IdentificationResults> result, Track file) {
                        sTrackDataLoader = null;
                        identificationFound(result, file);
                    }

                    @Override
                    public void onIdentificationError(Track file, String error) {
                        sTrackDataLoader = null;
                    }

                    @Override
                    public void onIdentificationCancelled(Track file) {
                        sTrackDataLoader = null;
                    }

                    @Override
                    public void onIdentificationNotFound(Track file) {
                        sTrackDataLoader = null;
                    }
                });

                sIdentifier.identify(data.get(0));
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
        if(mIds!= null && mIds.size()>0)
            mIds.remove(0);
        shouldContinue();
    }

    private void identificationNotFound(Track track) {
        if(sIdentifier != null){
            sIdentifier = null;
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
        if(mIds != null && mIds.size()>0)
            mIds.remove(0);
        shouldContinue();
    }

    public void identificationFound(List<Identifier.IdentificationResults> result, Track track) {
        startNotification(TrackUtils.getPath(track.getPath()), getString(R.string.match_found),
                getString(R.string.starting_correction), track.getMediaStoreId() );
        sFixer = new MetadataFixer(new AsyncOperation<Track, MetadataFixer.Result, Track, MetadataFixer.Error>() {
            @Override
            public void onAsyncOperationStarted(Track params) {

            }

            @Override
            public void onAsyncOperationFinished(MetadataFixer.Result result) {

            }

            @Override
            public void onAsyncOperationCancelled(Track cancellation) {

            }

            @Override
            public void onAsyncOperationError(MetadataFixer.Error error) {

            }
        }, new FileTagger(tagger), new FileTagger.InputParams(), track);

        sFixer.executeOnExecutor(Executors.newSingleThreadExecutor());
    }

    public void onIdentificationCancelled(String cancelledReason, Track track) {
        messageFinishTask = getString(R.string.task_cancelled);

        if(mTrackRepository != null && track != null) {
            track.setChecked(0);
            track.setProcessing(0);
            mTrackRepository.update(track);
        }
        notifyFinished();
        stopSelf();
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
        super.onDestroy();
        //remove this service from foreground and close the notification
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mReceiver);
        mReceiver.clearReceiver();
        mReceiver = null;
        mIds.clear();
        mIds = null;
        resourceManager = null;
        Log.d("destroy","releasing resources...");
    }

    private void stopAsyncTasks(){
        stopIdentification();
        stopCorrection();
        stopRetrievingTrack();
    }

    private void notifyFinished(){
        notifyCompleteCorrection();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            stopForeground(true);
    }

    private void stopIdentification(){
        if(sIdentifier != null){
            sIdentifier.cancel();
        }
        sIdentifier = null;
    }

    private void stopCorrection(){
        if(sFixer != null && (sFixer.getStatus() == AsyncTask.Status.PENDING
                || sFixer.getStatus() == AsyncTask.Status.RUNNING)){
            sFixer.cancel(true);
        }
        sFixer = null;
    }

    private void stopRetrievingTrack(){
        if(sTrackDataLoader != null && (sTrackDataLoader.getStatus() == AsyncTask.Status.PENDING
                || sTrackDataLoader.getStatus() == AsyncTask.Status.RUNNING)){
            sTrackDataLoader.cancel(true);
        }
        sTrackDataLoader = null;
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

    private void notifyCompleteCorrection(){
        Intent intentFinishTask = new Intent(Constants.Actions.ACTION_COMPLETE_TASK);
        intentFinishTask.putExtra("message", messageFinishTask);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcastSync(intentFinishTask);
    }

    private void notifyStartingCorrection(){
        Intent results = new Intent(Constants.Actions.ACTION_START_TASK);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(results);
    }

    @Override
    public void onCorrectionStarted(Track track) {
        startNotification(TrackUtils.getFilename(track.getPath()), getString(R.string.starting_correction),
                getString(R.string.applying_tags), track.getMediaStoreId() );
    }

    @Override
    public void onCorrectionCompleted(Tagger.ResultCorrection resultCorrection, Track track) {
        startNotification(TrackUtils.getFilename(track.getPath()), getString(R.string.success), "", track.getMediaStoreId() );

        if(mTrackRepository != null) {
            track.setChecked(0);
            track.setProcessing(0);
            mTrackRepository.update(track);
        }

        if(mIds != null && mIds.size()>0)
            mIds.remove(0);

        isRunning = false;

        shouldContinue();
    }

    @Override
    public void onCorrectionCancelled(Track track) {
        messageFinishTask = getString(R.string.task_cancelled);
        if(mTrackRepository != null) {
            track.setChecked(0);
            track.setProcessing(0);
            mTrackRepository.update(track);
        }

        isRunning = false;
        notifyFinished();
        stopSelf();
    }

    @Override
    public void onCorrectionError(Tagger.ResultCorrection resultCorrection, Track track) {
        startNotification(getString(R.string.error_in_correction), getString(R.string.error_in_correction),
                getString(R.string.could_not_correct_file), track.getMediaStoreId()  );

        Intent intent = new Intent(Constants.Actions.ACTION_SD_CARD_ERROR);
        intent.putExtra("error", FixerService.ERROR_CODES.
                getErrorMessage(this, resultCorrection.code));
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcastSync(intent);

        if(mTrackRepository != null) {
            track.setChecked(0);
            track.setProcessing(0);
            mTrackRepository.update(track);
        }

        if(mIds != null && mIds.size()>0)
            mIds.remove(0);
        isRunning = false;

        shouldContinue();
    }

    private void shouldContinue(){
        if(mIsCancelled){
            notifyFinished();
            stopSelf();
        }
        else {
            if(mIds != null && !mIds.isEmpty()){
                startCorrection();
            }
            else {
                messageFinishTask = getString(R.string.complete_task);
                notifyFinished();
                stopSelf();
            }
        }
    }

    @Override
    public void onResponse(Intent intent) {
        String action = intent.getAction();
        if(action != null && (action.equals(Constants.Actions.ACTION_CONNECTION_LOST) ||
        action.equals(Intent.ACTION_MEDIA_MOUNTED) || action.equals(Intent.ACTION_MEDIA_UNMOUNTED))){
            stopAsyncTasks();
        }
    }


    enum FixerState {
        IDENTIFYING,
        FIXING
    }
}



package mx.dev.franco.automusictagfixer.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
import mx.dev.franco.automusictagfixer.BuildConfig;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.UI.main.MainActivity;
import mx.dev.franco.automusictagfixer.fixer.Fixer;
import mx.dev.franco.automusictagfixer.fixer.IdLoader;
import mx.dev.franco.automusictagfixer.fixer.TrackLoader;
import mx.dev.franco.automusictagfixer.identifier.GnResponseListener;
import mx.dev.franco.automusictagfixer.identifier.GnService;
import mx.dev.franco.automusictagfixer.identifier.TrackIdentifier;
import mx.dev.franco.automusictagfixer.interfaces.TrackListLoader;
import mx.dev.franco.automusictagfixer.interfaces.InfoTrackLoader;
import mx.dev.franco.automusictagfixer.network.ConnectivityDetector;
import mx.dev.franco.automusictagfixer.persistence.repository.TrackRepository;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.persistence.room.TrackRoomDatabase;
import mx.dev.franco.automusictagfixer.receivers.ResponseReceiver;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.Tagger;
import mx.dev.franco.automusictagfixer.utilities.TrackUtils;
import mx.dev.franco.automusictagfixer.utilities.resource_manager.ResourceManager;

/**
 * Created by franco on 17/08/17.
 */

public class FixerTrackService extends Service implements GnResponseListener.GnListener,
        Fixer.OnCorrectionListener, TrackListLoader<List<Integer>>, InfoTrackLoader<List<Track>>
        ,ResponseReceiver.OnResponse{
    public static String CLASS_NAME = FixerTrackService.class.getName();
    //Notification on status bar
    private Notification mNotification;
    private static volatile TrackIdentifier sIdentifier;
    private static Fixer sFixer;
    private static TrackLoader sTrackDataLoader;
    @Inject
    TrackRepository mTrackRepository;
    @Inject
    TrackRoomDatabase trackRoomDatabase;
    @Inject
    ResourceManager resourceManager;
    private static IdLoader sIdLoader;
    private boolean isRunning = false;
    private List<Integer> mIds = new ArrayList<>();
    private ResponseReceiver mReceiver;
    private String messageFinishTask = "";
    private boolean mIsCancelled = false;


    /**
     * Creates a Service.  Invoked by your subclass's constructor.
     */
    public FixerTrackService() {
        super();
    }

    @Override
    public void onCreate(){
        super.onCreate();
        AutoMusicTagFixer.getContextComponent().inject(this);
        setupReceiver();
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
        super.onStartCommand(intent,flags,startId);
        //When setting "Corrección en segundo plano" is on,
        //then the service will be able to run in background,
        //and a correction won't stop when app closes, but when you explicitly
        //stop the task by pressing stop button in main screen or notification or task finishes.
        String action = intent.getAction();
        if(action != null && action.equals(Constants.Actions.ACTION_COMPLETE_TASK)){
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
            sIdLoader = new IdLoader(this, trackRoomDatabase);
            sIdLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, order);
        }
        return START_NOT_STICKY;
    }


    @Override
    public void onDataLoaded(List<Integer> tracks) {
        sIdLoader = null;
        if(tracks.isEmpty()) {
            messageFinishTask = getString(R.string.no_songs_to_correct);
            notifyFinished();
            stopSelf();
        }
        else {
            mIds.addAll(tracks);
            shouldContinue();
        }
    }

    /**
     * Allows to register filters to handle
     * only certain actions sent by FixerTrackService
     */
    private void setupReceiver(){
        //create filters to listen for response from FixerTrackService
        IntentFilter mConnectionLost = new IntentFilter(Constants.Actions.ACTION_CONNECTION_LOST);
        IntentFilter mediaMounted = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
        IntentFilter mediaUnmounted = new IntentFilter(Intent.ACTION_MEDIA_UNMOUNTED);
        mReceiver = new ResponseReceiver(this, new Handler());
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mReceiver, mConnectionLost);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mReceiver, mediaMounted);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mReceiver, mediaUnmounted);

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
            sTrackDataLoader = new TrackLoader(this, trackRoomDatabase);
            sTrackDataLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mIds.get(0));
        }
    }


    private boolean canContinue(){
        if(!ConnectivityDetector.sIsConnected){
            ConnectivityDetector.getInstance(getApplicationContext()).onStartTestingNetwork();
            return false;
        }

        if(GnService.sIsInitializing || !GnService.sApiInitialized){
            GnService.getInstance(getApplicationContext()).initializeAPI();
            return false;
        }

        return true;
    }

    @Override
    public void onTrackDataLoaded(List<Track> data) {
        if(mIsCancelled){
            messageFinishTask = getString(R.string.task_cancelled);
            notifyFinished();
            stopSelf();
        }
        else {
            sIdentifier = new TrackIdentifier();
            sIdentifier.setResourceManager(resourceManager);
            sIdentifier.setTrack(data.get(0));
            sIdentifier.setGnListener(this);
            isRunning = true;
            try {
                AudioFileIO.read(new File(data.get(0).getPath()));
                startNotification(getString(R.string.correction_in_progress),
                        getString(R.string.correcting) +" " +
                                TrackUtils.getFilename(data.get(0).getPath()),
                        getString(R.string.starting_correction), data.get(0).getMediaStoreId());
                sIdentifier.execute();
            } catch (CannotReadException | IOException
                    | ReadOnlyFileException | TagException | InvalidAudioFrameException e) {
                e.printStackTrace();
                identificationError(getString(R.string.could_not_read_file), data.get(0));
            }
        }
        sTrackDataLoader = null;
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
            sIdentifier.cancelIdentification();
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
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcastSync(results);
    }

    /**************************IDENTIFICATION CALLBACKS
     * @param track************************/
    @Override
    public void onStartIdentification(Track track) {
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

    @Override
    public void statusIdentification(String status, Track track) {

    }

    @Override
    public void gatheringFingerprint(Track track) {
        startNotification(TrackUtils.getPath(track.getPath()),
                getString(R.string.correction_in_progress),
                getString(R.string.generating_fingerprint), track.getMediaStoreId());
    }

    @Override
    public void identificationError(String error, Track track) {
        if(sIdentifier != null){
            sIdentifier = null;
        }
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

    @Override
    public void identificationNotFound(Track track) {
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

    @Override
    public void identificationFound(GnResponseListener.IdentificationResults results, Track track) {
        if(sIdentifier != null){
            sIdentifier = null;
        }
        startNotification(TrackUtils.getPath(track.getPath()), getString(R.string.match_found),
                getString(R.string.starting_correction), track.getMediaStoreId() );
        sFixer = new Fixer(this);
        boolean shouldRename = PreferenceManager.
                getDefaultSharedPreferences(getApplicationContext()).
                getBoolean("key_rename_file_automatic_mode", true);
        int task = PreferenceManager.
                getDefaultSharedPreferences(getApplicationContext()).
                getBoolean("key_overwrite_all_tags_automatic_mode", true) ?
                Tagger.MODE_OVERWRITE_ALL_TAGS : Tagger.MODE_WRITE_ONLY_MISSING;

        sFixer.setShouldRename(shouldRename);
        sFixer.setTask(task);
        sFixer.setTrack(track);
        sFixer.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, results);
    }

    @Override
    public void identificationCompleted(Track track) {
        //Not used
    }

    @Override
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

    @Override
    public void status(String message) {

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
        intent.putExtra("error", Fixer.ERROR_CODES.
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
}



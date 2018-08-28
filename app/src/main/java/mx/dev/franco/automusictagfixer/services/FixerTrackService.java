package mx.dev.franco.automusictagfixer.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.UI.main.MainActivity;
import mx.dev.franco.automusictagfixer.list.AudioItem;
import mx.dev.franco.automusictagfixer.media_store_retriever.AsyncFileReader;
import mx.dev.franco.automusictagfixer.network.ConnectivityDetector;
import mx.dev.franco.automusictagfixer.receivers.ResponseReceiver;
import mx.dev.franco.automusictagfixer.repository.TrackRepository;
import mx.dev.franco.automusictagfixer.room.Track;
import mx.dev.franco.automusictagfixer.room.TrackRoomDatabase;
import mx.dev.franco.automusictagfixer.services.Fixer.DataLoader;
import mx.dev.franco.automusictagfixer.services.Fixer.DataTrackLoader;
import mx.dev.franco.automusictagfixer.services.Fixer.Fixer;
import mx.dev.franco.automusictagfixer.services.Fixer.IdLoader;
import mx.dev.franco.automusictagfixer.services.Fixer.TrackLoader;
import mx.dev.franco.automusictagfixer.services.gnservice.GnResponseListener;
import mx.dev.franco.automusictagfixer.services.gnservice.GnService;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.Tagger;

/**
 * Created by franco on 17/08/17.
 */

public class FixerTrackService extends Service implements GnResponseListener.GnListener,
        Fixer.OnCorrectionListener, DataLoader<List<Integer>>, DataTrackLoader<List<Track>>
        ,ResponseReceiver.OnResponse{
    public static String CLASS_NAME = FixerTrackService.class.getName();
    //Notification on status bar
    private Notification mNotification;
    private static TrackIdentifier sIdentifier;
    private static Fixer sFixer;
    private static TrackLoader sTrackDataLoader;
    @Inject
    TrackRepository mTrackRepository;
    @Inject
    TrackRoomDatabase trackRoomDatabase;
    private static IdLoader sIdLoader;
    private boolean isRunning = false;
    private List<Integer> mIds = new ArrayList<>();
    private static AsyncFileReader sAsyncFileReader;
    private ResponseReceiver mReceiver;


    /**
     * Creates a Service.  Invoked by your subclass's constructor.
     */
    public FixerTrackService() {
        super();
        Log.d(CLASS_NAME, "Constructor");
        AutoMusicTagFixer.getContextComponent().inject(this);
    }

    @Override
    public void onCreate(){
        super.onCreate();
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
        //Log.d(CLASS_NAME,"onStartCommand");

        //When setting "Corrección en segundo plano" is on,
        //then the service will be able to run in background,
        //and a correction won't stop when app closes, but when you explicitly
        //stop the task by pressing stop button in main screen or notification or task finishes.
        String action = intent.getAction();
        if(action != null && action.equals(Constants.Actions.ACTION_COMPLETE_TASK)){
            stopSelf();
        }
        else {
            int id = intent.getIntExtra(Constants.MEDIA_STORE_ID, -1);
            if (id == -1) {
                sIdLoader = new IdLoader(this, trackRoomDatabase);
                sIdLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, id);
            } else {
                mIds.add(id);
                startCorrection();
            }

        }
        return START_NOT_STICKY;//
    }


    @Override
    public void onDataLoaded(List<Integer> tracks) {
        mIds.addAll(tracks);
        sIdLoader = null;
        shouldContinue();
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
            stopSelf();
            return;
        }

        if(isRunning)
            return;

        sTrackDataLoader = new TrackLoader(this, trackRoomDatabase);
        sTrackDataLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mIds.get(0));
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
        //mCurrentTrack = data.get(0);
        sIdentifier = new TrackIdentifier(this);
        sIdentifier.setTrack(data.get(0));
        isRunning = true;
        try {
            AudioFileIO.read(new File(data.get(0).getPath()));
            startNotification("Corrección en progreso", "Corrigiendo " + AudioItem.getFilename(data.get(0).getPath()), "Iniciando corrección...", data.get(0).getMediaStoreId() );
            sIdentifier.execute();//executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (CannotReadException | IOException | ReadOnlyFileException | TagException | InvalidAudioFrameException e) {
            e.printStackTrace();
            identificationError(getString(R.string.could_not_read_file), data.get(0));
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
        //remove this service from foreground and close the notification
        stopIdentification();
        stopCorrection();
        stopRetrievingTrack();
        //stopUpdatingTrack();
        notifyCompleteCorrection();
        stopForeground(true);
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mReceiver);
        mReceiver.clearReceiver();
        mReceiver = null;
        System.gc();
        Log.d("destroy","releasing resources...");
        super.onDestroy();
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

    private void stopUpdatingTrack(){
        if(sAsyncFileReader != null && (sAsyncFileReader.getStatus() == AsyncTask.Status.PENDING
                || sAsyncFileReader.getStatus() == AsyncTask.Status.RUNNING)){
            sAsyncFileReader.cancel(true);
        }
        sTrackDataLoader = null;
    }

    /**
     * Starts a notification and set
     * this service as foreground service,
     * allowing run no matter if app closes
     * @param contentText the content text o notification
     * @param contentTitle the title of notification
     * @param status the status string to show in notification
     * @param mediaStoreId
     */
    private void startNotification(String contentText, String contentTitle, String status, int mediaStoreId) {

        Intent notificationIntent = new Intent(this,MainActivity.class);
        notificationIntent.setAction(Constants.ACTION_OPEN_MAIN_ACTIVITY);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
        notificationIntent.putExtra(Constants.MEDIA_STORE_ID, mediaStoreId);
        notificationIntent.putExtra("processing", true);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Intent stopTaskIntent = new Intent(this, FixerTrackService.class);
        stopTaskIntent.setAction(Constants.Actions.ACTION_COMPLETE_TASK);
        PendingIntent pendingStopIntent = PendingIntent.getService(this, 0, stopTaskIntent, 0);

        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                R.mipmap.ic_launcher);

            mNotification = new NotificationCompat.Builder(this, Constants.Application.FULL_QUALIFIED_NAME)
                    .setContentTitle(contentTitle != null ? contentTitle : "")
                    .setAutoCancel(true)
                    .setColor(ContextCompat.getColor(getApplicationContext(), R.color.grey_800))
                    .setTicker(getString(R.string.app_name))
                    .setSubText(contentText != null ? contentText : getString(R.string.fixing_task))
                    .setContentText(status != null ? status : "")
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .addAction(R.drawable.ic_stat_name, getString(R.string.stop), pendingStopIntent)
                    .build();

        startForeground(R.string.app_name, mNotification);

    }

    private void notifyCompleteCorrection(){
        Intent intentFinishTask = new Intent(Constants.Actions.ACTION_COMPLETE_TASK);
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
        intent.putExtra("processing", true);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcastSync(intent);
        if(track != null)
            track.setChecked(1);
        if(mTrackRepository != null)
            mTrackRepository.update(track);
        startNotification(AudioItem.getPath(track.getPath()),"Corrección en progreso", "Identificando...", track.getMediaStoreId()  );
    }

    @Override
    public void statusIdentification(String status, Track track) {
        //Intent intent = new Intent(Constants.Actions.STATUS);
        //intent.putExtra(Constants.TRACK_NAME, trackName);
        //intent.putExtra(Constants.STATUS, status);
        //LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcastSync(intent);
    }

    @Override
    public void gatheringFingerprint(Track track) {
        //Intent intent = new Intent(Constants.Actions.STATUS);
        //intent.putExtra(Constants.STATUS, String.format(getString(R.string.gathering_fingerprint),trackName) );
        //LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcastSync(intent);
        startNotification(AudioItem.getPath(track.getPath()),"Identificación en progreso", "Generando huella...", track.getMediaStoreId() );
    }

    @Override
    public void identificationError(String error, Track track) {
        if(sIdentifier != null){
            sIdentifier = null;
        }
        startNotification("Corrección en progreso", "", error, track.getMediaStoreId() );

        Intent intent = new Intent(Constants.Actions.FINISH_TRACK_PROCESSING);
        intent.putExtra(Constants.MEDIA_STORE_ID, track.getMediaStoreId());
        intent.putExtra("processing", false);
        intent.putExtra("error", error);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcastSync(intent);

        track.setChecked(0);
        if(mTrackRepository != null)
            mTrackRepository.update(track);

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
        startNotification(AudioItem.getPath(track.getPath()),"Identificación en progreso", "No se encontró ninguna coincidencia.", track.getMediaStoreId() );

        Intent intent = new Intent(Constants.Actions.FINISH_TRACK_PROCESSING);
        intent.putExtra(Constants.MEDIA_STORE_ID, track.getMediaStoreId());
        intent.putExtra("processing", false);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcastSync(intent);

        track.setChecked(0);
        if(mTrackRepository != null)
            mTrackRepository.update(track);

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
        startNotification(AudioItem.getPath(track.getPath()), "Coincidencia encontrada", "Iniciando corrección...", track.getMediaStoreId() );
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
        //Intent intent = new Intent(Constants.Actions.IDENTIFICATION_COMPLETE);
        //LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcastSync(intent);
    }

    @Override
    public void onIdentificationCancelled(String cancelledReason, Track track) {

        Intent intent = new Intent(Constants.Actions.FINISH_TRACK_PROCESSING);
        intent.putExtra("should_reload_cover", true);
        intent.putExtra(Constants.MEDIA_STORE_ID, track.getMediaStoreId());
        intent.putExtra("processing", false);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcastSync(intent);

        track.setChecked(0);
        if(mTrackRepository != null)
            mTrackRepository.update(track);

        isRunning = false;
        if(mIds != null && mIds.size()>0) {
            mIds.remove(0);
            mIds.clear();
            mIds = null;
        }
    }

    @Override
    public void status(String message) {

    }


    @Override
    public void onCorrectionStarted(Track track) {
        startNotification(AudioItem.getFilename(track.getPath()), getString(R.string.starting_correction), getString(R.string.applying_tags), track.getMediaStoreId() );
    }

    @Override
    public void onCorrectionCompleted(Tagger.ResultCorrection resultCorrection, Track track) {
        startNotification(AudioItem.getFilename(track.getPath()), getString(R.string.success), "", track.getMediaStoreId() );

        Intent intent = new Intent(Constants.Actions.FINISH_TRACK_PROCESSING);
        intent.putExtra("should_reload_cover", true);
        intent.putExtra(Constants.MEDIA_STORE_ID, track.getMediaStoreId());
        intent.putExtra("processing", false);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcastSync(intent);


        if(mTrackRepository != null) {
            track.setProcessing(false);
            track.setChecked(0);
            mTrackRepository.update(track);
        }

        if(mIds != null && mIds.size()>0)
            mIds.remove(0);
        isRunning = false;

        shouldContinue();
    }

    @Override
    public void onCorrectionCancelled(Track track) {

        Intent intent = new Intent(Constants.Actions.FINISH_TRACK_PROCESSING);
        intent.putExtra("should_reload_cover", true);
        intent.putExtra(Constants.MEDIA_STORE_ID, track.getMediaStoreId());
        intent.putExtra("processing", false);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcastSync(intent);

        if(mTrackRepository != null) {
            track.setProcessing(false);
            track.setChecked(0);
            mTrackRepository.update(track);
        }

        isRunning = false;
        if(mIds != null && mIds.size()>0) {
            mIds.remove(0);
            mIds.clear();
            mIds = null;
        }
    }

    @Override
    public void onCorrectionError(Tagger.ResultCorrection resultCorrection, Track track) {
        startNotification("Error", "Error", "No se pudo corregir el archivo.", track.getMediaStoreId()  );
        Log.d("Error", "eRROR");
        Intent intent = new Intent(Constants.Actions.ACTION_SD_CARD_ERROR);
        intent.putExtra("should_reload_cover", true);
        intent.putExtra(Constants.MEDIA_STORE_ID, track.getMediaStoreId());
        intent.putExtra("processing", false);
        intent.putExtra("error", Fixer.ERROR_CODES.getErrorMessage(this, resultCorrection.code));
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcastSync(intent);

        if(mTrackRepository != null) {
            track.setChecked(0);
            track.setProcessing(false);
            mTrackRepository.update(track);
        }

        if(mIds != null && mIds.size()>0)
            mIds.remove(0);
        isRunning = false;

        shouldContinue();
    }

    private void shouldContinue(){
        if(mIds != null &&!mIds.isEmpty()){
            startCorrection();
        }
        else {
            stopSelf();
        }
    }

    @Override
    public void onResponse(Intent intent) {
        String action = intent.getAction();
        if(action != null && (action.equals(Constants.Actions.ACTION_CONNECTION_LOST) ||
        action.equals(Intent.ACTION_MEDIA_MOUNTED) || action.equals(Intent.ACTION_MEDIA_UNMOUNTED))){
            stopSelf();
        }
    }
}



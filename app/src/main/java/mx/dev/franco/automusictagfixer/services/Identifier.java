package mx.dev.franco.automusictagfixer.services;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.gracenote.gnsdk.GnException;
import com.gracenote.gnsdk.GnLanguage;
import com.gracenote.gnsdk.GnLookupData;
import com.gracenote.gnsdk.GnMusicIdFile;
import com.gracenote.gnsdk.GnMusicIdFileInfo;
import com.gracenote.gnsdk.GnMusicIdFileInfoManager;
import com.gracenote.gnsdk.GnMusicIdFileProcessType;
import com.gracenote.gnsdk.GnMusicIdFileResponseType;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.repository.TrackRepository;
import mx.dev.franco.automusictagfixer.room.Track;
import mx.dev.franco.automusictagfixer.services.gnservice.GnResponseListener;
import mx.dev.franco.automusictagfixer.services.gnservice.GnService;

public class Identifier extends AsyncTask<Void, Void, Void> implements GnResponseListener.GnListener {
    public static final int ALL_TAGS = 0;
    public static final int JUST_COVER = 1;
    private static final String TAG = Identifier.class.getName();
    private volatile GnMusicIdFile mGnMusicIdFile;
    private GnResponseListener.GnListener mGnListener;
    private GnResponseListener mGnResponseListener;
    private GnMusicIdFileInfoManager mGnMusicIdFileInfoManager;
    private GnMusicIdFileInfo mGnMusicIdFileInfo;
    private Track mTrack;
    private volatile boolean mFinished = false;
    @Inject
    TrackRepository trackRepository;

    public Identifier(GnResponseListener.GnListener listener) {
        mGnListener = listener;
    }

    public void setTrack(Track track){
        mTrack = track;
    }

    @Override
    protected void onPreExecute(){
        mGnResponseListener = new GnResponseListener(this);
        mGnListener.onStartIdentification(mTrack.getTitle());
        try {
            mGnMusicIdFile = new GnMusicIdFile(GnService.sGnUser, mGnResponseListener);
        } catch (GnException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Void doInBackground(Void... voids) {
        mTrack.setChecked(1);
        //trackRepository.update(mTrack);
        //GnMusicIdFile object provides identification service
        try {

            if(isCancelled()){
                return null;
            }
            //set options of track id process
            mGnMusicIdFile.options().lookupData(GnLookupData.kLookupDataContent, true);
            if(isCancelled()){
                return null;
            }
            mGnMusicIdFile.options().preferResultLanguage(GnLanguage.kLanguageSpanish);
            //queue will be processed one by one
            if(isCancelled()){
                return null;
            }
            mGnMusicIdFile.options().batchSize(1);
            if(isCancelled()){
                return null;
            }
            //get the fileInfoManager
            mGnMusicIdFileInfoManager = mGnMusicIdFile.fileInfos();
            if(isCancelled()){
                return null;
            }
            mGnMusicIdFile.waitForComplete();
            if(isCancelled()){
                return null;
            }
            //add all info available for more accurate results.
            //Check if file already was previously added.
            mGnMusicIdFileInfo = mGnMusicIdFileInfoManager.add(mTrack.getPath());
            if(isCancelled()){
                return null;
            }
            mGnMusicIdFileInfo.fileName(mTrack.getPath());
            if(isCancelled()){
                return null;
            }
            mGnMusicIdFileInfo.trackTitle(mTrack.getTitle());
            if(isCancelled()){
                return null;
            }
            mGnMusicIdFileInfo.trackArtist(mTrack.getArtist());
            if(isCancelled()){
                return null;
            }
            mGnMusicIdFileInfo.albumTitle(mTrack.getAlbum());
            mGnMusicIdFileInfo.mediaId(mTrack.getMediaStoreId()+"");

            mGnMusicIdFile.doAlbumIdAsync(GnMusicIdFileProcessType.kQueryReturnSingle, GnMusicIdFileResponseType.kResponseAlbums);

            while (true){
                //Log.d("LOOP","LOOPING");
                if(mFinished) {
                    mGnMusicIdFile.delete();
                    mGnMusicIdFile = null;
                    Log.d(TAG,"FINISHED LOOPING");
                    break;
                }

                if(isCancelled()) {
                    mGnMusicIdFile.cancel();
                    mGnMusicIdFile.delete();
                    mGnMusicIdFile = null;
                    Log.d(TAG,"CANCELLED LOOPING");
                    break;
                }
            }
        }
        catch (GnException e) {
            e.printStackTrace();
            Crashlytics.logException(e);
            if(mGnListener != null) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(() -> mGnListener.identificationError(e.getMessage()));

            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void voids){
        Log.d(TAG, "POSTEXECUTE");
        mTrack.setChecked(0);
        clear();
    }


    @Override
    public void onCancelled(){
        Log.d(TAG, "CANCELLED");
        if(mGnListener != null)
            mGnListener.onIdentificationCancelled();
        mTrack.setChecked(0);
        clear();
    }

    public void cancelIdentification(){
        //mGnMusicIdFile.cancel();
        cancel(true);
    }

    private void clear(){
        mGnResponseListener = null;
        mGnListener = null;
        mTrack = null;
    }

    @Override
    public void statusIdentification(String status, String trackName) {
        if(mGnListener != null)
            mGnListener.statusIdentification(status, trackName);
    }

    @Override
    public void gatheringFingerprint(String trackName) {
        if(mGnListener != null)
            mGnListener.gatheringFingerprint(trackName);
    }

    @Override
    public void identificationError(String error) {
        if(mGnListener != null)
            mGnListener.identificationError(error);
    }

    @Override
    public void identificationNotFound(String trackName) {
        if(mGnListener != null)
            mGnListener.identificationNotFound(trackName);

        mFinished = true;
    }

    @Override
    public void identificationFound(GnResponseListener.IdentificationResults results) {
        if(mGnListener != null)
            mGnListener.identificationFound(results);
    }

    @Override
    public void identificationCompleted(String trackName) {
        if(mGnListener != null)
            mGnListener.identificationCompleted(trackName);

        mFinished = true;
    }

    @Override
    public void onStartIdentification(String trackName) {
        if(mGnListener != null)
            mGnListener.onStartIdentification(trackName);
    }

    @Override
    public void onIdentificationCancelled() {
        //mGnMusicIdFile.cancel();
        //mGnMusicIdFile.delete();
        //mGnMusicIdFile = null;
    }

    @Override
    public void status(String message) {
        if(mGnListener != null)
            mGnListener.status(message);
    }
}

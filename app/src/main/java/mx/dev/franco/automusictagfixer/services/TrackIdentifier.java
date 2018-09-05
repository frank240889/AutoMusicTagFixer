package mx.dev.franco.automusictagfixer.services;

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
import com.gracenote.gnsdk.GnThreadPriority;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;

import mx.dev.franco.automusictagfixer.room.Track;
import mx.dev.franco.automusictagfixer.services.gnservice.GnResponseListener;
import mx.dev.franco.automusictagfixer.services.gnservice.GnService;
import mx.dev.franco.automusictagfixer.utilities.Constants;

public class TrackIdentifier implements  GnResponseListener.GnListener{
    public static final int ALL_TAGS = 0;
    public static final int JUST_COVER = 1;
    private static final String TAG = TrackIdentifier.class.getName();
    private GnResponseListener.GnListener mGnListener;
    private GnResponseListener mGnResponseListener;
    private Track mTrack;
    private Deque<GnMusicIdFile> mDequeue = new ArrayDeque<>();
    private HashMap<String,String> mGnStatusToDisplay;
    private GnMusicIdFile mGnMusicIdFile;
    public TrackIdentifier(GnResponseListener.GnListener listener) {
        mGnListener = listener;
        mGnResponseListener = new GnResponseListener(this);
        mGnStatusToDisplay = new HashMap<>();
        mGnStatusToDisplay.put(Constants.State.BEGIN_PROCESSING,Constants.State.BEGIN_PROCESSING_MSG);
        mGnStatusToDisplay.put(Constants.State.QUERYING_INFO,Constants.State.QUERYING_INFO_MSG);
        mGnStatusToDisplay.put(Constants.State.COMPLETE_IDENTIFICATION,Constants.State.COMPLETE_IDENTIFICATION_MSG);
        mGnStatusToDisplay.put(Constants.State.STATUS_ERROR,Constants.State.STATUS_ERROR_MSG);
        mGnStatusToDisplay.put(Constants.State.STATUS_PROCESSING_ERROR,Constants.State.STATUS_PROCESSING_ERROR_MSG);
    }

    public void setTrack(Track track){
        mTrack = track;
    }

    public void execute(){
        mGnListener.onStartIdentification(mTrack);
        //set options of track id process
        try {
            mGnMusicIdFile = new GnMusicIdFile(GnService.sGnUser, mGnResponseListener);
            mGnMusicIdFile.options().lookupData(GnLookupData.kLookupDataContent, true);
            mGnMusicIdFile.options().preferResultLanguage(GnLanguage.kLanguageSpanish);
            //queue will be processed one by one
            //mGnMusicIdFile.options().batchSize(1);
            //get the fileInfoManager
            GnMusicIdFileInfoManager gnMusicIdFileInfoManager = mGnMusicIdFile.fileInfos();
            //add all info available for more accurate results.
            //Check if file already was previously added.
            GnMusicIdFileInfo gnMusicIdFileInfo = gnMusicIdFileInfoManager.add(mTrack.getPath());
            gnMusicIdFileInfo.fileName(mTrack.getPath());
            gnMusicIdFileInfo.trackTitle(mTrack.getTitle());
            gnMusicIdFileInfo.trackArtist(mTrack.getArtist());
            gnMusicIdFileInfo.albumTitle(mTrack.getAlbum());
            //mDequeue.add(gnMusicIdFile);
            mGnMusicIdFile.doTrackIdAsync(GnMusicIdFileProcessType.kQueryReturnSingle,GnMusicIdFileResponseType.kResponseAlbums);
            //mGnMusicIdFile.doLibraryIdAsync(GnMusicIdFileResponseType.kResponseAlbums);
        } catch (GnException e) {
            e.printStackTrace();
            Crashlytics.logException(e);
            identificationError(e.errorDescription(), mTrack);
        }
    }

    public void cancelIdentification(){
        //if(mDequeue.peek() != null){
        if(mGnMusicIdFile != null){
            //mDequeue.poll().cancel();
            mGnMusicIdFile.cancel();
        }
        mGnResponseListener.setCancel(true);
        mGnListener.onIdentificationCancelled("Cancelled", mTrack);

        clear();
    }

    @Override
    public void statusIdentification(String status, Track track) {

    }

    @Override
    public void gatheringFingerprint(Track track) {
        if(mGnListener != null)
            mGnListener.gatheringFingerprint(mTrack);
    }

    @Override
    public void identificationError(String error, Track track) {
        if(mGnListener != null)
            mGnListener.identificationError(error, mTrack);
    }

    @Override
    public void identificationNotFound(Track track) {
        if(mGnListener != null) {
            mGnListener.identificationNotFound(mTrack);
        }
        clear();
    }

    @Override
    public void identificationFound(GnResponseListener.IdentificationResults results, Track track) {
        if(mGnListener != null)
            mGnListener.identificationFound(results, mTrack);
    }

    @Override
    public void identificationCompleted(Track track) {
        if(mGnListener != null) {
            mGnListener.identificationCompleted(mTrack);
        }
        clear();
    }

    @Override
    public void onStartIdentification(Track track) {
        if(mGnListener != null)
            mGnListener.onStartIdentification(mTrack);
    }

    @Override
    public void onIdentificationCancelled(String cancelledReason, Track track) {
        if(mGnListener != null) {
            mGnListener.onIdentificationCancelled(cancelledReason, mTrack);
        }
        clear();
    }

    @Override
    public void status(String message) {
        Log.d("el status", message);
        String msg;

        switch (message) {
            case Constants.State.BEGIN_PROCESSING:
                msg = Constants.State.BEGIN_PROCESSING_MSG;
                //report status to notification
                if(mGnListener != null)
                    mGnListener.status(msg);
                break;
            case Constants.State.QUERYING_INFO:
                msg = Constants.State.QUERYING_INFO_MSG;
                //report status to notification
                if(mGnListener != null)
                    mGnListener.status(msg);
                break;
            case Constants.State.COMPLETE_IDENTIFICATION:
                msg = Constants.State.COMPLETE_IDENTIFICATION_MSG;
                //report status to notification
                if(mGnListener != null)
                    mGnListener.status(msg);
                break;
            case Constants.State.STATUS_ERROR:
            case Constants.State.STATUS_PROCESSING_ERROR:
                msg = Constants.State.STATUS_ERROR_MSG;
                if(mGnListener != null)
                    mGnListener.onIdentificationCancelled(msg, null);
                break;
            default:
                msg = "";
                break;
        }
    }

    private void clear(){
        mGnResponseListener = null;
        mGnListener = null;
        mTrack = null;
        if(mDequeue != null)
            mDequeue.clear();
        mDequeue = null;

        if(mGnStatusToDisplay != null)
            this.mGnStatusToDisplay.clear();
        this.mGnStatusToDisplay = null;
    }
}

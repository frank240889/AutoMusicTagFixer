package mx.dev.franco.automusictagfixer.services;

import com.crashlytics.android.Crashlytics;
import com.gracenote.gnsdk.GnException;
import com.gracenote.gnsdk.GnLanguage;
import com.gracenote.gnsdk.GnLookupData;
import com.gracenote.gnsdk.GnMusicIdFile;
import com.gracenote.gnsdk.GnMusicIdFileInfo;
import com.gracenote.gnsdk.GnMusicIdFileInfoManager;
import com.gracenote.gnsdk.GnMusicIdFileProcessType;
import com.gracenote.gnsdk.GnMusicIdFileResponseType;

import java.util.ArrayDeque;
import java.util.Deque;

import mx.dev.franco.automusictagfixer.room.Track;
import mx.dev.franco.automusictagfixer.services.gnservice.GnResponseListener;
import mx.dev.franco.automusictagfixer.services.gnservice.GnService;

public class TrackIdentifier implements  GnResponseListener.GnListener{
    public static final int ALL_TAGS = 0;
    public static final int JUST_COVER = 1;
    private static final String TAG = TrackIdentifier.class.getName();
    private GnMusicIdFile mGnMusicIdFile;
    private GnResponseListener.GnListener mGnListener;
    private GnResponseListener mGnResponseListener;
    private Track mTrack;
    private Deque<GnMusicIdFile> mDequeue = new ArrayDeque<>();
    public TrackIdentifier(GnResponseListener.GnListener listener) {
        mGnListener = listener;
        mGnResponseListener = new GnResponseListener(this);
    }

    public void setTrack(Track track){
        mTrack = track;
    }

    public void execute(){
        mGnListener.onStartIdentification(mTrack);
        //set options of track id process
        try {
            GnMusicIdFile gnMusicIdFile = new GnMusicIdFile(GnService.sGnUser, mGnResponseListener);
            gnMusicIdFile.options().lookupData(GnLookupData.kLookupDataContent, true);
            gnMusicIdFile.options().preferResultLanguage(GnLanguage.kLanguageSpanish);
            //queue will be processed one by one
            gnMusicIdFile.options().batchSize(1);
            gnMusicIdFile.waitForComplete();
            //get the fileInfoManager
            GnMusicIdFileInfoManager gnMusicIdFileInfoManager = gnMusicIdFile.fileInfos();
            //add all info available for more accurate results.
            //Check if file already was previously added.
            GnMusicIdFileInfo gnMusicIdFileInfo = gnMusicIdFileInfoManager.add(mTrack.getPath());
            gnMusicIdFileInfo.fileName(mTrack.getPath());
            gnMusicIdFileInfo.trackTitle(mTrack.getTitle());
            gnMusicIdFileInfo.trackArtist(mTrack.getArtist());
            gnMusicIdFileInfo.albumTitle(mTrack.getAlbum());
            mDequeue.add(gnMusicIdFile);
            gnMusicIdFile.doTrackIdAsync(GnMusicIdFileProcessType.kQueryReturnSingle,GnMusicIdFileResponseType.kResponseAlbums);
        } catch (GnException e) {
            e.printStackTrace();
            Crashlytics.logException(e);
            identificationError(e.errorDescription(), mTrack);
        }
    }

    public void cancelIdentification(){
        if(mDequeue.peek() != null){
            mDequeue.poll().cancel();
            mGnResponseListener.setCancel(true);
            mGnListener.onIdentificationCancelled("Cancelled", mTrack);
        }
    }

    @Override
    public void statusIdentification(String status, Track track) {
        if(mGnListener != null)
            mGnListener.statusIdentification(status, mTrack);
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
        if(mGnListener != null)
            mGnListener.status(message);
    }

    private void clear(){
        mGnResponseListener = null;
        mGnListener = null;
        mTrack = null;
        mGnMusicIdFile = null;
        mDequeue.clear();
        mDequeue = null;
    }
}

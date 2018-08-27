package mx.dev.franco.automusictagfixer.services;

import com.gracenote.gnsdk.GnException;
import com.gracenote.gnsdk.GnLanguage;
import com.gracenote.gnsdk.GnLookupData;
import com.gracenote.gnsdk.GnMusicIdFile;
import com.gracenote.gnsdk.GnMusicIdFileInfo;
import com.gracenote.gnsdk.GnMusicIdFileInfoManager;
import com.gracenote.gnsdk.GnMusicIdFileProcessType;
import com.gracenote.gnsdk.GnMusicIdFileResponseType;

import mx.dev.franco.automusictagfixer.room.Track;
import mx.dev.franco.automusictagfixer.services.gnservice.GnResponseListener;
import mx.dev.franco.automusictagfixer.services.gnservice.GnService;

public class TrackIdentifier implements  GnResponseListener.GnListener{
    public static final int ALL_TAGS = 0;
    public static final int JUST_COVER = 1;
    private static final String TAG = TrackIdentifier.class.getName();
    private volatile GnMusicIdFile mGnMusicIdFile;
    private GnResponseListener.GnListener mGnListener;
    private GnResponseListener mGnResponseListener;
    private GnMusicIdFileInfoManager mGnMusicIdFileInfoManager;
    private GnMusicIdFileInfo mGnMusicIdFileInfo;
    private Track mTrack;
    public TrackIdentifier(GnResponseListener.GnListener listener) {
        mGnListener = listener;
        mGnResponseListener = new GnResponseListener(this);
    }

    public void setTrack(Track track){
        mTrack = track;
    }

    public void execute(){
        mTrack.setChecked(1);
        mGnListener.onStartIdentification(mTrack);
        //set options of track id process
        try {
            mGnMusicIdFile = new GnMusicIdFile(GnService.sGnUser, mGnResponseListener);
            mGnMusicIdFile.options().lookupData(GnLookupData.kLookupDataContent, true);
            mGnMusicIdFile.options().preferResultLanguage(GnLanguage.kLanguageSpanish);
            //queue will be processed one by one
            mGnMusicIdFile.options().batchSize(1);
            //get the fileInfoManager
            mGnMusicIdFileInfoManager = mGnMusicIdFile.fileInfos();
            mGnMusicIdFile.waitForComplete();
            //add all info available for more accurate results.
            //Check if file already was previously added.
            mGnMusicIdFileInfo = mGnMusicIdFileInfoManager.add(mTrack.getPath());
            mGnMusicIdFileInfo.fileName(mTrack.getPath());
            mGnMusicIdFileInfo.trackTitle(mTrack.getTitle());
            mGnMusicIdFileInfo.trackArtist(mTrack.getArtist());
            mGnMusicIdFileInfo.albumTitle(mTrack.getAlbum());
            mGnMusicIdFile.doTrackIdAsync(GnMusicIdFileProcessType.kQueryReturnAll,GnMusicIdFileResponseType.kResponseAlbums);
        } catch (GnException e) {
            e.printStackTrace();
            identificationError(e.errorDescription(), mTrack);
        }
    }

    public void cancelIdentification(){
        if(mGnMusicIdFile != null)
            mGnMusicIdFile.cancel();


        try {
            if(mGnMusicIdFileInfoManager != null)
                mGnMusicIdFileInfoManager.remove(mGnMusicIdFileInfo);
        } catch (GnException e) {
            e.printStackTrace();
        }
        mGnMusicIdFile = null;
        if(mGnListener != null) {
            mGnListener.onIdentificationCancelled(null, mTrack);
        }
        if(mTrack != null)
            mTrack.setChecked(0);
        clear();
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
        try {
            if(mGnMusicIdFileInfoManager != null)
                mGnMusicIdFileInfoManager.remove(mGnMusicIdFileInfo);
        } catch (GnException e) {
            e.printStackTrace();
        }

        if(mGnListener != null)
            mGnListener.identificationError(error, mTrack);
    }

    @Override
    public void identificationNotFound(Track track) {
        if(mGnListener != null)
            mGnListener.identificationNotFound(mTrack);
    }

    @Override
    public void identificationFound(GnResponseListener.IdentificationResults results, Track track) {
        if(mGnListener != null)
            mGnListener.identificationFound(results, mTrack);
    }

    @Override
    public void identificationCompleted(Track track) {
        if(mGnListener != null) {
            mGnMusicIdFile.delete();
            mGnMusicIdFile = null;
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
        if(mGnListener != null)
            mGnListener.onIdentificationCancelled(cancelledReason, mTrack);
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
    }
}

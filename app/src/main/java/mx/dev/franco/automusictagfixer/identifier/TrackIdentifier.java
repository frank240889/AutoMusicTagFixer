package mx.dev.franco.automusictagfixer.identifier;

import com.crashlytics.android.Crashlytics;
import com.gracenote.gnsdk.GnException;
import com.gracenote.gnsdk.GnLanguage;
import com.gracenote.gnsdk.GnLookupData;
import com.gracenote.gnsdk.GnMusicIdFile;
import com.gracenote.gnsdk.GnMusicIdFileInfo;
import com.gracenote.gnsdk.GnMusicIdFileInfoManager;
import com.gracenote.gnsdk.GnMusicIdFileProcessType;
import com.gracenote.gnsdk.GnMusicIdFileResponseType;

import java.util.HashMap;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.resource_manager.ResourceManager;

public class TrackIdentifier implements  GnResponseListener.GnListener{
    public static final int ALL_TAGS = 0;
    public static final int JUST_COVER = 1;
    private static final String TAG = TrackIdentifier.class.getName();
    private GnResponseListener.GnListener mGnListener;
    private GnResponseListener mGnResponseListener;
    private Track mTrack;
    private HashMap<String,String> mGnStatusToDisplay;
    private GnMusicIdFile mGnMusicIdFile;
    private GnMusicIdFileInfo gnMusicIdFileInfo;
    private GnMusicIdFileInfoManager gnMusicIdFileInfoManager;
    private ResourceManager mResourceManager;

    public TrackIdentifier(){
        mGnStatusToDisplay = new HashMap<>();
        mGnStatusToDisplay.put(Constants.State.BEGIN_PROCESSING,Constants.State.BEGIN_PROCESSING_MSG);
        mGnStatusToDisplay.put(Constants.State.QUERYING_INFO,Constants.State.QUERYING_INFO_MSG);
        mGnStatusToDisplay.put(Constants.State.COMPLETE_IDENTIFICATION,Constants.State.COMPLETE_IDENTIFICATION_MSG);
        mGnStatusToDisplay.put(Constants.State.STATUS_ERROR,Constants.State.STATUS_ERROR_MSG);
        mGnStatusToDisplay.put(Constants.State.STATUS_PROCESSING_ERROR,Constants.State.STATUS_PROCESSING_ERROR_MSG);
    }

    public void setResourceManager(ResourceManager resourceManager){
        mResourceManager = resourceManager;
    }

    public void setGnListener(GnResponseListener.GnListener listener){
        mGnListener = listener;
        mGnResponseListener = new GnResponseListener();
        mGnResponseListener.addListener(this);
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

            mGnMusicIdFile.options().lookupData(GnLookupData.kLookupDataContent, true);
            mGnMusicIdFile.options().preferResultLanguage(GnLanguage.kLanguageSpanish);
            //queue will be processed one by one
            //mGnMusicIdFile.options().batchSize(1);
            //get the fileInfoManager
            gnMusicIdFileInfoManager = mGnMusicIdFile.fileInfos();
            //add all info available for more accurate results.
            //Check if file already was previously added.
            gnMusicIdFileInfo = gnMusicIdFileInfoManager.add(mTrack.getPath());
            gnMusicIdFileInfo.fileName(mTrack.getPath());
            gnMusicIdFileInfo.trackTitle(mTrack.getTitle());
            gnMusicIdFileInfo.trackArtist(mTrack.getArtist());
            gnMusicIdFileInfo.albumTitle(mTrack.getAlbum());
            mGnMusicIdFile.doTrackIdAsync(GnMusicIdFileProcessType.kQueryReturnSingle,GnMusicIdFileResponseType.kResponseAlbums);
        } catch (GnException e) {
            e.printStackTrace();
            Crashlytics.logException(e);
            identificationError(e.errorDescription(), mTrack);
        }
    }

    public void cancelIdentification(){
        if(mGnListener != null)
            mGnListener.onIdentificationCancelled(mResourceManager.getString(R.string.task_cancelled), mTrack);

        if(mGnMusicIdFile != null){
            mGnMusicIdFile.cancel();
        }
        if(mGnResponseListener != null) {
            mGnResponseListener.removeListener();
            mGnResponseListener.setCancel(true);
        }

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
    public synchronized void status(String message) {
        //Log.d("el status", message);
        String msg;

        switch (message) {
            case Constants.State.BEGIN_PROCESSING:
                msg = mResourceManager.getString(R.string.begin_processing);
                //report status to notification
                if(mGnListener != null)
                    mGnListener.status(msg);
                break;
            case Constants.State.QUERYING_INFO:
                msg = mResourceManager.getString(R.string.querying_info);
                //report status to notification
                if(mGnListener != null)
                    mGnListener.status(msg);
                break;
            case Constants.State.COMPLETE_IDENTIFICATION:
                msg = mResourceManager.getString(R.string.complete_identification);
                //report status to notification
                if(mGnListener != null)
                    mGnListener.status(msg);
                break;
            case Constants.State.STATUS_ERROR:
            case Constants.State.STATUS_PROCESSING_ERROR:
                msg = mResourceManager.getString(R.string.processing_error);
                if(mGnListener != null)
                    mGnListener.onIdentificationCancelled(msg, null);
                break;
            default:
                msg = "";
                break;
        }
    }

    private void clear(){
        //mGnResponseListener = null;
        mGnListener = null;
        mTrack = null;
        gnMusicIdFileInfoManager = null;
        gnMusicIdFileInfo = null;

        if(mGnStatusToDisplay != null)
            this.mGnStatusToDisplay.clear();
        this.mGnStatusToDisplay = null;
        mResourceManager = null;
    }
}

package mx.dev.franco.automusictagfixer.services.gnservice;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.gracenote.gnsdk.GnAssetFetch;
import com.gracenote.gnsdk.GnAudioFile;
import com.gracenote.gnsdk.GnContent;
import com.gracenote.gnsdk.GnDataLevel;
import com.gracenote.gnsdk.GnError;
import com.gracenote.gnsdk.GnException;
import com.gracenote.gnsdk.GnImageSize;
import com.gracenote.gnsdk.GnMusicIdFileCallbackStatus;
import com.gracenote.gnsdk.GnMusicIdFileInfo;
import com.gracenote.gnsdk.GnResponseAlbums;
import com.gracenote.gnsdk.GnResponseDataMatches;
import com.gracenote.gnsdk.GnStatus;
import com.gracenote.gnsdk.IGnCancellable;
import com.gracenote.gnsdk.IGnMusicIdFileEvents;

import java.io.File;
import java.util.HashMap;

import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.Settings;

public class GnResponseListener implements IGnMusicIdFileEvents, IGnCancellable {
    private static final String TAG = GnResponseListener.class.getName();

    private boolean mCancelled = false;

    @Override
    public void setCancel(boolean b) {
        mCancelled = b;
    }

    @Override
    public boolean isCancelled() {
        Log.d(TAG,"isCancelled() " + mCancelled);
        return mCancelled;
    }

    public interface GnListener{
        void statusIdentification(String status, String trackName);
        void gatheringFingerprint(String trackName);
        void identificationError(String error);
        void identificationNotFound(String trackName);
        void identificationFound(IdentificationResults results);
        void identificationCompleted(String trackName);
        void onStartIdentification(String trackName);
        void onIdentificationCancelled();
        void status(String message);
    }

    private GnListener mListener;
    private HashMap<String,String> mGnStatusToDisplay;
    private Handler mHandler;

    public GnResponseListener(GnListener listener){
        mListener = listener;
        mGnStatusToDisplay = new HashMap<>();
        mGnStatusToDisplay.put(Constants.State.BEGIN_PROCESSING,Constants.State.BEGIN_PROCESSING_MSG);
        mGnStatusToDisplay.put(Constants.State.QUERYING_INFO,Constants.State.QUERYING_INFO_MSG);
        mGnStatusToDisplay.put(Constants.State.COMPLETE_IDENTIFICATION,Constants.State.COMPLETE_IDENTIFICATION_MSG);
        mGnStatusToDisplay.put(Constants.State.STATUS_ERROR,Constants.State.STATUS_ERROR_MSG);
        mGnStatusToDisplay.put(Constants.State.STATUS_PROCESSING_ERROR,Constants.State.STATUS_PROCESSING_ERROR_MSG);
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void musicIdFileStatusEvent(GnMusicIdFileInfo gnMusicIdFileInfo, GnMusicIdFileCallbackStatus gnMusicIdFileCallbackStatus, long currentFile, long totalFiles, IGnCancellable iGnCancellable) {
        if(iGnCancellable.isCancelled())
            return;
        //Retrieve current status of current tracked id song
        //check the current state
        String status = gnMusicIdFileCallbackStatus.toString();
        Log.d(TAG,gnMusicIdFileCallbackStatus.toString());
        if (mGnStatusToDisplay.containsKey(status)) {
            //report status to notification
            mHandler.post(() -> {
                if(mListener != null) {
                    String msg;
                    switch (status){
                        case Constants.State.BEGIN_PROCESSING:
                            msg = Constants.State.BEGIN_PROCESSING_MSG;
                            break;
                        case Constants.State.QUERYING_INFO:
                            msg = Constants.State.QUERYING_INFO_MSG;
                            break;
                        case Constants.State.COMPLETE_IDENTIFICATION:
                            msg = Constants.State.COMPLETE_IDENTIFICATION_MSG;
                            break;
                        default:
                            msg = "";
                            break;
                    }
                    mListener.status(msg);
                }
            });
        }

        else if(status.equals(Constants.State.STATUS_ERROR) || status.equals(Constants.State.STATUS_PROCESSING_ERROR)){

                mHandler.post(() -> {
                    if(mListener != null) {
                        mListener.identificationError(Constants.State.STATUS_ERROR_MSG);
                    }
                });
        }

        /*else if(status.equals("kMusicIdFileCallbackStatusProcessingComplete")){

        }*/
    }

    @Override
    public void gatherFingerprint(GnMusicIdFileInfo fileInfo, long l, long l1, IGnCancellable iGnCancellable) {
        Log.d(TAG,"gatherFingerprint");
        if(iGnCancellable.isCancelled())
            return;
        try {
            String path = fileInfo.fileName();
            File file = new File(path);
            GnAudioFile gnAudioFile = new GnAudioFile(new File(path));
            Log.d("gatherFingerprint",file.getAbsolutePath());
            if (GnAudioFile.isFileFormatSupported(path)) {
                mHandler.post(() -> {
                    if(mListener != null) {
                        mListener.gatheringFingerprint("Generando huella...");
                    }
                });
                fileInfo.fingerprintFromSource(gnAudioFile);
            }

        }
        catch (GnException e) {
            if (!GnError.isErrorEqual(e.errorCode(), GnError.GNSDKERR_Aborted)) {
                    mHandler.post(() -> {
                        if(mListener != null) {
                            mListener.identificationError("IdentificationError in fingerprinting file: " + e.errorAPI() + ", " + e.errorModule() + ", " + e.errorDescription());
                            clear();
                        }
                    });
                //Log.e(sAppString, "IdentificationError in fingerprinting file: " + e.errorAPI() + ", " + e.errorModule() + ", " + e.errorDescription());

            }
        }
    }

    @Override
    public void gatherMetadata(GnMusicIdFileInfo gnMusicIdFileInfo, long l, long l1, IGnCancellable iGnCancellable) {
        //Log.d("cancelled8",iGnCancellable.isCancelled()+"");
        //Log.d("gatherMetadata", "gatherMetadata");
    }

    @Override
    public void musicIdFileAlbumResult(GnResponseAlbums gnResponseAlbums, long l, long l1, IGnCancellable iGnCancellable) {
        if(iGnCancellable.isCancelled())
            return;

        final IdentificationResults identificationResults = new IdentificationResults();
        String title = "";
        String artist = "";
        String album = "";
        String cover = "";
        String number = "";
        String year = "";
        String genre = "";

        //retrieve title results identificationFound
        try {
            title = gnResponseAlbums.albums().at(0).next().trackMatched().title().display();
            identificationResults.title = title;
        } catch (GnException e) {
            e.printStackTrace();
        }

        try {
            //get artist name of song if exist
            //otherwise get artist album
            if(!gnResponseAlbums.albums().at(0).next().trackMatched().artist().name().display().isEmpty()) {
                artist = gnResponseAlbums.albums().at(0).next().trackMatched().artist().name().display();
            }
            else {
                artist = gnResponseAlbums.albums().at(0).next().artist().name().display();
            }
            identificationResults.artist = artist;
        } catch (GnException e) {
            e.printStackTrace();
        }
        try {
            album = gnResponseAlbums.albums().at(0).next().title().display();
            identificationResults.album = album;
        } catch (GnException e) {
            e.printStackTrace();
        }

        try {
            //If is selected "No descargar imagen"
            //don't retrieve the url from the cover
            if (Settings.SETTING_SIZE_ALBUM_ART == null) {
                cover = "";
            }
            //If is selected "De mejor calidad disponible"
            //iterate from higher to lower quality and select the first higher quality identificationFound.
            else if (Settings.SETTING_SIZE_ALBUM_ART == GnImageSize.kImageSizeXLarge) {
                GnContent gnContent = gnResponseAlbums.albums().at(0).next().coverArt();
                GnImageSize[] values = GnImageSize.values();
                for (int sizes = values.length - 1; sizes >= 0; --sizes) {
                    String url = gnContent.asset(values[sizes]).url();
                    if (!gnContent.asset(values[sizes]).url().equals("")) {
                        identificationResults.cover = new GnAssetFetch(GnService.sGnUser, url).data();
                        break;
                    }
                }
            }
            //If is selected "De menor calidad disponible"
            //iterate from lower to higher quality and select the first lower quality identificationFound.
            else if (Settings.SETTING_SIZE_ALBUM_ART == GnImageSize.kImageSizeThumbnail) {
                GnContent gnContent = gnResponseAlbums.albums().at(0).next().coverArt();
                GnImageSize[] values = GnImageSize.values();
                for (int sizes = 0; sizes < values.length ; sizes++) {
                    String url = gnContent.asset(values[sizes]).url();
                    if (!gnContent.asset(values[sizes]).url().equals("")) {
                        identificationResults.cover = new GnAssetFetch(GnService.sGnUser, url).data();
                        break;
                    }
                }
            }
            //get the first identificationFound in any of those predefined sizes:
            //"De baja calidad", "De media calidad", "De alta calidad", "De muy alta calidad"
            else {
                GnContent gnContent = gnResponseAlbums.albums().at(0).next().coverArt();
                cover = gnContent.asset(Settings.SETTING_SIZE_ALBUM_ART).url();
                identificationResults.cover = new GnAssetFetch(GnService.sGnUser, cover).data();
            }

        } catch (GnException e) {
            e.printStackTrace();
        }

        try {
            number = gnResponseAlbums.albums().at(0).next().trackMatchNumber() + "";
            identificationResults.trackNumber = number;
        } catch (GnException e) {
            e.printStackTrace();
        }
        try {
            if(!gnResponseAlbums.albums().at(0).next().trackMatched().year().isEmpty()){
                year = gnResponseAlbums.albums().at(0).next().trackMatched().year();
            }
            else {
                year = gnResponseAlbums.albums().at(0).next().year();
            }
            identificationResults.trackYear = year;
        } catch (GnException e) {
            e.printStackTrace();
        }
        try {
            //Get the first level identificationFound of genre, first from track matched if exist, if not, then from album identificationFound.

            //The Gracenote Genre System contains more than 2200 genres from around the world.
            //To make this list easier to manage and give more display options for client applications,
            //the Gracenote Genre System groups these genres into a relationship hierarchy.
            //Most hierarchies consists of three levels: level-1. level-2, and level-3. For example:
            //Level-1
                /*Rock
                    //Level-2
                    Heavy Metal
                        //Level-3
                        Grindcore
                        Black Metal
                        Death Metal
                    //Level-2
                    50's Rock
                        //Level-3
                        Doo Wop
                        Rockabilly
                        Early Rock & Roll
                 */
            if(!gnResponseAlbums.albums().at(0).next().trackMatched().genre(GnDataLevel.kDataLevel_3).isEmpty()){
                genre = gnResponseAlbums.albums().at(0).next().trackMatched().genre(GnDataLevel.kDataLevel_3);
                identificationResults.genre = genre;
            }
            else if(!gnResponseAlbums.albums().at(0).next().trackMatched().genre(GnDataLevel.kDataLevel_2).isEmpty()){
                genre = gnResponseAlbums.albums().at(0).next().trackMatched().genre(GnDataLevel.kDataLevel_2);
                identificationResults.genre = genre;
            }
            else if(!gnResponseAlbums.albums().at(0).next().trackMatched().genre(GnDataLevel.kDataLevel_1).isEmpty()){
                genre = gnResponseAlbums.albums().at(0).next().trackMatched().genre(GnDataLevel.kDataLevel_1);
                identificationResults.genre = genre;
            }
            else if(!gnResponseAlbums.albums().at(0).next().genre(GnDataLevel.kDataLevel_3).isEmpty()){
                genre = gnResponseAlbums.albums().at(0).next().genre(GnDataLevel.kDataLevel_3);
                identificationResults.genre = genre;
            }
            else if(!gnResponseAlbums.albums().at(0).next().genre(GnDataLevel.kDataLevel_2).isEmpty()){
                genre = gnResponseAlbums.albums().at(0).next().genre(GnDataLevel.kDataLevel_2);
                identificationResults.genre = genre;
            }
            else if(!gnResponseAlbums.albums().at(0).next().genre(GnDataLevel.kDataLevel_1).isEmpty()){
                genre = gnResponseAlbums.albums().at(0).next().genre(GnDataLevel.kDataLevel_1);
                identificationResults.genre = genre;
            }
        } catch (GnException e) {
            e.printStackTrace();
        }
        if(iGnCancellable.isCancelled())
            return;

        mHandler.post(() -> {
            if(mListener != null)
                mListener.identificationFound(identificationResults);
        });

    }

    @Override
    public void musicIdFileMatchResult(GnResponseDataMatches gnResponseDataMatches, long l, long l1, IGnCancellable iGnCancellable) {

    }

    @Override
    public void musicIdFileResultNotFound(GnMusicIdFileInfo gnMusicIdFileInfo, long l, long l1, IGnCancellable iGnCancellable) {
        if(iGnCancellable.isCancelled())
            return;

            mHandler.post(() -> {
                try {
                    if(mListener != null)
                        mListener.identificationNotFound(gnMusicIdFileInfo.fileName());
                    clear();
                } catch (GnException e) {
                    e.printStackTrace();
                }
            });
    }

    @Override
    public void musicIdFileComplete(GnError gnError) {
        //triggered when all files were processed by doTrackId method

            mHandler.post(() -> {
                if(mListener != null) {
                    mListener.identificationCompleted("");
                    clear();
                }
                });
        Log.d("musicIdFileComplete","complete");
    }

    @Override
    public void statusEvent(GnStatus gnStatus, long percentComplete, long bytesTotalSent, long bytesTotalReceived, IGnCancellable iGnCancellable) {
        if(iGnCancellable.isCancelled())
            return;
        Log.d("gnStatus","gnStatus");
    }

    private void clear(){
        this.mHandler = null;
        this.mListener = null;
        if(mGnStatusToDisplay != null)
            this.mGnStatusToDisplay.clear();
        this.mGnStatusToDisplay = null;
    }

    public static class IdentificationResults{
        public String title = null;
        public String artist = null;
        public String album = null;
        public String trackNumber = null;
        public String trackYear = null;
        public String genre = null;
        public byte[] cover = null;
    }

}

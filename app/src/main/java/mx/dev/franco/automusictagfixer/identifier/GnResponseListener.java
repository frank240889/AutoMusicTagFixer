package mx.dev.franco.automusictagfixer.identifier;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.gracenote.gnsdk.GnAlbumIterator;
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

import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.utilities.Settings;

public class GnResponseListener implements IGnMusicIdFileEvents, IGnCancellable {
    public interface GnListener{
        void statusIdentification(String status, Track track);
        void gatheringFingerprint(Track track);
        void identificationError(String error, Track track);
        void identificationNotFound(Track track);
        void identificationFound(IdentificationResults results, Track track);
        void identificationCompleted(Track track);
        void onStartIdentification(Track track);
        void onIdentificationCancelled(String cancelledReason, Track track);
        void status(String message);
    }

    public static final String TAG = GnResponseListener.class.getName();
    private volatile boolean mCancelled = false;
    private volatile GnListener mListener;

    public GnResponseListener(){}

    public void addListener(GnListener gnListener){
        mListener = gnListener;
    }

    public void removeListener(){
        mListener = null;
    }

    @Override
    public synchronized void musicIdFileStatusEvent(GnMusicIdFileInfo gnMusicIdFileInfo, GnMusicIdFileCallbackStatus gnMusicIdFileCallbackStatus, long currentFile, long totalFiles, IGnCancellable iGnCancellable) {
        //Retrieve current status of current tracked id song
        //check the current state
        Log.d(TAG,"isCancelled musicidfilesstatusevent() " + mCancelled);
        Log.d(TAG,"GNThreadName " + Thread.currentThread().getName());
        Log.d(TAG,"GNThreadID " + Thread.currentThread().getId());
        if(mCancelled){
            //if(!Thread.currentThread().isInterrupted())
            //    Thread.currentThread().interrupt();
            clear();
            return;
        }

        String status = gnMusicIdFileCallbackStatus.toString();
        Handler handler = new Handler(Looper.getMainLooper());
        Log.d(TAG,gnMusicIdFileCallbackStatus.toString());

        handler.post(() -> {
                 //report status to notification
            if (mListener != null)
                mListener.status(status);
        });
    }

    @Override
    public synchronized void gatherFingerprint(GnMusicIdFileInfo fileInfo, long l, long l1, IGnCancellable iGnCancellable) {
        Log.d(TAG,"isCancelled gatherFingerprint" + mCancelled);
        if(mCancelled){
            //if(!Thread.currentThread().isInterrupted())
            //    Thread.currentThread().interrupt();
            clear();
            return;
        }

        Handler handler = new Handler(Looper.getMainLooper());
        try {
            String path = fileInfo.fileName();
            File file = new File(path);
            GnAudioFile gnAudioFile = new GnAudioFile(new File(path));
            Log.d("gatherFingerprint",file.getAbsolutePath());
            if (GnAudioFile.isFileFormatSupported(path)) {
                handler.post(() -> {
                    if(mListener != null)
                        mListener.gatheringFingerprint(null);
                });
                fileInfo.fingerprintFromSource(gnAudioFile);
            }

        }
        catch (GnException e) {
            if (!GnError.isErrorEqual(e.errorCode(), GnError.GNSDKERR_Aborted)) {
                    handler.post(() -> {
                        if(mListener != null) {
                            mListener.identificationError("IdentificationError in fingerprinting file: " + e.errorAPI() + ", " + e.errorModule() + ", " + e.errorDescription(), null);
                            clear();
                        }
                    });
                //Log.e(sAppString, "IdentificationError in fingerprinting file: " + e.errorAPI() + ", " + e.errorModule() + ", " + e.errorDescription());

            }
        }
    }

    @Override
    public synchronized void gatherMetadata(GnMusicIdFileInfo gnMusicIdFileInfo, long l, long l1, IGnCancellable iGnCancellable) {
        Log.d(TAG,"isCancelled gathermetadata" + mCancelled);
        if(mCancelled){
            //if(!Thread.currentThread().isInterrupted())
            //    Thread.currentThread().interrupt();
            clear();
            return;
        }
    }

    @Override
    public synchronized void musicIdFileAlbumResult(GnResponseAlbums gnResponseAlbums, long l, long l1, IGnCancellable iGnCancellable) {
        Log.d(TAG,"isCancelled musicidfilesstatusevent() " + mCancelled);
        if(mCancelled){
            //if(!Thread.currentThread().isInterrupted())
            //    Thread.currentThread().interrupt();
            clear();
            return;
        }

        Log.d(TAG,"isCancelled musicIdFIleALbum" + mCancelled);
        Handler handler = new Handler(Looper.getMainLooper());
        final IdentificationResults identificationResults = new IdentificationResults();
        String title = "";
        String artist = "";
        String album = "";
        String cover = "";
        String number = "";
        String year = "";
        String genre = "";
        Log.d(TAG,"isCancelled musicidfilesstatusevent() " + mCancelled);
        if(mCancelled){
            //if(!Thread.currentThread().isInterrupted())
            //    Thread.currentThread().interrupt();
            clear();
            return;
        }
        //retrieve title results identificationFound
        try {
            title = gnResponseAlbums.albums().at(0).next().trackMatched().title().display();
            identificationResults.title = title;
        } catch (GnException e) {
            e.printStackTrace();
        }
        Log.d(TAG,"isCancelled musicidfilesstatusevent() " + mCancelled);

        if(mCancelled){
            //if(!Thread.currentThread().isInterrupted())
            //    Thread.currentThread().interrupt();
            clear();
            return;
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
        Log.d(TAG,"isCancelled musicidfilesstatusevent() " + mCancelled);
        if(mCancelled){
            //if(!Thread.currentThread().isInterrupted())
            //    Thread.currentThread().interrupt();
            clear();
            return;
        }

        try {
            album = gnResponseAlbums.albums().at(0).next().title().display();
            identificationResults.album = album;
        } catch (GnException e) {
            e.printStackTrace();
        }
        Log.d(TAG,"isCancelled musicidfilesstatusevent() " + mCancelled);
        if(mCancelled){
            //if(!Thread.currentThread().isInterrupted())
            //    Thread.currentThread().interrupt();
            clear();
            return;
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
                Log.d(TAG,"isCancelled musicidfilesstatusevent() " + mCancelled);
                if(mCancelled){
                    //if(!Thread.currentThread().isInterrupted())
                    //    Thread.currentThread().interrupt();
                    clear();
                    return;
                }

                if(gnResponseAlbums.albums().count() > 0){
                    Log.d("Albums", gnResponseAlbums.albums().count()+"");
                    if(gnResponseAlbums.albums().count() == 1){
                        GnContent gnContent = gnResponseAlbums.albums().at(0).next().coverArt();
                        Log.d("Albums", gnResponseAlbums.albums().count()+"");
                        GnImageSize[] values = GnImageSize.values();
                        for (int sizes = values.length - 1; sizes >= 0; --sizes) {
                            String url = gnContent.asset(values[sizes]).url();
                            if (!gnContent.asset(values[sizes]).url().equals("")) {
                                identificationResults.cover = new GnAssetFetch(GnService.sGnUser, url).data();
                                break;
                            }
                        }
                    }
                    else {
                        GnAlbumIterator iterator = gnResponseAlbums.albums().getIterator();
                        while(iterator.hasNext()){
                            GnContent gnContent = iterator.next().coverArt();
                            GnImageSize[] values = GnImageSize.values();
                            for (int sizes = values.length - 1; sizes >= 0; --sizes) {
                                String url = gnContent.asset(values[sizes]).url();
                                if (!gnContent.asset(values[sizes]).url().equals("")) {
                                    identificationResults.cover = new GnAssetFetch(GnService.sGnUser, url).data();
                                    break;
                                }
                            }
                        }
                    }
                }

                /*GnContent gnContent = gnResponseAlbums.albums().at(0).next().coverArt();
                GnImageSize[] values = GnImageSize.values();
                for (int sizes = values.length - 1; sizes >= 0; --sizes) {
                    String url = gnContent.asset(values[sizes]).url();
                    if (!gnContent.asset(values[sizes]).url().equals("")) {
                        identificationResults.cover = new GnAssetFetch(GnService.sGnUser, url).data();
                        break;
                    }
                }*/
            }

            //If is selected "De menor calidad disponible"
            //iterate from lower to higher quality and select the first lower quality identificationFound.
            else if (Settings.SETTING_SIZE_ALBUM_ART == GnImageSize.kImageSizeThumbnail) {
                Log.d(TAG,"isCancelled musicidfilesstatusevent() " + mCancelled);
                if(mCancelled){
                    //if(!Thread.currentThread().isInterrupted())
                    //    Thread.currentThread().interrupt();
                    clear();
                    return;
                }
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
                Log.d(TAG,"isCancelled musicidfilesstatusevent() " + mCancelled);
                if(mCancelled){
                    //if(!Thread.currentThread().isInterrupted())
                    //    Thread.currentThread().interrupt();
                    clear();
                    return;
                }
                GnContent gnContent = gnResponseAlbums.albums().at(0).next().coverArt();
                cover = gnContent.asset(Settings.SETTING_SIZE_ALBUM_ART).url();
                identificationResults.cover = new GnAssetFetch(GnService.sGnUser, cover).data();
            }

        } catch (GnException e) {
            e.printStackTrace();
        }

        Log.d(TAG,"isCancelled musicidfilesstatusevent() " + mCancelled);
        if(mCancelled){
            //if(!Thread.currentThread().isInterrupted())
            //    Thread.currentThread().interrupt();
            clear();
            return;
        }

        try {
            number = gnResponseAlbums.albums().at(0).next().trackMatchNumber() + "";
            identificationResults.trackNumber = number;
        } catch (GnException e) {
            e.printStackTrace();
        }

        Log.d(TAG,"isCancelled musicidfilesstatusevent() " + mCancelled);
        if(mCancelled){
            //if(!Thread.currentThread().isInterrupted())
            //    Thread.currentThread().interrupt();
            clear();
            return;
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

        Log.d(TAG,"isCancelled musicidfilesstatusevent() " + mCancelled);
        if(mCancelled){
            //if(!Thread.currentThread().isInterrupted())
            //    Thread.currentThread().interrupt();
            clear();
            return;
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

        Log.d(TAG,"isCancelled musicidfilesstatusevent() " + mCancelled);
        if(mCancelled){
            //if(!Thread.currentThread().isInterrupted())
            //    Thread.currentThread().interrupt();
            clear();
            return;
        }

        handler.post(() -> {
            if(mListener != null)
                mListener.identificationFound(identificationResults, null);
        });

    }

    @Override
    public synchronized void musicIdFileMatchResult(GnResponseDataMatches gnResponseDataMatches, long l, long l1, IGnCancellable iGnCancellable) {
        Log.d(TAG,"isCancelled filematchresult" + mCancelled);
    }

    @Override
    public void musicIdFileResultNotFound(GnMusicIdFileInfo gnMusicIdFileInfo, long l, long l1, IGnCancellable iGnCancellable) {
        Log.d(TAG,"isCancelled resultnotfound" + mCancelled);
        if(mCancelled){
            //if(!Thread.currentThread().isInterrupted())
            //    Thread.currentThread().interrupt();
            clear();
            return;
        }

        Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {
                if(mListener != null)
                    mListener.identificationNotFound(null);
                clear();
            });
    }

    @Override
    public synchronized void musicIdFileComplete(GnError gnError) {
        //triggered when all files were processed by doTrackId method
        Log.d(TAG,"isCancelled musicidfilesstatusevent() " + mCancelled);
        if(mCancelled){
            //if(!Thread.currentThread().isInterrupted())
            //    Thread.currentThread().interrupt();
            clear();
            return;
        }

        Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {
                if(mListener != null)
                    mListener.identificationCompleted(null);
            clear();
            });
        Log.d("musicIdFileComplete","complete");
    }

    @Override
    public synchronized void statusEvent(GnStatus gnStatus, long percentComplete, long bytesTotalSent, long bytesTotalReceived, IGnCancellable iGnCancellable) {
        Log.d(TAG,"isCancelled statusEvent" + mCancelled);
    }

    private void clear(){
        this.mListener = null;
    }

    @Override
    public synchronized void setCancel(boolean b) {
        mCancelled = b;
        removeListener();
    }

    @Override
    public synchronized boolean isCancelled() {
        return mCancelled;
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

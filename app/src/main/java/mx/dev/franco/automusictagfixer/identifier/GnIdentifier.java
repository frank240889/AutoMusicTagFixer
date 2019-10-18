package mx.dev.franco.automusictagfixer.identifier;

import android.os.Handler;
import android.os.Looper;

import com.gracenote.gnsdk.GnAlbum;
import com.gracenote.gnsdk.GnAlbumIterator;
import com.gracenote.gnsdk.GnContent;
import com.gracenote.gnsdk.GnDataLevel;
import com.gracenote.gnsdk.GnError;
import com.gracenote.gnsdk.GnException;
import com.gracenote.gnsdk.GnImageSize;
import com.gracenote.gnsdk.GnLookupData;
import com.gracenote.gnsdk.GnMusicIdFile;
import com.gracenote.gnsdk.GnMusicIdFileCallbackStatus;
import com.gracenote.gnsdk.GnMusicIdFileInfo;
import com.gracenote.gnsdk.GnMusicIdFileInfoManager;
import com.gracenote.gnsdk.GnMusicIdFileProcessType;
import com.gracenote.gnsdk.GnMusicIdFileResponseType;
import com.gracenote.gnsdk.GnResponseAlbums;
import com.gracenote.gnsdk.GnResponseDataMatches;
import com.gracenote.gnsdk.GnStatus;
import com.gracenote.gnsdk.IGnCancellable;
import com.gracenote.gnsdk.IGnMusicIdFileEvents;

import java.util.ArrayList;
import java.util.List;

import mx.dev.franco.automusictagfixer.identifier.Identifier.IdentificationResults;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;

/**
 * A concrete identifier that implements {@link Identifier} interface.
 */
public class GnIdentifier implements Identifier<Track, List<IdentificationResults>> {

    private GnApiService gnApiService;
    private AbstractSharedPreferences sharedPreferences;
    private IdentificationListener<List<IdentificationResults>, Track> identificationListener;
    private GnMusicIdFile mGnMusicIdFile;
    private GnMusicIdFileInfo gnMusicIdFileInfo;
    private GnMusicIdFileInfoManager gnMusicIdFileInfoManager;
    private Track track;
    private Handler mHandler;

    public GnIdentifier(GnApiService gnApiService, AbstractSharedPreferences sharedPreferences){
        this.gnApiService = gnApiService;
        this.sharedPreferences = sharedPreferences;
        mHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * @inheritDoc
     */
    @Override
    public void identify(Track input) {
        track = input;
        if(identificationListener != null)
            identificationListener.onIdentificationStart(track);
        try {
            mGnMusicIdFile = new GnMusicIdFile(gnApiService.getGnUser(), new IGnMusicIdFileEvents() {
                @Override
                public void musicIdFileStatusEvent(GnMusicIdFileInfo gnMusicIdFileInfo, GnMusicIdFileCallbackStatus gnMusicIdFileCallbackStatus, long l, long l1, IGnCancellable iGnCancellable) { }
                @Override
                public void gatherFingerprint(GnMusicIdFileInfo gnMusicIdFileInfo, long l, long l1, IGnCancellable iGnCancellable) {}
                @Override
                public void gatherMetadata(GnMusicIdFileInfo gnMusicIdFileInfo, long l, long l1, IGnCancellable iGnCancellable) { }
                @Override
                public void musicIdFileAlbumResult(GnResponseAlbums gnResponseAlbums, long l, long l1, IGnCancellable iGnCancellable) {
                    List<IdentificationResults> results = processResponse(gnResponseAlbums);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if(identificationListener != null)
                                identificationListener.onIdentificationFinished(results, track);

                            identificationListener = null;
                        }
                    });
                }

                @Override
                public void musicIdFileMatchResult(GnResponseDataMatches gnResponseDataMatches, long l, long l1, IGnCancellable iGnCancellable) { }

                @Override
                public void musicIdFileResultNotFound(GnMusicIdFileInfo gnMusicIdFileInfo, long l, long l1, IGnCancellable iGnCancellable) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if(identificationListener != null)
                                identificationListener.onIdentificationNotFound(track);
                            identificationListener = null;
                        }
                    });
                }

                @Override
                public void musicIdFileComplete(GnError gnError) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if(identificationListener != null)
                                identificationListener.onIdentificationError(track, "Error");
                            identificationListener = null;
                        }
                    });
                }

                @Override
                public void statusEvent(GnStatus gnStatus, long l, long l1, long l2, IGnCancellable iGnCancellable) {}
            });
            mGnMusicIdFile.options().lookupData(GnLookupData.kLookupDataContent, true);
            mGnMusicIdFile.options().preferResultLanguage(gnApiService.getLanguage());
            //queue will be processed one by one
            //mGnMusicIdFile.options().batchSize(1);
            //get the fileInfoManager
            gnMusicIdFileInfoManager = mGnMusicIdFile.fileInfos();
            //add all info available for more accurate results.
            //Check if file already was previously added.
            gnMusicIdFileInfo = gnMusicIdFileInfoManager.add(track.getPath());
            gnMusicIdFileInfo.fileName(track.getPath());
            gnMusicIdFileInfo.trackTitle(track.getTitle());
            gnMusicIdFileInfo.trackArtist(track.getArtist());
            gnMusicIdFileInfo.albumTitle(track.getAlbum());
            mGnMusicIdFile.doTrackIdAsync(GnMusicIdFileProcessType.kQueryReturnAll, GnMusicIdFileResponseType.kResponseAlbums);
        } catch (GnException e) {
            e.printStackTrace();
            if(identificationListener != null)
                identificationListener.onIdentificationError(track, e.toString());

            //identificationListener = null;
        }

    }

    /**
     * @inheritDoc
     */
    @Override
    public void cancel() {
        if(mGnMusicIdFile != null)
            mGnMusicIdFile.cancel();

        if(identificationListener != null)
            identificationListener.onIdentificationCancelled(track);

        identificationListener = null;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void registerCallback(IdentificationListener<List<IdentificationResults>, Track> identificationListener) {
        this.identificationListener = identificationListener;
    }

    /**
     * Process the response from the API.
     * @param gnResponseAlbums The response from the API.
     * @return A list of results.
     */
    private List<IdentificationResults> processResponse(GnResponseAlbums gnResponseAlbums) {
        List<IdentificationResults> results = new ArrayList<>();

        GnAlbumIterator albumIterator = gnResponseAlbums.albums().getIterator();

        while(albumIterator.hasNext()) {
            GnAlbum gnAlbum;
            try {
                gnAlbum = albumIterator.next();
                Result result = processAlbum(gnAlbum);
                results.add(result);
            } catch (GnException e) {
                e.printStackTrace();
            }
        }

        return results;

    }

    /**
     * Process every album found in response.
     * @param gnAlbum The album to response.
     * @return A result object containing the info for the Input identified.
     */
    private Result processAlbum(GnAlbum gnAlbum) {
        final Result identificationResults = new Result();
        String title = "";
        String artist = "";
        String album = "";
        String cover = "";
        String number = "";
        String year = "";
        String genre = "";

        //retrieve title results identificationFound
        title = gnAlbum.trackMatched().title().display();
        identificationResults.setTitle(title);

        //get artist name of song if exist
        //otherwise get artist album
        if(!gnAlbum.trackMatched().artist().name().display().isEmpty()) {
            artist = gnAlbum.trackMatched().artist().name().display();
        }
        else {
            artist = gnAlbum.artist().name().display();
        }
        identificationResults.setArtist(artist);

        album = gnAlbum.title().display();
        identificationResults.setAlbum(album);

        GnContent gnContent = gnAlbum.coverArt();
        GnImageSize[] values = GnImageSize.values();
        for (int sizes = values.length - 1; sizes >= 0; --sizes) {
            String url = gnContent.asset(values[sizes]).url();
            if (!gnContent.asset(values[sizes]).url().equals("")) {
                identificationResults.addCover(GnImageSize.valueOf(values[sizes]+""),url);
                break;
            }
        }

        number = gnAlbum.trackMatchNumber() + "";
        identificationResults.setTrackNumber(number);

        if(!gnAlbum.trackMatched().year().isEmpty()){
            year = gnAlbum.trackMatched().year();
        }
        else {
            year = gnAlbum.year();
        }
        identificationResults.setTrackYear(year);


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
        if(!gnAlbum.trackMatched().genre(GnDataLevel.kDataLevel_3).isEmpty()){
            genre = gnAlbum.trackMatched().genre(GnDataLevel.kDataLevel_3);
            identificationResults.setGenre(genre);
        }
        else if(!gnAlbum.trackMatched().genre(GnDataLevel.kDataLevel_2).isEmpty()){
            genre = gnAlbum.trackMatched().genre(GnDataLevel.kDataLevel_2);
            identificationResults.setGenre(genre);
        }
        else if(!gnAlbum.trackMatched().genre(GnDataLevel.kDataLevel_1).isEmpty()){
            genre = gnAlbum.trackMatched().genre(GnDataLevel.kDataLevel_1);
            identificationResults.setGenre(genre);
        }
        else if(!gnAlbum.genre(GnDataLevel.kDataLevel_3).isEmpty()){
            genre = gnAlbum.genre(GnDataLevel.kDataLevel_3);
            identificationResults.setGenre(genre);
        }
        else if(!gnAlbum.genre(GnDataLevel.kDataLevel_2).isEmpty()){
            genre = gnAlbum.genre(GnDataLevel.kDataLevel_2);
            identificationResults.setGenre(genre);
        }
        else if(!gnAlbum.genre(GnDataLevel.kDataLevel_1).isEmpty()){
            genre = gnAlbum.genre(GnDataLevel.kDataLevel_1);
            identificationResults.setGenre(genre);
        }

        return identificationResults;
    }
}

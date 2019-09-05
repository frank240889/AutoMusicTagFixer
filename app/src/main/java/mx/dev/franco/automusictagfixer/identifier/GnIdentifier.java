package mx.dev.franco.automusictagfixer.identifier;

import android.util.Log;

import com.gracenote.gnsdk.GnAlbum;
import com.gracenote.gnsdk.GnAlbumIterator;
import com.gracenote.gnsdk.GnAssetFetch;
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
import mx.dev.franco.automusictagfixer.utilities.Settings;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;

public class GnIdentifier implements Identifier<Track, List<IdentificationResults>> {

    private GnService gnService;
    private AbstractSharedPreferences sharedPreferences;
    private IdentificationListener<List<IdentificationResults>, Track> identificationListener;
    private GnMusicIdFile mGnMusicIdFile;
    private GnMusicIdFileInfo gnMusicIdFileInfo;
    private GnMusicIdFileInfoManager gnMusicIdFileInfoManager;
    private Track track;

    public GnIdentifier(GnService gnService, AbstractSharedPreferences sharedPreferences){
        this.gnService = gnService;
        this.sharedPreferences = sharedPreferences;
    }

    @Override
    public void identify(Track input) {
        track = input;
        if(gnService.isApiInitialized()) {
            try {
                mGnMusicIdFile = new GnMusicIdFile(gnService.getGnUser(), new IGnMusicIdFileEvents() {
                    @Override
                    public void musicIdFileStatusEvent(GnMusicIdFileInfo gnMusicIdFileInfo, GnMusicIdFileCallbackStatus gnMusicIdFileCallbackStatus, long l, long l1, IGnCancellable iGnCancellable) { }
                    @Override
                    public void gatherFingerprint(GnMusicIdFileInfo gnMusicIdFileInfo, long l, long l1, IGnCancellable iGnCancellable) {}
                    @Override
                    public void gatherMetadata(GnMusicIdFileInfo gnMusicIdFileInfo, long l, long l1, IGnCancellable iGnCancellable) { }
                    @Override
                    public void musicIdFileAlbumResult(GnResponseAlbums gnResponseAlbums, long l, long l1, IGnCancellable iGnCancellable) {
                        processResponse(gnResponseAlbums);
                    }

                    @Override
                    public void musicIdFileMatchResult(GnResponseDataMatches gnResponseDataMatches, long l, long l1, IGnCancellable iGnCancellable) { }

                    @Override
                    public void musicIdFileResultNotFound(GnMusicIdFileInfo gnMusicIdFileInfo, long l, long l1, IGnCancellable iGnCancellable) { }

                    @Override
                    public void musicIdFileComplete(GnError gnError) {}

                    @Override
                    public void statusEvent(GnStatus gnStatus, long l, long l1, long l2, IGnCancellable iGnCancellable) {}
                });
                mGnMusicIdFile.options().lookupData(GnLookupData.kLookupDataContent, true);
                mGnMusicIdFile.options().preferResultLanguage(Settings.SETTING_LANGUAGE);
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
                mGnMusicIdFile.doTrackIdAsync(GnMusicIdFileProcessType.kQueryReturnSingle, GnMusicIdFileResponseType.kResponseAlbums);
            } catch (GnException e) {
                e.printStackTrace();
            }
        }
        else {
            gnService.initializeAPI();
        }

    }

    @Override
    public void cancel() {

        if(mGnMusicIdFile != null)
            mGnMusicIdFile.cancel();

        if(identificationListener != null)
            identificationListener.onIdentificationCancelled(track);

        identificationListener = null;
    }

    @Override
    public void registerCallback(IdentificationListener<List<IdentificationResults>, Track> identificationListener) {
        this.identificationListener = identificationListener;
    }

    private void processResponse(GnResponseAlbums gnResponseAlbums) {
        List<IdentificationResults> results = new ArrayList<>();

        GnAlbumIterator albumIterator = gnResponseAlbums.albums().getIterator();

        while(albumIterator.hasNext()) {
            GnAlbum gnAlbum = albumIterator.next();
            processAlbum(gnAlbum);
        }


    }

    private void processAlbum(GnAlbum gnAlbum) {
        final Result identificationResults = new Result();
        String title = "";
        String artist = "";
        String album = "";
        String cover = "";
        String number = "";
        String year = "";
        String genre = "";

        //retrieve title results identificationFound
        try {
            title = gnAlbum.trackMatched().title().display();
            identificationResults.setTitle(title);
        } catch (GnException e) {
            e.printStackTrace();
        }

        try {
            //get artist name of song if exist
            //otherwise get artist album
            if(!gnAlbum.trackMatched().artist().name().display().isEmpty()) {
                artist = gnAlbum.trackMatched().artist().name().display();
            }
            else {
                artist = gnAlbum.artist().name().display();
            }
            identificationResults.setArtist(artist);
        } catch (GnException e) {
            e.printStackTrace();
        }

        try {
            album = gnAlbum.title().display();
            identificationResults.setAlbum(album);
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

                if(gnResponseAlbums.albums().count() > 0) {
                    Log.d("Albums", gnResponseAlbums.albums().count()+"");
                    if(gnResponseAlbums.albums().count() == 1){
                        GnContent gnContent = gnAlbum.coverArt();
                        GnImageSize[] values = GnImageSize.values();
                        for (int sizes = values.length - 1; sizes >= 0; --sizes) {
                            String url = gnContent.asset(values[sizes]).url();
                            if (!gnContent.asset(values[sizes]).url().equals("")) {
                                identificationResults.setCover(new GnAssetFetch(gnService.getGnUser(), url).data());
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
                                    identificationResults.setCover(new GnAssetFetch(gnService.getGnUser(), url).data());
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
                GnContent gnContent = gnResponseAlbums.albums().at(0).next().coverArt();
                GnImageSize[] values = GnImageSize.values();
                for (int sizes = 0; sizes < values.length ; sizes++) {
                    String url = gnContent.asset(values[sizes]).url();
                    if (!gnContent.asset(values[sizes]).url().equals("")) {
                        identificationResults.setCover(new GnAssetFetch(gnService.getGnUser(), url).data());
                        break;
                    }
                }
            }
            //get the first identificationFound in any of those predefined sizes:
            //"De baja calidad", "De media calidad", "De alta calidad", "De muy alta calidad"
            else {
                GnContent gnContent = gnResponseAlbums.albums().at(0).next().coverArt();
                cover = gnContent.asset(Settings.SETTING_SIZE_ALBUM_ART).url();
                identificationResults.setCover(new GnAssetFetch(gnService.getGnUser(), cover).data());
            }

        } catch (GnException e) {
            e.printStackTrace();
        }

        try {
            number = gnResponseAlbums.albums().at(0).next().trackMatchNumber() + "";
            identificationResults.setTrackNumber(number);
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
            identificationResults.setTrackYear(year);
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
                identificationResults.setGenre(genre);
            }
            else if(!gnResponseAlbums.albums().at(0).next().trackMatched().genre(GnDataLevel.kDataLevel_2).isEmpty()){
                genre = gnResponseAlbums.albums().at(0).next().trackMatched().genre(GnDataLevel.kDataLevel_2);
                identificationResults.setGenre(genre);
            }
            else if(!gnResponseAlbums.albums().at(0).next().trackMatched().genre(GnDataLevel.kDataLevel_1).isEmpty()){
                genre = gnResponseAlbums.albums().at(0).next().trackMatched().genre(GnDataLevel.kDataLevel_1);
                identificationResults.setGenre(genre);
            }
            else if(!gnResponseAlbums.albums().at(0).next().genre(GnDataLevel.kDataLevel_3).isEmpty()){
                genre = gnResponseAlbums.albums().at(0).next().genre(GnDataLevel.kDataLevel_3);
                identificationResults.setGenre(genre);
            }
            else if(!gnResponseAlbums.albums().at(0).next().genre(GnDataLevel.kDataLevel_2).isEmpty()){
                genre = gnResponseAlbums.albums().at(0).next().genre(GnDataLevel.kDataLevel_2);
                identificationResults.setGenre(genre);
            }
            else if(!gnResponseAlbums.albums().at(0).next().genre(GnDataLevel.kDataLevel_1).isEmpty()){
                genre = gnResponseAlbums.albums().at(0).next().genre(GnDataLevel.kDataLevel_1);
                identificationResults.setGenre(genre);
            }
        } catch (GnException e) {
            e.printStackTrace();
        }
    }

}

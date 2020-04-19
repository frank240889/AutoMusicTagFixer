package mx.dev.franco.automusictagfixer.identifier;

import android.content.Context;

import com.gracenote.gnsdk.GnAlbum;
import com.gracenote.gnsdk.GnAsset;
import com.gracenote.gnsdk.GnAssetFetch;
import com.gracenote.gnsdk.GnAssetIterable;
import com.gracenote.gnsdk.GnAssetIterator;
import com.gracenote.gnsdk.GnContent;
import com.gracenote.gnsdk.GnDataLevel;
import com.gracenote.gnsdk.GnException;
import com.gracenote.gnsdk.GnImageSize;

import java.util.ArrayList;
import java.util.List;

public class GnUtils {

    /**
     * Process every album found in response.
     * @param gnAlbum The album to response.
     * @return A result object containing the info for the Input identified.
     */
    public static Result processAlbum(GnAlbum gnAlbum) {
        final Result identificationResults = new Result();
        String title = "";
        String artist = "";
        String album = "";
        String number = "";
        String year = "";
        String genre = "";

        identificationResults.setId(gnAlbum.trackMatched().gnId());
        //retrieve title results identificationFound
        title = gnAlbum.trackMatched().title().display();
        identificationResults.setTitle(title);
        identificationResults.addField(Identifier.Field.TITLE.name(), title);

        //get artist name of song if exist
        //otherwise get artist album
        if(!gnAlbum.trackMatched().artist().name().display().isEmpty()) {
            artist = gnAlbum.trackMatched().artist().name().display();
        }
        else {
            artist = gnAlbum.artist().name().display();
        }
        identificationResults.setArtist(artist);
        identificationResults.addField(Identifier.Field.ARTIST.name(), artist);

        album = gnAlbum.title().display();
        identificationResults.setAlbum(album);
        identificationResults.addField(Identifier.Field.ALBUM.name(), album);

        GnContent gnContent = gnAlbum.coverArt();
        try {
            int nIterables = (int) gnContent.assets().count();
            if (nIterables > 0) {
                List<CoverArt> covers = new ArrayList<>();
                GnAssetIterable gnAssetIterable = gnContent.assets();

                for(int i = 0 ; i < nIterables ; i++) {
                    GnAssetIterator gnAssetIterator = gnAssetIterable.at(i);
                    while (gnAssetIterator.hasNext()) {
                        GnAsset asset = gnAssetIterator.next();
                        String url = asset.urlHttp();
                        String size = asset.dimension();
                        int width = parseResolution(size);
                        String gnSize = width > 700 ? getGnSize(width) : asset.size().name();
                        covers.add(new CoverArt(size, url, gnSize));
                    }
                }
                identificationResults.addField(Identifier.Field.COVER_ART.name(), covers);
            }
        }
        catch (GnException ignored) {}

        number = gnAlbum.trackMatchNumber() + "";
        identificationResults.setTrackNumber(number);
        identificationResults.addField(Identifier.Field.TRACK_NUMBER.name(), number);

        if(!gnAlbum.trackMatched().year().isEmpty()){
            year = gnAlbum.trackMatched().year();
        }
        else {
            year = gnAlbum.year();
        }
        identificationResults.setTrackYear(year);
        identificationResults.addField(Identifier.Field.TRACK_YEAR.name(), year);


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
            identificationResults.addField(Identifier.Field.GENRE.name(), genre);
        }
        else if(!gnAlbum.trackMatched().genre(GnDataLevel.kDataLevel_2).isEmpty()){
            genre = gnAlbum.trackMatched().genre(GnDataLevel.kDataLevel_2);
            identificationResults.setGenre(genre);
            identificationResults.addField(Identifier.Field.GENRE.name(), genre);
        }
        else if(!gnAlbum.trackMatched().genre(GnDataLevel.kDataLevel_1).isEmpty()){
            genre = gnAlbum.trackMatched().genre(GnDataLevel.kDataLevel_1);
            identificationResults.setGenre(genre);
            identificationResults.addField(Identifier.Field.GENRE.name(), genre);
        }
        else if(!gnAlbum.genre(GnDataLevel.kDataLevel_3).isEmpty()){
            genre = gnAlbum.genre(GnDataLevel.kDataLevel_3);
            identificationResults.setGenre(genre);
            identificationResults.addField(Identifier.Field.GENRE.name(), genre);
        }
        else if(!gnAlbum.genre(GnDataLevel.kDataLevel_2).isEmpty()){
            genre = gnAlbum.genre(GnDataLevel.kDataLevel_2);
            identificationResults.setGenre(genre);
            identificationResults.addField(Identifier.Field.GENRE.name(), genre);
        }
        else if(!gnAlbum.genre(GnDataLevel.kDataLevel_1).isEmpty()){
            genre = gnAlbum.genre(GnDataLevel.kDataLevel_1);
            identificationResults.setGenre(genre);
            identificationResults.addField(Identifier.Field.GENRE.name(), genre);
        }

        return identificationResults;
    }

    public static byte[] fetchGnCover(String url, GnApiService gnApiService) throws GnException {
        return new GnAssetFetch(gnApiService.getGnUser(), url).data();
    }

    public static byte[] fetchGnCover(String url, Context context) throws GnException {
        return new GnAssetFetch(GnApiService.getInstance(context).getGnUser(), url).data();
    }

    public static int getValue(String gnSize) {
        if (gnSize.equals(GnImageSize.kImageSize75.name()) ||
                gnSize.equals(GnImageSize.kImageSizeThumbnail.name()))
            return 1;

        if (gnSize.equals(GnImageSize.kImageSize110.name()) ||
                gnSize.equals(GnImageSize.kImageSize170.name()))
            return 2;

        if (gnSize.equals(GnImageSize.kImageSize220.name()) ||
                gnSize.equals(GnImageSize.kImageSize300.name()) ||
                gnSize.equals(GnImageSize.kImageSize450.name()) ||
                gnSize.equals(GnImageSize.kImageSizeMedium.name())
        )
            return 3;

        if (gnSize.equals(GnImageSize.kImageSizeLarge.name()) ||
                gnSize.equals(GnImageSize.kImageSize720.name()))
            return 4;

        if (gnSize.equals(GnImageSize.kImageSizeXLarge.name()) ||
                gnSize.equals(GnImageSize.kImageSize1080.name()))
            return 5;

        return 0;
    }

    /**
     * This method is added as a fixbug because when covers are bigger than medium size, its {@link GnImageSize}
     * value from service still makes reference to a medium size image,
     * specifically to a size of {@link GnImageSize#kImageSize450}.
     * @param resolution The resolution to parse.
     * @return a integer representing the width of image size.
     */
    private static int parseResolution(String resolution) {
        if (resolution == null || resolution.isEmpty())
            return -1;

        String[] size = resolution.split("x");
        String width = size[0];
        int intSize = -1;

        try {
            intSize = Integer.parseInt(width);
        }
        catch (NumberFormatException ignored) {}
        return intSize;
    }


    private static String getGnSize(int size) {
        if (size > 700 && size < 1000) {
            return GnImageSize.kImageSize720.name();
        }
        if (size > 1000) {
            return GnImageSize.kImageSize1080.name();
        }
        return null;
    }
}

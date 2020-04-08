package mx.dev.franco.automusictagfixer.identifier;

import android.os.Handler;
import android.os.Looper;

import com.gracenote.gnsdk.GnAlbum;
import com.gracenote.gnsdk.GnAlbumIterator;
import com.gracenote.gnsdk.GnError;
import com.gracenote.gnsdk.GnException;
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
import java.util.Map;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.identifier.Identifier.IdentificationResults;
import mx.dev.franco.automusictagfixer.utilities.resource_manager.ResourceManager;

/**
 * A concrete identifier that implements {@link Identifier} interface.
 */
public class AudioFingerprintIdentifier implements Identifier<Map<String, String>, List<IdentificationResults>> {

    private GnApiService gnApiService;
    private ResourceManager resourceManager;
    private IdentificationListener<List<IdentificationResults>> identificationListener;
    private GnMusicIdFile mGnMusicIdFile;
    private GnMusicIdFileInfo gnMusicIdFileInfo;
    private GnMusicIdFileInfoManager gnMusicIdFileInfoManager;
    private Handler mHandler;

    public AudioFingerprintIdentifier(GnApiService gnApiService, ResourceManager androidResourceManager){
        this.gnApiService = gnApiService;
        this.resourceManager = androidResourceManager;
        mHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * @inheritDoc
     */
    @Override
    public void identify(Map<String, String> input) {
        String file = input.get(Field.FILENAME.name());
        String title = input.get(Field.TITLE.name());
        String artist = input.get(Field.ARTIST.name());
        String album = input.get(Field.ALBUM.name());

        if(identificationListener != null)
            identificationListener.onIdentificationStart();
        try {
            mGnMusicIdFile = new GnMusicIdFile(gnApiService.getGnUser(), new IGnMusicIdFileEvents() {
                @Override
                public void musicIdFileStatusEvent(GnMusicIdFileInfo gnMusicIdFileInfo,
                                                   GnMusicIdFileCallbackStatus gnMusicIdFileCallbackStatus,
                                                   long l,
                                                   long l1,
                                                   IGnCancellable iGnCancellable) {}
                @Override
                public void gatherFingerprint(GnMusicIdFileInfo gnMusicIdFileInfo,
                                              long l, long l1, IGnCancellable iGnCancellable) {}
                @Override
                public void gatherMetadata(GnMusicIdFileInfo gnMusicIdFileInfo, long l, long l1,
                                           IGnCancellable iGnCancellable) {}
                @Override
                public void musicIdFileAlbumResult(GnResponseAlbums gnResponseAlbums, long l,
                                                   long l1, IGnCancellable iGnCancellable) {

                    List<IdentificationResults> results = processResponse(gnResponseAlbums);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if(identificationListener != null)
                                identificationListener.onIdentificationFinished(results);

                            identificationListener = null;
                        }
                    });
                }

                @Override
                public void musicIdFileMatchResult(GnResponseDataMatches gnResponseDataMatches,
                                                   long l, long l1, IGnCancellable iGnCancellable) {}

                @Override
                public void musicIdFileResultNotFound(GnMusicIdFileInfo gnMusicIdFileInfo, long l,
                                                      long l1, IGnCancellable iGnCancellable) {

                    mHandler.post(() -> {
                        if(identificationListener != null)
                            identificationListener.onIdentificationNotFound();
                        identificationListener = null;
                    });
                }

                @Override
                public void musicIdFileComplete(GnError gnError) {
                    mHandler.post(() -> {
                        String error = gnError != null &&
                                gnError.errorDescription() != null &&
                                !gnError.errorDescription().isEmpty() ?
                                gnError.errorDescription() :
                                resourceManager.getString(R.string.identification_error);

                        if(identificationListener != null)
                            identificationListener.onIdentificationError(new IdentificationException(error));
                        identificationListener = null;
                    });
                }

                @Override
                public void statusEvent(GnStatus gnStatus, long l, long l1, long l2, IGnCancellable iGnCancellable) {}
            });
            mGnMusicIdFile.options().lookupData(GnLookupData.kLookupDataContent, true);
            mGnMusicIdFile.options().preferResultLanguage(gnApiService.getLanguage());
            //queue will be processed one by one
            mGnMusicIdFile.options().batchSize(1);
            //get the fileInfoManager
            gnMusicIdFileInfoManager = mGnMusicIdFile.fileInfos();
            //add all info available for more accurate results.
            //Check if file already was previously added.
            gnMusicIdFileInfo = gnMusicIdFileInfoManager.add(file);
            gnMusicIdFileInfo.fileName(file);
            if(title != null && !title.equals(""))
                gnMusicIdFileInfo.trackTitle(title);
            if(artist != null && !artist.equals(""))
                gnMusicIdFileInfo.trackArtist(artist);
            if(album != null && !album.equals(""))
                gnMusicIdFileInfo.albumTitle(album);
            mGnMusicIdFile.doTrackIdAsync(GnMusicIdFileProcessType.kQueryReturnAll, GnMusicIdFileResponseType.kResponseAlbums);
        } catch (GnException e) {
            e.printStackTrace();
            if(identificationListener != null)
                identificationListener.onIdentificationError(e);

            identificationListener = null;
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
            identificationListener.onIdentificationCancelled();

        mGnMusicIdFile = null;
        identificationListener = null;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void registerCallback(IdentificationListener<List<IdentificationResults>> identificationListener) {
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
                Result result = GnUtils.processAlbum(gnAlbum);
                results.add(result);
            } catch (GnException e) {
                e.printStackTrace();
            }
        }
        return results;
    }

}

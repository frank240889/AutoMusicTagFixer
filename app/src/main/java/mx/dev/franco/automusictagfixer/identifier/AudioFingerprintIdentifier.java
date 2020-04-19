package mx.dev.franco.automusictagfixer.identifier;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.gracenote.gnsdk.GnAlbum;
import com.gracenote.gnsdk.GnAlbumIterator;
import com.gracenote.gnsdk.GnAudioFile;
import com.gracenote.gnsdk.GnDataMatchIterable;
import com.gracenote.gnsdk.GnDataMatchIterator;
import com.gracenote.gnsdk.GnError;
import com.gracenote.gnsdk.GnException;
import com.gracenote.gnsdk.GnLookupData;
import com.gracenote.gnsdk.GnLookupMode;
import com.gracenote.gnsdk.GnMusicIdFile;
import com.gracenote.gnsdk.GnMusicIdFileCallbackStatus;
import com.gracenote.gnsdk.GnMusicIdFileInfo;
import com.gracenote.gnsdk.GnMusicIdFileInfoManager;
import com.gracenote.gnsdk.GnMusicIdFileProcessType;
import com.gracenote.gnsdk.GnMusicIdFileResponseType;
import com.gracenote.gnsdk.GnResponseAlbums;
import com.gracenote.gnsdk.GnResponseDataMatches;
import com.gracenote.gnsdk.GnStatus;
import com.gracenote.gnsdk.GnThreadPriority;
import com.gracenote.gnsdk.IGnCancellable;
import com.gracenote.gnsdk.IGnMusicIdFileEvents;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.common.AutoMusicTagFixerException;
import mx.dev.franco.automusictagfixer.common.ErrorCode;
import mx.dev.franco.automusictagfixer.utilities.resource_manager.ResourceManager;

/**
 * A concrete identifier that implements {@link Identifier} interface.
 */
public class AudioFingerprintIdentifier implements Identifier<Map<String, String>, List<? extends Identifier.IdentificationResults>> {

    private GnApiService gnApiService;
    private ResourceManager resourceManager;
    private IdentificationListener<List<? extends Identifier.IdentificationResults>> identificationListener;
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
        /*String title = input.get(Field.TITLE.name());
        String artist = input.get(Field.ARTIST.name());
        String album = input.get(Field.ALBUM.name());*/

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
                                              long l, long l1, IGnCancellable iGnCancellable) {
                    try {
                        gnMusicIdFileInfo.fingerprintFromSource( new GnAudioFile( new File(gnMusicIdFileInfo.fileName())) );
                    } catch (GnException e) {
                        Log.e(AudioFingerprintIdentifier.class.getName(),
                                "error in fingerprinting file: " + e.errorAPI() + ", " + e.errorModule() + ", " + e.errorDescription());
                    }
                }
                @Override
                public void gatherMetadata(GnMusicIdFileInfo gnMusicIdFileInfo, long l, long l1,
                                           IGnCancellable iGnCancellable) {}
                @Override
                public void musicIdFileAlbumResult(GnResponseAlbums gnResponseAlbums, long l,
                                                   long l1, IGnCancellable iGnCancellable) {

                    List<? extends Identifier.IdentificationResults> results = processAlbums(gnResponseAlbums);
                    mHandler.post(() -> {

                        if (identificationListener != null)
                            identificationListener.onIdentificationFinished(results);

                        identificationListener = null;
                    });
                }

                @Override
                public void musicIdFileMatchResult(GnResponseDataMatches gnResponseDataMatches,
                                                   long l, long l1, IGnCancellable iGnCancellable) {

                    List<? extends Identifier.IdentificationResults> results = processMatches(gnResponseDataMatches);
                    mHandler.post(() -> {

                        if (identificationListener != null)
                            identificationListener.onIdentificationFinished(results);

                        identificationListener = null;
                    });
                }

                @Override
                public void musicIdFileResultNotFound(GnMusicIdFileInfo gnMusicIdFileInfo, long l,
                                                      long l1, IGnCancellable iGnCancellable) {

                    mHandler.post(() -> {
                        if (identificationListener != null)
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

                        if (identificationListener != null)
                            identificationListener.
                                    onIdentificationError(new AutoMusicTagFixerException(error, ErrorCode.RECOGNITION_ERROR));
                        identificationListener = null;
                    });
                }

                @Override
                public void statusEvent(GnStatus gnStatus, long l, long l1, long l2, IGnCancellable iGnCancellable) {}
            });

            mGnMusicIdFile.options().lookupData(GnLookupData.kLookupDataContent, true);
            mGnMusicIdFile.options().preferResultLanguage(gnApiService.getLanguage());
            mGnMusicIdFile.options().lookupMode(GnLookupMode.kLookupModeOnline);
            mGnMusicIdFile.options().threadPriority(GnThreadPriority.kThreadPriorityNormal);

            //queue will be processed one by one
            mGnMusicIdFile.options().batchSize(1);
            //get the fileInfoManager
            gnMusicIdFileInfoManager = mGnMusicIdFile.fileInfos();
            //add all info available for more accurate results.
            //Check if file already was previously added.
            gnMusicIdFileInfo = gnMusicIdFileInfoManager.add(file);;
            gnMusicIdFileInfo.fileName(file);
            /*if(title != null && !title.equals(""))
                gnMusicIdFileInfo.trackTitle(title);
            if(artist != null && !artist.equals(""))
                gnMusicIdFileInfo.trackArtist(artist);
            if(album != null && !album.equals(""))
                gnMusicIdFileInfo.albumTitle(album);*/
            mGnMusicIdFile.doTrackIdAsync(GnMusicIdFileProcessType.kQueryReturnSingle, GnMusicIdFileResponseType.kResponseAlbums);
        } catch (GnException e) {
            e.printStackTrace();
            if(identificationListener != null)
                identificationListener.onIdentificationError(new AutoMusicTagFixerException(e.getMessage(),e));

            identificationListener = null;
        }

    }

    /**
     * @inheritDoc
     */
    @Override
    public void cancel() {
        if (identificationListener != null)
            identificationListener.onIdentificationCancelled();
        identificationListener = null;

        if (mGnMusicIdFile != null)
            mGnMusicIdFile.cancel();
    }

    /**
     * @inheritDoc
     */
    @Override
    public void registerCallback(IdentificationListener<List<? extends Identifier.IdentificationResults>> identificationListener) {
        this.identificationListener = identificationListener;
    }



    /**
     * Process the response from the API.
     * @param gnResponseAlbums The response from the API.
     * @return A list of results.
     */
    private List<IdentificationResults> processAlbums(GnResponseAlbums gnResponseAlbums) {
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

    /**
     * Process the response from the API.
     * @param gnResponseDataMatches The response from the API.
     * @return A list of results.
     */
    private List<IdentificationResults> processMatches(GnResponseDataMatches gnResponseDataMatches) {
        List<IdentificationResults> results = new ArrayList<>();
        GnDataMatchIterable iterable = gnResponseDataMatches.dataMatches();
        try {
            long count = iterable.count();
            for (int i = 0 ; i < count ; i++) {
                GnDataMatchIterator iterator = iterable.at(i);
                while(iterator.hasNext()) {
                    GnAlbum gnAlbum;

                        gnAlbum = iterator.next().getAsAlbum();
                        Result result = GnUtils.processAlbum(gnAlbum);
                        results.add(result);

                }

            }
        } catch (GnException e) {
            e.printStackTrace();
        }


        return results;
    }

}

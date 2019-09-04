package mx.dev.franco.automusictagfixer.identifier;

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

import java.util.List;

import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.utilities.Settings;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;

public class GnIdentifier implements Identifier<Track, List<GnIdentifier.IdentificationResults>> {

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
                    public void musicIdFileAlbumResult(GnResponseAlbums gnResponseAlbums, long l, long l1, IGnCancellable iGnCancellable) { }

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

    public static class IdentificationResults {}
}

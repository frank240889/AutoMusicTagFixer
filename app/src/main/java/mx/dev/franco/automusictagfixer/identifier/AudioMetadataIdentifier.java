package mx.dev.franco.automusictagfixer.identifier;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import com.gracenote.gnsdk.GnAlbum;
import com.gracenote.gnsdk.GnAlbumIterator;
import com.gracenote.gnsdk.GnDataMatchIterable;
import com.gracenote.gnsdk.GnDataMatchIterator;
import com.gracenote.gnsdk.GnException;
import com.gracenote.gnsdk.GnLookupData;
import com.gracenote.gnsdk.GnLookupMode;
import com.gracenote.gnsdk.GnMusicId;
import com.gracenote.gnsdk.GnResponseAlbums;
import com.gracenote.gnsdk.GnResponseDataMatches;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import mx.dev.franco.automusictagfixer.common.AutoMusicTagFixerException;
import mx.dev.franco.automusictagfixer.utilities.resource_manager.ResourceManager;

/**
 * A concrete identifier that implements {@link Identifier} interface.
 */
public class AudioMetadataIdentifier implements Identifier<Map<String, String>, List<? extends Identifier.IdentificationResults>> {

    private GnApiService gnApiService;
    private AsyncIdentification mAsyncIdentification;
    IdentificationListener<List<? extends IdentificationResults>> identificationListener;

    public AudioMetadataIdentifier(GnApiService gnApiService, ResourceManager androidResourceManager){
        this.gnApiService = gnApiService;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void identify(Map<String, String> input) {
        mAsyncIdentification = new AsyncIdentification(input, gnApiService, identificationListener);
        mAsyncIdentification.execute();
    }

    /**
     * @inheritDoc
     */
    @Override
    public void cancel() {
        if (mAsyncIdentification != null &&
                (mAsyncIdentification.getStatus() == AsyncIdentification.Status.PENDING ||
                        mAsyncIdentification.getStatus() == AsyncIdentification.Status.RUNNING)) {
            mAsyncIdentification.cancel(true);
        }

        if (identificationListener != null)
            identificationListener.onIdentificationCancelled();

    }

    /**
     * @inheritDoc
     */
    @Override
    public void registerCallback(IdentificationListener<List<? extends IdentificationResults>> identificationListener) {
        this.identificationListener = identificationListener;
    }

    /**
     * Process the response from the API.
     * @param gnResponseAlbums The response from the API.
     * @return A list of results.
     */
    private static List<IdentificationResults> processAlbums(GnResponseAlbums gnResponseAlbums) {
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
    private static List<IdentificationResults> processMatches(GnResponseDataMatches gnResponseDataMatches) {
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

    private static final class AsyncIdentification extends AsyncTask<Void, Void, List<? extends IdentificationResults>> {
        private IdentificationListener<List<? extends IdentificationResults>> identificationListener;
        private Map<String, String> input;
        private GnMusicId mGnMusicIdFile;
        private GnApiService gnApiService;
        private Handler mHandler;
        AsyncIdentification(Map<String, String> data,
                            GnApiService gnApiService,
                            IdentificationListener<List<? extends IdentificationResults>> listener) {
            identificationListener = listener;
            input = data;
            this.gnApiService = gnApiService;
            mHandler = new Handler(Looper.getMainLooper());
        }

        @Override
        protected void onPreExecute() {
            if(identificationListener != null)
                identificationListener.onIdentificationStart();
        }

        @Override
        protected List<? extends IdentificationResults> doInBackground(Void... voids) {
            String title = input.get(Field.TITLE.name());
            String artist = input.get(Field.ARTIST.name());
            String album = input.get(Field.ALBUM.name());

            try {
                mGnMusicIdFile = new GnMusicId(gnApiService.getGnUser());
                mGnMusicIdFile.options().lookupData(GnLookupData.kLookupDataContent, true);
                mGnMusicIdFile.options().preferResultLanguage(gnApiService.getLanguage());
                mGnMusicIdFile.options().lookupMode(GnLookupMode.kLookupModeOnline);
                mGnMusicIdFile.options().preferResultCoverart(true);
                GnResponseDataMatches response = mGnMusicIdFile.findMatches(album, title, artist, null, null);
                return processMatches(response);
            } catch (GnException e) {
                e.printStackTrace();
                mHandler.post(() -> {
                    if(identificationListener != null)
                        identificationListener.onIdentificationError(new AutoMusicTagFixerException(e.getMessage(),e));

                    identificationListener = null;
                });
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<? extends IdentificationResults> results) {
            if (identificationListener != null) {
                if (results != null) {
                    if (results.size() > 0) {
                        identificationListener.onIdentificationFinished(results);
                    }
                    else {
                        identificationListener.onIdentificationNotFound();
                    }
                    identificationListener = null;
                }

            }
            this.gnApiService = null;
            this.input = null;
            this.mGnMusicIdFile = null;
        }

        @Override
        protected void onCancelled(List<? extends IdentificationResults> results) {
            super.onCancelled(results);
            this.gnApiService = null;
            this.input = null;
            this.mGnMusicIdFile = null;
            this.identificationListener = null;
        }
    }

}

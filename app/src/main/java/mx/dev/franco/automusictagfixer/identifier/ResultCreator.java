package mx.dev.franco.automusictagfixer.identifier;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.gracenote.gnsdk.GnException;
import com.gracenote.gnsdk.GnImageSize;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.interfaces.Cache;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;

import static mx.dev.franco.automusictagfixer.utilities.AndroidUtils.generateBitmap;
import static mx.dev.franco.automusictagfixer.utilities.AndroidUtils.getAsset;

public class ResultCreator extends AsyncTask<List<? extends Identifier.IdentificationResults>, Void, Void> {
    private AsyncOperation<Void, Void, Void, Void> mCallback;
    private Cache<String, List<CoverIdentificationResult>> mCoverCache;
    private Cache<String, List<TrackIdentificationResult>> mTrackCache;
    private String mTrackId;
    @SuppressLint("StaticFieldLeak")
    private Context mContext;

    public ResultCreator(AsyncOperation<Void, Void, Void, Void> callback,
                         Context context,
                         String trackId,
                         Cache<String, List<CoverIdentificationResult>> coverCache,
                         Cache<String, List<TrackIdentificationResult>> trackCache) {
        mCallback = callback;
        mContext = context;
        mTrackId = trackId;
        mCoverCache = coverCache;
        mTrackCache = trackCache;
    }

    @Override
    protected void onPreExecute() {
        if(mCallback != null)
            mCallback.onAsyncOperationStarted(null);
    }

    @SafeVarargs
    @Override
    protected final Void doInBackground(List<? extends Identifier.IdentificationResults>... lists) {
        List<TrackIdentificationResult> trackIdentificationResults = new ArrayList<>();
        for(Identifier.IdentificationResults result : lists[0]) {
            Result r = (Result) result;
            TrackIdentificationResult trackIdentificationResult = AndroidUtils.createTrackResult(r);
            trackIdentificationResult.setId(r.getId());
            trackIdentificationResults.add(trackIdentificationResult);
            if(isCancelled())
                return null;

            List<CoverIdentificationResult> c = new ArrayList<>();
            Map<GnImageSize, String> covers = r.getCovers();
            Set<Map.Entry<GnImageSize, String>> entries = covers.entrySet();
            for(Map.Entry<GnImageSize, String> entry : entries){
                if(isCancelled())
                    return null;
                try {
                    byte[] cover = getAsset(entry.getValue(), mContext);
                    if(isCancelled())
                        return null;
                    Bitmap bitmap = generateBitmap(cover);
                    if(bitmap != null) {
                        String size = bitmap.getWidth() + " * " + bitmap.getHeight();
                        CoverIdentificationResult coverIdentificationResult = new CoverIdentificationResult(cover, size, entry.getKey());
                        coverIdentificationResult.setId(result.getId()+ entry.getValue());
                        c.add(coverIdentificationResult);
                    }
                } catch (IllegalArgumentException | GnException e) {
                    e.printStackTrace();
                }
            }

            mCoverCache.add(mTrackId , c);
        }
        mTrackCache.add(mTrackId, trackIdentificationResults);
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        if(mCallback != null)
            mCallback.onAsyncOperationFinished(null);

        mCoverCache = null;
        mContext = null;
        mTrackCache = null;
        mTrackId = null;
    }

    @Override
    protected void onCancelled(Void aVoid) {
        super.onCancelled(aVoid);
        if(mCallback != null)
            mCallback.onAsyncOperationCancelled(null);

        mCoverCache = null;
        mContext = null;
        mTrackCache = null;
        mTrackId = null;
    }
}

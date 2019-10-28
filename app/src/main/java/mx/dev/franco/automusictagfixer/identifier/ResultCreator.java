package mx.dev.franco.automusictagfixer.identifier;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;

import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.interfaces.Cache;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;

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
        processResults(lists[0]);
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

    private void processResults(List<? extends Identifier.IdentificationResults> results) {
        List<TrackIdentificationResult> trackIdentificationResults = new ArrayList<>();
        for(Identifier.IdentificationResults result : results) {
            Result r = (Result) result;
            TrackIdentificationResult trackIdentificationResult = AndroidUtils.createTrackResult(r);
            trackIdentificationResult.setId(r.getId());
            trackIdentificationResults.add(trackIdentificationResult);
            processCovers(r);
        }
        mTrackCache.add(mTrackId, trackIdentificationResults);
    }

    private void processCovers(Result r) {
        List<CoverIdentificationResult> coverIdentificationResultList =
                AndroidUtils.createListCoverResult(r, mContext);

        mCoverCache.add(mTrackId , coverIdentificationResultList);

    }
}

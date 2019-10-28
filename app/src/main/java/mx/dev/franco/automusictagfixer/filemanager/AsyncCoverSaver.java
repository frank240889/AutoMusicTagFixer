package mx.dev.franco.automusictagfixer.filemanager;

import android.os.AsyncTask;

import java.io.IOException;
import java.util.List;

import mx.dev.franco.automusictagfixer.identifier.CoverIdentificationResult;
import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.interfaces.Cache;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;

public class AsyncCoverSaver extends AsyncTask<Void, Void, String> {
    private AsyncOperation<Void, String, Void, String> mCallback;
    private String mFilename;
    private Cache<String, List<CoverIdentificationResult>> mCoverCache;
    private String mId;
    private String mTrackId;
    private byte[] mData;


    public AsyncCoverSaver(){}

    public AsyncCoverSaver(AsyncOperation<Void, String, Void, String> callback,
                           String filename,
                           String id,
                           String trackId,
                           Cache<String, List<CoverIdentificationResult>> coverCache,
                           byte[] data) {
        this();
        mId = id;
        mTrackId = trackId;
        mCallback = callback;
        mFilename = filename;
        mCoverCache = coverCache;
        mData = data;
    }

    @Override
    protected void onPreExecute() {
        if(mCallback != null)
            mCallback.onAsyncOperationStarted(null);
    }

    @Override
    protected String doInBackground(Void... voids) {

        if(mData == null)
            mData = findId(mId);

        try {
            return ImageFileSaver.saveImageFile(mData, mFilename);
        } catch (IOException e) {
            return null;
        }
    }

    private byte[] findId(String mId) {
        return ((CoverIdentificationResult)AndroidUtils.findId(mCoverCache.load(mTrackId), mId)).getCover();
    }

    @Override
    protected void onPostExecute(String s) {
        if(mCallback != null) {
            if(ImageFileSaver.INPUT_OUTPUT_ERROR.equals(s) || ImageFileSaver.NULL_DATA.equals(s)
                    || ImageFileSaver.NO_EXTERNAL_STORAGE_WRITABLE.equals(s)) {
                mCallback.onAsyncOperationError(s);
            }
            else {
                mCallback.onAsyncOperationFinished(s);
            }
        }
    }

}

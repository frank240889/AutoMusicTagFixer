package mx.dev.franco.automusictagfixer.filemanager;

import android.os.AsyncTask;

import com.gracenote.gnsdk.GnAssetFetch;
import com.gracenote.gnsdk.GnException;
import com.gracenote.gnsdk.GnUser;

import java.io.IOException;

import mx.dev.franco.automusictagfixer.identifier.GnApiService;
import mx.dev.franco.automusictagfixer.identifier.Result;
import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;

public class AsyncCoverSaver extends AsyncTask<GnApiService, Void, String> {
    private AsyncOperation<Void, String, Void, String> mCallback;
    private Object mResult;
    private String mFilename;


    public AsyncCoverSaver(){}

    public AsyncCoverSaver(Object result,
                           String filename,
                           AsyncOperation<Void, String, Void, String> callback) {
        this();
        mCallback = callback;
        mResult = result;
        mFilename = filename;
    }

    @Override
    protected void onPreExecute() {
        if(mCallback != null)
            mCallback.onAsyncOperationStarted(null);
    }

    @Override
    protected String doInBackground(GnApiService... apiServices) {

        if (mResult instanceof Result) {
            Result result = (Result) mResult;
            try {
                GnUser user = apiServices[0].getGnUser();
                GnAssetFetch gnAsset = new GnAssetFetch(user, result.getCoverArt().getUrl());
                byte[] data = gnAsset.data();
                return ImageFileSaver.saveImageFile(data, mFilename);
            } catch (IOException | GnException e) {
                return null;
            }
        }
        else {
            try {
                return ImageFileSaver.saveImageFile((byte[]) mResult, mFilename);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    @Override
    protected void onPostExecute(String s) {
        if(mCallback != null) {
            if(s == null || ImageFileSaver.INPUT_OUTPUT_ERROR.equals(s) || ImageFileSaver.NULL_DATA.equals(s)
                    || ImageFileSaver.NO_EXTERNAL_STORAGE_WRITABLE.equals(s)) {
                mCallback.onAsyncOperationError(s);
            }
            else {
                mCallback.onAsyncOperationFinished(s);
            }
        }
    }

}

package mx.dev.franco.automusictagfixer.filemanager;

import android.os.AsyncTask;

import java.io.IOException;

import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;

public class AsyncFileSaver extends AsyncTask<Void, Void, String> {
    private AsyncOperation<Void, String, Void, String> mCallback;
    private String mFilename;
    private byte[] mData;


    public AsyncFileSaver(){}

    public AsyncFileSaver(AsyncOperation<Void, String, Void, String> callback, byte[] data, String filename) {
        this();
        mCallback = callback;
        mData = data;
        mFilename = filename;
    }

    @Override
    protected void onPreExecute() {
        if(mCallback != null)
            mCallback.onAsyncOperationStarted(null);
    }

    @Override
    protected String doInBackground(Void... voids) {
        try {
            return ImageFileSaver.saveImageFile(mData, mFilename);
        } catch (IOException e) {
            return null;
        }
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

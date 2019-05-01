package mx.dev.franco.automusictagfixer.modelsUI.main;

import android.os.AsyncTask;

import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.utilities.Tagger;

/**
 * Extracts a loads cover from audiofiles using
 * another thread.
 */
public class AsyncLoaderCover extends AsyncTask<String, Void, byte[]> {
    private AsyncOperation<Void, byte[], byte[], Void> mListener;
    public AsyncLoaderCover() {}

    public void setListener(AsyncOperation<Void, byte[], byte[], Void> listener){
        mListener = listener;
    }

    @Override
    protected void onPreExecute(){
        if(mListener != null)
            mListener.onAsyncOperationStarted(null);
    }

    @Override
    protected byte[] doInBackground(String... params) {
        return Tagger.getCover(params[0]);
    }
    @Override
    protected void onPostExecute(byte[] cover){
        if(mListener != null)
            mListener.onAsyncOperationFinished(cover);
        else
            mListener.onAsyncOperationError(null);
        mListener = null;
    }

    @Override
    public void onCancelled(byte[] cover){
        if(mListener != null)
            mListener.onAsyncOperationCancelled(null);
        mListener = null;
    }
}
package mx.dev.franco.automusictagfixer.common;

import android.content.Context;
import android.os.AsyncTask;

import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;

public class ConnectionChecker extends AsyncTask<Context, Void, Boolean> {
    private AsyncOperation<Void, Boolean, Void, Void> mCallback;

    public ConnectionChecker(AsyncOperation<Void, Boolean, Void, Void> callback) {
        mCallback = callback;
    }

    @Override
    protected Boolean doInBackground(Context... contexts) {
        return AndroidUtils.isConnected(contexts[0]);
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        if (mCallback != null)
            mCallback.onAsyncOperationFinished(aBoolean);
    }
}

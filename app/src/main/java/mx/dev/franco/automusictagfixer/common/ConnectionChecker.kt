package mx.dev.franco.automusictagfixer.common

import android.content.Context
import android.os.AsyncTask
import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils

class ConnectionChecker(private val mCallback: AsyncOperation<Void, Boolean, Void, Void>?) :
    AsyncTask<Context?, Void?, Boolean>() {
    override fun doInBackground(vararg contexts: Context?): Boolean {
        return AndroidUtils.isConnected(contexts[0])
    }

    override fun onPostExecute(aBoolean: Boolean) {
        mCallback?.onAsyncOperationFinished(aBoolean)
    }
}
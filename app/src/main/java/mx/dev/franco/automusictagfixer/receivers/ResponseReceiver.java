package mx.dev.franco.automusictagfixer.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

/**
 * Created by franco on 23/03/18.
 */

public class ResponseReceiver extends BroadcastReceiver {
    private Handler mHandler;
    private OnResponse mCallback;

    public interface OnResponse{
        void onResponse(Intent intent);
    }

    public ResponseReceiver(OnResponse callback, Handler handler){
        mCallback = callback;
        mHandler = handler;
    }

    public void clearReceiver(){
        mCallback = null;
        mHandler = null;
    }

    @Override
    public void onReceive(Context context, final Intent intent) {
        if(mCallback != null && mHandler != null) {
            mHandler.post(() -> {
                if(mCallback != null)
                    mCallback.onResponse(intent);
            });
        }
    }
}

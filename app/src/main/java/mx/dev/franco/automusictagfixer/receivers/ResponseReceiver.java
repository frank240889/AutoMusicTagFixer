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

    @Override
    public void onReceive(Context context, final Intent intent) {
        if(mCallback != null && mHandler != null){
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onResponse(intent);
                }
            });
        }
    }
}

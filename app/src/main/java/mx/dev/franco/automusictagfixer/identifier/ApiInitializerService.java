package mx.dev.franco.automusictagfixer.identifier;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.Settings;

/**
 * @author Franco Castillo
 * The service that initialize the GNSDK.
 */
public class ApiInitializerService extends Service {
    private Thread mThread;

    @Override
    public void onCreate() {
        mThread = new Thread(() -> {
            GnApiService.getInstance(this).initializeAPI(Settings.SETTING_LANGUAGE);
            Handler handler = new Handler(Looper.getMainLooper());
            if(GnApiService.getInstance(this).isApiInitialized()) {
                handler.post(() ->
                        AndroidUtils.showToast(R.string.api_connected, ApiInitializerService.this));
            }
            else {
                handler.post(() ->
                        AndroidUtils.showToast(R.string.could_not_init_api, ApiInitializerService.this));
            }
            stopSelf();
        });
        mThread.start();
    }

    @Override
    public void onDestroy() {
        if(mThread != null && !mThread.isInterrupted()) {
            mThread.interrupt();
        }
        mThread = null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

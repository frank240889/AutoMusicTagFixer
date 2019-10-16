package mx.dev.franco.automusictagfixer.identifier;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * @author Franco Castillo
 * The service that initialize the GNSDK.
 */
public class ApiInitializerService extends Service {
    private Thread mThread;

    @Override
    public void onCreate() {
        GnApiService.getInstance(this);
        mThread = new Thread(() -> {
            GnApiService.getInstance(this).initializeAPI();
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

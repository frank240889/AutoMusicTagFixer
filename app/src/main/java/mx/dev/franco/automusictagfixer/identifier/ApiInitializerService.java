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
    private GnApiService mGnApiService;

    @Override
    public void onCreate() {
        GnApiService.init(this);
        mThread = new Thread(() -> {
            mGnApiService.initializeAPI();
            stopSelf();
        });
        mThread.start();
    }

    @Override
    public void onDestroy() {
        if(mThread != null && !mThread.isInterrupted()) {
            mThread.interrupt();
        }
        mGnApiService = null;
        mThread = null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

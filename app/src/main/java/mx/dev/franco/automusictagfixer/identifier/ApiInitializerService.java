package mx.dev.franco.automusictagfixer.identifier;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import mx.dev.franco.automusictagfixer.BuildConfig;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.Settings;

/**
 * @author Franco Castillo
 * The service that initialize the GNSDK.
 */
public class ApiInitializerService extends IntentService {
    public static final int MAX_API_INIT_ATTEMPTS = 5;
    private int mCurrentAttempt = 0;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public ApiInitializerService() {
        super(BuildConfig.APP_STRING);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        GnApiService apiService = GnApiService.getInstance(this);
        do {
            apiService.initializeAPI(Settings.SETTING_LANGUAGE);
            mCurrentAttempt++;
            if (apiService.isApiInitialized()) break;
        }
        while (mCurrentAttempt < MAX_API_INIT_ATTEMPTS);

        Handler handler = new Handler(Looper.getMainLooper());
        if(apiService.isApiInitialized()) {
            handler.post(() -> broadcastMessage(getString(R.string.api_connected)));
        }
        else {
            handler.post(() -> broadcastMessage(getString(R.string.could_not_init_api)));
        }
    }

    private void broadcastMessage(String message) {
        Intent intent = new Intent(Constants.Actions.ACTION_BROADCAST_MESSAGE);
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }
}

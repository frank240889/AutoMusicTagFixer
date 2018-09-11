package mx.dev.franco.automusictagfixer.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

/**
 * Created by franco on 26/12/17.
 * Checks changes in connectivity state and make
 * a test to prove it in case if one change is detected.
 */

public final class ConnectivityChangesDetector extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action != null && action.equals(ConnectivityManager.CONNECTIVITY_ACTION)){
                ConnectivityDetector.getInstance(context).onStartTestingNetwork();
            }
    }
}

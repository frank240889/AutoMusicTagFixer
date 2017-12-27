package mx.dev.franco.automusictagfixer.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;

/**
 * Created by franco on 26/12/17.
 */

public final class ConnectivityChangesDetector extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action != null && action.equals(ConnectivityManager.CONNECTIVITY_ACTION)){
                Bundle bundle = intent.getExtras();
                boolean noConnectivityExtra = bundle.getBoolean(ConnectivityManager.EXTRA_NO_CONNECTIVITY);
                boolean failOverConnection = bundle.getBoolean(ConnectivityManager.EXTRA_IS_FAILOVER);
                //if(noConnectivityExtra || failOverConnection) {
                    ConnectivityDetector.withContext(context).startCheckingConnection();
                //}

            }
    }
}

package mx.dev.franco.automusictagfixer.services;

import android.app.ActivityManager;
import android.content.Context;

import mx.dev.franco.automusictagfixer.network.ConnectivityDetector;

import static android.content.Context.ACTIVITY_SERVICE;
import static mx.dev.franco.automusictagfixer.services.gnservice.GnService.sApiInitialized;

/**
 * Created by franco on 30/10/17.
 */

public class ServiceHelper {
    private static ServiceHelper sServiceHelper;
    private String mService;
    private Context mContext;
    private ServiceHelper(Context context){
        mContext = context.getApplicationContext();
    }

    /**
     * Creates (if is not created) and return a single instance
     * of this helper
     * @param context
     * @return ServicerHelper singleton
     */
    public static ServiceHelper getInstance(Context context){
        if(sServiceHelper == null)
            sServiceHelper = new ServiceHelper(context);
        return sServiceHelper;
    }

    public boolean checkIfServiceIsRunning(String serviceName){
        ActivityManager manager = (ActivityManager) mContext.getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if(serviceName.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}

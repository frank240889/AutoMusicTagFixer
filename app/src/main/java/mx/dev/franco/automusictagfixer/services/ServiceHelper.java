package mx.dev.franco.automusictagfixer.services;

import android.app.ActivityManager;
import android.content.Context;

import static android.content.Context.ACTIVITY_SERVICE;

/**
 * Created by franco on 30/10/17.
 */

public class ServiceHelper {
    private static ServiceHelper sServiceHelper;
    private String mService;
    private Context mContext;
    private ServiceHelper(Context context){
        mContext = context;
    }

    public static ServiceHelper withContext(Context context){
        if(sServiceHelper == null)
            sServiceHelper = new ServiceHelper(context);
        return sServiceHelper;
    }

    public ServiceHelper withService(String service){
        mService = service;
        return this;
    }

    public boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) mContext.getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if(mService.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}

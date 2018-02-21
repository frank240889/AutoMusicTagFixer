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
        mContext = context.getApplicationContext();
    }

    /**
     * Creates (if is not created) and return a single instance
     * of this helper
     * @param context
     * @return ServicerHelper singleton
     */
    public static ServiceHelper withContext(Context context){
        if(sServiceHelper == null)
            sServiceHelper = new ServiceHelper(context);
        return sServiceHelper;
    }

    /**
     * Service to verify if is running
     * @param service String name of service, regularly is the name returned by
     *                YourServiceClass.class.getName()
     * @return ServicerHelper object
     */
    public ServiceHelper withService(String service){
        mService = service;
        return this;
    }

    /**
     * Check if service is running.
     * @return True if is running, false otherwise.
     */
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

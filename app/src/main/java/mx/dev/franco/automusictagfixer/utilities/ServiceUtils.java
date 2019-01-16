package mx.dev.franco.automusictagfixer.utilities;

import android.app.ActivityManager;
import android.content.Context;

import static android.content.Context.ACTIVITY_SERVICE;

/**
 * Created by franco on 30/10/17.
 */

public class ServiceUtils {
    private static ServiceUtils sServiceUtils;
    private Context mContext;
    private ServiceUtils(Context context){
        mContext = context.getApplicationContext();
    }

    /**
     * Creates (if is not created) and return a single instance
     * of this helper
     * @param context
     * @return ServicerHelper singleton
     */
    public static ServiceUtils getInstance(Context context){
        if(sServiceUtils == null)
            sServiceUtils = new ServiceUtils(context);
        return sServiceUtils;
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

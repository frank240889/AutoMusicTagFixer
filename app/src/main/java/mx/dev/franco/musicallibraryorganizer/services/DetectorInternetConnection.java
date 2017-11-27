package mx.dev.franco.musicallibraryorganizer.services;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * Created by franco on 6/07/17.
 */

public class DetectorInternetConnection extends JobService {
    private static boolean isInitializing = false;
    /**
     * Callback when service start, here is when we execute our
     * task in background, so an intensive task
     * needs to run in another thread because
     * this callback it executes on UI Thread
     * @param params
     * @return true for execute the code
     */

    @Override
    public boolean onStartJob(final JobParameters params) {
        Log.d("ONSTART","onstart");
        startCheckingConnection(params);

        return true;
    }

    /**
     * If case Job stops abruptly, we can handle here, if we
     * want to restart, we need to return true, false if not.
     * This callback it executes on UI Thread too
     * @param params
     * @return
     */
    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d("ONSTOP","onstop");
        return true;
    }


    /**
     * Here we verify the result of isConnected method
     * and initialize GNSDK API
     * in case if is not initialized.
     * @param parameters
     */
    private void startCheckingConnection(final JobParameters parameters){
        boolean isConnected = isConnected(getApplicationContext());
        //We set context and initialize the GNSDK API if it was not before.
        if(isConnected && !GnService.apiInitialized && !isInitializing) {
            //only request initialize GNSDK API one time
            isInitializing = true;
            //GnService.API_INITIALIZED_AFTER_CONNECTED flag indicates
            //that service was not initialized from Splash.
            //is useful to inform to user in MainActivity
            //that API of GNSDK has been initialized
            GnService.withContext(getApplicationContext()).initializeAPI(GnService.API_INITIALIZED_AFTER_CONNECTED);

        }

        //if is connected, we finalize this job
        jobFinished(parameters, !isConnected);

    }

    /**
     * This method help us to check if there is connectivity
     * to any network.
     * @param context
     * @return
     */
    public static boolean isConnected(Context context){
        boolean isConnected = false;
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        //activeNetwork could be NULL if for example, airplane mode is activated.

        Runtime runtime = Runtime.getRuntime();
        int termination = 1;
        try {
            //send only 1 ping to Google DNS's.
            Process process = runtime.exec("system/bin/ping -c 1 8.8.8.8");
            //anormal termination will be different than 0,
            termination = process.waitFor();
        }
        finally {
            isConnected = activeNetwork != null && activeNetwork.isConnected() && (termination == 0);
            Log.d("isConnected",isConnected+"");
            return isConnected;
        }


    }

}

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
    /**
     * Callback when observice start, here we execute our
     * task in background
     * @param params
     * @return
     */

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d("ONSTART","onstart");
        startCheckingConnection(params);
        return true;
    }

    /**
     * If JOb stops abruptly, we can handle here, if we
     * want to restart, we need to return true, false if not.
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
    private void startCheckingConnection(JobParameters parameters){
        boolean isConnected = isConnected(getApplicationContext());

        //We set context and initialize the GNSDK API if it was not.
        if(isConnected && !GnService.apiInitialized) {
            GnService.setAppContext(getApplicationContext());
            GnService.initializeAPI();
        }

        //if is connected, we finalize this job
        jobFinished(parameters, !isConnected);
    }

    /**
     * This static method help us to check if there is internet connection,
     * useful when in any part of app need this result
     * @param context
     * @return
     */
    public static boolean isConnected(Context context){
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        Log.d("isConnected",isConnected+"");
        return isConnected;
    }
}

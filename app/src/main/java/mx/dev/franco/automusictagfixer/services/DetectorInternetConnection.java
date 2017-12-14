package mx.dev.franco.automusictagfixer.services;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.io.IOException;

/**
 * Created by franco on 6/07/17.
 */

public class DetectorInternetConnection extends JobService {
    public static volatile boolean sIsConnected = false;
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                DetectorInternetConnection.startCheckingConnection(DetectorInternetConnection.this);
                //We set context and initialize the GNSDK API if it was not before.
                if(sIsConnected && !GnService.sApiInitialized && !GnService.sIsInitializing) {
                    //only request initialize GNSDK API one time
                    GnService.sIsInitializing = true;
                    //GnService.API_INITIALIZED_AFTER_CONNECTED flag indicates
                    //that service was not initialized from Splash.
                    //is useful to inform to user in MainActivity
                    //that API of GNSDK has been initialized
                    GnService.withContext(getApplicationContext()).initializeAPI(GnService.API_INITIALIZED_AFTER_CONNECTED);

                }

                //if is connected, we finalize this job
                jobFinished(parameters, !sIsConnected);
            }
        }).start();



    }

    public static void startCheckingConnection(Context context){
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        //activeNetwork could be NULL if for example, airplane mode is activated.

        Runtime runtime = Runtime.getRuntime();
        int termination = 1;
        try {
            //send only 1 ping to Google DNS's.
            Process process = runtime.exec("system/bin/ping -c 1 8.8.8.8");
            //abnormal termination will be different than 0,
            termination = process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            sIsConnected = activeNetwork != null && activeNetwork.isConnected() && (termination == 0);
            Log.d("sIsConnected"," -  "+ sIsConnected );
        }
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
            //send only 1 ping to Google DNS's to check internet connection
            Process process = runtime.exec("system/bin/ping -c 1 8.8.8.8");
            //abnormal termination will be different than 0,
            termination = process.waitFor();
        }
        finally {
            sIsConnected = isConnected = activeNetwork != null && activeNetwork.isConnected() && (termination == 0);
            Log.d("isConnected",isConnected+" -  "+ termination);

            return isConnected;
        }


    }

}

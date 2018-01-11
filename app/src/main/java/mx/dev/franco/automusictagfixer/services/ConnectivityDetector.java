package mx.dev.franco.automusictagfixer.services;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import mx.dev.franco.automusictagfixer.BuildConfig;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.utilities.Constants;

import static android.content.Context.CONNECTIVITY_SERVICE;

/**
 * Created by franco on 26/12/17.
 */

public class ConnectivityDetector {
    public static volatile boolean sIsConnected = true;
    private static Context sContext;
    private static ConnectivityDetector  sConnectivityDetector = new ConnectivityDetector();
    private static AsyncConnectivityDetection sAsyncConnectivityDetection;
    private static int sStartedFrom = GnService.API_INITIALIZED_AFTER_CONNECTED;

    private ConnectivityDetector(){

    }

    public static ConnectivityDetector withContext(Context context){
        if(sContext == null) {
            sContext = context.getApplicationContext();
        }
        if(sConnectivityDetector == null){
            sConnectivityDetector = new ConnectivityDetector();
        }
        return sConnectivityDetector;
    }

    public ConnectivityDetector setStartedFrom(int startedFrom){
        sStartedFrom = startedFrom;
        return this;
    }

    public void startCheckingConnection(){
        if(sAsyncConnectivityDetection == null){
            sAsyncConnectivityDetection = new AsyncConnectivityDetection();
            sAsyncConnectivityDetection.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,sContext);
        }
    }


    /**
     * This class checks the connection to internet
     */

    public static class AsyncConnectivityDetection extends AsyncTask<Context,Void,Boolean>{

        @Override
        protected Boolean doInBackground(Context... contexts) {
            boolean hasConnectivity = hasConnectivity();
            if(!hasConnectivity)
                return false;

            boolean isConnectedToInternet = isConnectedToInternet(null);
            if(!isConnectedToInternet)
                return false;

            return true;
        }

        @Override
        protected void onPostExecute(Boolean res){
            //if previously was disconnected and now is connected
            // this message indicates that connection has restored
            if(res && !sIsConnected){
                Toast toast = Toast.makeText(sContext, sContext.getString(R.string.connection_recovered), Toast.LENGTH_LONG);
                View view = toast.getView();
                TextView text = (TextView) view.findViewById(android.R.id.message);
                text.setTextColor(ContextCompat.getColor(sContext, R.color.grey_900));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    text.setTextAppearance(R.style.CustomToast);
                }
                else {
                    text.setTextAppearance(sContext,R.style.CustomToast);
                }
                view.setBackground(ContextCompat.getDrawable(sContext, R.drawable.background_custom_toast) );
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }

            //set value to global var
            sIsConnected = res;

            //Initialize GNSDK API if is not initialized
            if(sIsConnected){
                if(!GnService.sApiInitialized){
                    GnService.withContext(sContext).initializeAPI(sStartedFrom);
                }
            }

            else {
                //Inform to user that connection has lost
                if (ServiceHelper.withContext(sContext).withService(FixerTrackService.CLASS_NAME).isServiceRunning()){
                    Toast toast = Toast.makeText(sContext, sContext.getString(R.string.connection_lost), Toast.LENGTH_LONG);
                    View view = toast.getView();
                    TextView text = (TextView) view.findViewById(android.R.id.message);
                    text.setTextColor(ContextCompat.getColor(sContext, R.color.grey_900));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        text.setTextAppearance(R.style.CustomToast);
                    } else {
                        text.setTextAppearance(sContext, R.style.CustomToast);
                    }
                    view.setBackground(ContextCompat.getDrawable(sContext, R.drawable.background_custom_toast));
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();

                    //Send this request to stop service
                    Intent stopIntent = new Intent(sContext, FixerTrackService.class);
                    stopIntent.setAction(Constants.Actions.ACTION_STOP_SERVICE);
                    stopIntent.putExtra(Constants.Actions.ACTION_STOP_SERVICE, Constants.StopsReasons.LOST_CONNECTION_TASK);
                    sContext.startService(stopIntent);
                }
            }
            sAsyncConnectivityDetection = null;
        }

        /**
         * Sends a ping to a server to
         * to check if really exist connection to internet,
         * as a developer you can change the ip
         * to against any other server you
         * want to test this ping
         * @param ip The ip to send the ping in format "XXX.XXX.XXX.XXX"
         * @return
         */
        private static boolean isConnectedToInternet(@Nullable String ip){
            //Send only 1 ping to Google DNS's to check internet connection
            //or provided your desired server ip
            String ping = "system/bin/ping -c 1 8.8.8.8";
            if(ip != null && !ip.isEmpty() ){
                ping = "system/bin/ping -c 1 " + ip;
            }
            Runtime runtime = Runtime.getRuntime();
            int termination = 1;
            try {
                java.lang.Process process = runtime.exec(ping);

                //abnormal termination will be different than 0,
                termination = process.waitFor();
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
            if(BuildConfig.DEBUG)
                return true;

            return (termination == 0);
        }

        private static boolean hasConnectivity(){
            ConnectivityManager cm = (ConnectivityManager)sContext.getSystemService(CONNECTIVITY_SERVICE);
            //cm can be null if for example Airplane mode is activated
            if(cm == null)
                return false;

            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

            return (activeNetwork != null && activeNetwork.isConnected());
        }

    }
}


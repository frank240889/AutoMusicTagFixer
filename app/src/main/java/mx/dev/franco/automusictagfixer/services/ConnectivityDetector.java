package mx.dev.franco.automusictagfixer.services;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import mx.dev.franco.automusictagfixer.R;

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


    private static class AsyncConnectivityDetection extends AsyncTask<Context,Void,Boolean>{

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

            Log.d("sIsConnected", sIsConnected+"");
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

            sIsConnected = res;

            if(sIsConnected){
                if(!GnService.sApiInitialized){
                    GnService.withContext(sContext).initializeAPI(sStartedFrom);
                }
            }

            else {

                Toast toast = Toast.makeText(sContext, sContext.getString(R.string.connection_lost), Toast.LENGTH_LONG);
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

                if(ServiceHelper.withContext(sContext.getApplicationContext()).withService(FixerTrackService.CLASS_NAME).isServiceRunning()){
                    FixerTrackService.lostConnection(false);
                    Intent stopIntent = new Intent(sContext, FixerTrackService.class);
                    sContext.stopService(stopIntent);
                }

            }
            sAsyncConnectivityDetection = null;
        }

        private static boolean isConnectedToInternet(@Nullable String ip){
            String ping = "system/bin/ping -c 1 8.8.8.8";
            if(ip != null ){
                ping = "system/bin/ping -c 1 " + ip;
            }
            Runtime runtime = Runtime.getRuntime();
            int termination = 1;
            try {
                //send only 1 ping to Google DNS's to check internet connection
                java.lang.Process process = runtime.exec(ping);
                //abnormal termination will be different than 0,
                termination = process.waitFor();
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
            finally {
                return (termination == 0);
            }
        }

        private static boolean hasConnectivity(){
            ConnectivityManager cm = (ConnectivityManager)sContext.getSystemService(CONNECTIVITY_SERVICE);
            if(cm == null)
                return false;

            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

            return (activeNetwork != null && activeNetwork.isConnected());
        }

    }
}


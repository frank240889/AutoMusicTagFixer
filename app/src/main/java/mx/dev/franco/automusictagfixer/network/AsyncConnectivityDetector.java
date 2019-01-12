package mx.dev.franco.automusictagfixer.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import java.io.IOException;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
import mx.dev.franco.automusictagfixer.BuildConfig;
import mx.dev.franco.automusictagfixer.interfaces.OnTestingNetwork;

import static android.content.Context.CONNECTIVITY_SERVICE;

/**
 * This class implements the connection testing using an asynctask
 */
public class AsyncConnectivityDetector extends AsyncTask<Context,Void,Boolean> {
    private static final String TAG = AsyncConnectivityDetector.class.getName();
    private OnTestingNetwork.OnTestingResult<String> mListener;
    @Inject
    Context mContext;
    public AsyncConnectivityDetector(){
        AutoMusicTagFixer.getContextComponent().inject(this);
    }

    public void setResultNetworkListener(OnTestingNetwork.OnTestingResult<String> onResultListener){
        mListener = onResultListener;
    }

    @Override
    protected Boolean doInBackground(Context... contexts) {
        boolean hasConnectivity = hasConnectivity();
        if(!hasConnectivity)
            return false;

        return isConnectedToInternet(null);
    }

    /**
     * Broadcast the result of connectivity detection and
     * saves this state in sIsconnected flag.
     * @param res
     */
    @Override
    protected void onPostExecute(Boolean res){
        if (res) {
            mListener.onNetworkConnected("");
        } else {
            mListener.onNetworkDisconnected("");
        }

        mListener = null;
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
    private boolean isConnectedToInternet(@Nullable String ip){
        //Send only 1 ping to Google DNS's to check internet connection
        //or provided your desired server ip
        String ping = "system/bin/ping -c 1 8.8.8.8";
        if(ip != null && !ip.isEmpty() ){
            ping = "system/bin/ping -c 1 " + ip;
        }
        Runtime runtime = Runtime.getRuntime();
        int termination = 1;
        try {
            Process process = runtime.exec(ping);

            //abnormal termination will be different than 0,
            termination = process.waitFor();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
        if(BuildConfig.DEBUG)
            return true;

        return (termination == 0);
    }

    private boolean hasConnectivity(){
        ConnectivityManager cm = (ConnectivityManager)mContext.getSystemService(CONNECTIVITY_SERVICE);
        //cm can be null if for example Airplane mode is activated
        if(cm == null)
            return false;

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        return (activeNetwork != null && activeNetwork.isConnected());
    }

}
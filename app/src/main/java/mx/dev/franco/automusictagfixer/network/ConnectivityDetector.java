package mx.dev.franco.automusictagfixer.network;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.identifier.GnService;
import mx.dev.franco.automusictagfixer.interfaces.OnTestingNetwork;
import mx.dev.franco.automusictagfixer.services.Job;
import mx.dev.franco.automusictagfixer.utilities.Constants;

/**
 * Created by franco on 26/12/17.
 */

public class ConnectivityDetector implements OnTestingNetwork, OnTestingNetwork.OnTestingResult<String> {
    private static final String TAG = ConnectivityDetector.class.getName();
    public static volatile boolean sIsConnected = false;
    private AsyncConnectivityDetector mAsyncConnectivityDetector;
    private static ConnectivityDetector sInstance;
    private Context mContext;

    private ConnectivityDetector(Context context){
        this.mContext = context;
    }

    public static ConnectivityDetector getInstance(Context context){
        if(sInstance == null){
            sInstance = new ConnectivityDetector(context);
        }
        return sInstance;
    }


    @Override
    public void onStartTestingNetwork() {
        if(mAsyncConnectivityDetector == null) {
            mAsyncConnectivityDetector = new AsyncConnectivityDetector();
            mAsyncConnectivityDetector.setResultNetworkListener(this);
            mAsyncConnectivityDetector.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    @Override
    public void onNetworkConnected(String param) {
        if(!GnService.sApiInitialized) {
            Job.scheduleJob(mContext);
        }

        ConnectivityDetector.sIsConnected = true;
        Intent intent = new Intent(Constants.Actions.ACTION_CONNECTION_RECOVERED);
        intent.putExtra(Constants.MESSAGE, mContext.getResources().getString(R.string.connection_recovered));
        LocalBroadcastManager.getInstance(mContext).sendBroadcastSync(intent);
        mAsyncConnectivityDetector = null;
    }

    @Override
    public void onNetworkDisconnected(String param) {
        Intent intent = new Intent(Constants.Actions.ACTION_CONNECTION_LOST);
        intent.putExtra(Constants.MESSAGE, mContext.getResources().getString(R.string.connection_lost));
        LocalBroadcastManager.getInstance(mContext).sendBroadcastSync(intent);
        ConnectivityDetector.sIsConnected = false;
        mAsyncConnectivityDetector = null;
    }
}


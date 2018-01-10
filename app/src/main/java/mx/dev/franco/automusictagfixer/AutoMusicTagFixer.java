package mx.dev.franco.automusictagfixer;

import android.app.Application;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.ConnectivityManager;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;

import io.fabric.sdk.android.Fabric;
import mx.dev.franco.automusictagfixer.services.ConnectivityChangesDetector;


/**
 * Created by franco on 26/12/17.
 */

public final class AutoMusicTagFixer extends Application {
    private ConnectivityChangesDetector mConnectivityChangesDetector;
    private IntentFilter mIntentFilter;
    // Called when the application is starting, before any other application objects have been created.
    // Overriding this method is totally optional!
    @Override
    public void onCreate() {
        super.onCreate();
        //Fabric.with(this, new Crashlytics());
// Set up Crashlytics, disabled for debug builds
        Crashlytics crashlyticsKit = new Crashlytics.Builder()
                .core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
                .build();

// Initialize Fabric with the debug-disabled crashlytics.
        Fabric.with(this, crashlyticsKit);
        // Required initialization logic here!
        mConnectivityChangesDetector = new ConnectivityChangesDetector();
        mIntentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mConnectivityChangesDetector,mIntentFilter);
    }

    // Called by the system when the device configuration changes while your component is running.
    // Overriding this method is totally optional!
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    // This is called when the overall system is running low on memory,
    // and would like actively running processes to tighten their belts.
    // Overriding this method is totally optional!
    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }
}

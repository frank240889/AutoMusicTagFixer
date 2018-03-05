package mx.dev.franco.automusictagfixer;

import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.os.Build;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.squareup.leakcanary.LeakCanary;

import io.fabric.sdk.android.Fabric;
import mx.dev.franco.automusictagfixer.services.ConnectivityChangesDetector;
import mx.dev.franco.automusictagfixer.services.DetectorRemovableMediaStorages;
import mx.dev.franco.automusictagfixer.utilities.StorageHelper;


/**
 * Created by franco on 26/12/17.
 */

public final class AutoMusicTagFixer extends Application {
    private ConnectivityChangesDetector mConnectivityChangesDetector;
    private IntentFilter mIntentFilter;

    private IntentFilter mIntentFilterMediaMounted;
    private IntentFilter mIntentFilterMediaUnmounted;
    private DetectorRemovableMediaStorages mDetectorRemovableMediaStorages;
    public static boolean IS_LOLLIPOP = Build.VERSION.SDK_INT < Build.VERSION_CODES.M;
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
        //Detect connectivity changes
        mConnectivityChangesDetector = new ConnectivityChangesDetector();
        mIntentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);

        registerReceiver(mConnectivityChangesDetector,mIntentFilter);

        //Detect if a media is removed or added while app is running
        mDetectorRemovableMediaStorages = new DetectorRemovableMediaStorages(getApplicationContext());
        mIntentFilterMediaMounted = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
        mIntentFilterMediaMounted.addDataScheme("file");
        mIntentFilterMediaUnmounted = new IntentFilter(Intent.ACTION_MEDIA_UNMOUNTED);
        mIntentFilterMediaUnmounted.addDataScheme("file");

        registerReceiver(mDetectorRemovableMediaStorages, mIntentFilterMediaMounted);
        registerReceiver(mDetectorRemovableMediaStorages, mIntentFilterMediaUnmounted);
        StorageHelper.getInstance(getApplicationContext()).detectStorages();

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);
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

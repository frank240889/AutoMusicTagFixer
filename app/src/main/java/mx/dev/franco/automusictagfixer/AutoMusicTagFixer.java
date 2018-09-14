package mx.dev.franco.automusictagfixer;

import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.support.v7.app.AppCompatDelegate;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.squareup.leakcanary.LeakCanary;

import io.fabric.sdk.android.Fabric;
import mx.dev.franco.automusictagfixer.dagger.ContextComponent;
import mx.dev.franco.automusictagfixer.dagger.ContextModule;
import mx.dev.franco.automusictagfixer.dagger.DaggerContextComponent;
import mx.dev.franco.automusictagfixer.network.ConnectivityChangesDetector;
import mx.dev.franco.automusictagfixer.receivers.DetectorRemovableMediaStorages;
import mx.dev.franco.automusictagfixer.utilities.StorageHelper;


/**
 * Created by franco on 26/12/17.
 */

public final class AutoMusicTagFixer extends Application {
    private static ContextComponent mContextComponent;
    private ConnectivityChangesDetector mConnectivityChangesDetector;
    private DetectorRemovableMediaStorages mDetectorRemovableMediaStorages;
    // Called when the application is starting, before any other application objects have been created.
    // Overriding this method is totally optional!
    @Override
    public void onCreate() {
        super.onCreate();
        mContextComponent = DaggerContextComponent.builder().
                contextModule(new ContextModule(this)).
                build();


        //Fabric.with(this, new Crashlytics());
// Set up Crashlytics, disabled for debug builds
        Crashlytics crashlyticsKit = new Crashlytics.Builder()
                .core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
                .build();
// Initialize Fabric with the debug-disabled crashlytics.
        Fabric.with(this, crashlyticsKit);
        //Detect connectivity changes
        mConnectivityChangesDetector = new ConnectivityChangesDetector();
        IntentFilter filterConnectivity = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);

        registerReceiver(mConnectivityChangesDetector,filterConnectivity);

        //Detect if a media is removed or added while app is running
        mDetectorRemovableMediaStorages = new DetectorRemovableMediaStorages();
        IntentFilter filterMediaMounted = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
        filterMediaMounted.addDataScheme("file");
        IntentFilter filterMediaUnmounted = new IntentFilter(Intent.ACTION_MEDIA_UNMOUNTED);
        filterMediaUnmounted.addDataScheme("file");

        registerReceiver(mDetectorRemovableMediaStorages, filterMediaMounted);
        registerReceiver(mDetectorRemovableMediaStorages, filterMediaUnmounted);

        StorageHelper.getInstance(getApplicationContext()).detectStorages();

        LeakCanary.install(this);
        //Stetho.initializeWithDefaults(this);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
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

    public static ContextComponent getContextComponent(){
        return mContextComponent;
    }
}

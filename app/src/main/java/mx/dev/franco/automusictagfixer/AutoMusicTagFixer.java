package mx.dev.franco.automusictagfixer;

import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatDelegate;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;

import io.fabric.sdk.android.Fabric;
import mx.dev.franco.automusictagfixer.identifier.GnApiService;
import mx.dev.franco.automusictagfixer.receivers.DetectorRemovableMediaStorages;
import mx.dev.franco.automusictagfixer.utilities.StorageHelper;
import mx.dev.franco.automusictagfixer.utilities.Tagger;


/**
 * Created by franco on 26/12/17.
 */

public final class AutoMusicTagFixer extends Application {
    private DetectorRemovableMediaStorages mDetectorRemovableMediaStorages;
    // Called when the application is starting, before any other application objects have been created.
    // Overriding this method is totally optional!
    @Override
    public void onCreate() {
        super.onCreate();
        Tagger.init(this);
        GnApiService.init(this);
        //GnApiService.getInstance().initializeAPI();


        //Fabric.with(this, new Crashlytics());
        // Set up Crashlytics, disabled for debug builds
        Crashlytics crashlyticsKit = new Crashlytics.Builder()
                .core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
                .build();
        // Initialize Fabric with the debug-disabled crashlytics.
        Fabric.with(this, crashlyticsKit);

        //Detect if a media is removed or added while app is running
        mDetectorRemovableMediaStorages = new DetectorRemovableMediaStorages();
        IntentFilter filterMediaMounted = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
        filterMediaMounted.addDataScheme("file");
        IntentFilter filterMediaUnmounted = new IntentFilter(Intent.ACTION_MEDIA_UNMOUNTED);
        filterMediaUnmounted.addDataScheme("file");

        registerReceiver(mDetectorRemovableMediaStorages, filterMediaMounted);
        registerReceiver(mDetectorRemovableMediaStorages, filterMediaUnmounted);

        StorageHelper.getInstance(getApplicationContext()).detectStorages();

        //LeakCanary.install(this);
        //Stetho.initializeWithDefaults(this);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }
}

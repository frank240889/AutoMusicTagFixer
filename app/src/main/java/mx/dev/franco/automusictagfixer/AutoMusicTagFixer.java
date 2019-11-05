package mx.dev.franco.automusictagfixer;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.Intent;

import androidx.appcompat.app.AppCompatDelegate;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.facebook.stetho.Stetho;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import dagger.android.HasServiceInjector;
import io.fabric.sdk.android.Fabric;
import mx.dev.franco.automusictagfixer.covermanager.CoverManager;
import mx.dev.franco.automusictagfixer.di.DaggerApplicationComponent;
import mx.dev.franco.automusictagfixer.identifier.ApiInitializerService;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;


/**
 * Created by franco on 26/12/17.
 */

public final class AutoMusicTagFixer extends Application implements HasActivityInjector, HasServiceInjector {
    private static ExecutorService executorService;
    @Inject
    DispatchingAndroidInjector<Activity> dispatchingAndroidInjector;
    @Inject
    DispatchingAndroidInjector<Service> serviceDispatchingAndroidInjector;
    @Inject
    AbstractSharedPreferences mAbstractSharedPreferences;
    // Called when the application is starting, before any other application objects have been created.
    @Override
    public void onCreate() {
        super.onCreate();
        CoverManager.getInstance();
        Intent intent = new Intent(getApplicationContext(), ApiInitializerService.class);
        startService(intent);
        DaggerApplicationComponent.builder()
            .application(this)
            .build()
            .inject(this);

        //Fabric.with(this, new Crashlytics());
        // Set up Crashlytics, disabled for debug builds
        Crashlytics crashlyticsKit = new Crashlytics.Builder()
                .core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
                .build();
        // Initialize Fabric with the debug-disabled crashlytics.
        Fabric.with(this, crashlyticsKit);

        //LeakCanary.install(this);
        Stetho.initializeWithDefaults(this);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        if(AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
            if(mAbstractSharedPreferences.getBoolean("dark_mode")) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                mAbstractSharedPreferences.putBoolean("dark_mode", true);
            }
            else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                mAbstractSharedPreferences.putBoolean("dark_mode", false);
            }
        }
    }

    @Override
    public DispatchingAndroidInjector<Activity> activityInjector() {
        return dispatchingAndroidInjector;
    }

    @Override
    public AndroidInjector<Service> serviceInjector() {
        return serviceDispatchingAndroidInjector;
    }

    public static ExecutorService getExecutorService() {
        if(executorService == null || executorService.isShutdown()) {
            executorService = Executors.newCachedThreadPool();
        }
        return executorService;
    }
}

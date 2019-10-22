package mx.dev.franco.automusictagfixer;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.Intent;

import androidx.appcompat.app.AppCompatDelegate;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.facebook.stetho.Stetho;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import dagger.android.HasServiceInjector;
import io.fabric.sdk.android.Fabric;
import mx.dev.franco.automusictagfixer.di.DaggerApplicationComponent;
import mx.dev.franco.automusictagfixer.identifier.ApiInitializerService;


/**
 * Created by franco on 26/12/17.
 */

public final class AutoMusicTagFixer extends Application implements HasActivityInjector, HasServiceInjector {
    @Inject
    DispatchingAndroidInjector<Activity> dispatchingAndroidInjector;
    @Inject
    DispatchingAndroidInjector<Service> serviceDispatchingAndroidInjector;
    // Called when the application is starting, before any other application objects have been created.
    // Overriding this method is totally optional!
    @Override
    public void onCreate() {
        super.onCreate();
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
    }

    @Override
    public DispatchingAndroidInjector<Activity> activityInjector() {
        return dispatchingAndroidInjector;
    }

    @Override
    public AndroidInjector<Service> serviceInjector() {
        return serviceDispatchingAndroidInjector;
    }
}

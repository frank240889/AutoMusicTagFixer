package mx.dev.franco.automusictagfixer

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import mx.dev.franco.automusictagfixer.covermanager.CoverLoader
import mx.dev.franco.automusictagfixer.di.DaggerApplicationComponent
import mx.dev.franco.automusictagfixer.fixer.StorageHelper
import mx.dev.franco.automusictagfixer.persistence.room.TrackRoomDatabase
import mx.dev.franco.automusictagfixer.receivers.DetectorRemovableMediaStorage
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

/**
 * Created by franco on 26/12/17.
 */
class AutoMusicTagFixer : Application(), HasAndroidInjector{
    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var mAbstractSharedPreferences: AbstractSharedPreferences

    @Inject
    lateinit var storageHelper: StorageHelper

    @Inject
    lateinit var mTrackRoomDatabase: TrackRoomDatabase
    private var mDetectorRemovableMediaStorage: DetectorRemovableMediaStorage? = null

    // Called when the application is starting, before any other application objects have been created.
    override fun onCreate() {
        super.onCreate()
        DaggerApplicationComponent.builder()
            .application(this)
            .build()?.inject(this)

        CoverLoader.getInstance()

        //LeakCanary.install(this);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        val nightMode: Int = AppCompatDelegate.getDefaultNightMode()
        if (nightMode == AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY ||
            nightMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        ) {
            val currentNightMode =
                resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (currentNightMode == Configuration.UI_MODE_NIGHT_NO) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        } else {
            if (mAbstractSharedPreferences.getBoolean(DARK_MODE)) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                mAbstractSharedPreferences.putBoolean(DARK_MODE, true)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                mAbstractSharedPreferences.putBoolean(DARK_MODE, false)
            }
        }
        //storageHelper.detectStorage()
        mDetectorRemovableMediaStorage = DetectorRemovableMediaStorage()
        val actionMediaMountedFilter = IntentFilter(Intent.ACTION_MEDIA_MOUNTED)
        actionMediaMountedFilter.addDataScheme("file")
        val actionMediaUnmountedFilter = IntentFilter(Intent.ACTION_MEDIA_UNMOUNTED)
        actionMediaUnmountedFilter.addDataScheme("file")
        registerReceiver(mDetectorRemovableMediaStorage, actionMediaMountedFilter)
        registerReceiver(mDetectorRemovableMediaStorage, actionMediaUnmountedFilter)
    }

    override fun androidInjector(): AndroidInjector<Any> = dispatchingAndroidInjector

    companion object {
        const val DARK_MODE = BuildConfig.APPLICATION_ID + ".dark_mode"
        @JvmStatic
        var executorService: ExecutorService? = null
            get() {
                if (field == null || field!!.isShutdown) {
                    field = Executors.newCachedThreadPool()
                }
                return field
            }
            private set
    }
}
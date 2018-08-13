package mx.dev.franco.automusictagfixer.dagger;

import android.app.Application;
import android.arch.persistence.room.Room;
import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import mx.dev.franco.automusictagfixer.network.ConnectivityDetector;
import mx.dev.franco.automusictagfixer.repository.TrackRepository;
import mx.dev.franco.automusictagfixer.room.TrackRoomDatabase;
import mx.dev.franco.automusictagfixer.services.ServiceHelper;
import mx.dev.franco.automusictagfixer.services.gnservice.GnService;
import mx.dev.franco.automusictagfixer.utilities.SimpleMediaPlayer;
import mx.dev.franco.automusictagfixer.utilities.StorageHelper;
import mx.dev.franco.automusictagfixer.utilities.Tagger;
import mx.dev.franco.automusictagfixer.utilities.resource_manager.AndroidResourceManager;
import mx.dev.franco.automusictagfixer.utilities.resource_manager.ResourceManager;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.DefaultSharedPreferencesImpl;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.SharedPreferencesImpl;

/**
 * Created by Franco Castillo on 27/03/2018.
 * Module to provide Context, ResourceManager and SharedPreferences to
 * no Android components that require it.
 */

@Module
public class ContextModule {
    private final Application mApp;
    private ResourceManager mResourceManager;
    private AbstractSharedPreferences mAbstractSharedPreferences;
    private DefaultSharedPreferencesImpl mDefaultAbstractSharedPreferences;
    private TrackRoomDatabase mTrackRoomDatabase;
    private TrackRepository mTrackRepository;
    private ServiceHelper mServiceHelper;
    private SimpleMediaPlayer mSimpleMediaPlayer;
    private StorageHelper mStorageHelper;
    private Tagger mTagger;
    private GnService mGnService;
    private ConnectivityDetector mConnectivityDetector;

    public ContextModule(Application application){
        mApp = application;
        mTrackRoomDatabase = Room.databaseBuilder(mApp, TrackRoomDatabase.class, "track_database").build();
        mResourceManager =  new AndroidResourceManager(mApp);
        mAbstractSharedPreferences = new SharedPreferencesImpl(mApp);
        mDefaultAbstractSharedPreferences = new DefaultSharedPreferencesImpl(mApp);
        mTrackRepository = new TrackRepository(provideTrackRoomDatabase(), provideSharedPreferences(), mApp);
        mServiceHelper = ServiceHelper.getInstance(mApp);
        mSimpleMediaPlayer = SimpleMediaPlayer.getInstance(mApp);
        mStorageHelper = StorageHelper.getInstance(mApp);
        mTagger = Tagger.getInstance(mApp, mStorageHelper);
        mGnService = GnService.getInstance(mApp);
        mConnectivityDetector = ConnectivityDetector.getInstance(mApp);
    }

    /**
     * Just use one instance of app
     * @return
     */
    @Provides
    @Singleton
    Context provideContext(){
        return mApp;
    }

    @Provides
    @Singleton
    TrackRoomDatabase provideTrackRoomDatabase(){
        return mTrackRoomDatabase;
    }

    @Provides
    @Singleton
    ResourceManager provideResourcesManager(){
        return mResourceManager;
    }

    @Provides
    @Singleton
    AbstractSharedPreferences provideSharedPreferences(){
        return mAbstractSharedPreferences;
    }

    @Provides
    @Singleton
    DefaultSharedPreferencesImpl provideDefaultSharedPreferences(){
        return mDefaultAbstractSharedPreferences;
    }

    @Provides
    @Singleton
    TrackRepository provideTrackRepository(){
        return mTrackRepository;
    }

    @Provides
    ServiceHelper provideServiceHelper(){
        return mServiceHelper;
    }

    @Provides
    Tagger provideTagger(){
        return mTagger;
    }

    @Provides
    SimpleMediaPlayer provideMediaPlayer(){
        return mSimpleMediaPlayer;
    }

    @Provides
    StorageHelper provideStorageHelper(){
        return mStorageHelper;
    }

    @Provides
    GnService provideGnService(){
        return mGnService;
    }

    @Provides
    ConnectivityDetector provideConnectivityDetector(){
        return mConnectivityDetector;
    }
}

package mx.dev.franco.automusictagfixer.di;

import android.app.Application;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;

@Module
public class StorageModule {

    @Singleton
    @Provides
    AudioTagger.StorageHelper providesStorageHelper(Application application) {
        return AudioTagger.StorageHelper.getInstance(application);
    }
}

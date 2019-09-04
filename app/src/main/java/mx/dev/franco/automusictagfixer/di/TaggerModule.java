package mx.dev.franco.automusictagfixer.di;

import android.app.Application;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import mx.dev.franco.automusictagfixer.utilities.AudioTagger;
import mx.dev.franco.automusictagfixer.utilities.StorageHelper;

@Module
public class TaggerModule {

    @Singleton
    @Provides
    AudioTagger providesTagger(Application application, StorageHelper storageHelper) {
        return new AudioTagger(application, storageHelper);
    }
}

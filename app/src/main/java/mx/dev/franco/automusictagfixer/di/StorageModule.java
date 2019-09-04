package mx.dev.franco.automusictagfixer.di;

import android.app.Application;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import mx.dev.franco.automusictagfixer.utilities.StorageHelper;

@Module
public class StorageModule {

@Singleton
    @Provides
    StorageHelper providesStorageHelper(Application application) {
    return StorageHelper.getInstance(application);
}
}

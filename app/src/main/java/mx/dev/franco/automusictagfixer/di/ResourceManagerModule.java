package mx.dev.franco.automusictagfixer.di;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import mx.dev.franco.automusictagfixer.utilities.resource_manager.AndroidResourceManager;
import mx.dev.franco.automusictagfixer.utilities.resource_manager.ResourceManager;

@Module
public class ResourceManagerModule {
    @Provides
    @Singleton
    ResourceManager provideResourceManager(Context context) {
        return new AndroidResourceManager(context);
    }
}

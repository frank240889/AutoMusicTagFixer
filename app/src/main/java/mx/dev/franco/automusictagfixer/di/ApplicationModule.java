package mx.dev.franco.automusictagfixer.di;

import android.app.Application;
import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * The module that provides the context.
 */
@Module
public class ApplicationModule {

    @Singleton
    @Provides
    Context provideContext(Application application) {
        return application;
    }
}

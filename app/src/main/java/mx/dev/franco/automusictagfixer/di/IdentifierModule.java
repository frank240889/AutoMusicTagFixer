package mx.dev.franco.automusictagfixer.di;

import android.app.Application;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import mx.dev.franco.automusictagfixer.identifier.GnService;
import mx.dev.franco.automusictagfixer.identifier.IdentifierFactory;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;

@Module
public class IdentifierModule {

    @Singleton
    @Provides
    GnService provideGnService(Application application){
        GnService.init(application);
        return GnService.getInstance();
    }

    @Singleton
    @Provides
    IdentifierFactory provideIdentifierFactory(GnService gnService, AbstractSharedPreferences sharedPreferences) {
        return new IdentifierFactory(gnService, sharedPreferences);
    }

}

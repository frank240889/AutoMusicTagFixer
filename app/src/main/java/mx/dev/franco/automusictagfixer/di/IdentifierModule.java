package mx.dev.franco.automusictagfixer.di;

import android.app.Application;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import mx.dev.franco.automusictagfixer.identifier.GnApiService;
import mx.dev.franco.automusictagfixer.identifier.IdentifierFactory;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;

@Module
public class IdentifierModule {

    @Singleton
    @Provides
    GnApiService provideGnService(Application application){
        return GnApiService.getInstance(application);
    }

    @Singleton
    @Provides
    IdentifierFactory provideIdentifierFactory(GnApiService gnApiService, AbstractSharedPreferences sharedPreferences) {
        return new IdentifierFactory(gnApiService, sharedPreferences);
    }

}

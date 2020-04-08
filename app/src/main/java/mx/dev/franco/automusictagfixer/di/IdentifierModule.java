package mx.dev.franco.automusictagfixer.di;

import android.app.Application;
import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import mx.dev.franco.automusictagfixer.identifier.GnApiService;
import mx.dev.franco.automusictagfixer.identifier.IdentificationManager;
import mx.dev.franco.automusictagfixer.identifier.IdentifierFactory;
import mx.dev.franco.automusictagfixer.persistence.cache.IdentificationResultsCache;
import mx.dev.franco.automusictagfixer.utilities.resource_manager.ResourceManager;

@Module
public class IdentifierModule {

    @Singleton
    @Provides
    GnApiService provideGnService(Application application){
        return GnApiService.getInstance(application);
    }

    @Singleton
    @Provides
    IdentifierFactory provideIdentifierFactory(GnApiService gnApiService, ResourceManager resourceManager) {
        return new IdentifierFactory(gnApiService, resourceManager);
    }

    @Provides
    IdentificationManager provideIdentificationManager(IdentificationResultsCache trackResultsCache,
                                                       IdentifierFactory identifierFactory,
                                                       GnApiService gnApiService,
                                                       Context context) {
        return new IdentificationManager(trackResultsCache, identifierFactory, gnApiService, context);
    }

}

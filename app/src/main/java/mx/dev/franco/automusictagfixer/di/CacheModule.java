package mx.dev.franco.automusictagfixer.di;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import mx.dev.franco.automusictagfixer.persistence.cache.CoverDataCache;
import mx.dev.franco.automusictagfixer.persistence.cache.IdentificationResultsCache;

@Module
public class CacheModule {

    @Singleton
    @Provides
    public IdentificationResultsCache providesCache() {
        return new IdentificationResultsCache();
    }

    @Singleton
    @Provides
    public CoverDataCache provideCoverDataCache() {
        return new CoverDataCache();
    }

}

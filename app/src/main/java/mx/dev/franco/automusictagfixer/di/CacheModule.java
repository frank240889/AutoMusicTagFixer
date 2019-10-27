package mx.dev.franco.automusictagfixer.di;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import mx.dev.franco.automusictagfixer.persistence.cache.CoverResultsCache;
import mx.dev.franco.automusictagfixer.persistence.cache.IdentificationResultsCache;
import mx.dev.franco.automusictagfixer.persistence.cache.TrackResultsCache;

@Module
public class CacheModule {

@Singleton
@Provides
public IdentificationResultsCache providesCache() {
    return new IdentificationResultsCache();
}

@Singleton
@Provides
public TrackResultsCache providesTrackResultsCache() {
    return new TrackResultsCache();
}

@Singleton
@Provides
public CoverResultsCache providesCoverResultsCache() {
    return new CoverResultsCache();
}

}

package mx.dev.franco.automusictagfixer.di;

import java.util.List;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import mx.dev.franco.automusictagfixer.identifier.Identifier;
import mx.dev.franco.automusictagfixer.interfaces.Cache;
import mx.dev.franco.automusictagfixer.persistence.cache.DownloadedTrackDataCacheImpl;

@Module
public class CacheModule {

@Singleton
@Provides
Cache<String, List<Identifier.IdentificationResults>> providesCache() {
    return new DownloadedTrackDataCacheImpl();
}
}

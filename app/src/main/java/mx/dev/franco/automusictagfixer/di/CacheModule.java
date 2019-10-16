package mx.dev.franco.automusictagfixer.di;

import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import mx.dev.franco.automusictagfixer.persistence.cache.DownloadedTrackDataCacheImpl;

@Module
public class CacheModule {

@Singleton
@Provides
DownloadedTrackDataCacheImpl providesCache() {
        return new DownloadedTrackDataCacheImpl();
    }
}

package mx.dev.franco.automusictagfixer.di;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import mx.dev.franco.automusictagfixer.persistence.mediastore.MediaStoreManager;
import mx.dev.franco.automusictagfixer.persistence.room.TrackDAO;

@Module
public class MediaStoreManagerModule {
    @Singleton
    @Provides
    MediaStoreManager provideMediaStoreManager(Context context, TrackDAO trackDAO) {
        return new MediaStoreManager(context, trackDAO);
    }
}

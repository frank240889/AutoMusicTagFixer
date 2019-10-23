package mx.dev.franco.automusictagfixer.di;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import mx.dev.franco.automusictagfixer.persistence.mediastore.MediaStoreManager;

@Module
public class MediaStoreManagerModule {
    @Singleton
    @Provides
    MediaStoreManager provideMediaStoreManager(Context context) {
        return new MediaStoreManager(context);
    }
}

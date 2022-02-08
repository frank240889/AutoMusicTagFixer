package mx.dev.franco.automusictagfixer.di

import android.content.Context
import dagger.Module
import dagger.Provides
import mx.dev.franco.automusictagfixer.persistence.mediastore.MediaStoreAccess
import javax.inject.Singleton

@Module
class MediaStoreManagerModule {
    @Singleton
    @Provides
    fun provideMediaStoreManager(application: Context) =  MediaStoreAccess(application)

}
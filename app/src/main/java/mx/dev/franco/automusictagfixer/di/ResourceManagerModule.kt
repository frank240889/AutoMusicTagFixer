package mx.dev.franco.automusictagfixer.di

import android.content.Context
import dagger.Module
import dagger.Provides
import mx.dev.franco.automusictagfixer.utilities.resource_manager.AndroidResourceManager
import mx.dev.franco.automusictagfixer.utilities.resource_manager.ResourceManager
import javax.inject.Singleton

@Module
class ResourceManagerModule {
    @Provides
    @Singleton
    fun provideResourceManager(application: Context): ResourceManager = AndroidResourceManager(application)

}
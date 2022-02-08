package mx.dev.franco.automusictagfixer.di

import android.app.Application
import dagger.Module
import dagger.Provides
import mx.dev.franco.automusictagfixer.fixer.AudioTagger
import mx.dev.franco.automusictagfixer.fixer.StorageHelper
import javax.inject.Singleton

@Module
class TaggerModule {

    @Singleton
    @Provides
    fun providesTagger(application: Application, storageHelper: StorageHelper) =
        AudioTagger(application, storageHelper)

}
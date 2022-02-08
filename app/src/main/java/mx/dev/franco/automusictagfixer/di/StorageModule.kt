package mx.dev.franco.automusictagfixer.di

import android.content.Context
import dagger.Module
import dagger.Provides
import mx.dev.franco.automusictagfixer.fixer.StorageHelper
import javax.inject.Singleton

@Module
class StorageModule {
    @Singleton
    @Provides
    fun providesStorageHelper(application: Context): StorageHelper {
        return StorageHelper.getInstance(application)
    }
}
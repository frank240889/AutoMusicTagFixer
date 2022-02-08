package mx.dev.franco.automusictagfixer.di

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * The module that provides the context.
 */
@Module
class ApplicationModule {

    @Singleton
    @Provides
    fun provideContext(application: Application): Context = run {application}

}
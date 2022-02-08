package mx.dev.franco.automusictagfixer.di

import android.content.Context
import dagger.Module
import dagger.Provides
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.SharedPreferencesImpl
import javax.inject.Singleton

@Module
class SharedPreferencesModule {

    @Singleton
    @Provides
    fun provideSharedPreferences(application: Context): AbstractSharedPreferences = SharedPreferencesImpl(application)
}
package mx.dev.franco.automusictagfixer.di;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.SharedPreferencesImpl;

@Module
public class SharedPreferencesModule {

    @Singleton
    @Provides
    public AbstractSharedPreferences provideSharedPreferences(Context application) {
        return new SharedPreferencesImpl(application);
    }
}

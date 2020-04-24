package mx.dev.franco.automusictagfixer.di;

import android.content.Context;

import androidx.annotation.NonNull;

import dagger.Module;
import dagger.Provides;
import mx.dev.franco.automusictagfixer.persistence.repository.TrackRepository;
import mx.dev.franco.automusictagfixer.persistence.room.TrackRoomDatabase;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;

@Module
public class RepositoryModule {

    @ActivityScope
    @Provides
    TrackRepository providesTrackRepository(@NonNull TrackRoomDatabase db,
                                            @NonNull AbstractSharedPreferences abstractSharedPreferences,
                                            @NonNull Context context) {
        return new TrackRepository(db,abstractSharedPreferences, context);
    }
}

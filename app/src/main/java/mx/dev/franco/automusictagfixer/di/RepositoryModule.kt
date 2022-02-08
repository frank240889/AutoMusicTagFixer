package mx.dev.franco.automusictagfixer.di

import dagger.Module
import dagger.Provides
import mx.dev.franco.automusictagfixer.persistence.repository.TrackRepository
import mx.dev.franco.automusictagfixer.persistence.room.TrackRoomDatabase
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences

@Module
class RepositoryModule {

    @ActivityScope
    @Provides
    fun providesTrackRepository(
        db: TrackRoomDatabase,
        abstractSharedPreferences: AbstractSharedPreferences
    ) = TrackRepository(db, abstractSharedPreferences)

}
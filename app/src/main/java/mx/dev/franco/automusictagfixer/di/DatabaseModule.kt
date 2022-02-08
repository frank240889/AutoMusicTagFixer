package mx.dev.franco.automusictagfixer.di

import android.content.Context
import dagger.Module
import dagger.Provides
import mx.dev.franco.automusictagfixer.persistence.room.TrackRoomDatabase
import javax.inject.Singleton

@Module
class DatabaseModule {
    @Singleton
    @Provides
    fun provideDatabase(application: Context) = TrackRoomDatabase.getDatabase(application)

}
package mx.dev.franco.automusictagfixer.di;

import android.app.Application;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import mx.dev.franco.automusictagfixer.persistence.room.TrackDAO;
import mx.dev.franco.automusictagfixer.persistence.room.TrackRoomDatabase;

@Module
public class DatabaseModule {

    @Singleton
    @Provides
    public TrackRoomDatabase provideDatabase(Application application) {
        return TrackRoomDatabase.getDatabase(application);
    }

    @Singleton
    @Provides
    public TrackDAO provideEventDao(TrackRoomDatabase trackRoomDatabase) {
        return trackRoomDatabase.trackDao();
    }
}

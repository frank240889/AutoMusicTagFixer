package mx.dev.franco.automusictagfixer.di;

import android.app.Application;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import mx.dev.franco.automusictagfixer.audioplayer.SimpleMediaPlayer;

@Module
public class MediaPlayerModule {
    @Singleton
    @Provides
    SimpleMediaPlayer providesMediaPlayer(Application application) {
        return SimpleMediaPlayer.getInstance(application);
    }
}

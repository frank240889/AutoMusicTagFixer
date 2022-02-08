package mx.dev.franco.automusictagfixer.di

import android.content.Context
import dagger.Module
import dagger.Provides
import mx.dev.franco.automusictagfixer.audioplayer.SimpleMediaPlayer
import javax.inject.Singleton

@Module
class MediaPlayerModule {

    @Singleton
    @Provides
    fun providesMediaPlayer(application: Context) = SimpleMediaPlayer.getInstance(application)

}
package mx.dev.franco.automusictagfixer.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import mx.dev.franco.automusictagfixer.services.FixerTrackService;

@Module
public abstract class ServiceModule {
    @ContributesAndroidInjector
    abstract FixerTrackService contributesFixerTrackService();
}

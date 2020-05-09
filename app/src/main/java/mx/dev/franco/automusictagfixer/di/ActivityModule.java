package mx.dev.franco.automusictagfixer.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import mx.dev.franco.automusictagfixer.ui.main.MainActivity;
import mx.dev.franco.automusictagfixer.ui.settings.ConfigurationActivity;
import mx.dev.franco.automusictagfixer.ui.trackdetail.TrackDetailActivity;

/**
 * The module that injects the dependencies into fragments.
 */
@Module
public abstract class ActivityModule {
    @ActivityScope
    @ContributesAndroidInjector(modules = FragmentModule.class)
    public abstract MainActivity contributeMainActivity();
    @ActivityScope
    @ContributesAndroidInjector(modules = FragmentModule.class)
    public abstract TrackDetailActivity contributeTrackDetailActivity();
    @ActivityScope
    @ContributesAndroidInjector(modules = FragmentModule.class)
    public abstract ConfigurationActivity contributeConfigurationActivity();
}

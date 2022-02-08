package mx.dev.franco.automusictagfixer.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import mx.dev.franco.automusictagfixer.ui.main.MainActivity
import mx.dev.franco.automusictagfixer.ui.settings.ConfigurationActivity
import mx.dev.franco.automusictagfixer.ui.trackdetail.TrackDetailActivity

/**
 * The module that injects the dependencies into fragments.
 */
@Module
abstract class ActivityModule {

    @ActivityScope
    @ContributesAndroidInjector(modules = [FragmentModule::class])
    abstract fun contributeMainActivity(): MainActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [FragmentModule::class])
    abstract fun contributeTrackDetailActivity(): TrackDetailActivity
    
    @ActivityScope
    @ContributesAndroidInjector(modules = [FragmentModule::class])
    abstract fun contributeConfigurationActivity(): ConfigurationActivity
}
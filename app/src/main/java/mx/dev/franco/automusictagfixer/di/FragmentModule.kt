package mx.dev.franco.automusictagfixer.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import mx.dev.franco.automusictagfixer.ui.main.MainFragment
import mx.dev.franco.automusictagfixer.ui.search.ResultSearchFragment
import mx.dev.franco.automusictagfixer.ui.settings.SettingsFragment
import mx.dev.franco.automusictagfixer.ui.trackdetail.MetadataDetailsFragment
import mx.dev.franco.automusictagfixer.ui.trackdetail.TrackDetailFragment

@Module
abstract class FragmentModule {

    @ContributesAndroidInjector
    abstract fun contributeListFragment(): MainFragment

    @ContributesAndroidInjector
    abstract fun contributeTrackDetailFragment(): TrackDetailFragment

    @ContributesAndroidInjector
    abstract fun contributeResultSearchListFragment(): ResultSearchFragment

    @ContributesAndroidInjector
    abstract fun contributeMetadataDetailsFragment(): MetadataDetailsFragment
    
    @ContributesAndroidInjector
    abstract fun contributeSettingsFragmentFragment(): SettingsFragment
}
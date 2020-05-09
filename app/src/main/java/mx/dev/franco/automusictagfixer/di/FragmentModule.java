package mx.dev.franco.automusictagfixer.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import mx.dev.franco.automusictagfixer.ui.main.MainFragment;
import mx.dev.franco.automusictagfixer.ui.search.ResultSearchFragment;
import mx.dev.franco.automusictagfixer.ui.settings.SettingsFragment;
import mx.dev.franco.automusictagfixer.ui.trackdetail.MetadataDetailsFragment;
import mx.dev.franco.automusictagfixer.ui.trackdetail.TrackDetailFragment;

@Module
public abstract class FragmentModule {

    @ContributesAndroidInjector
    abstract MainFragment contributeListFragment();

    @ContributesAndroidInjector
    abstract TrackDetailFragment contributeTrackDetailFragment();

    @ContributesAndroidInjector
    abstract ResultSearchFragment contributeResultSearchListFragment();

    @ContributesAndroidInjector
    abstract MetadataDetailsFragment contributeMetadataDetailsFragment();

    @ContributesAndroidInjector
    abstract SettingsFragment contributeSettingsFragmentFragment();

}

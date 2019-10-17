package mx.dev.franco.automusictagfixer.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import mx.dev.franco.automusictagfixer.ui.main.ListFragment;
import mx.dev.franco.automusictagfixer.ui.search.ResultSearchListFragment;
import mx.dev.franco.automusictagfixer.ui.trackdetail.TrackDetailFragment;

@Module
public abstract class FragmentModule {

    @ContributesAndroidInjector
    abstract ListFragment contributeListFragment();

    @ContributesAndroidInjector
    abstract TrackDetailFragment contributeTrackDetailFragment();

    @ContributesAndroidInjector
    abstract ResultSearchListFragment contributeResultSearchListFragment();
}

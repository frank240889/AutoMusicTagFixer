package mx.dev.franco.automusictagfixer.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import mx.dev.franco.automusictagfixer.UI.main.ListFragment;
import mx.dev.franco.automusictagfixer.UI.track_detail.TrackDetailFragment;

@Module
public abstract class FragmentModule {

    @ContributesAndroidInjector
    abstract ListFragment contributeListFragment();

    @ContributesAndroidInjector
    abstract TrackDetailFragment contributeTrackDetailFragment();
}

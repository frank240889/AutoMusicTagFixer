package mx.dev.franco.automusictagfixer.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import mx.dev.franco.automusictagfixer.ui.trackdetail.CoverIdentificationResultsFragment;
import mx.dev.franco.automusictagfixer.ui.trackdetail.SemiAutoCorrectionDialogFragment;

@Module
public abstract class ChildFragmentModule {

    @ContributesAndroidInjector(modules = FragmentModule.class)
    abstract SemiAutoCorrectionDialogFragment contributeSemiAutoCorrectionDialogFragment();

    @ContributesAndroidInjector(modules = FragmentModule.class)
    abstract CoverIdentificationResultsFragment contributeCoverIdentificationResultsFragmentBase();
}

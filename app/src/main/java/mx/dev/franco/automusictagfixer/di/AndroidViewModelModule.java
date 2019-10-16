package mx.dev.franco.automusictagfixer.di;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;
import mx.dev.franco.automusictagfixer.modelsUI.main.ListViewModel;
import mx.dev.franco.automusictagfixer.ui.AndroidViewModelFactory;
import mx.dev.franco.automusictagfixer.ui.trackdetail.TrackDetailViewModel;


/**
 * The module that provides the ViewModels.
 */
@Module
public abstract class AndroidViewModelModule {
    @Binds
    abstract ViewModelProvider.Factory bindAndroidViewModelFactory(AndroidViewModelFactory androidViewModelFactory);

    @Binds
    @IntoMap
    @ViewModelKey(ListViewModel.class)
    abstract ViewModel provideListViewModel(ListViewModel mainViewModel);

    @Binds
    @IntoMap
    @ViewModelKey(TrackDetailViewModel.class)
    abstract ViewModel provideTrackDetailViewModel(TrackDetailViewModel eventDetailViewModel);

}

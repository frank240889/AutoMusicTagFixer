package mx.dev.franco.automusictagfixer.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import mx.dev.franco.automusictagfixer.ui.AndroidViewModelFactory
import mx.dev.franco.automusictagfixer.ui.main.ListViewModel
import mx.dev.franco.automusictagfixer.ui.search.SearchListViewModel
import mx.dev.franco.automusictagfixer.ui.trackdetail.TrackDetailViewModel

/**
 * The module that provides the ViewModels.
 */
@Module
abstract class AndroidViewModelModule {

    @Binds
    abstract fun bindAndroidViewModelFactory(androidViewModelFactory: AndroidViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @ViewModelKey(ListViewModel::class)
    abstract fun provideListViewModel(mainViewModel: ListViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TrackDetailViewModel::class)
    abstract fun provideTrackDetailViewModel(trackDetailViewModel: TrackDetailViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SearchListViewModel::class)
    abstract fun provideSearchListViewModel(searchListViewModel: SearchListViewModel): ViewModel
}
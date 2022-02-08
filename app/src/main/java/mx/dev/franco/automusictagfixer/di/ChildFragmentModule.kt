package mx.dev.franco.automusictagfixer.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import mx.dev.franco.automusictagfixer.ui.trackdetail.ChangeFilenameDialogFragment

@Module
abstract class ChildFragmentModule {

    @ContributesAndroidInjector(modules = [FragmentModule::class])
    abstract fun contributeChangeFilenameDialogFragment(): ChangeFilenameDialogFragment
}
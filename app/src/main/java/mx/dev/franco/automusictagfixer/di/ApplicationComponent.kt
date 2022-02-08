package mx.dev.franco.automusictagfixer.di

import android.app.Application
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import dagger.android.AndroidInjector
import mx.dev.franco.automusictagfixer.AutoMusicTagFixer
import javax.inject.Singleton

@Component(
    modules = [
        AndroidInjectionModule::class,
        ApplicationModule::class,  //MediaPlayerModule.class,
        ActivityModule::class,
        FragmentModule::class,
        ChildFragmentModule::class,
        DatabaseModule::class,
        SharedPreferencesModule::class,
        MediaStoreManagerModule::class,
        StorageModule::class,
        TaggerModule::class,
        ResourceManagerModule::class,
        AndroidViewModelModule::class
    ]
)
@Singleton
interface ApplicationComponent : AndroidInjector<AutoMusicTagFixer> {

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: Application): Builder
        fun build(): ApplicationComponent?
    }

    override fun inject(autoMusicTagFixer: AutoMusicTagFixer)
}
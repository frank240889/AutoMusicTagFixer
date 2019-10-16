package mx.dev.franco.automusictagfixer.di;


import android.app.Application;
import dagger.BindsInstance;
import dagger.Component;
import dagger.android.AndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;
import javax.inject.Singleton;
import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;

@Component(modules = {
        AndroidSupportInjectionModule.class,
        ApplicationModule.class,
        ServiceUtilsModule.class,
        ActivityModule.class,
        FragmentModule.class,
        DatabaseModule.class,
        CacheModule.class,
        DatabaseModule.class,
        IdentifierModule.class,
        MediaPlayerModule.class,
        SharedPreferencesModule.class,
        SharedPreferencesModule.class,
        StorageModule.class,
        TaggerModule.class,
        FileManagerModule.class,
        AndroidViewModelModule.class
        })

@Singleton
public interface ApplicationComponent extends AndroidInjector<AutoMusicTagFixer> {

    @Component.Builder
    interface Builder {

        @BindsInstance
        Builder application(Application application);

        ApplicationComponent build();
    }

    void inject(AutoMusicTagFixer autoMusicTagFixer);
}

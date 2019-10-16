package mx.dev.franco.automusictagfixer.di;
import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import mx.dev.franco.automusictagfixer.ui.MainActivity;

/**
 * The module that injects the dependencies into fragments.
 */
@Module
public abstract class ActivityModule {
    @ContributesAndroidInjector(modules = FragmentModule.class)
    public abstract MainActivity contributeMainActivity();
}

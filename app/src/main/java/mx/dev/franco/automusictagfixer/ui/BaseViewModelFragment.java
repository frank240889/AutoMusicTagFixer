package mx.dev.franco.automusictagfixer.ui;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.AndroidSupportInjection;
import dagger.android.support.HasSupportFragmentInjector;

/**
 * Base fragment that abstract the common functionality for fragments
 * that inherits from it.
 *
 * @author Franco Castillo
 */
public abstract class BaseViewModelFragment<ViewModel> extends BaseFragment implements HasSupportFragmentInjector {
    @Inject
    protected DispatchingAndroidInjector<Fragment> fragmentDispatchingAndroidInjector;
    @Inject
    protected AndroidViewModelFactory androidViewModelFactory;
    protected ViewModel mViewModel;

    /**
     * Called when a fragment is first attached to its context.
     * {@link #onCreate(Bundle)} will be called after this.
     */
    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = getViewModel();
    }

    protected abstract ViewModel getViewModel();

    protected void loading(boolean isLoading){ }

    @Override
    public AndroidInjector<Fragment> supportFragmentInjector() {
        return fragmentDispatchingAndroidInjector;
    }
}

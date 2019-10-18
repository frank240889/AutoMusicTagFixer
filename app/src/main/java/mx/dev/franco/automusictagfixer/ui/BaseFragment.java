package mx.dev.franco.automusictagfixer.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.AndroidSupportInjection;
import dagger.android.support.HasSupportFragmentInjector;
import mx.dev.franco.automusictagfixer.interfaces.OnBackPressedListener;
import mx.dev.franco.automusictagfixer.utilities.ActionableMessage;
import mx.dev.franco.automusictagfixer.utilities.Message;

/**
 * Base fragment that abstract the common functionality for fragments
 * that inherits from it.
 *
 * @author Franco Castillo
 */
public abstract class BaseFragment<ViewModel> extends Fragment implements
        OnBackPressedListener, HasSupportFragmentInjector {
    @Inject
    protected DispatchingAndroidInjector<Fragment> fragmentDispatchingAndroidInjector;
    @Inject
    protected AndroidViewModelFactory androidViewModelFactory;

    public static final String BASE_FRAGMENT_TAG = BaseFragment.class.getName();
    public static final int CROSS_FADE_DURATION = 200;
    //Intent type for pick an image
    public static final int INTENT_OPEN_GALLERY = 1;
    public static final int INTENT_GET_AND_UPDATE_FROM_GALLERY = 2;
    public static String TAG;
    protected OnConfirmBackPressedListener mOnConfirmBackPressedListener;
    protected ViewModel mViewModel;

    /**
     * Called when a fragment is first attached to its context.
     * {@link #onCreate(Bundle)} will be called after this.
     */
    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
        if(context instanceof OnConfirmBackPressedListener)
            mOnConfirmBackPressedListener = (OnConfirmBackPressedListener) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = getViewModel();
    }

    protected abstract ViewModel getViewModel();

    @Override
    public void onDetach() {
        super.onDetach();
        mOnConfirmBackPressedListener = null;
    }

    /**
     * Call super.onBackPressed of host Activity
     */
    protected void callSuperOnBackPressed(){
        if(mOnConfirmBackPressedListener != null)
            mOnConfirmBackPressedListener.callSuperOnBackPressed();
    }

    /**
     * Interface to implement to communicate back press event from
     * Fragment to its host Activity.
     */
    public interface OnConfirmBackPressedListener {
        void callSuperOnBackPressed();
    }

    protected void loading(boolean isLoading){ }

    @Override
    public AndroidInjector<Fragment> supportFragmentInjector() {
        return fragmentDispatchingAndroidInjector;
    }

    protected void onMessage(Message message){}

    protected void onActionableMessage(ActionableMessage actionableMessage) { }

}

package mx.dev.franco.automusictagfixer.UI;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import mx.dev.franco.automusictagfixer.identifier.GnService;
import mx.dev.franco.automusictagfixer.interfaces.OnBackPressedListener;
import mx.dev.franco.automusictagfixer.interfaces.OnTestingNetwork;

/**
 * Base fragment that abstract the common functionality for fragments
 * that inherits from it.
 *
 * @author Franco Castillo
 */
public abstract class BaseFragment extends Fragment implements
        OnBackPressedListener, GnService.OnApiListener, OnTestingNetwork.OnTestingResult<Void> {
    public static final String BASE_FRAGMENT_TAG = BaseFragment.class.getName();
    public static String TAG;
    protected OnConfirmBackPressedListener mOnConfirmBackPressedListener;

    /**
     * Called when a fragment is first attached to its context.
     * {@link #onCreate(Bundle)} will be called after this.
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if(context instanceof OnConfirmBackPressedListener)
            mOnConfirmBackPressedListener = (OnConfirmBackPressedListener) context;
    }

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

}

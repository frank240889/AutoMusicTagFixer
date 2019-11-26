package mx.dev.franco.automusictagfixer.ui;

import androidx.fragment.app.Fragment;

/**
 * Base fragment that abstract the common functionality for fragments
 * that inherits from it.
 *
 * @author Franco Castillo
 */
public abstract class BaseFragment extends Fragment {

    public static final String BASE_FRAGMENT_TAG = BaseFragment.class.getName();
    public static final int CROSS_FADE_DURATION = 200;
    //Intent type for pick an image
    public static final int INTENT_OPEN_GALLERY = 1;
    public static final int INTENT_GET_AND_UPDATE_FROM_GALLERY = 2;
    public static String TAG;

    public String getTagName() {
        return getClass().getName();
    }
}

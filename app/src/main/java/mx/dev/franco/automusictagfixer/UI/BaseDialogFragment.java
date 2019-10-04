package mx.dev.franco.automusictagfixer.UI;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gentera.sdk.R;

/**
 * A base dialog that has the basic functionality for all common dialogs.
 */
public abstract class BaseDialogFragment extends DialogFragment {
    public static final String LAYOUT_ID = "layout_id";

    @LayoutRes int mLayout;

    public BaseDialogFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if(bundle != null) {
            mLayout = bundle.getInt(LAYOUT_ID);
        }

        if(mLayout == 0) {
            throw new IllegalArgumentException("Required layout id to initialize this dialog fragment.");
        }
    }

    @Override
    public int getTheme() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ?
                android.R.style.Theme_Material_Light :
                android.R.style.Theme_Light_Panel;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(mLayout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mToolbar = view.findViewById(R.id.toolbar);
        setToolbarInfo();
    }


}

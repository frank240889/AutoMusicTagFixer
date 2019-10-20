package mx.dev.franco.automusictagfixer.ui;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

/**
 * A base dialog that has the basic functionality for all common dialogs.
 */
public abstract class BaseDialogFragment extends DialogFragment {
    public static final String LAYOUT_ID = "layout_id";

    protected @LayoutRes int mLayout;

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
}

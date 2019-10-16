package mx.dev.franco.automusictagfixer.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.BottomSheetDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import mx.dev.franco.automusictagfixer.R;


/**
 * @author Franco Castillo
 * A simple bottom sheet dialog fragment with rounded corners used as base for all bottom sheet fragments.
 */
public abstract class BaseRoundedBottomSheetDialogFragment extends BottomSheetDialogFragment {
    protected static final String LAYOUT_ID = "layout";
    private @LayoutRes int mLayout = -1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments =  getArguments();
        if(arguments != null) {
            mLayout = arguments.getInt(LAYOUT_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(mLayout,container, false);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new BottomSheetDialog(getContext(), getTheme());
    }

    @Override
    public int getTheme() {
        return R.style.BottomSheetDialogTheme;
    }
}

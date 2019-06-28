package mx.dev.franco.automusictagfixer.UI;

import android.app.Dialog;
import android.os.Bundle;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.BottomSheetDialogFragment;

import mx.dev.franco.automusictagfixer.R;


/**
 * @author Franco Castillo
 * A simple bottom sheet dialog fragment with rounded corners.
 */
public class RoundedBottomSheetDialogFragment extends BottomSheetDialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new BottomSheetDialog(getContext(), getTheme());
    }

    @Override
    public int getTheme() {
        return R.style.BottomSheetDialogTheme;
    }
}

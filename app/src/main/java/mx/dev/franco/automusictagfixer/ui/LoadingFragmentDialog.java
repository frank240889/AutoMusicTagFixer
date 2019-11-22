package mx.dev.franco.automusictagfixer.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;

import mx.dev.franco.automusictagfixer.R;

public class LoadingFragmentDialog extends BaseRoundedBottomSheetDialogFragment {
    private static final String CANCELABLE = "cancelable";
    public interface OnCancelTaskFragmentDialogListener {
        void onCancelTask();
    }

    private OnCancelTaskFragmentDialogListener mOnCancelTaskFragmentDialogListener;
    private boolean mCancelableAction;

    public static LoadingFragmentDialog newInstance(boolean cancelable) {
        Bundle bundle = new Bundle();
        bundle.putInt(LAYOUT_ID, R.layout.layout_progress);
        LoadingFragmentDialog loadingFragmentDialog = new LoadingFragmentDialog();
        loadingFragmentDialog.setArguments(bundle);
        return loadingFragmentDialog;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if(context instanceof OnCancelTaskFragmentDialogListener)
            mOnCancelTaskFragmentDialogListener = (OnCancelTaskFragmentDialogListener) context;
        else if (getParentFragment() instanceof OnCancelTaskFragmentDialogListener)
            mOnCancelTaskFragmentDialogListener = (OnCancelTaskFragmentDialogListener) getParentFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        if(getArguments() != null) {
            mCancelableAction = getArguments().getBoolean(CANCELABLE);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MaterialButton cancelButton = view.findViewById(R.id.cancel_button);

        if(mCancelableAction) {
            cancelButton.setVisibility(View.VISIBLE);
            cancelButton.setEnabled(true);
            cancelButton.setOnClickListener(v -> {
                if(mOnCancelTaskFragmentDialogListener != null)
                    mOnCancelTaskFragmentDialogListener.onCancelTask();
            });
        }
        else {
            cancelButton.setVisibility(View.VISIBLE);
            cancelButton.setEnabled(false);
            cancelButton.setOnClickListener(null);
        }
    }

    public void setOnCancelTaskListener(OnCancelTaskFragmentDialogListener listener) {
        mOnCancelTaskFragmentDialogListener = listener;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mOnCancelTaskFragmentDialogListener = null;
    }
}

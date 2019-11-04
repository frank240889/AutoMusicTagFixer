package mx.dev.franco.automusictagfixer.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import mx.dev.franco.automusictagfixer.R;

public class InformativeFragmentDialog extends BaseRoundedBottomSheetDialogFragment {
    private static final String TITLE = "title";
    private static final String CONTENT = "content";
    private static final String POSITIVE_BUTTON_TEXT = "positive_button_text";
    private static final String NEGATIVE_BUTTON_TEXT = "negative_button_text";

    @StringRes
    private int mTitle;
    @StringRes
    private int mContent;
    @StringRes
    private int mPositiveText;
    @StringRes
    private int mNegativeText;


    public interface OnClickBasicFragmentDialogListener {
        void onPositiveButton();
        void onNegativeButton();
    }

    private OnClickBasicFragmentDialogListener mOnClickBasicFragmentDialogListener;

    public static InformativeFragmentDialog newInstance(@StringRes int title,
                                                        @StringRes int content,
                                                        @StringRes int positiveButtonText,
                                                        @StringRes int negativeButtonText){
        Bundle bundle = new Bundle();
        bundle.putInt(LAYOUT_ID, R.layout.layout_basic_dialog);
        bundle.putInt(TITLE, title);
        bundle.putInt(CONTENT, content);
        bundle.putInt(POSITIVE_BUTTON_TEXT, positiveButtonText);
        bundle.putInt(NEGATIVE_BUTTON_TEXT, negativeButtonText);

        InformativeFragmentDialog informativeFragmentDialog = new InformativeFragmentDialog();
        informativeFragmentDialog.setArguments(bundle);
        return informativeFragmentDialog;

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if(context instanceof OnClickBasicFragmentDialogListener)
            mOnClickBasicFragmentDialogListener = (OnClickBasicFragmentDialogListener) context;
        else if (getParentFragment() instanceof OnClickBasicFragmentDialogListener)
            mOnClickBasicFragmentDialogListener = (OnClickBasicFragmentDialogListener) getParentFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null) {
            mTitle = getArguments().getInt(TITLE);
            mContent = getArguments().getInt(CONTENT);
            mPositiveText = getArguments().getInt(POSITIVE_BUTTON_TEXT);
            mNegativeText = getArguments().getInt(NEGATIVE_BUTTON_TEXT);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Button positive = view.findViewById(R.id.accept_button);
        Button negative = view.findViewById(R.id.cancel_button);
        TextView title = view.findViewById(R.id.title_basic_dialog);
        TextView content = view.findViewById(R.id.content_basic_dialog);

        title.setText(mTitle);
        content.setText(mContent);
        positive.setText(mPositiveText);
        negative.setText(mNegativeText);

        positive.setOnClickListener(view1 -> {
            dismiss();
                if(mOnClickBasicFragmentDialogListener != null)
                    mOnClickBasicFragmentDialogListener.onPositiveButton();
            getChildFragmentManager().popBackStack();
        });

        negative.setOnClickListener(view12 -> {
            dismiss();
                if(mOnClickBasicFragmentDialogListener != null)
                    mOnClickBasicFragmentDialogListener.onNegativeButton();
            getChildFragmentManager().popBackStack();
        });
    }

    public void setOnClickBasicFragmentDialogListener(OnClickBasicFragmentDialogListener listener) {
        mOnClickBasicFragmentDialogListener = listener;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mOnClickBasicFragmentDialogListener = null;
    }
}

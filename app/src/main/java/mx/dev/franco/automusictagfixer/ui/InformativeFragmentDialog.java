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

import static android.view.View.GONE;

public class InformativeFragmentDialog extends BaseRoundedBottomSheetDialogFragment {
    private static final String TITLE = "title";
    private static final String CONTENT = "content";
    private static final String POSITIVE_BUTTON_TEXT = "positive_button_text";
    private static final String NEGATIVE_BUTTON_TEXT = "negative_button_text";

    private String mTitle;
    private String mContent;
    private String mPositiveText;
    private String mNegativeText;


    public interface OnClickBasicFragmentDialogListener {
        void onPositiveButton();
        void onNegativeButton();
    }

    private OnClickBasicFragmentDialogListener mOnClickBasicFragmentDialogListener;

    public static InformativeFragmentDialog newInstance(@StringRes int title,
                                                        @StringRes int content,
                                                        @StringRes int positiveButtonText,
                                                        @StringRes int negativeButtonText,Context context) {
        return newInstance(context.getString(title),
                context.getString(content),
                context.getString(positiveButtonText),
                context.getString(negativeButtonText));

    }

    public static InformativeFragmentDialog newInstance(String title,
                                                        String content,
                                                        String positiveButtonText,
                                                        String negativeButtonText){
        Bundle bundle = new Bundle();
        bundle.putInt(LAYOUT_ID, R.layout.layout_basic_dialog);
        bundle.putString(TITLE, title);
        bundle.putString(CONTENT, content);
        bundle.putString(POSITIVE_BUTTON_TEXT, positiveButtonText);
        bundle.putString(NEGATIVE_BUTTON_TEXT, negativeButtonText);

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
            mTitle = getArguments().getString(TITLE);
            mContent = getArguments().getString(CONTENT);
            mPositiveText = getArguments().getString(POSITIVE_BUTTON_TEXT);
            mNegativeText = getArguments().getString(NEGATIVE_BUTTON_TEXT);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Button positive = view.findViewById(R.id.accept_button);
        Button negative = view.findViewById(R.id.cancel_button);
        TextView title = view.findViewById(R.id.title_basic_dialog);
        TextView content = view.findViewById(R.id.content_basic_dialog);

        if(mTitle == null || mTitle.isEmpty()) {
            title.setVisibility(GONE);
        }
        else {
            title.setText(mTitle);
        }
        if(mContent == null || mContent.isEmpty()) {
            content.setVisibility(GONE);
        }
        else {
            content.setVisibility(View.VISIBLE);
            content.setText(mContent);
        }

        positive.setText(mPositiveText);

        positive.setOnClickListener(view1 -> {
            dismiss();
                if(mOnClickBasicFragmentDialogListener != null)
                    mOnClickBasicFragmentDialogListener.onPositiveButton();
            getChildFragmentManager().popBackStack();
        });

        if(mNegativeText == null || mNegativeText.isEmpty()) {
            negative.setVisibility(GONE);
        }
        else {
            negative.setVisibility(View.VISIBLE);
            negative.setText(mNegativeText);
            negative.setOnClickListener(view12 -> {
                dismiss();
                if(mOnClickBasicFragmentDialogListener != null)
                    mOnClickBasicFragmentDialogListener.onNegativeButton();
                getChildFragmentManager().popBackStack();
            });
        }
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

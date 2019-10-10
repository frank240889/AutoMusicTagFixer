package mx.dev.franco.automusictagfixer.UI;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import mx.dev.franco.automusictagfixer.R;

public class BasicFragmentDialog extends BaseDialogFragment {
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

    public static BasicFragmentDialog newInstance(@StringRes int title,
                                                 @StringRes int content,
                                                 @StringRes int positiveButtonText,
                                                 @StringRes int negativeButtonText){
        Bundle bundle = new Bundle();
        bundle.putInt(LAYOUT_ID, R.layout.layout_basic_dialog);
        bundle.putInt(TITLE, title);
        bundle.putInt(CONTENT, content);
        bundle.putInt(POSITIVE_BUTTON_TEXT, positiveButtonText);
        bundle.putInt(NEGATIVE_BUTTON_TEXT, negativeButtonText);

        BasicFragmentDialog basicFragmentDialog = new BasicFragmentDialog();
        basicFragmentDialog.setArguments(bundle);
        return basicFragmentDialog;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(mLayout, container, false);
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
        });

        negative.setOnClickListener(view12 -> {
            dismiss();
                if(mOnClickBasicFragmentDialogListener != null)
                    mOnClickBasicFragmentDialogListener.onNegativeButton();
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

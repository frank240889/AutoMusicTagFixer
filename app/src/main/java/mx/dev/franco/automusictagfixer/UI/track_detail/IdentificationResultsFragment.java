package mx.dev.franco.automusictagfixer.UI.track_detail;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import mx.dev.franco.automusictagfixer.UI.RoundedBottomSheetDialogFragment;

import static mx.dev.franco.automusictagfixer.UI.track_detail.ResultsFragment.LAYOUT_ID;

public class IdentificationResultsFragment extends RoundedBottomSheetDialogFragment {
    public interface OnBottomSheetFragmentInteraction {
        void applyMissingTagsButton(CorrectionParams correctionParams);
        void applyOverwriteTagsButton(CorrectionParams correctionParams);
    }
    private OnBottomSheetFragmentInteraction mCallback;
    private Bundle mArguments;
    private OnClickTextView mOnClickTextView;
    public IdentificationResultsFragment(){}

    public static IdentificationResultsFragment newInstance(@LayoutRes int layoutId) {
        Bundle arguments = new Bundle();
        arguments.putInt(LAYOUT_ID, layoutId);
        IdentificationResultsFragment identificationResultsFragment = new IdentificationResultsFragment();
        identificationResultsFragment.setArguments(arguments);
        return identificationResultsFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if(getParentFragment() instanceof OnBottomSheetFragmentInteraction )
            mCallback = (OnBottomSheetFragmentInteraction) getParentFragment();
        else if(context instanceof OnBottomSheetFragmentInteraction)
            mCallback = (OnBottomSheetFragmentInteraction) context;
        else
            throw new RuntimeException(context.toString() + "must implement " +
                    OnBottomSheetFragmentInteraction.class.getCanonicalName());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

    }


    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = null;
    }
}

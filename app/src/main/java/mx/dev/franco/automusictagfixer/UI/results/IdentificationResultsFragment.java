package mx.dev.franco.automusictagfixer.UI.results;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.UI.RoundedBottomSheetDialogFragment;
import mx.dev.franco.automusictagfixer.UI.track_detail.CorrectionParams;

import static mx.dev.franco.automusictagfixer.UI.ResultsFragment.LAYOUT_ID;

public class IdentificationResultsFragment extends RoundedBottomSheetDialogFragment {
    public interface OnResultSelected {
        void applyMissingTagsButton(CorrectionParams correctionParams);
        void applyOverwriteTagsButton(CorrectionParams correctionParams);
    }

    private OnResultSelected mOnResultSelected;

    public IdentificationResultsFragment(){}

    public static IdentificationResultsFragment newInstance(String id) {
        Bundle arguments = new Bundle();
        arguments.putInt(LAYOUT_ID, R.layout.layout_results_track_id);
        arguments.putString(TRACK_ID, id);
        IdentificationResultsFragment identificationResultsFragment = new IdentificationResultsFragment();
        identificationResultsFragment.setArguments(arguments);
        return identificationResultsFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if(getParentFragment() instanceof OnResultSelected)
            mOnResultSelected = (OnResultSelected) getParentFragment();
        else if(context instanceof OnResultSelected)
            mOnResultSelected = (OnResultSelected) context;
        else
            throw new RuntimeException(context.toString() + " must implement " +
                    OnResultSelected.class.getCanonicalName());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

    }


    @Override
    public void onDetach() {
        super.onDetach();
        mOnResultSelected = null;
    }
}

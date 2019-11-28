package mx.dev.franco.automusictagfixer.ui.trackdetail;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;
import mx.dev.franco.automusictagfixer.ui.ResultsFragmentBase;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.Constants;


public class CoverIdentificationResultsFragmentBase extends ResultsFragmentBase<ResultsViewModel> {
    public interface OnCoverCorrectionListener {
        void saveAsImageButton(CoverCorrectionParams coverCorrectionParams);
        void saveAsCover(CoverCorrectionParams coverCorrectionParams);
    }
    private OnCoverCorrectionListener mOnCoverCorrectionListener;
    private int mCenteredItem = 0;
    private CoverCorrectionParams mCoverCorrectionParams;
    private CoverIdentificationResultsAdapter adapter;
    public CoverIdentificationResultsFragmentBase(){}

    public static CoverIdentificationResultsFragmentBase newInstance(String id) {
        Bundle arguments = new Bundle();
        arguments.putInt(LAYOUT_ID, R.layout.layout_results_cover_id);
        arguments.putString(TRACK_ID, id);
        CoverIdentificationResultsFragmentBase identificationResultsFragment = new CoverIdentificationResultsFragmentBase();
        identificationResultsFragment.setArguments(arguments);
        return identificationResultsFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel.observeProgress().observe(this, this::onLoading);
        mViewModel.observeCoverResults().observe(this, adapter);
        mViewModel.observeZeroResults().observe(this, this::onZeroResults);
        mViewModel.fetchCoverResults(mTrackId);
    }

    private void onZeroResults(Void aVoid) {
        AndroidUtils.showToast(R.string.no_cover_art_found, getActivity());
        dismiss();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if(getParentFragment() instanceof OnCoverCorrectionListener)
            mOnCoverCorrectionListener = (OnCoverCorrectionListener) getParentFragment();
        else if(context instanceof OnCoverCorrectionListener)
            mOnCoverCorrectionListener = (OnCoverCorrectionListener) context;
        else
            throw new RuntimeException(context.toString() + " must implement " +
                    OnCoverCorrectionListener.class.getCanonicalName());

        mCoverCorrectionParams = new CoverCorrectionParams();
        mCoverCorrectionParams.setCorrectionMode(Constants.CACHED);
        adapter = new CoverIdentificationResultsAdapter();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Button saveAsCoverButton = view.findViewById(R.id.save_as_cover_button);
        Button saveAsImageFileButton = view.findViewById(R.id.save_as_image_file_button);

        RecyclerView listResults = view.findViewById(R.id.cover_results_list);
        SnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(listResults);
        LinearLayoutManager layoutManager = new LinearLayoutManager(listResults.getContext());
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        listResults.setLayoutManager(layoutManager);
        listResults.setItemViewCacheSize(5);
        listResults.setAdapter(adapter);

        listResults.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if(newState == RecyclerView.SCROLL_STATE_IDLE) {
                    View snapView = snapHelper.findSnapView(layoutManager);
                    if(snapView != null) {
                        mCenteredItem = layoutManager.getPosition(snapView);
                        listResults.smoothScrollToPosition(mCenteredItem);
                    }
                }
            }
        });



        saveAsCoverButton.setOnClickListener(v ->{
                dismiss();
                mCoverCorrectionParams.setCoverId(mViewModel.getCoverResult(mCenteredItem).getId());
                mCoverCorrectionParams.setCorrectionMode(AudioTagger.MODE_ADD_COVER);
                mOnCoverCorrectionListener.saveAsCover(mCoverCorrectionParams);});

        saveAsImageFileButton.setOnClickListener(v -> {
                dismiss();
                mCoverCorrectionParams.setCoverId(mViewModel.getCoverResult(mCenteredItem).getId());
                mOnCoverCorrectionListener.saveAsImageButton(mCoverCorrectionParams);});
    }


    @Override
    public void onDetach() {
        super.onDetach();
        mOnCoverCorrectionListener = null;
    }

    @Override
    protected ResultsViewModel getViewModel() {
        return ViewModelProviders.of(this, androidViewModelFactory).get(ResultsViewModel.class);
    }

    @NonNull @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        dialog.setOnShowListener(dialog1 -> {
            BottomSheetDialog d = (BottomSheetDialog) dialog1;

            FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
        });
        return dialog;
    }
}

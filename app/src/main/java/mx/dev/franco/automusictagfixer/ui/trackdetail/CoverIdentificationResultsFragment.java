package mx.dev.franco.automusictagfixer.ui.trackdetail;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;
import mx.dev.franco.automusictagfixer.fixer.CorrectionParams;
import mx.dev.franco.automusictagfixer.ui.ResultsFragmentBase;
import mx.dev.franco.automusictagfixer.utilities.Constants;


public class CoverIdentificationResultsFragment extends ResultsFragmentBase<ResultsViewModel> {
    private int mNumberResults = 0;

    public interface OnCoverCorrectionListener {
        void saveAsImageButton(String id);
        void saveAsCover(CorrectionParams coverCorrectionParams);
    }
    private OnCoverCorrectionListener mOnCoverCorrectionListener;
    private int mCenteredItem = 0;
    private CorrectionParams mCoverCorrectionParams;
    private CoverIdentificationResultsAdapter adapter;
    public CoverIdentificationResultsFragment(){}

    public static CoverIdentificationResultsFragment newInstance(String id) {
        Bundle arguments = new Bundle();
        arguments.putInt(LAYOUT_ID, R.layout.layout_results_cover_id);
        arguments.putString(TRACK_ID, id);
        CoverIdentificationResultsFragment identificationResultsFragment = new CoverIdentificationResultsFragment();
        identificationResultsFragment.setArguments(arguments);
        return identificationResultsFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel.observeProgress().observe(this, this::onLoading);
        mViewModel.observeCoverResults().observe(this, adapter);
        mViewModel.fetchCoverResults(mTrackId);
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

        mCoverCorrectionParams = new CorrectionParams();
        mCoverCorrectionParams.setTagsSource(Constants.CACHED);
        mCoverCorrectionParams.setCorrectionMode(AudioTagger.MODE_ADD_COVER);
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
        listResults.setAdapter(adapter);
        ImageButton leftChevron = view.findViewById(R.id.iv_left_chevron);
        ImageButton rightChevron = view.findViewById(R.id.iv_right_chevron);
        TextView title = view.findViewById(R.id.title_results);

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
                mCoverCorrectionParams.setTagsSource(Constants.CACHED);
                mCoverCorrectionParams.setCorrectionMode(AudioTagger.MODE_ADD_COVER);
                mOnCoverCorrectionListener.saveAsCover(mCoverCorrectionParams);});

        saveAsImageFileButton.setOnClickListener(v -> {
                dismiss();
                mCoverCorrectionParams.setCoverId(mViewModel.getCoverResult(mCenteredItem).getId());
                mOnCoverCorrectionListener.saveAsImageButton(mViewModel.getCoverResult(mCenteredItem).getId());
        });

        leftChevron.setOnClickListener(v -> {
            if (mCenteredItem < layoutManager.getItemCount()) {
                listResults.smoothScrollToPosition(mCenteredItem + 1);
            }
        });

        rightChevron.setOnClickListener(v -> {
            if (mCenteredItem > 0) {
                listResults.smoothScrollToPosition(mCenteredItem - 1);
            }
        });

        mViewModel.observeCoverResults().observe(getViewLifecycleOwner(), identificationResults -> {
            mNumberResults = identificationResults.size();
            title.setText(String.format(getString(R.string.results_found), mNumberResults));
        });
    }


    @Override
    public void onDetach() {
        super.onDetach();
        mOnCoverCorrectionListener = null;
    }

    @Override
    protected ResultsViewModel getViewModel() {
        return new ViewModelProvider(this, androidViewModelFactory).get(ResultsViewModel.class);
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

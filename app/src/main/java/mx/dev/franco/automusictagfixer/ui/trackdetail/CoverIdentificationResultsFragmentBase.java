package mx.dev.franco.automusictagfixer.ui.trackdetail;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.ui.ResultsFragmentBase;
import mx.dev.franco.automusictagfixer.utilities.Constants;


public class CoverIdentificationResultsFragmentBase extends ResultsFragmentBase<ResultsViewModel> {
    public interface OnCoverCorrectionListener {
        void saveAsImageButton(CoverCorrectionParams coverCorrectionParams);
        void saveAsCover(CoverCorrectionParams coverCorrectionParams);
    }
    private OnCoverCorrectionListener mOnCoverCorrectionListener;
    private int mCenteredPosition;
    private CoverCorrectionParams mCoverCorrectionParams;
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
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Button saveAsCoverButton = view.findViewById(R.id.save_as_cover_button);
        Button saveAsImageFileButton = view.findViewById(R.id.save_as_image_file_button);

        //Todo: change the adapter with the appropriate adapter.
        RecyclerView listResults = view.findViewById(R.id.results_list);

        listResults.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if(newState == RecyclerView.SCROLL_STATE_IDLE) {
                    int firstVisible = ((LinearLayoutManager)recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
                    int lastVisible = ((LinearLayoutManager)recyclerView.getLayoutManager()).findLastVisibleItemPosition();
                    mCenteredPosition = lastVisible - firstVisible;
                    recyclerView.scrollToPosition(mCenteredPosition);
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {}
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(listResults.getContext());
        listResults.setLayoutManager(layoutManager);
        listResults.setItemViewCacheSize(5);
        IdentificationResultsAdapter adapter = new IdentificationResultsAdapter();
        listResults.setAdapter(adapter);

        saveAsCoverButton.setOnClickListener(v ->
                mOnCoverCorrectionListener.saveAsCover(mCoverCorrectionParams));

        saveAsImageFileButton.setOnClickListener(v ->
                mOnCoverCorrectionListener.saveAsImageButton(mCoverCorrectionParams));

    }


    @Override
    public void onDetach() {
        super.onDetach();
        mOnCoverCorrectionListener = null;
    }
}

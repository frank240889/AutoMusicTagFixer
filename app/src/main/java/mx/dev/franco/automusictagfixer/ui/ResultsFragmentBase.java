package mx.dev.franco.automusictagfixer.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;

/**
 * Base bottom fragment sheet dialog with view model and track feature added.
 * @param <ViewModel> The ViewModel for this fragment.
 */
public abstract class ResultsFragmentBase<ViewModel> extends BaseRoundedBottomSheetDialogFragment {
    protected static final String TRACK_ID = "track_id";
    protected String mTrackId;
    protected ViewModel mViewModel;
    @Inject
    protected AndroidViewModelFactory androidViewModelFactory;


    public ResultsFragmentBase(){}

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        if(arguments != null)
            mTrackId = arguments.getString(TRACK_ID);

        mViewModel = getViewModel();
    }

    protected abstract ViewModel getViewModel();

    protected void onLoading(boolean loading){}
}

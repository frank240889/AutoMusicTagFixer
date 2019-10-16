package mx.dev.franco.automusictagfixer.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 * Base bottom fragment sheet dialog with view model and track feature added.
 * @param <ViewModel> The ViewModel for this fragment.
 */
public abstract class ResultsFragmentBase<ViewModel> extends BaseRoundedBottomSheetDialogFragment {
    protected static final String TRACK_ID = "track_id";
    protected String mTrackId;

    public ResultsFragmentBase(){}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        if(arguments != null)
            mTrackId = arguments.getString(TRACK_ID);
    }

    protected ViewModel getViewModel(){
        return null;
    }

    protected void onLoading(boolean loading){}
}

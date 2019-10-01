package mx.dev.franco.automusictagfixer.UI;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public abstract class ResultsFragment extends RoundedBottomSheetDialogFragment {
    public static final String LAYOUT_ID = "layout_id";
    @LayoutRes
    protected int mLayoutId = -1;

    public ResultsFragment(){}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        if(arguments != null)
            mLayoutId = arguments.getInt(LAYOUT_ID);

        if(mLayoutId == -1)
            throw new IllegalArgumentException("Layout id required to instantiate this fragment.");

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(mLayoutId, container, false);
    }
}

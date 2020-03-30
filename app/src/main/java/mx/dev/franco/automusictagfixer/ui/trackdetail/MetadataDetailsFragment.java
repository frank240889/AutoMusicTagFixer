package mx.dev.franco.automusictagfixer.ui.trackdetail;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import dagger.android.AndroidInjector;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.databinding.FragmentMetadataDetailsBinding;
import mx.dev.franco.automusictagfixer.ui.BaseViewModelRoundedBottomSheetDialogFragment;

public class MetadataDetailsFragment extends BaseViewModelRoundedBottomSheetDialogFragment<TrackDetailViewModel> {

    private FragmentMetadataDetailsBinding mBinding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_metadata_details, container, false);
        mBinding.setLifecycleOwner(this);
        mBinding.setViewmodel(mViewModel);
        return mBinding.getRoot();
    }

    @Override
    public TrackDetailViewModel getViewModel() {
        return new ViewModelProvider(requireActivity(), androidViewModelFactory).
                get(TrackDetailViewModel.class);
    }

    @Override
    public AndroidInjector<Fragment> supportFragmentInjector() {
        return fragmentDispatchingAndroidInjector;
    }
}

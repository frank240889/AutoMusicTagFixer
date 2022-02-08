package mx.dev.franco.automusictagfixer.ui.trackdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import mx.dev.franco.automusictagfixer.R
import mx.dev.franco.automusictagfixer.databinding.FragmentMetadataDetailsBinding
import mx.dev.franco.automusictagfixer.ui.BaseViewModelRoundedBottomSheetDialogFragment

class MetadataDetailsFragment :
    BaseViewModelRoundedBottomSheetDialogFragment<TrackDetailViewModel>() {
    private lateinit var mBinding: FragmentMetadataDetailsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_metadata_details, container, false)
        mBinding.lifecycleOwner = this
        mBinding.viewmodel = vm
        return mBinding.root
    }

    override fun getViewModel(): TrackDetailViewModel {
        return ViewModelProvider(requireActivity(), androidViewModelFactory).get(
            TrackDetailViewModel::class.java
        )
    }


    companion object {
        @JvmStatic
        fun newInstance(): MetadataDetailsFragment {
            return MetadataDetailsFragment()
        }
    }
}
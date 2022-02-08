package mx.dev.franco.automusictagfixer.ui.trackdetail

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import mx.dev.franco.automusictagfixer.R
import mx.dev.franco.automusictagfixer.databinding.FragmentTrackDetailBinding
import mx.dev.franco.automusictagfixer.ui.BaseViewModelFragment
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils
import mx.dev.franco.automusictagfixer.utilities.Constants
import mx.dev.franco.automusictagfixer.utilities.Constants.GOOGLE_SEARCH

/**
 * Use the [TrackDetailFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class TrackDetailFragment : BaseViewModelFragment<TrackDetailViewModel>() {

    private lateinit var binding: FragmentTrackDetailBinding

    private var mBundle: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBundle = arguments
        /*obtainViewModel.setInitialAction(CorrectionActions.VIEW_INFO)
        obtainViewModel.observeReadingResult()
            .observe(requireActivity(), { voids: Unit -> onSuccessLoad(voids) })
        obtainViewModel.observeAudioData().observe(this, { aVoid: Void? -> })
        obtainViewModel.observeInvalidInputsValidation().observe(
            this,
            { validationWrapper: ValidationWrapper -> onInputDataInvalid(validationWrapper) })
        obtainViewModel.observeWritingFinishedEvent()
            .observe(requireActivity(), { voids: Void -> onWritingResult(voids) })
        obtainViewModel.observeIsStoredInSD().observe(this, { aBoolean: Boolean ->
                binding.ibInfoTrack.visibility =
                    if (aBoolean) View.VISIBLE else View.INVISIBLE
            }
        )
        Log.e(javaClass.name, "onCreate")

        obtainViewModel.loadInfoTrack(mBundle!!.getInt(
            Constants.MEDIA_STORE_ID,
            -1
        ))*/
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.e(javaClass.name, "onCreateView")
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_track_detail,
            container,
            false
        )
        /*binding.apply {
            lifecycleOwner = this@TrackDetailFragment
            viewmodel = obtainViewModel
        }*/
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.trackNameDetails.doOnTextChanged { text, start, before, count ->
            binding.albumNameDetails.setText(text)
        }
    }

    private fun onWritingResult(voids: Void) {
        disableFields()
        removeErrorTags()
    }

    /**
     * Callback when data from track is completely
     * loaded.
     * @param voids No object.
     */
    private fun onSuccessLoad(voids: Unit) {
        Log.e(javaClass.name, "onSuccessLoad")
        binding.changeImageButton.setOnClickListener { v: View? ->
            (activity as TrackDetailActivity?)!!.editCover(
                TrackDetailActivity.INTENT_OPEN_GALLERY
            )
        }
        binding.ibInfoTrack.setOnClickListener {
            AndroidUtils.showToast(
                R.string.sd_track_message,
                requireActivity()
            )
        }
    }

    private fun onInputDataInvalid(validationWrapper: ValidationWrapper) {
        val editText =
            binding.root.findViewById<EditText>(validationWrapper.field)
        val animation = AnimationUtils.loadAnimation(requireActivity(), R.anim.blink)
        editText.requestFocus()
        editText.error = getString(validationWrapper.message)
        editText.animation = animation
        editText.startAnimation(animation)
        val imm =
            (requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    /**
     * Enters edit mode, for modify manually
     * the information about the song
     */
    fun enableFieldsToEdit() {
        //Shrink toolbar to make it easy to user
        //focus in editing tags

        //Enable edit text for edit them
        //mFragmentTrackDetailBinding.trackNameDetails.setFocusable(true);
        binding!!.trackNameDetails.isEnabled = true
        binding!!.trackNameDetails.requestFocus()
        binding!!.artistNameDetails.isEnabled = true
        //mFragmentTrackDetailBinding.artistNameDetails.setFocusable(true);
        binding!!.albumNameDetails.isEnabled = true
        //mFragmentTrackDetailBinding.albumNameDetails.setFocusable(true);
        binding!!.trackNumber.isEnabled = true
        //mFragmentTrackDetailBinding.trackNumber.setFocusable(true);
        binding!!.trackYear.isEnabled = true
        //mFragmentTrackDetailBinding.trackYear.setFocusable(true);
        binding!!.trackGenre.isEnabled = true
        //mFragmentTrackDetailBinding.trackGenre.setFocusable(true);
        binding!!.changeImageButton.visibility = View.VISIBLE
        val imm =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(
            binding!!.trackNameDetails,
            InputMethodManager.SHOW_IMPLICIT
        )
        (requireActivity() as TrackDetailActivity).mEditMode = true
    }

    /**
     * Remove error tags from editable fields.
     */
    private fun removeErrorTags() {
        //get descendants instances of edit text
        val fields = binding!!.root.getFocusables(View.FOCUS_DOWN)
        val numElements = fields.size
        for (i in 0 until numElements) {
            if (fields[i] is EditText) {
                (fields[i] as EditText).error = null
            }
        }
    }

    /**
     * Disables the fields and
     * leaves out from edit mode
     */
    fun disableFields() {
        removeErrorTags()
        //mFragmentTrackDetailBinding.trackNameDetails.setFocusable(false);
        binding!!.trackNameDetails.isEnabled = false
        binding!!.trackNameDetails.clearFocus()
        binding!!.artistNameDetails.clearFocus()
        binding!!.artistNameDetails.isEnabled = false
        //mFragmentTrackDetailBinding.artistNameDetails.setFocusable(false);
        binding!!.albumNameDetails.clearFocus()
        binding!!.albumNameDetails.isEnabled = false
        //mFragmentTrackDetailBinding.albumNameDetails.setFocusable(false);
        binding!!.trackNumber.clearFocus()
        binding!!.trackNumber.isEnabled = false
        //mFragmentTrackDetailBinding.trackNumber.setFocusable(false);
        binding!!.trackYear.clearFocus()
        binding!!.trackYear.isEnabled = false
        //mFragmentTrackDetailBinding.trackYear.setFocusable(false);
        binding!!.trackGenre.clearFocus()
        binding!!.trackGenre.isEnabled = false
        //mFragmentTrackDetailBinding.trackGenre.setFocusable(false);
        binding!!.changeImageButton.visibility = View.GONE
        //to hide it, call the method again
        val imm =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        try {
            assert(imm != null)
            imm.hideSoftInputFromWindow(requireActivity().currentFocus!!.windowToken, 0)
            requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        } catch (ignored: Exception) {
        }
        (requireActivity() as TrackDetailActivity).mEditMode = false
    }

    /**
     * Starts a external app to search info about the current track.
     */
    fun searchInfoForTrack() {
        //Todo: Add null validation, title or artist may be null.
        val title = binding!!.trackNameDetails.text.toString()
        val artist = binding!!.artistNameDetails.text.toString()
        val queryString = title + if (!artist.isEmpty()) " $artist" else ""
        val query: String = GOOGLE_SEARCH + queryString
        val uri = Uri.parse(query)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }

    companion object {
        val INTENT_GET_AND_UPDATE_FROM_GALLERY = 10000
        val TAG = TrackDetailFragment::class.java.name
        fun newInstance(idTrack: Int): TrackDetailFragment {
            val fragment = TrackDetailFragment()
            val bundle = Bundle()
            bundle.putInt(Constants.MEDIA_STORE_ID, idTrack)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun obtainViewModel() = ViewModelProvider(
        requireActivity(),
        androidViewModelFactory
    )[TrackDetailViewModel::class.java]
}
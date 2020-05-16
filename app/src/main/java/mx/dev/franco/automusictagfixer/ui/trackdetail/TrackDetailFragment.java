package mx.dev.franco.automusictagfixer.ui.trackdetail;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.databinding.FragmentTrackDetailBinding;
import mx.dev.franco.automusictagfixer.ui.BaseViewModelFragment;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.Constants.CorrectionActions;

import static android.view.View.VISIBLE;
import static mx.dev.franco.automusictagfixer.utilities.Constants.GOOGLE_SEARCH;

/**
 * Use the {@link TrackDetailFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TrackDetailFragment extends BaseViewModelFragment<TrackDetailViewModel> {

    public static final String TAG = TrackDetailFragment.class.getName();
    private FragmentTrackDetailBinding mFragmentTrackDetailBinding;
    private Bundle mBundle;
    public TrackDetailFragment() {}

    public static TrackDetailFragment newInstance(int idTrack, int correctionMode) {
        TrackDetailFragment fragment = new TrackDetailFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.MEDIA_STORE_ID, idTrack);
        bundle.putInt(CorrectionActions.MODE, correctionMode);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(getClass().getName(), "onCreate");
        mBundle = getArguments();
        if(mBundle != null)
           mViewModel.setInitialAction(
                    mBundle.getInt(CorrectionActions.MODE, CorrectionActions.VIEW_INFO));
        else
            mViewModel.setInitialAction(CorrectionActions.VIEW_INFO);

        mViewModel.observeReadingResult().observe(requireActivity(), this::onSuccessLoad);
        mViewModel.observeAudioData().observe(this, aVoid -> {});
        mViewModel.observeInvalidInputsValidation().observe(this, this::onInputDataInvalid);
        mViewModel.observeWritingFinishedEvent().observe(requireActivity(), this::onWritingResult);
        mViewModel.observeIsStoredInSD().observe(this, aBoolean ->
                mFragmentTrackDetailBinding.ibInfoTrack.setVisibility(aBoolean ? VISIBLE : View.INVISIBLE));
        if (savedInstanceState == null)
            mViewModel.loadInfoTrack(mBundle.getInt(Constants.MEDIA_STORE_ID,-1));
    }

    @Override
    public TrackDetailViewModel getViewModel() {
        return new ViewModelProvider(requireActivity(), androidViewModelFactory).
                get(TrackDetailViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.e(getClass().getName(), "onCreateView");
        mFragmentTrackDetailBinding = DataBindingUtil.inflate(inflater,
                R.layout.fragment_track_detail,
                container,
                false);
        mFragmentTrackDetailBinding.setLifecycleOwner(this);
        mFragmentTrackDetailBinding.setViewmodel(mViewModel);

        return mFragmentTrackDetailBinding.getRoot();
    }

    private void onWritingResult(Void voids) {
        disableFields();
        removeErrorTags();
    }

    /**
     * Callback when data from track is completely
     * loaded.
     * @param voids No object.
     */
    private void onSuccessLoad(Void voids) {
        Log.e(getClass().getName(), "onSuccessLoad");
        mFragmentTrackDetailBinding.
                changeImageButton.setOnClickListener(v ->
                ((TrackDetailActivity)getActivity()).editCover(TrackDetailActivity.INTENT_OPEN_GALLERY));
        mFragmentTrackDetailBinding.ibInfoTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AndroidUtils.showToast(R.string.sd_track_message, requireActivity());
            }
        });
    }

    private void onInputDataInvalid(ValidationWrapper validationWrapper) {
        EditText editText = mFragmentTrackDetailBinding.getRoot().findViewById(validationWrapper.getField());
        Animation animation = AnimationUtils.loadAnimation(requireActivity(), R.anim.blink);
        editText.requestFocus();
        editText.setError(getString(validationWrapper.getMessage()));
        editText.setAnimation(animation);
        editText.startAnimation(animation);
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        assert imm != null;
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
    }

    /**
     * Enters edit mode, for modify manually
     * the information about the song
     */
    void enableFieldsToEdit(){
        //Shrink toolbar to make it easy to user
        //focus in editing tags

        //Enable edit text for edit them
        //mFragmentTrackDetailBinding.trackNameDetails.setFocusable(true);
        mFragmentTrackDetailBinding.trackNameDetails.setEnabled(true);
        mFragmentTrackDetailBinding.trackNameDetails.requestFocus();

        mFragmentTrackDetailBinding.artistNameDetails.setEnabled(true);
        //mFragmentTrackDetailBinding.artistNameDetails.setFocusable(true);

        mFragmentTrackDetailBinding.albumNameDetails.setEnabled(true);
        //mFragmentTrackDetailBinding.albumNameDetails.setFocusable(true);

        mFragmentTrackDetailBinding.trackNumber.setEnabled(true);
        //mFragmentTrackDetailBinding.trackNumber.setFocusable(true);

        mFragmentTrackDetailBinding.trackYear.setEnabled(true);
        //mFragmentTrackDetailBinding.trackYear.setFocusable(true);

        mFragmentTrackDetailBinding.trackGenre.setEnabled(true);
        //mFragmentTrackDetailBinding.trackGenre.setFocusable(true);

        mFragmentTrackDetailBinding.changeImageButton.setVisibility(VISIBLE);
        InputMethodManager imm =(InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mFragmentTrackDetailBinding.trackNameDetails,
                InputMethodManager.SHOW_IMPLICIT);
        ((TrackDetailActivity)requireActivity()).mEditMode = true;
    }

    /**
     * Remove error tags from editable fields.
     */
    private void removeErrorTags(){
        //get descendants instances of edit text
        ArrayList<View> fields = mFragmentTrackDetailBinding.getRoot().getFocusables(View.FOCUS_DOWN);
        int numElements = fields.size();

        for (int i = 0 ; i < numElements ; i++) {
            if (fields.get(i) instanceof EditText) {
                ((EditText) fields.get(i)).setError(null);
            }
        }
    }

    /**
     * Disables the fields and
     * leaves out from edit mode
     */
    void disableFields(){
        removeErrorTags();
        //mFragmentTrackDetailBinding.trackNameDetails.setFocusable(false);
        mFragmentTrackDetailBinding.trackNameDetails.setEnabled(false);
        mFragmentTrackDetailBinding.trackNameDetails.clearFocus();

        mFragmentTrackDetailBinding.artistNameDetails.clearFocus();
        mFragmentTrackDetailBinding.artistNameDetails.setEnabled(false);
        //mFragmentTrackDetailBinding.artistNameDetails.setFocusable(false);

        mFragmentTrackDetailBinding.albumNameDetails.clearFocus();
        mFragmentTrackDetailBinding.albumNameDetails.setEnabled(false);
        //mFragmentTrackDetailBinding.albumNameDetails.setFocusable(false);

        mFragmentTrackDetailBinding.trackNumber.clearFocus();
        mFragmentTrackDetailBinding.trackNumber.setEnabled(false);
        //mFragmentTrackDetailBinding.trackNumber.setFocusable(false);

        mFragmentTrackDetailBinding.trackYear.clearFocus();
        mFragmentTrackDetailBinding.trackYear.setEnabled(false);
        //mFragmentTrackDetailBinding.trackYear.setFocusable(false);

        mFragmentTrackDetailBinding.trackGenre.clearFocus();
        mFragmentTrackDetailBinding.trackGenre.setEnabled(false);
        //mFragmentTrackDetailBinding.trackGenre.setFocusable(false);

        mFragmentTrackDetailBinding.changeImageButton.setVisibility(View.GONE);
        //to hide it, call the method again
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        try {
            assert imm != null;
            imm.hideSoftInputFromWindow(requireActivity().getCurrentFocus().getWindowToken(), 0);
            requireActivity().
                    getWindow().
                    setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
        catch (Exception ignored){}

        ((TrackDetailActivity)requireActivity()).mEditMode = false;
    }

    /**
     * Starts a external app to search info about the current track.
     */
    void searchInfoForTrack(){
        //Todo: Add null validation, title or artist may be null.
        String title = mFragmentTrackDetailBinding.trackNameDetails.
                getText().toString();
        String artist = mFragmentTrackDetailBinding.artistNameDetails.
                getText().toString();

        String queryString = title + (!artist.isEmpty() ? (" " + artist) : "");
        String query = GOOGLE_SEARCH + queryString;
        Uri uri = Uri.parse(query);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }
}

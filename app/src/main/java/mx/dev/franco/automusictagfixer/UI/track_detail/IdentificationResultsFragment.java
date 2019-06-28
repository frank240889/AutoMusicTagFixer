package mx.dev.franco.automusictagfixer.UI.track_detail;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.UI.RoundedBottomSheetDialogFragment;
import mx.dev.franco.automusictagfixer.fixer.Fixer;
import mx.dev.franco.automusictagfixer.identifier.GnResponseListener;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.GlideApp;
import mx.dev.franco.automusictagfixer.utilities.Tagger;
import mx.dev.franco.automusictagfixer.utilities.TrackUtils;

public class IdentificationResultsFragment extends RoundedBottomSheetDialogFragment {
    public interface OnBottomSheetFragmentInteraction {
        void onMissingTagsButton(Fixer.CorrectionParams correctionParams);
        void onOverwriteTagsButton(Fixer.CorrectionParams correctionParams);
        void onSaveAsImageFile();
    }
    private OnBottomSheetFragmentInteraction mCallback;
    private Bundle mArguments;
    private OnClickTextView mOnClickTextView;
    public IdentificationResultsFragment(){}

    public static IdentificationResultsFragment newInstance(GnResponseListener.IdentificationResults identificationResults, boolean onlyCover) {
        Bundle bundle = new Bundle();
        bundle.putString("title", identificationResults.title);
        bundle.putString("artist", identificationResults.artist);
        bundle.putString("album", identificationResults.album);
        bundle.putString("track_number", identificationResults.trackNumber);
        bundle.putString("track_year", identificationResults.trackYear);
        bundle.putString("genre", identificationResults.genre);
        bundle.putByteArray("cover", identificationResults.cover);
        bundle.putBoolean("only_cover", onlyCover);
        IdentificationResultsFragment identificationResultsFragment = new IdentificationResultsFragment();
        identificationResultsFragment.setArguments(bundle);
        return identificationResultsFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if(getParentFragment() instanceof OnBottomSheetFragmentInteraction )
            mCallback = (OnBottomSheetFragmentInteraction) getParentFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mArguments = getArguments();
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if(mArguments.getBoolean("only_cover"))
            return inflater.inflate(R.layout.layout_results_cover_id, container, false);
        else
            return inflater.inflate(R.layout.layout_results_track_id, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Fixer.CorrectionParams correctionParams = new Fixer.CorrectionParams();

        Button missingTagsButton = view.findViewById(R.id.missing_tags_button);
        Button allTagsButton = view.findViewById(R.id.all_tags_button);

        missingTagsButton.setOnClickListener(v -> {
            if(mCallback != null) {
                correctionParams.dataFrom = Constants.CACHED;
                if(mArguments.getBoolean("only_cover")) {
                    correctionParams.mode = Tagger.MODE_ADD_COVER;
                }
                else {
                    correctionParams.mode = Tagger.MODE_OVERWRITE_ALL_TAGS;
                }
                mCallback.onOverwriteTagsButton(correctionParams);
                dismiss();
            }
        });

        allTagsButton.setOnClickListener(v -> {
            if(mCallback != null) {
                if(mArguments.getBoolean("only_cover")) {
                    mCallback.onSaveAsImageFile();
                }
                else {
                    correctionParams.mode = Tagger.MODE_WRITE_ONLY_MISSING;
                    correctionParams.dataFrom = Constants.CACHED;
                    mCallback.onMissingTagsButton(correctionParams);
                }
                dismiss();
            }
        });

        if(mArguments != null) {
            if(mArguments.getBoolean("only_cover")) {
                setCover(AndroidUtils.getResults(mArguments), view);
            }
            else {
                setValues(AndroidUtils.getResults(mArguments), view, correctionParams);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mArguments = null;
        mOnClickTextView = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = null;
    }

    private void setValues(GnResponseListener.IdentificationResults results, View view, Fixer.CorrectionParams correctionParams) {
        final CheckBox checkBox = view.findViewById(R.id.checkbox_rename);
        TextInputLayout textInputLayout = view.findViewById(R.id.label_rename_to);
        EditText editText = view.findViewById(R.id.rename_to);
        TextView textView = view.findViewById(R.id.message_rename_hint);
        mOnClickTextView = new OnClickTextView();
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                correctionParams.newName = editText.getText().toString();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(!isChecked){
                textInputLayout.setVisibility(View.GONE);
                textView.setVisibility(View.GONE);
                editText.setText("");
                correctionParams.newName = "";
                correctionParams.shouldRename = false;
                editText.clearFocus();
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                try {
                    assert imm != null;
                    imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
            else{
                textInputLayout.setVisibility(View.VISIBLE);
                textView.setVisibility(View.VISIBLE);
                correctionParams.newName = editText.getText().toString();
                correctionParams.shouldRename = true;
                editText.requestFocus();
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                assert imm != null;
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        setCover(results, view);

        if(!results.title.isEmpty()) {
            TextView title = view.findViewById(R.id.track_id_title);
            title.setVisibility(View.VISIBLE);
            title.setText(results.title);
            title.setOnClickListener(mOnClickTextView);
        }

        if(!results.artist.isEmpty()) {
            TextView artist = view.findViewById(R.id.track_id_artist);
            artist.setVisibility(View.VISIBLE);
            artist.setText(results.artist);
            artist.setOnClickListener(mOnClickTextView);
        }

        if(!results.album.isEmpty()) {
            TextView album = view.findViewById(R.id.trackid_album);
            album.setVisibility(View.VISIBLE);
            album.setText(results.album);
            album.setOnClickListener(mOnClickTextView);
        }

        if(!results.genre.isEmpty()) {
            TextView genre = view.findViewById(R.id.trackid_genre);
            genre.setVisibility(View.VISIBLE);
            genre.setText(results.genre);
            genre.setOnClickListener(mOnClickTextView);
        }

        if(!results.trackNumber.isEmpty()) {
            TextView trackNumber = view.findViewById(R.id.track_id_number);
            trackNumber.setVisibility(View.VISIBLE);
            trackNumber.setText(results.trackNumber);
        }

        if(!results.trackYear.isEmpty()) {
            TextView year = view.findViewById(R.id.track_id_year);
            year.setVisibility(View.VISIBLE);
            year.setText(results.trackYear);
        }
    }

    private void setCover(GnResponseListener.IdentificationResults results, View view){
        ImageView cover = view.findViewById(R.id.trackid_cover);
        GlideApp.with(view.getContext()).
                load(results.cover).
                diskCacheStrategy(DiskCacheStrategy.NONE).
                skipMemoryCache(true).
                placeholder(R.drawable.ic_album_white_48px).
                transition(DrawableTransitionOptions.withCrossFade(200)).
                fitCenter().
                into(cover);
        TextView coverDimensions = view.findViewById(R.id.trackid_cover_dimensions);
        coverDimensions.setText(TrackUtils.getStringImageSize(results.cover, getActivity().getApplicationContext())) ;
    }

    /**
     * Helper class to show the information of textview
     * in toast when value is too long
     */
    private static class OnClickTextView implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Toast t = AndroidUtils.getToast(v.getContext());
            t.setDuration(Toast.LENGTH_SHORT);
            TextView textView = (TextView) v;
            t.setText(textView.getText());
            t.show();
        }
    }

}

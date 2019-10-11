package mx.dev.franco.automusictagfixer.UI.results;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.UI.BaseRoundedBottomSheetDialogFragment;
import mx.dev.franco.automusictagfixer.UI.track_detail.IdentificationResultsAdapter;
import mx.dev.franco.automusictagfixer.UI.track_detail.SemiAutoCorrectionParams;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;

public class IdentificationResultsFragmentBase extends BaseRoundedBottomSheetDialogFragment {
    public interface OnResultSelectedListener {
        void applyTagsButton(SemiAutoCorrectionParams semiAutoCorrectionParams);
    }

    private OnResultSelectedListener mOnResultSelectedListener;
    private SemiAutoCorrectionParams mSemiAutoCorrectionParams;
    private int mCenteredItem = -1;

    public IdentificationResultsFragmentBase(){}

    public static IdentificationResultsFragmentBase newInstance(String id) {
        Bundle arguments = new Bundle();
        arguments.putInt(LAYOUT_ID, R.layout.layout_results_track_id);
        arguments.putString(TRACK_ID, id);
        IdentificationResultsFragmentBase identificationResultsFragment = new IdentificationResultsFragmentBase();
        identificationResultsFragment.setArguments(arguments);
        return identificationResultsFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if(getParentFragment() instanceof OnResultSelectedListener)
            mOnResultSelectedListener = (OnResultSelectedListener) getParentFragment();
        else if(context instanceof OnResultSelectedListener)
            mOnResultSelectedListener = (OnResultSelectedListener) context;
        else
            throw new RuntimeException(context.toString() + " must implement " +
                    OnResultSelectedListener.class.getCanonicalName());

        mSemiAutoCorrectionParams = new SemiAutoCorrectionParams();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Button missingTagsButton = view.findViewById(R.id.missing_tags_button);
        Button allTagsButton = view.findViewById(R.id.all_tags_button);
        CheckBox checkBoxRename = view.findViewById(R.id.checkbox_rename);
        EditText newNameEditText = view.findViewById(R.id.new_name_edit_text);
        RecyclerView listResults = view.findViewById(R.id.results_list);
        listResults.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if(newState == RecyclerView.SCROLL_STATE_IDLE) {
                    int firstVisible = ((LinearLayoutManager)recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
                    int lastVisible = ((LinearLayoutManager)recyclerView.getLayoutManager()).findLastVisibleItemPosition();
                    mCenteredItem = lastVisible - firstVisible;
                    recyclerView.scrollToPosition(mCenteredItem);
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

        newNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mSemiAutoCorrectionParams.setNewName(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        checkBoxRename.setOnCheckedChangeListener((compoundButton, checked) -> {
            if(!checked) {
                newNameEditText.setText(null);
            }
        });

        missingTagsButton.setOnClickListener(view1 -> {
            mSemiAutoCorrectionParams.setCodeRequest(AudioTagger.MODE_WRITE_ONLY_MISSING);
            mSemiAutoCorrectionParams.setPosition(mCenteredItem+"");
            mOnResultSelectedListener.applyTagsButton(mSemiAutoCorrectionParams);
        });

        allTagsButton.setOnClickListener(view12 -> {
            mSemiAutoCorrectionParams.setCodeRequest(AudioTagger.MODE_OVERWRITE_ALL_TAGS);
            mSemiAutoCorrectionParams.setPosition(mCenteredItem+"");
            mOnResultSelectedListener.applyTagsButton(mSemiAutoCorrectionParams);
        });
    }


    @Override
    public void onDetach() {
        super.onDetach();
        mOnResultSelectedListener = null;
    }
}

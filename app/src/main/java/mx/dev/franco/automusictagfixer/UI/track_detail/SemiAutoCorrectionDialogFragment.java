package mx.dev.franco.automusictagfixer.UI.track_detail;

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
import mx.dev.franco.automusictagfixer.UI.ResultsFragmentBase;
import mx.dev.franco.automusictagfixer.UI.results.IdentificationResultsFragmentBase;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;

public class SemiAutoCorrectionDialogFragment extends ResultsFragmentBase<ResultsViewModel> {

  public interface OnSemiAutoCorrectionListener {
    void onMissingTagsButton(SemiAutoCorrectionParams semiAutoCorrectionParams);
    void onOverwriteTagsButton(SemiAutoCorrectionParams semiAutoCorrectionParams);
  }

  private OnSemiAutoCorrectionListener mOnSemiAutoCorrectionListener;
  private SemiAutoCorrectionParams mSemiAutoCorrectionParams;
  private int mCenteredItem = -1;

  public SemiAutoCorrectionDialogFragment(){}

  public static SemiAutoCorrectionDialogFragment newInstance(String id) {
    SemiAutoCorrectionDialogFragment semiAutoCorrectionDialogFragment = new SemiAutoCorrectionDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putInt(LAYOUT_ID, R.layout.rename_file_layout);
    bundle.putString(TRACK_ID, id);
    semiAutoCorrectionDialogFragment.setArguments(bundle);
    return semiAutoCorrectionDialogFragment;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    if(getParentFragment() instanceof OnSemiAutoCorrectionListener)
      mOnSemiAutoCorrectionListener = (OnSemiAutoCorrectionListener) getParentFragment();
    else if(context instanceof IdentificationResultsFragmentBase.OnResultSelectedListener)
      mOnSemiAutoCorrectionListener = (OnSemiAutoCorrectionListener) context;
    else
      throw new RuntimeException(context.toString() + " must implement " +
              IdentificationResultsFragmentBase.OnResultSelectedListener.class.getCanonicalName());

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
      mOnSemiAutoCorrectionListener.onMissingTagsButton(mSemiAutoCorrectionParams);
    });

    allTagsButton.setOnClickListener(view12 -> {
      mSemiAutoCorrectionParams.setCodeRequest(AudioTagger.MODE_OVERWRITE_ALL_TAGS);
      mSemiAutoCorrectionParams.setPosition(mCenteredItem+"");
      mOnSemiAutoCorrectionListener.onOverwriteTagsButton(mSemiAutoCorrectionParams);
    });
  }


}

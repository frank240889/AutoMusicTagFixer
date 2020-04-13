package mx.dev.franco.automusictagfixer.ui.trackdetail;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputLayout;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;
import mx.dev.franco.automusictagfixer.fixer.CorrectionParams;
import mx.dev.franco.automusictagfixer.identifier.Result;
import mx.dev.franco.automusictagfixer.ui.ResultsFragmentBase;
import mx.dev.franco.automusictagfixer.utilities.Constants;

public class SemiAutoCorrectionDialogFragment extends ResultsFragmentBase<ResultsViewModel> {

  public interface OnSemiAutoCorrectionListener {
    void onMissingTagsButton(CorrectionParams semiAutoCorrectionParams);
    void onOverwriteTagsButton(CorrectionParams semiAutoCorrectionParams);
  }

  private OnSemiAutoCorrectionListener mOnSemiAutoCorrectionListener;
  private CorrectionParams mSemiAutoCorrectionParams;
  private int mTrackCenteredItem = 0;
  private int mNumberResults = 0;
  private IdentificationResultsAdapter mTrackAdapter;

  public SemiAutoCorrectionDialogFragment(){}

  public static SemiAutoCorrectionDialogFragment newInstance(String id) {
    SemiAutoCorrectionDialogFragment semiAutoCorrectionDialogFragment = new SemiAutoCorrectionDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putInt(LAYOUT_ID, R.layout.layout_results_track_id);
    bundle.putString(TRACK_ID, id);
    semiAutoCorrectionDialogFragment.setArguments(bundle);
    return semiAutoCorrectionDialogFragment;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    if(getParentFragment() instanceof OnSemiAutoCorrectionListener)
      mOnSemiAutoCorrectionListener = (OnSemiAutoCorrectionListener) getParentFragment();
    else if(context instanceof OnSemiAutoCorrectionListener)
      mOnSemiAutoCorrectionListener = (OnSemiAutoCorrectionListener) context;
    else
      throw new RuntimeException(context.toString() + " must implement " +
          OnSemiAutoCorrectionListener.class.getCanonicalName());

    mSemiAutoCorrectionParams = new CorrectionParams();
    mSemiAutoCorrectionParams.setTagsSource(Constants.CACHED);
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mTrackAdapter = new IdentificationResultsAdapter();
    mViewModel.observeProgress().observe(this, this::onLoading);
    mViewModel.observeTrackResults().observe(this, mTrackAdapter);
    mViewModel.fetchResults(mTrackId);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    Button missingTagsButton = view.findViewById(R.id.missing_tags_button);
    Button allTagsButton = view.findViewById(R.id.all_tags_button);
    ImageButton leftChevron = view.findViewById(R.id.iv_left_chevron);
    ImageButton rightChevron = view.findViewById(R.id.iv_right_chevron);
    CheckBox checkBoxRename = view.findViewById(R.id.checkbox_rename);
    TextInputLayout label = view.findViewById(R.id.label_rename_to);
    EditText newNameEditText = view.findViewById(R.id.new_name_edit_text);
    RecyclerView listResults = view.findViewById(R.id.results_list);
    TextView title = view.findViewById(R.id.title_results);
    NestedScrollView scrollablleContainer = view.findViewById(R.id.scrollable_container);

    LinearLayoutManager layoutManager = new LinearLayoutManager(listResults.getContext());
    layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
    listResults.setLayoutManager(layoutManager);
    listResults.setAdapter(mTrackAdapter);
    SnapHelper snapHelper = new LinearSnapHelper();
    snapHelper.attachToRecyclerView(listResults);


    listResults.addOnScrollListener(new RecyclerView.OnScrollListener() {
      @Override
      public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        if(newState == RecyclerView.SCROLL_STATE_IDLE) {
          View snapView = snapHelper.findSnapView(layoutManager);
          if(snapView != null) {
            mTrackCenteredItem = layoutManager.getPosition(snapView);
            listResults.smoothScrollToPosition(mTrackCenteredItem);
          }
        }
      }
    });

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
        newNameEditText.clearFocus();
        newNameEditText.setText("");
        label.setVisibility(View.GONE);
        mSemiAutoCorrectionParams.setRenameFile(false);
      }
      else {
        newNameEditText.requestFocus();
        String suggestedName = ((Result)mViewModel.
                getTrackResult(mTrackCenteredItem)).getTitle();
        newNameEditText.setText(suggestedName != null && !suggestedName.equals("") ? suggestedName : "");
        mSemiAutoCorrectionParams.setRenameFile(true);
        label.setVisibility(View.VISIBLE);
        label.postDelayed(new Runnable() {
          @Override
          public void run() {
            scrollablleContainer.fullScroll(View.FOCUS_DOWN);
          }
        },100);
      }
    });

    missingTagsButton.setOnClickListener(view1 -> {
      if(checkBoxRename.isChecked() && newNameEditText.getText().toString().isEmpty()) {
        newNameEditText.setError(getString(R.string.new_name_empty));
      }
      else {
        mSemiAutoCorrectionParams.setCorrectionMode(AudioTagger.MODE_WRITE_ONLY_MISSING);
        mSemiAutoCorrectionParams.setTrackId(mViewModel.getTrackResult(mTrackCenteredItem).getId());
        mOnSemiAutoCorrectionListener.onMissingTagsButton(mSemiAutoCorrectionParams);
        dismiss();
      }
    });

    allTagsButton.setOnClickListener(view12 -> {
      if(checkBoxRename.isChecked() && newNameEditText.getText().toString().isEmpty()) {
        newNameEditText.setError(getString(R.string.new_name_empty));
      }
      else {
        mSemiAutoCorrectionParams.setCorrectionMode(AudioTagger.MODE_OVERWRITE_ALL_TAGS);
        mSemiAutoCorrectionParams.setTrackId(mViewModel.getTrackResult(mTrackCenteredItem).getId());
        mOnSemiAutoCorrectionListener.onOverwriteTagsButton(mSemiAutoCorrectionParams);
        dismiss();
      }
    });

    leftChevron.setOnClickListener(v -> {
      if (mTrackCenteredItem < layoutManager.getItemCount()) {
        listResults.smoothScrollToPosition(++mTrackCenteredItem);
      }
    });

    rightChevron.setOnClickListener(v -> {
      if (mTrackCenteredItem > 0) {
        listResults.smoothScrollToPosition(--mTrackCenteredItem);
      }
    });

    mViewModel.observeTrackResults().observe(getViewLifecycleOwner(), identificationResults -> {
      mNumberResults = identificationResults.size();
      title.setText(String.format(getString(R.string.results_found), mNumberResults));
    });
  }

  @Override
  protected ResultsViewModel getViewModel() {
    return new ViewModelProvider(this, androidViewModelFactory).get(ResultsViewModel.class);
  }

  @NonNull @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {

    BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST);
    dialog.setOnShowListener(dialog1 -> {
      BottomSheetDialog d = (BottomSheetDialog) dialog1;

      FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
      BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
    });
    return dialog;
  }
}

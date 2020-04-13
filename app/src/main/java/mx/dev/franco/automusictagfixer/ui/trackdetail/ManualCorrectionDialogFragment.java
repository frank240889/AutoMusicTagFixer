package mx.dev.franco.automusictagfixer.ui.trackdetail;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputLayout;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;
import mx.dev.franco.automusictagfixer.fixer.CorrectionParams;
import mx.dev.franco.automusictagfixer.ui.BaseRoundedBottomSheetDialogFragment;
import mx.dev.franco.automusictagfixer.utilities.Constants;

public class ManualCorrectionDialogFragment extends BaseRoundedBottomSheetDialogFragment {
  private static final String POSSIBLE_TITLE = "possible_title";
  private String mPossibleTitle;
  public interface OnManualCorrectionListener {
    void onManualCorrection(CorrectionParams inputParams);
    void onCancelManualCorrection();
  }

  private OnManualCorrectionListener mOnManualCorrectionListener;

  public ManualCorrectionDialogFragment(){}

  public static ManualCorrectionDialogFragment newInstance(String title) {
    ManualCorrectionDialogFragment manualCorrectionDialogFragment = new ManualCorrectionDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putString(POSSIBLE_TITLE, title);
    bundle.putInt(LAYOUT_ID, R.layout.layout_manual_track_id);
    manualCorrectionDialogFragment.setArguments(bundle);
    return manualCorrectionDialogFragment;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    if(context instanceof OnManualCorrectionListener)
      mOnManualCorrectionListener = (OnManualCorrectionListener) context;
    else if(getParentFragment() instanceof  OnManualCorrectionListener)
      mOnManualCorrectionListener = (OnManualCorrectionListener) getParentFragment();
    else
      throw new RuntimeException(context.getClass().getName() + " must implement " +
              OnManualCorrectionListener.class.getName());

  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mPossibleTitle = getArguments() != null ? getArguments().getString(POSSIBLE_TITLE):"";
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    CorrectionParams manualCorrectionParams = new CorrectionParams();
    final CheckBox checkBox = view.findViewById(R.id.manual_checkbox_rename);
    Button acceptButton = view.findViewById(R.id.accept_changes);
    Button cancelButton = view.findViewById(R.id.cancel_changes);
    TextInputLayout textInputLayout = view.findViewById(R.id.manual_label_rename_to);
    EditText editText = view.findViewById(R.id.manual_rename_to);
    editText.setText(mPossibleTitle);
    acceptButton.setOnClickListener(v -> {
      if(checkBox.isChecked() && (manualCorrectionParams.getNewName() == null ||
        manualCorrectionParams.getNewName().equals(""))) {
          editText.setError(getString(R.string.new_name_empty));
      }
      else {
        manualCorrectionParams.setTagsSource(Constants.MANUAL);
        manualCorrectionParams.setCorrectionMode(AudioTagger.MODE_OVERWRITE_ALL_TAGS);
        mOnManualCorrectionListener.onManualCorrection(manualCorrectionParams);
        dismiss();
      }
    });

    cancelButton.setOnClickListener(v -> {
      mOnManualCorrectionListener.onCancelManualCorrection();
      dismiss();
    });

    editText.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        manualCorrectionParams.setNewName(editText.getText().toString());
      }
      @Override
      public void afterTextChanged(Editable s) {}
    });
    //TextView textView = view.findViewById(R.id.manual_message_rename_hint);
    checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
      if(!isChecked){
        textInputLayout.setVisibility(View.GONE);
        manualCorrectionParams.setNewName(null);
        manualCorrectionParams.setRenameFile(false);
      }
      else{
        textInputLayout.setVisibility(View.VISIBLE);
        manualCorrectionParams.setNewName(editText.getText().toString());
        manualCorrectionParams.setRenameFile(true);
      }
    });
  }

  @NonNull @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {

    BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

    dialog.setOnShowListener(dialog1 -> {
      BottomSheetDialog d = (BottomSheetDialog) dialog1;

      FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
      BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_COLLAPSED);
    });

    return dialog;
  }


}

package mx.dev.franco.automusictagfixer.ui.trackdetail;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputLayout;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;
import mx.dev.franco.automusictagfixer.ui.BaseRoundedBottomSheetDialogFragment;
import mx.dev.franco.automusictagfixer.utilities.Constants;

public class ManualCorrectionDialogFragment extends BaseRoundedBottomSheetDialogFragment {

  public interface OnManualCorrectionListener {
    void onManualCorrection(ManualCorrectionParams inputParams);
    void onCancelManualCorrection();
  }

  private OnManualCorrectionListener mOnManualCorrectionListener;

  public ManualCorrectionDialogFragment(){}

  public static ManualCorrectionDialogFragment newInstance() {
    ManualCorrectionDialogFragment manualCorrectionDialogFragment = new ManualCorrectionDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putInt(LAYOUT_ID, R.layout.rename_file_layout);
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
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    ManualCorrectionParams manualCorrectionParams = new ManualCorrectionParams();
    final CheckBox checkBox = view.findViewById(R.id.manual_checkbox_rename);
    Button acceptButton = view.findViewById(R.id.accept_changes);
    Button cancelButton = view.findViewById(R.id.cancel_changes);

    acceptButton.setOnClickListener(v -> {
              manualCorrectionParams.setCorrectionMode(Constants.MANUAL);
              manualCorrectionParams.setCodeRequest(AudioTagger.MODE_OVERWRITE_ALL_TAGS);
              mOnManualCorrectionListener.onManualCorrection(manualCorrectionParams);
      dismiss();
    });

    cancelButton.setOnClickListener(v -> {
      mOnManualCorrectionListener.onCancelManualCorrection();
      dismiss();
    });

    TextInputLayout textInputLayout = view.findViewById(R.id.manual_label_rename_to);
    EditText editText = view.findViewById(R.id.manual_rename_to);
    editText.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        manualCorrectionParams.setTargetFile(editText.getText().toString());
      }
      @Override
      public void afterTextChanged(Editable s) {}
    });
    TextView textView = view.findViewById(R.id.manual_message_rename_hint);
    checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
      if(!isChecked){
        textInputLayout.setVisibility(View.GONE);
        textView.setVisibility(View.GONE);
        editText.setText("");
        manualCorrectionParams.setNewName(null);
        manualCorrectionParams.setRenameFile(false);
      }
      else{
        textInputLayout.setVisibility(View.VISIBLE);
        textView.setVisibility(View.VISIBLE);
        manualCorrectionParams.setNewName(editText.getText().toString());
        manualCorrectionParams.setRenameFile(true);
      }
    });
  }

  @NonNull @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {

    BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

    dialog.setOnShowListener(new DialogInterface.OnShowListener() {
      @Override
      public void onShow(DialogInterface dialog) {
        BottomSheetDialog d = (BottomSheetDialog) dialog;

        FrameLayout bottomSheet = (FrameLayout) d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
      }
    });
    // Do something with your dialog like setContentView() or whatever
    return dialog;
  }


}

package mx.dev.franco.automusictagfixer.UI.track_detail;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.UI.BaseDialogFragment;
import mx.dev.franco.automusictagfixer.fixer.AudioMetadataTagger;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;
import mx.dev.franco.automusictagfixer.utilities.Constants;

public class ManualCorrectionDialogFragment extends BaseDialogFragment {

  public interface OnManualCorrectionListener {
    void onAccept(UIInputParams inputParams);
    void onCancel();
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
    UIInputParams uiInputParams = new UIInputParams();
    final CheckBox checkBox = view.findViewById(R.id.manual_checkbox_rename);
    Button acceptButton = view.findViewById(R.id.accept_button);
    Button cancelButton = view.findViewById(R.id.cancel_button);

    acceptButton.setOnClickListener(v -> {
              uiInputParams.setCorrectionMode(Constants.MANUAL);
              uiInputParams.setCodeRequest(AudioTagger.MODE_OVERWRITE_ALL_TAGS);
              mOnManualCorrectionListener.onAccept(uiInputParams);
      dismiss();
    });

    cancelButton.setOnClickListener(v -> {
      mOnManualCorrectionListener.onCancel();
      dismiss();
    });

    TextInputLayout textInputLayout = view.findViewById(R.id.manual_label_rename_to);
    EditText editText = view.findViewById(R.id.manual_rename_to);
    editText.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        uiInputParams.setTargetFile(editText.getText().toString());
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
        uiInputParams.setTargetFile(null);
        uiInputParams.setRenameFile(false);
      }
      else{
        textInputLayout.setVisibility(View.VISIBLE);
        textView.setVisibility(View.VISIBLE);
        uiInputParams.setTargetFile(editText.getText().toString());
        uiInputParams.setRenameFile(true);
      }
    });
  }



  public static final class UIInputParams extends AudioMetadataTagger.InputParams {
    private boolean renameFile;
    private int correctionMode;
    public UIInputParams(){}

    public boolean isRenameFile() {
      return renameFile;
    }

    public void setRenameFile(boolean renameFile) {
      this.renameFile = renameFile;
    }

    public int getCorrectionMode() {
      return correctionMode;
    }

    public void setCorrectionMode(int correctionMode) {
      this.correctionMode = correctionMode;
    }
  }
}

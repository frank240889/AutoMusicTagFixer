package mx.dev.franco.automusictagfixer.UI.track_detail;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.UI.BaseDialogFragment;

public class ManualCorrectionDialogFragment extends BaseDialogFragment {
  public interface OnCorrectionConfirmListener {

  }


  public ManualCorrectionDialogFragment(){}

  public static ManualCorrectionDialogFragment newInstance() {
    ManualCorrectionDialogFragment manualCorrectionDialogFragment = new ManualCorrectionDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putInt(LAYOUT_ID, R.layout.rename_file_layout);
    manualCorrectionDialogFragment.setArguments(bundle);
    return manualCorrectionDialogFragment;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    UIInputParams uiInputParams = new UIInputParams();
    final CheckBox checkBox = view.findViewById(R.id.manual_checkbox_rename);
    TextInputLayout textInputLayout = view.findViewById(R.id.manual_label_rename_to);
    EditText editText = view.findViewById(R.id.manual_rename_to);
    editText.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        uiInputParams.name = editText.getText().toString();
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
        uiInputParams.name = null;
        uiInputParams.renameFile = false;
      }
      else{
        textInputLayout.setVisibility(View.VISIBLE);
        textView.setVisibility(View.VISIBLE);
        uiInputParams.name = editText.getText().toString();
        uiInputParams.renameFile = true;
      }
    });
  }



  public static final class UIInputParams {
    public String name;
    public boolean renameFile;
    public UIInputParams(){}
  }
}

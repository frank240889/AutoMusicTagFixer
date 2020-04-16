package mx.dev.franco.automusictagfixer.ui.trackdetail;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import mx.dev.franco.automusictagfixer.BuildConfig;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;
import mx.dev.franco.automusictagfixer.fixer.CorrectionParams;
import mx.dev.franco.automusictagfixer.ui.ResultsFragmentBase;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;

public class ChangeFilenameDialogFragment extends ResultsFragmentBase<ResultsViewModel> {

  public interface OnChangeNameListener {
    void onAcceptNewName(CorrectionParams inputParams);
    void onCancelRename();
  }

  private static final String MEDIA_STORE_ID = BuildConfig.APPLICATION_ID + ".media_store_id";

  private OnChangeNameListener mOnChangeNameListener;

  public ChangeFilenameDialogFragment(){}

  public static ChangeFilenameDialogFragment newInstance(String id) {
    ChangeFilenameDialogFragment manualCorrectionDialogFragment = new ChangeFilenameDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putInt(LAYOUT_ID, R.layout.layout_change_filename);
    bundle.putString(MEDIA_STORE_ID, id);
    manualCorrectionDialogFragment.setArguments(bundle);
    return manualCorrectionDialogFragment;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    CorrectionParams manualCorrectionParams = new CorrectionParams();
    manualCorrectionParams.setCorrectionMode(AudioTagger.MODE_RENAME_FILE);
    Button acceptButton = view.findViewById(R.id.accept_changes);
    Button cancelButton = view.findViewById(R.id.cancel_changes);
    Button fromResultsButton = view.findViewById(R.id.mb_from_results);
    String id = getArguments().getString(MEDIA_STORE_ID);

    EditText editText = view.findViewById(R.id.filename_rename);
    acceptButton.setOnClickListener(v -> {
      if (mOnChangeNameListener != null) {
        String newName = editText.getText().toString();
        if (newName.equals("")) {
          editText.setError(getString(R.string.empty_tag));
        }
        else {
          manualCorrectionParams.setNewName(newName.trim());
          mOnChangeNameListener.onAcceptNewName(manualCorrectionParams);
          dismiss();
        }
      }
    });

    cancelButton.setOnClickListener(v -> {
      mOnChangeNameListener.onCancelRename();
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

    fromResultsButton.setOnClickListener(v -> {
      String title = mViewModel.getTitle(id);
      if (title == null) {
        AndroidUtils.showToast(R.string.identify_track_first, getActivity());
      }
      else {
        editText.setText(title);
      }

    });
  }

  public void setOnChangeNameListener(OnChangeNameListener listener) {
    mOnChangeNameListener = listener;
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

  @Override
  protected ResultsViewModel getViewModel() {
    return new ViewModelProvider(this, androidViewModelFactory).get(ResultsViewModel.class);
  }

  @Override
  public void onDetach() {
    super.onDetach();
    mOnChangeNameListener = null;
  }
}

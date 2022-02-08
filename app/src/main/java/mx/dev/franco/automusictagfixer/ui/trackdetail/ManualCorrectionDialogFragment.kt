package mx.dev.franco.automusictagfixer.ui.trackdetail

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputLayout
import mx.dev.franco.automusictagfixer.R
import mx.dev.franco.automusictagfixer.fixer.AudioTagger
import mx.dev.franco.automusictagfixer.fixer.CorrectionParams
import mx.dev.franco.automusictagfixer.ui.BaseRoundedBottomSheetDialogFragment
import mx.dev.franco.automusictagfixer.utilities.Constants

class ManualCorrectionDialogFragment : BaseRoundedBottomSheetDialogFragment() {
    private var mPossibleTitle: String? = null

    interface OnManualCorrectionListener {
        fun onManualCorrection(inputParams: CorrectionParams?)
        fun onCancelManualCorrection()
    }

    private var mOnManualCorrectionListener: OnManualCorrectionListener? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        mOnManualCorrectionListener =
            when {
                context is OnManualCorrectionListener -> context
                parentFragment is OnManualCorrectionListener -> parentFragment as OnManualCorrectionListener?
                else -> throw RuntimeException(
                    context.javaClass.name + " must implement " +
                            OnManualCorrectionListener::class.java.name
                )
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mPossibleTitle = if (arguments != null) requireArguments().getString(POSSIBLE_TITLE) else ""
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val manualCorrectionParams = CorrectionParams()
        val checkBox = view.findViewById<CheckBox>(R.id.manual_checkbox_rename)
        val acceptButton = view.findViewById<Button>(R.id.accept_changes)
        val cancelButton = view.findViewById<Button>(R.id.cancel_changes)
        val textInputLayout: TextInputLayout = view.findViewById(R.id.manual_label_rename_to)
        val editText = view.findViewById<EditText>(R.id.manual_rename_to)
        editText.setText(mPossibleTitle)
        acceptButton.setOnClickListener {
            if (checkBox.isChecked && (manualCorrectionParams.newName == null || manualCorrectionParams.newName == "")) {
                editText.error = getString(R.string.new_name_empty)
            } else {
                manualCorrectionParams.tagsSource = Constants.MANUAL
                manualCorrectionParams.correctionMode = AudioTagger.MODE_OVERWRITE_ALL_TAGS
                mOnManualCorrectionListener!!.onManualCorrection(manualCorrectionParams)
                dismiss()
            }
        }
        cancelButton.setOnClickListener {
            mOnManualCorrectionListener!!.onCancelManualCorrection()
            dismiss()
        }
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                manualCorrectionParams.newName = editText.text.toString()
            }

            override fun afterTextChanged(s: Editable) {}
        })
        //TextView textView = view.findViewById(R.id.manual_message_rename_hint);
        checkBox.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            if (!isChecked) {
                textInputLayout.visibility = View.GONE
                manualCorrectionParams.newName = null
                manualCorrectionParams.setRenameFile(false)
            } else {
                textInputLayout.visibility = View.VISIBLE
                manualCorrectionParams.newName = editText.text.toString()
                manualCorrectionParams.setRenameFile(true)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { dialog1: DialogInterface ->
            val d = dialog1 as BottomSheetDialog
            val bottomSheet =
                d.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            BottomSheetBehavior.from(bottomSheet!!).setState(BottomSheetBehavior.STATE_COLLAPSED)
        }
        return dialog
    }

    companion object {
        private const val POSSIBLE_TITLE = "possible_title"
        @JvmStatic
        fun newInstance(title: String?): ManualCorrectionDialogFragment {
            val manualCorrectionDialogFragment = ManualCorrectionDialogFragment()
            val bundle = Bundle()
            bundle.putString(POSSIBLE_TITLE, title)
            bundle.putInt(LAYOUT_ID, R.layout.layout_manual_track_id)
            manualCorrectionDialogFragment.arguments = bundle
            return manualCorrectionDialogFragment
        }
    }
}
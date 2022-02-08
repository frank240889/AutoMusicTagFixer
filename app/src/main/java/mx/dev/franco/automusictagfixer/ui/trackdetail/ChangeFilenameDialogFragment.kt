package mx.dev.franco.automusictagfixer.ui.trackdetail

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import mx.dev.franco.automusictagfixer.BuildConfig
import mx.dev.franco.automusictagfixer.R
import mx.dev.franco.automusictagfixer.fixer.AudioTagger
import mx.dev.franco.automusictagfixer.fixer.CorrectionParams
import mx.dev.franco.automusictagfixer.ui.BaseRoundedBottomSheetDialogFragment

class ChangeFilenameDialogFragment : BaseRoundedBottomSheetDialogFragment() {
    interface OnChangeNameListener {
        fun onAcceptNewName(inputParams: CorrectionParams?)
        fun onCancelRename()
    }

    private var mOnChangeNameListener: OnChangeNameListener? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val manualCorrectionParams = CorrectionParams()
        manualCorrectionParams.correctionMode = AudioTagger.MODE_RENAME_FILE
        val acceptButton = view.findViewById<Button>(R.id.accept_changes)
        val cancelButton = view.findViewById<Button>(R.id.cancel_changes)
        val fromResultsButton = view.findViewById<Button>(R.id.mb_from_results)
        val id = requireArguments().getString(MEDIA_STORE_ID)
        val editText = view.findViewById<EditText>(R.id.filename_rename)
        acceptButton.setOnClickListener {
            if (mOnChangeNameListener != null) {
                val newName = editText.text.toString()
                if (newName == "") {
                    editText.error = getString(R.string.empty_tag)
                } else {
                    manualCorrectionParams.newName = newName.trim { it <= ' ' }
                    mOnChangeNameListener!!.onAcceptNewName(manualCorrectionParams)
                    dismiss()
                }
            }
        }
        cancelButton.setOnClickListener {
            mOnChangeNameListener!!.onCancelRename()
            dismiss()
        }
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                manualCorrectionParams.newName = editText.text.toString()
            }

            override fun afterTextChanged(s: Editable) {}
        })
    }

    fun setOnChangeNameListener(listener: OnChangeNameListener?) {
        mOnChangeNameListener = listener
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



    override fun onDetach() {
        super.onDetach()
        mOnChangeNameListener = null
    }

    companion object {
        private const val MEDIA_STORE_ID = BuildConfig.APPLICATION_ID + ".media_store_id"
        @JvmStatic
        fun newInstance(id: String?): ChangeFilenameDialogFragment {
            val manualCorrectionDialogFragment = ChangeFilenameDialogFragment()
            val bundle = Bundle()
            bundle.putInt(LAYOUT_ID, R.layout.layout_change_filename)
            bundle.putString(MEDIA_STORE_ID, id)
            manualCorrectionDialogFragment.arguments = bundle
            return manualCorrectionDialogFragment
        }
    }
}
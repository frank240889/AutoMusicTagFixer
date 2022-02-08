package mx.dev.franco.automusictagfixer.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.StringRes
import mx.dev.franco.automusictagfixer.R

class InformativeFragmentDialog : BaseRoundedBottomSheetDialogFragment() {
    private var mTitle: String? = null
    private var mContent: String? = null
    private var mPositiveText: String? = null
    private var mNegativeText: String? = null

    interface OnClickBasicFragmentDialogListener {
        fun onPositiveButton()
        fun onNegativeButton() {}
    }

    private var mOnClickBasicFragmentDialogListener: OnClickBasicFragmentDialogListener? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnClickBasicFragmentDialogListener) mOnClickBasicFragmentDialogListener =
            context else if (parentFragment is OnClickBasicFragmentDialogListener) mOnClickBasicFragmentDialogListener =
            parentFragment as OnClickBasicFragmentDialogListener?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mTitle = requireArguments().getString(TITLE)
            mContent = requireArguments().getString(CONTENT)
            mPositiveText = requireArguments().getString(POSITIVE_BUTTON_TEXT)
            mNegativeText = requireArguments().getString(NEGATIVE_BUTTON_TEXT)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val positive = view.findViewById<Button>(R.id.accept_button)
        val negative = view.findViewById<Button>(R.id.cancel_button)
        val title = view.findViewById<TextView>(R.id.title_basic_dialog)
        val content = view.findViewById<TextView>(R.id.content_basic_dialog)
        if (mTitle == null || mTitle!!.isEmpty()) {
            title.visibility = View.GONE
        } else {
            title.text = mTitle
        }
        if (mContent == null || mContent!!.isEmpty()) {
            content.visibility = View.GONE
        } else {
            content.visibility = View.VISIBLE
            content.text = mContent
        }
        positive.text = mPositiveText
        positive.setOnClickListener { view1: View? ->
            dismiss()
            if (mOnClickBasicFragmentDialogListener != null) mOnClickBasicFragmentDialogListener!!.onPositiveButton()
            childFragmentManager.popBackStack()
        }
        if (mNegativeText == null || mNegativeText!!.isEmpty()) {
            negative.visibility = View.GONE
        } else {
            negative.visibility = View.VISIBLE
            negative.text = mNegativeText
            negative.setOnClickListener { view12: View? ->
                dismiss()
                if (mOnClickBasicFragmentDialogListener != null) mOnClickBasicFragmentDialogListener!!.onNegativeButton()
                childFragmentManager.popBackStack()
            }
        }
    }

    fun setOnClickBasicFragmentDialogListener(listener: OnClickBasicFragmentDialogListener?) {
        mOnClickBasicFragmentDialogListener = listener
    }

    override fun onDetach() {
        super.onDetach()
        mOnClickBasicFragmentDialogListener = null
    }

    companion object {
        private const val TITLE = "title"
        private const val CONTENT = "content"
        private const val POSITIVE_BUTTON_TEXT = "positive_button_text"
        private const val NEGATIVE_BUTTON_TEXT = "negative_button_text"
        fun newInstance(
            @StringRes title: Int,
            @StringRes content: Int,
            @StringRes positiveButtonText: Int,
            @StringRes negativeButtonText: Int, context: Context
        ): InformativeFragmentDialog {
            return newInstance(
                context.getString(title),
                context.getString(content),
                context.getString(positiveButtonText),
                context.getString(negativeButtonText)
            )
        }

        fun newInstance(
            title: String?,
            content: String?,
            positiveButtonText: String?,
            negativeButtonText: String?
        ): InformativeFragmentDialog {
            val bundle = Bundle()
            bundle.putInt(LAYOUT_ID, R.layout.layout_basic_dialog)
            bundle.putString(TITLE, title)
            bundle.putString(CONTENT, content)
            bundle.putString(POSITIVE_BUTTON_TEXT, positiveButtonText)
            bundle.putString(NEGATIVE_BUTTON_TEXT, negativeButtonText)
            val informativeFragmentDialog = InformativeFragmentDialog()
            informativeFragmentDialog.arguments = bundle
            return informativeFragmentDialog
        }
    }
}
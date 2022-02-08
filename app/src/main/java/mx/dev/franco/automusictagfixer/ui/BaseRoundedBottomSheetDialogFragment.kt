package mx.dev.franco.automusictagfixer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * @author Franco Castillo
 * A simple bottom sheet dialog fragment with rounded corners used as base for all bottom sheet fragments.
 */
abstract class BaseRoundedBottomSheetDialogFragment : BottomSheetDialogFragment() {
    @LayoutRes
    private var mLayout = -1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val arguments = arguments
        if (arguments != null) {
            mLayout = arguments.getInt(LAYOUT_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(mLayout, container, false)
    }

    companion object {
        const val LAYOUT_ID = "layout"
    }
}
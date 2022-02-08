package mx.dev.franco.automusictagfixer.ui

import androidx.fragment.app.Fragment

/**
 * Base fragment that abstract the common functionality for fragments
 * that inherits from it.
 *
 * @author Franco Castillo
 */
abstract class BaseFragment : Fragment() {
    val tagName: String
        get() = javaClass.name

    companion object {
        val BASE_FRAGMENT_TAG = BaseFragment::class.java.name
        const val CROSS_FADE_DURATION = 200

        //Intent type for pick an image
        const val INTENT_OPEN_GALLERY = 1
        const val INTENT_GET_AND_UPDATE_FROM_GALLERY = 2
        var TAG: String? = null
    }
}
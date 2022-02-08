package mx.dev.franco.automusictagfixer.ui.trackdetail

import android.graphics.Bitmap
import android.graphics.ImageDecoder

/**
 * Wrapper class for bitmap class android.
 */
class ImageWrapper {
    var source: ImageDecoder.Source? = null
    @JvmField
    var width = 0
    @JvmField
    var height = 0
    @JvmField
    var bitmap: Bitmap? = null
    @JvmField
    var requestCode = 0

    companion object {
        const val MAX_WIDTH = 1080
        const val MAX_HEIGHT = 1080
    }
}
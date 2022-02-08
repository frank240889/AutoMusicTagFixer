package mx.dev.franco.automusictagfixer.ui.main

import android.view.View
import mx.dev.franco.automusictagfixer.persistence.room.Track

class ViewWrapper {
    @JvmField
    var view: View? = null
    @JvmField
    var track: Track? = null
    @JvmField
    var position = 0
    @JvmField
    var mode = 0
}
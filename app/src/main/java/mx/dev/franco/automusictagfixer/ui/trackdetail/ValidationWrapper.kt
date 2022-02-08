package mx.dev.franco.automusictagfixer.ui.trackdetail

import androidx.annotation.IntegerRes

class ValidationWrapper() {
    @IntegerRes
    var field = 0
    var message = 0

    constructor(field: Int, message: Int) : this() {
        this.field = field
        this.message = message
    }
}
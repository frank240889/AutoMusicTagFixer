package mx.dev.franco.automusictagfixer.utilities

import android.content.Context
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar
import mx.dev.franco.automusictagfixer.common.Action
import java.lang.ref.WeakReference

/**
 * A class that wraps a message or its id resource.
 */
class SnackbarMessage<D>(builder: Builder<D>) {
    val title: String?
    val body: String?
    val mainActionText: String?
    val mainAction: Action
    val data: D
    val duration: Int
    val isDismissible: Boolean

    class Builder<D>(context: Context) {
        var title: String? = null
        var body: String? = null
        var mainActionText: String? = null
        var mainAction = Action.NONE
        var data: D? = null
        var duration = Snackbar.LENGTH_SHORT
        var dismissible = true
        private var contextRef: WeakReference<Context>?
        fun title(@StringRes title: Int): Builder<D> {
            this.title = contextRef!!.get()!!.getString(title)
            return this
        }

        fun title(title: String?): Builder<D> {
            this.title = title
            return this
        }

        fun body(@StringRes body: Int): Builder<D> {
            this.body = contextRef!!.get()!!.getString(body)
            return this
        }

        fun body(body: String?): Builder<D> {
            this.body = body
            return this
        }

        fun mainActionText(@StringRes mainActionText: Int): Builder<D> {
            this.mainActionText = contextRef!!.get()!!.getString(mainActionText)
            return this
        }

        fun mainActionText(mainActionText: String?): Builder<D> {
            this.mainActionText = mainActionText
            return this
        }

        fun data(data: D): Builder<D> {
            this.data = data
            return this
        }

        fun action(action: Action): Builder<D> {
            mainAction = action
            return this
        }

        fun duration(duration: Int): Builder<D> {
            this.duration = duration
            return this
        }

        fun dismissible(dismissible: Boolean): Builder<D> {
            this.dismissible = dismissible
            return this
        }

        fun build(): SnackbarMessage<D> {
            val snackbarMessage: SnackbarMessage<D> = SnackbarMessage(this)
            contextRef!!.clear()
            contextRef = null
            return snackbarMessage
        }

        init {
            contextRef = WeakReference(context)
        }
    }

    init {
        title = builder.title
        body = builder.body
        mainActionText = builder.mainActionText
        data = builder.data!!
        mainAction = builder.mainAction
        duration = builder.duration
        isDismissible = builder.dismissible
    }
}
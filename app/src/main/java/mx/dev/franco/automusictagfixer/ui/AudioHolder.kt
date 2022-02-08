package mx.dev.franco.automusictagfixer.ui

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

abstract class AudioHolder(itemView: View?) : RecyclerView.ViewHolder(
    itemView!!
) {
    lateinit var cover: ImageView
}
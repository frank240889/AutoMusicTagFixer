package mx.dev.franco.automusictagfixer.ui.main

import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import mx.dev.franco.automusictagfixer.R
import mx.dev.franco.automusictagfixer.ui.AudioHolder

/**
 * @author Franco Castillo
 */
class AudioItemHolder(itemView: View, clickListener: ClickListener?) : AudioHolder(itemView),
    View.OnClickListener {
    interface ClickListener {
        fun onItemClick(position: Int, view: View?)
    }

    @JvmField
    var trackName: TextView
    var artistName: TextView? = null
    var albumName: TextView? = null
    @JvmField
    var progressBar: ProgressBar
    private var mListener: ClickListener?

    /**
     * This method of listener is implemented in
     * the host that creates the adapter and data source
     * @param v The view clicked.
     */
    override fun onClick(v: View) {
        if (mListener != null) {
            mListener!!.onItemClick(adapterPosition, cover)
        }
    }

    init {
        cover = itemView.findViewById(R.id.iv_cover_art)
        trackName = itemView.findViewById(R.id.tv_item_track_name)
        progressBar = itemView.findViewById(R.id.pb_item_track_progress_correction)
        mListener = clickListener
        itemView.setOnClickListener(this)
    }
}
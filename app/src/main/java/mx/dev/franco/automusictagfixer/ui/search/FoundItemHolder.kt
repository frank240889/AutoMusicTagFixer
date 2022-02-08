package mx.dev.franco.automusictagfixer.ui.search

import android.view.View
import android.widget.TextView
import mx.dev.franco.automusictagfixer.R
import mx.dev.franco.automusictagfixer.ui.AudioHolder

/**
 * This class helps to maintain the reference to
 * every element of item, avoiding call findViewById()
 * in every element for data source, making a considerable
 * improvement in performance of list
 */
class FoundItemHolder(itemView: View, clickListener: ClickListener?) : AudioHolder(itemView),
    View.OnClickListener {
    interface ClickListener {
        fun onItemClick(position: Int, view: View?)
    }

    @JvmField
    var trackName: TextView
    @JvmField
    var artistName: TextView
    @JvmField
    var albumName: TextView
    private var mListener: ClickListener?

    /**
     * This method of listener is implemented in
     * activity that creates the adapter and data source
     * @param v
     */
    override fun onClick(v: View) {
        if (mListener != null) {
            mListener!!.onItemClick(adapterPosition, cover)
        }
    }

    init {
        cover = itemView.findViewById(R.id.iv_found_cover_art)
        trackName = itemView.findViewById(R.id.tv_found_track_name)
        artistName = itemView.findViewById(R.id.tv_found_track_artist_name)
        albumName = itemView.findViewById(R.id.tv_found_track_album_name)
        mListener = clickListener
        cover.setOnClickListener(this)
        itemView.setOnClickListener(this)
    }
}
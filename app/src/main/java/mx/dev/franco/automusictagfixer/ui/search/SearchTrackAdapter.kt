package mx.dev.franco.automusictagfixer.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.AsyncListDiffer.ListListener
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import mx.dev.franco.automusictagfixer.R
import mx.dev.franco.automusictagfixer.covermanager.CoverLoader
import mx.dev.franco.automusictagfixer.persistence.room.Track
import mx.dev.franco.automusictagfixer.ui.AudioHolder
import mx.dev.franco.automusictagfixer.ui.main.DiffCallback
import mx.dev.franco.automusictagfixer.utilities.ServiceUtils

class SearchTrackAdapter(private var mListener: FoundItemHolder.ClickListener?) :
    RecyclerView.Adapter<FoundItemHolder>(), Observer<List<Track>?> {
    private var serviceUtils: ServiceUtils? = null
    private val asyncListDiffer: AsyncListDiffer<Track>? = AsyncListDiffer(this, DiffCallback())
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        serviceUtils = ServiceUtils.getInstance(recyclerView.context)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        if (mListener is ListListener<*>) asyncListDiffer!!.removeListListener((mListener as ListListener<Track>?)!!)
        serviceUtils = null
        mListener = null
        CoverLoader.cancelAll()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoundItemHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.result_search_item, parent, false)
        return FoundItemHolder(itemView, mListener)
    }

    override fun onBindViewHolder(holder: FoundItemHolder, position: Int) {
        val track = asyncListDiffer!!.currentList[position]
        enqueue(holder, track)
        holder.trackName.text = track.title
        holder.artistName.text = track.artist
        holder.albumName.text = track.album
    }

    override fun onBindViewHolder(holder: FoundItemHolder, position: Int, payloads: List<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            val track = asyncListDiffer!!.currentList[position]
            val o = payloads[0] as Bundle
            for (key in o.keySet()) {
                if (key == "title") {
                    holder.trackName.text = track.title
                }
                if (key == "artist") {
                    holder.artistName.text = track.artist
                }
                if (key == "album") {
                    holder.albumName.text = track.album
                }
                if (key == "should_reload_cover") {
                    enqueue(holder, track)
                }
            }
        }
    }

    private fun enqueue(holder: AudioHolder, track: Track) {
        CoverLoader.startFetchingCover(holder, track.path, track.mediaStoreId.toString() + "")
    }

    override fun onViewRecycled(holder: FoundItemHolder) {
        if (holder.itemView.context != null) Glide.with(holder.itemView.context).clear(holder.cover)
    }

    /**
     * Get size of data source
     * @return zero if data source is null, otherwise size of data source
     */
    override fun getItemCount(): Int {
        return asyncListDiffer?.currentList?.size ?: 0
    }

    /**
     * Indicates whether each item in the data set
     * can be represented with a unique identifier of type Long.
     * @param hasStableIds true
     */
    override fun setHasStableIds(hasStableIds: Boolean) {
        super.setHasStableIds(true)
    }

    override fun onChanged(tracks: List<Track>?) {
        asyncListDiffer!!.submitList(tracks)
    }

    companion object {
        private val TAG = SearchTrackAdapter::class.java.name
    }
}
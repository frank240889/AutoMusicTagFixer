package mx.dev.franco.automusictagfixer.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.AsyncListDiffer.ListListener
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import mx.dev.franco.automusictagfixer.R
import mx.dev.franco.automusictagfixer.covermanager.CoverLoader
import mx.dev.franco.automusictagfixer.persistence.room.Track
import mx.dev.franco.automusictagfixer.utilities.ServiceUtils

class TrackAdapter() : RecyclerView.Adapter<AudioItemHolder>(), Observer<List<Track>?> {
    private val serviceUtils: ServiceUtils? = null
    private val asyncListDiffer: AsyncListDiffer<Track>? = AsyncListDiffer(this, DiffCallback())
    private var mListener: AudioItemHolder.ClickListener? = null

    constructor(listener: AudioItemHolder.ClickListener?) : this() {
        mListener = listener
        if (mListener is ListListener<*>) asyncListDiffer!!.addListListener((mListener as ListListener<Track>?)!!)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        mListener = null
        CoverLoader.cancelAll()
    }

    /**
     * @inheritDoc
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioItemHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.track_item, parent, false)
        return AudioItemHolder(itemView, mListener)
    }

    /**
     * @inheritDoc
     */
    override fun onBindViewHolder(
        holder: AudioItemHolder, position: Int,
        payloads: List<Any>
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            //Track track = mTrackList.get(position);
            val o = payloads[0] as Bundle
            if (o.getString("title") != null) holder.trackName.text = o.getString("title")
            if (o.getInt("processing") == 1) {
                holder.progressBar.visibility = View.VISIBLE
            } else {
                holder.progressBar.visibility = View.INVISIBLE
            }
            if (o.getBoolean("should_reload_cover")) {
                loadCover(
                    holder,
                    o.getString("path"),
                    asyncListDiffer!!.currentList[position].mediaStoreId.toString() + ""
                )
            }
        }
    }

    /**
     * @inheritDoc
     */
    override fun onBindViewHolder(holder: AudioItemHolder, position: Int) {
        val track = asyncListDiffer!!.currentList[position]
        holder.trackName.text = track.title
        //holder.artistName.setText(track.getArtist());
        if (track.processing() == 1) {
            holder.progressBar.visibility = View.VISIBLE
        } else {
            holder.progressBar.visibility = View.INVISIBLE
        }
        loadCover(holder, track.path, track.mediaStoreId.toString() + "")
    }

    private fun loadCover(holder: AudioItemHolder, path: String?, mediaStoreId: String) {
        CoverLoader.startFetchingCover(holder, path, mediaStoreId)
    }

    /**
     * @inheritDoc
     */
    override fun onViewRecycled(holder: AudioItemHolder) {
        if (holder.itemView.context != null) Glide.with(holder.itemView.context).clear(holder.cover)
    }

    /**
     * @inheritDoc
     */
    override fun onFailedToRecycleView(holder: AudioItemHolder): Boolean {
        if (holder.itemView.context != null) Glide.with(holder.itemView.context).clear(holder.cover)
        return true
    }

    /**
     * @inheritDoc
     */
    override fun getItemCount(): Int {
        return asyncListDiffer?.currentList?.size ?: 0
    }

    /**
     * @inheritDoc
     */
    override fun setHasStableIds(hasStableIds: Boolean) {
        super.setHasStableIds(true)
    }

    override fun onChanged(tracks: List<Track>?) {
        asyncListDiffer!!.submitList(tracks)
    }

    companion object {
        private val TAG = TrackAdapter::class.java.name
    }
}
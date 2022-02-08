package mx.dev.franco.automusictagfixer.ui.main

import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil
import mx.dev.franco.automusictagfixer.persistence.room.Track

class DiffCallback : DiffUtil.ItemCallback<Track>() {
    override fun areItemsTheSame(oldItem: Track, newItem: Track): Boolean {
        return oldItem.mediaStoreId == newItem.mediaStoreId
    }

    override fun areContentsTheSame(oldItem: Track, newItem: Track): Boolean {
        if (oldItem.version != newItem.version) return false
        if (oldItem.checked() != newItem.checked()) return false
        return oldItem.processing() == newItem.processing()
    }

    override fun getChangePayload(oldItem: Track, newItem: Track): Any? {
        val diff = Bundle()
        if (oldItem.title != newItem.title) diff.putString("title", newItem.title)
        if (oldItem.artist != newItem.artist) diff.putString("artist", newItem.artist)
        if (oldItem.album != newItem.album) diff.putString("album", newItem.album)
        if (oldItem.version != newItem.version) {
            diff.putString("path", newItem.path)
            diff.putBoolean("should_reload_cover", true)
        }
        if (oldItem.checked() != newItem.checked()) diff.putInt("checked", newItem.checked())
        if (oldItem.processing() != newItem.processing()) diff.putInt(
            "processing",
            newItem.processing()
        )
        return diff
    }
}
package mx.dev.franco.automusictagfixer.ui.main;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import mx.dev.franco.automusictagfixer.persistence.room.Track;

public class DiffCallback extends DiffUtil.ItemCallback<Track>{

    public DiffCallback() {
    }

    @Override
    public boolean areItemsTheSame(@NonNull Track oldItem, @NonNull Track newItem) {
        return oldItem.getMediaStoreId() == newItem.getMediaStoreId();
    }

    @Override
    public boolean areContentsTheSame(@NonNull Track oldItem, @NonNull Track newItem) {
        if (!oldItem.getTitle().equals(newItem.getTitle()))
            return false;
        if (!oldItem.getArtist().equals(newItem.getArtist()))
            return false;
        if (!oldItem.getAlbum().equals(newItem.getAlbum()))
            return false;
        if (oldItem.checked() != newItem.checked())
            return false;
        if (oldItem.processing() != newItem.processing())
            return false;

        return oldItem.getVersion() == newItem.getVersion();
    }

    @Nullable
    @Override
    public Object getChangePayload(@NonNull Track oldItem, @NonNull Track newItem) {

        Bundle diff = new Bundle();

        if (!oldItem.getTitle().equals(newItem.getTitle()))
            diff.putString("title", newItem.getTitle());
        if (!oldItem.getArtist().equals(newItem.getArtist()))
            diff.putString("artist", newItem.getArtist());
        if (!oldItem.getAlbum().equals(newItem.getAlbum()))
            diff.putString("album", newItem.getAlbum());
        diff.putBoolean("should_reload_cover", true);

        if (oldItem.checked() != newItem.checked())
            diff.putInt("checked", newItem.checked());
        if (oldItem.processing() != newItem.processing())
            diff.putInt("processing", newItem.processing());
        return diff;
    }
}
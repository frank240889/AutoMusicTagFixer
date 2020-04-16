package mx.dev.franco.automusictagfixer.ui.main;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

import mx.dev.franco.automusictagfixer.persistence.room.Track;

public class DiffCallback extends DiffUtil.Callback{

    private List<Track> oldTracks;
    private List<Track> newTracks;

    public DiffCallback(List<Track> oldTracks, List<Track> newTracks) {
        this.newTracks = newTracks;
        this.oldTracks = oldTracks;
    }

    @Override
    public int getOldListSize() {
        return oldTracks.size();
    }

    @Override
    public int getNewListSize() {
        return newTracks.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldTracks.get(oldItemPosition).getMediaStoreId() == newTracks.get(newItemPosition).getMediaStoreId();
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        Track oldTrack = oldTracks.get(oldItemPosition);
        Track newTrack = newTracks.get(newItemPosition);
        if(!oldTrack.getTitle().equals(newTrack.getTitle()))
            return false;
        if(!oldTrack.getArtist().equals(newTrack.getArtist()))
            return false;
        if(!oldTrack.getAlbum().equals(newTrack.getAlbum()))
            return false;

        return oldTrack.getVersion() == newTrack.getVersion();
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        Track newModel = newTracks.get(newItemPosition);

        Bundle diff = new Bundle();

        diff.putString("title", newModel.getTitle());
        diff.putString("artist", newModel.getArtist());
        diff.putString("album", newModel.getAlbum());
        diff.putBoolean("should_reload_cover", true);
        diff.putString("path", newModel.getPath());
        diff.putInt("checked", newModel.checked());
        diff.putInt("processing", newModel.processing());
        return diff;
    }
}
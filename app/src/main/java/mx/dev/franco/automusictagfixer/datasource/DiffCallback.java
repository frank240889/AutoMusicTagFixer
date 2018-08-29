package mx.dev.franco.automusictagfixer.datasource;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;

import java.util.List;

import mx.dev.franco.automusictagfixer.room.Track;

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
        return oldTracks.get(oldItemPosition).equals(newTracks.get(newItemPosition));
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        Track newModel = newTracks.get(newItemPosition);
        Track oldModel = oldTracks.get(oldItemPosition);

        Bundle diff = new Bundle();

        if (!newModel.getTitle().equals(oldModel.getTitle())) {
            diff.putString("title", newModel.getTitle());
        }

        if(!newModel.getArtist().equals(oldModel.getArtist())){
            diff.putString("artist", newModel.getArtist());
        }

        if(!newModel.getAlbum().equals(oldModel.getAlbum())){
            diff.putString("album", newModel.getAlbum());
        }

        if(newModel.getState() != oldModel.getState()){
            diff.putInt("state", newModel.getState());
            diff.putBoolean("should_reload_cover", true);
        }

        if(!newModel.getPath().equals(oldModel.getPath())){
            diff.putString("path", newModel.getPath());
        }

        diff.putInt("checked", newModel.checked());
        diff.putInt("processing", newModel.processing());


        if (diff.size() == 0) {
            return null;
        }
        return diff;
    }
}
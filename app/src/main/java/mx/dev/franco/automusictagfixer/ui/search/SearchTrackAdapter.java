package mx.dev.franco.automusictagfixer.ui.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.covermanager.CoverLoader;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.ui.AudioHolder;
import mx.dev.franco.automusictagfixer.ui.main.DiffCallback;
import mx.dev.franco.automusictagfixer.utilities.ServiceUtils;

public class SearchTrackAdapter extends RecyclerView.Adapter<FoundItemHolder> implements
        Observer<List<Track>> {
    private static final String TAG = SearchTrackAdapter.class.getName();
    private ServiceUtils serviceUtils;
    private FoundItemHolder.ClickListener mListener;
    private AsyncListDiffer<Track> asyncListDiffer = new AsyncListDiffer<>(this, new DiffCallback());



    public SearchTrackAdapter(FoundItemHolder.ClickListener listener){
        mListener = listener;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        serviceUtils = ServiceUtils.getInstance(recyclerView.getContext());
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        if (mListener instanceof AsyncListDiffer.ListListener)
            asyncListDiffer.removeListListener((AsyncListDiffer.ListListener<Track>) mListener);

        serviceUtils = null;
        mListener = null;
        CoverLoader.cancelAll();
    }

    @Override
    public FoundItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.result_search_item, parent, false);
        return new FoundItemHolder(itemView, mListener);
    }

    @Override
    public void onBindViewHolder(final FoundItemHolder holder, final int position) {
        Track track = asyncListDiffer.getCurrentList().get(position);
        enqueue(holder, track);
        holder.trackName.setText(track.getTitle());
        holder.artistName.setText(track.getArtist());
        holder.albumName.setText(track.getAlbum());

    }

    @Override
    public void onBindViewHolder(@NonNull final FoundItemHolder holder, int position, List<Object> payloads) {
        if(payloads.isEmpty()) {
            super.onBindViewHolder(holder,position,payloads);
        }
        else {
            Track track = asyncListDiffer.getCurrentList().get(position);
            Bundle o = (Bundle) payloads.get(0);
            for (String key : o.keySet()) {
                if (key.equals("title")) {
                    holder.trackName.setText(track.getTitle());
                }
                if (key.equals("artist")) {
                    holder.artistName.setText(track.getArtist());
                }
                if (key.equals("album")) {
                    holder.albumName.setText(track.getAlbum());
                }

                if (key.equals("should_reload_cover")){
                    enqueue(holder, track);
                }
            }
        }
    }

    private void enqueue(AudioHolder holder, Track track) {
        CoverLoader.startFetchingCover(holder, track.getPath(), track.getMediaStoreId()+"");
    }

    @Override
    public void onViewRecycled(@NonNull FoundItemHolder holder) {
        if(holder.itemView.getContext() != null)
            Glide.with(holder.itemView.getContext()).clear(holder.cover);
    }

    /**
     * Get size of data source
     * @return zero if data source is null, otherwise size of data source
     */
    @Override
    public int getItemCount() {
        if(asyncListDiffer != null)
            return asyncListDiffer.getCurrentList().size();
        return 0;
    }

    /**
     * Indicates whether each item in the data set
     * can be represented with a unique identifier of type Long.
     * @param hasStableIds true
     */
    @Override
    public void setHasStableIds(boolean hasStableIds) {
        super.setHasStableIds(true);
    }

    @Override
    public void onChanged(@Nullable List<Track> tracks) {
        asyncListDiffer.submitList(tracks);
    }
}



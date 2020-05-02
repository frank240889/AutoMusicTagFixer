package mx.dev.franco.automusictagfixer.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.covermanager.CoverLoader;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.utilities.ServiceUtils;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class TrackAdapter extends RecyclerView.Adapter<AudioItemHolder> {

    private static final String TAG = TrackAdapter.class.getName();
    private ServiceUtils serviceUtils;
    private AsyncListDiffer<Track> asyncListDiffer = new AsyncListDiffer<>(this, new DiffCallback());
    private AudioItemHolder.ClickListener mListener;


    public TrackAdapter(){
    }
    public TrackAdapter(AudioItemHolder.ClickListener listener){
        this();
        mListener = listener;
        if (mListener instanceof AsyncListDiffer.ListListener)
            asyncListDiffer.addListListener((AsyncListDiffer.ListListener<Track>) mListener);
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        serviceUtils = ServiceUtils.getInstance(recyclerView.getContext());
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        serviceUtils = null;
        if (mListener instanceof AsyncListDiffer.ListListener)
            asyncListDiffer.removeListListener((AsyncListDiffer.ListListener<Track>) mListener);
        mListener = null;
        CoverLoader.cancelAll();
    }

    /**
     * @inheritDoc
     */
    @Override
    public AudioItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.main_item_card, parent, false);
        return new AudioItemHolder(itemView, mListener);
    }

    /**
     * @inheritDoc
     */
    @Override
    public void onBindViewHolder(@NonNull final AudioItemHolder holder, int position,
                                 @NonNull List<Object> payloads) {
        if(payloads.isEmpty()) {
            super.onBindViewHolder(holder,position,payloads);
        }
        else {
            //Track track = mTrackList.get(position);
            Bundle o = (Bundle) payloads.get(0);
                if (o.getString("title") != null)
                    holder.trackName.setText(o.getString("title"));
                if (o.getString("artist") != null)
                    holder.artistName.setText(o.getString("artist"));

                holder.checkBox.setChecked(o.getInt("checked") == 1);


                if (o.getInt("processing") == 1) {
                    holder.progressBar.setVisibility(View.VISIBLE);
                    holder.checkBox.setVisibility(INVISIBLE);
                } else {
                    holder.checkBox.setVisibility(VISIBLE);
                    holder.progressBar.setVisibility(INVISIBLE);
                }

                if (o.getBoolean("should_reload_cover")){
                    loadCover(holder, o.getString("path"), asyncListDiffer.getCurrentList().get(position).getMediaStoreId()+"");
                }
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public void onBindViewHolder(@NonNull final AudioItemHolder holder, final int position) {
        Track track = asyncListDiffer.getCurrentList().get(position);
        holder.checkBox.setChecked(track.checked() == 1);
        holder.trackName.setText(track.getTitle());
        holder.artistName.setText(track.getArtist());
        if (track.processing() == 1) {
            holder.progressBar.setVisibility(View.VISIBLE);
            holder.checkBox.setVisibility(INVISIBLE);
        } else {
            holder.checkBox.setVisibility(VISIBLE);
            holder.progressBar.setVisibility(INVISIBLE);
        }
        loadCover(holder, track.getPath(), track.getMediaStoreId()+"");
    }

    private void loadCover(AudioItemHolder holder, String path, String mediaStoreId) {
        CoverLoader.startFetchingCover(holder, path, mediaStoreId);
    }

    /**
     * @inheritDoc
     */
    @Override
    public void onViewRecycled(AudioItemHolder holder) {
        if(holder.itemView.getContext() != null)
            Glide.with(holder.itemView.getContext()).clear(holder.cover);
    }

    /**
     * @inheritDoc
     */
    @Override
    public boolean onFailedToRecycleView(@NonNull AudioItemHolder holder) {
        if(holder.itemView.getContext() != null)
            Glide.with(holder.itemView.getContext()).clear(holder.cover);
        return true;
    }

    /**
     * @inheritDoc
     */
    @Override
    public int getItemCount() {
        if(asyncListDiffer != null)
            return asyncListDiffer.getCurrentList().size();
        return 0;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void setHasStableIds(boolean hasStableIds) {
        super.setHasStableIds(true);
    }

    void onChanged(@Nullable List<Track> tracks) {
        asyncListDiffer.submitList(tracks);
    }
}



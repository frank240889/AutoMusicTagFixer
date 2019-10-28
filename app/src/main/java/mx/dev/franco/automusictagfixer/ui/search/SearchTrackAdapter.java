package mx.dev.franco.automusictagfixer.ui.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.covermanager.CoverManager;
import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.interfaces.Destructible;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.ui.AudioHolder;
import mx.dev.franco.automusictagfixer.ui.main.DiffExecutor;
import mx.dev.franco.automusictagfixer.ui.main.DiffResults;
import mx.dev.franco.automusictagfixer.utilities.ServiceUtils;

public class SearchTrackAdapter extends RecyclerView.Adapter<FoundItemHolder> implements
        Destructible, Observer<List<Track>> {
    private static final String TAG = SearchTrackAdapter.class.getName();
    @Inject
    ServiceUtils serviceUtils;
    private List<Track> mTrackList = new ArrayList<>();
    private FoundItemHolder.ClickListener mListener;
    private Deque<List<Track>> mPendingUpdates = new ArrayDeque<>();
    private static DiffExecutor sDiffExecutor;


    public SearchTrackAdapter(FoundItemHolder.ClickListener listener){
        mListener = listener;
    }


    @Override
    public FoundItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.found_item_list, parent, false);
        return new FoundItemHolder(itemView, mListener);
    }

    @Override
    public void onBindViewHolder(final FoundItemHolder holder, final int position) {
        Track track = mTrackList.get(position);
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
            Track track = mTrackList.get(position);
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
        CoverManager.startFetchingCover(holder, track.getPath(), track.getMediaStoreId()+"");
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
        if(mTrackList != null)
            return mTrackList.size();
        return 0;
    }

    public List<Track> getDatasource(){
        return mTrackList;
    }

    /**
     * Indicates whether each item in the data set
     * can be represented with a unique identifier of type Long.
     * @param hasStableIds
     */
    @Override
    public void setHasStableIds(boolean hasStableIds) {
        super.setHasStableIds(true);
    }

    @Override
    public void destroy() {
        serviceUtils = null;
        mTrackList.clear();
        mTrackList = null;
        mListener = null;
        CoverManager.cancelAll();
    }

    @Override
    public void onChanged(@Nullable List<Track> tracks) {
        if(tracks != null) {
            //Update only if exist items
            if (getItemCount() > 0) {
                if(mPendingUpdates != null) {
                    mPendingUpdates.push(tracks);
                }
                updateInBackground(tracks);
            } else {
                mTrackList = tracks;
                notifyDataSetChanged();
            }
        }
    }

    private void updateInBackground(List<Track> newItems){
        if (mPendingUpdates != null && mPendingUpdates.size() > 1) {
            return;
        }
        sDiffExecutor = new DiffExecutor(new AsyncOperation<Void, DiffResults<Track>, Void, Void>() {
            @Override
            public void onAsyncOperationFinished(DiffResults<Track> result) {
                processResult(result);
            }
        });
        sDiffExecutor.executeOnExecutor(AutoMusicTagFixer.getExecutorService(), mTrackList, newItems);

    }

    private void processResult(DiffResults<Track> result) {
        if (mPendingUpdates != null)
            mPendingUpdates.remove();

        if (result.diffResult != null) {
            result.diffResult.dispatchUpdatesTo(this);
            mTrackList.clear();
            mTrackList.addAll(result.list);

            sDiffExecutor = null;
            //Try to perform next latest setChecked.
            if (mPendingUpdates != null && mPendingUpdates.size() > 0) {
                updateInBackground(mPendingUpdates.peek());
            }
        }
    }
}



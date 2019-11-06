package mx.dev.franco.automusictagfixer.ui.main;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.covermanager.CoverManager;
import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.interfaces.Destructible;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.persistence.room.TrackState;
import mx.dev.franco.automusictagfixer.services.FixerTrackService;
import mx.dev.franco.automusictagfixer.utilities.ServiceUtils;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class TrackAdapter extends RecyclerView.Adapter<AudioItemHolder> implements
        Destructible {

    private static final String TAG = TrackAdapter.class.getName();

    @Inject
    public ServiceUtils serviceUtils;
    private List<Track> mTrackList = new ArrayList<>();
    private AudioItemHolder.ClickListener mListener;
    private Deque<List<Track>> mPendingUpdates = new ArrayDeque<>();
    private static DiffExecutor sDiffExecutor;

    public TrackAdapter(){}
    public TrackAdapter(AudioItemHolder.ClickListener listener){
        this();
        mListener = listener;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        serviceUtils = ServiceUtils.getInstance(recyclerView.getContext());
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        serviceUtils = null;
    }

    /**
     * @inheritDoc
     */
    @Override
    public AudioItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.item_list, parent, false);
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
                if(!serviceUtils.checkIfServiceIsRunning(FixerTrackService.CLASS_NAME)) {
                    holder.checkBox.setVisibility(VISIBLE);
                    holder.progressBar.setVisibility(GONE);
                    if (key.equals("checked")) {
                        holder.checkBox.setChecked(track.checked() == 1);
                    }
                }
                else {
                    holder.checkBox.setVisibility(View.INVISIBLE);
                    if (key.equals("processing")) {
                        if (track.processing() == 1) {
                            holder.progressBar.setVisibility(VISIBLE);
                        } else {
                            holder.progressBar.setVisibility(GONE);
                        }
                    }
                }

                //if (key.equals("should_reload_cover")){
                    enqueue(holder, track);
                //}

                if (key.equals("state")) {
                    switch (track.getState()) {
                        case TrackState.ALL_TAGS_FOUND:
                            holder.stateMark.setImageResource(R.drawable.ic_done_all_white);
                            holder.stateMark.setVisibility(VISIBLE);
                            break;
                        case TrackState.ALL_TAGS_NOT_FOUND:
                            holder.stateMark.setImageResource(R.drawable.ic_done_white);
                            holder.stateMark.setVisibility(VISIBLE);
                            break;
                        default:
                            holder.stateMark.setImageResource(0);
                            holder.stateMark.setVisibility(GONE);
                            break;
                    }
                }
            }
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public void onBindViewHolder(@NonNull final AudioItemHolder holder, final int position) {
        Track track = mTrackList.get(position);
        if(!serviceUtils.checkIfServiceIsRunning(FixerTrackService.CLASS_NAME)) {
            holder.checkBox.setVisibility(VISIBLE);
            holder.progressBar.setVisibility(GONE);
        }
        else {
            holder.checkBox.setVisibility(View.INVISIBLE);
            if (track.processing() == 1) {
                holder.progressBar.setVisibility(View.VISIBLE);
            } else {
                holder.progressBar.setVisibility(View.GONE);
            }
        }

        enqueue(holder, track);

        switch (track.getState()) {
            case TrackState.ALL_TAGS_FOUND:
                holder.stateMark.setImageResource(R.drawable.ic_done_all_white);
                holder.stateMark.setVisibility(VISIBLE);
                break;
            case TrackState.ALL_TAGS_NOT_FOUND:
                holder.stateMark.setImageResource(R.drawable.ic_done_white);
                holder.stateMark.setVisibility(VISIBLE);
                break;
            default:
                holder.stateMark.setImageResource(0);
                holder.stateMark.setVisibility(GONE);
                break;
        }
        holder.checkBox.setChecked(track.checked() == 1);
        holder.trackName.setText(track.getTitle());
        holder.artistName.setText(track.getArtist());
        holder.albumName.setText(track.getAlbum());

    }

    private void enqueue(AudioItemHolder holder, Track track) {
        CoverManager.startFetchingCover(holder, track.getPath(), track.getMediaStoreId()+"");
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
        if(mTrackList != null)
            return mTrackList.size();
        return 0;
    }

    /**
     * @inheritDoc
     */
    @Override
    public void setHasStableIds(boolean hasStableIds) {
        super.setHasStableIds(true);
    }

    @Override
    public void destroy() {
        if(sDiffExecutor != null && (sDiffExecutor.getStatus() == AsyncTask.Status.PENDING ||
                sDiffExecutor.getStatus() == AsyncTask.Status.RUNNING)){
            sDiffExecutor.cancel(true);
        }
        sDiffExecutor = null;
        mListener = null;
        CoverManager.cancelAll();
    }

    void onChanged(@Nullable List<Track> tracks) {
        if(tracks != null) {
            //Update only if exist items
            if (getItemCount() > 0) {
                if(tracks.size() > 1000) {
                    if (mPendingUpdates != null) {
                        mPendingUpdates.push(tracks);
                    }
                    updateInBackground(tracks);

                }
                else {
                    updateInUIThread(tracks);
                }
            } else {
                mTrackList = tracks;
                notifyDataSetChanged();
            }
        }
    }

    private void updateInUIThread(List<Track> newItems) {
        DiffUtil.DiffResult diffResult = DiffUtil.
                calculateDiff(new DiffCallback(newItems, mTrackList),false);
        diffResult.dispatchUpdatesTo(this);
        mTrackList.clear();
        mTrackList.addAll(newItems);
    }

    @SuppressWarnings("unchecked")
    private void updateInBackground(List<Track> newItems){
        if (mPendingUpdates != null && mPendingUpdates.size() > 1) {
            return;
        }

        sDiffExecutor = new DiffExecutor(new AsyncOperation<Void, DiffResults<Track>, Void, Void>() {
            @Override
            public void onAsyncOperationFinished(DiffResults<Track> result) {
                if (mPendingUpdates != null)
                    mPendingUpdates.remove();

                if (result.diffResult != null) {
                    result.diffResult.dispatchUpdatesTo(TrackAdapter.this);
                    mTrackList.clear();
                    mTrackList.addAll(result.list);

                    sDiffExecutor = null;
                    //Try to perform next latest setChecked.
                    if (mPendingUpdates != null && mPendingUpdates.size() > 0) {
                        updateInBackground(mPendingUpdates.peek());
                    }
                }
            }
        });
        sDiffExecutor.executeOnExecutor(Executors.newSingleThreadExecutor(), mTrackList, newItems);
    }
}



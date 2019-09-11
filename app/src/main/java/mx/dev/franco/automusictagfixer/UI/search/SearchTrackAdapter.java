package mx.dev.franco.automusictagfixer.UI.search;

import android.arch.lifecycle.Observer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.UI.AudioHolder;
import mx.dev.franco.automusictagfixer.covermanager.CoverManager;
import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.interfaces.Destructible;
import mx.dev.franco.automusictagfixer.modelsUI.main.AsyncLoaderCover;
import mx.dev.franco.automusictagfixer.modelsUI.main.DiffExecutor;
import mx.dev.franco.automusictagfixer.modelsUI.main.DiffResults;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.utilities.ServiceUtils;

public class SearchTrackAdapter extends RecyclerView.Adapter<FoundItemHolder> implements
        Destructible, Observer<List<Track>>,
        AsyncOperation<Void, DiffResults<Track>, Void, Void>{
    private static final String TAG = SearchTrackAdapter.class.getName();
    @Inject
    ServiceUtils serviceUtils;
    private List<Track> mTrackList = new ArrayList<>();
    private FoundItemHolder.ClickListener mListener;
    private List<AsyncLoaderCover> mAsyncTaskQueue =  new ArrayList<>();
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
        CoverManager.startFetchingCover(holder, track.getPath());
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
        reset();
        mAsyncTaskQueue = null;
        serviceUtils = null;
        mTrackList.clear();
        mTrackList = null;
        mListener = null;
        CoverManager.cancelAll();
    }

    public void reset() {
        if(mAsyncTaskQueue != null && mAsyncTaskQueue.size() > 0 ){
            for(AsyncLoaderCover asyncLoaderCover: mAsyncTaskQueue){
                if(asyncLoaderCover.getStatus() == AsyncTask.Status.RUNNING ||
                        asyncLoaderCover.getStatus() == AsyncTask.Status.PENDING)
                asyncLoaderCover.cancel(true);
            }
            mAsyncTaskQueue.clear();
        }
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
        sDiffExecutor = new DiffExecutor(this);
        sDiffExecutor.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mTrackList, newItems);

    }

    @Override
    public void onAsyncOperationStarted(Void params) {}

    @Override
    public void onAsyncOperationFinished(DiffResults<Track> result) {
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

    @Override
    public void onAsyncOperationCancelled(Void cancellation) {/*Do nothing*/}

    @Override
    public void onAsyncOperationError(Void error) {/*Do nothing*/}
}



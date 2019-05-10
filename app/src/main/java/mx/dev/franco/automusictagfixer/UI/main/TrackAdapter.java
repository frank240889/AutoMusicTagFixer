package mx.dev.franco.automusictagfixer.UI.main;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.covermanager.CoverManager;
import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.interfaces.Destructible;
import mx.dev.franco.automusictagfixer.modelsUI.main.DiffCallback;
import mx.dev.franco.automusictagfixer.modelsUI.main.DiffExecutor;
import mx.dev.franco.automusictagfixer.modelsUI.main.DiffResults;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.persistence.room.TrackState;
import mx.dev.franco.automusictagfixer.services.FixerTrackService;
import mx.dev.franco.automusictagfixer.utilities.ServiceUtils;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class TrackAdapter extends RecyclerView.Adapter<AudioItemHolder> implements
        Destructible,
        AsyncOperation<Void, DiffResults<Track>, Void, Void> {

    /**
     * Interface to communicate when the list is in process
     * of sorting.
     */
    public interface OnSortingListener{
        void onStartSorting();
        void onFinishSorting();
    }
    //Constants for indicate the sort order
    public static final int ASC = 0;
    public static final int DESC = 1;
    private static final String TAG = TrackAdapter.class.getName();
    @Inject
    public ServiceUtils serviceUtils;
    @Inject
    public AbstractSharedPreferences sharedPreferences;
    private List<Track> mTrackList = new ArrayList<>();
    private AudioItemHolder.ClickListener mListener;
    private OnSortingListener mOnSortingListener;
    private Deque<List<Track>> mPendingUpdates = new ArrayDeque<>();
    private static DiffExecutor sDiffExecutor;
    private RecyclerView mRecyclerView;

    public TrackAdapter(){}
    public TrackAdapter(AudioItemHolder.ClickListener listener){
        this();
        mListener = listener;
        AutoMusicTagFixer.getContextComponent().inject(this);
        if(listener instanceof OnSortingListener)
            mOnSortingListener = (OnSortingListener) listener;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        mRecyclerView = null;
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



                if (key.equals("should_reload_cover")){
                    enqueue(holder, track);
                }

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
        CoverManager.startFetchingCover(holder, track.getPath());
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
        mOnSortingListener = null;
        CoverManager.cancelAll();
    }

    public void onChanged(@Nullable List<Track> tracks) {
        if(tracks != null) {
            //Update only if exist items
            if (getItemCount() > 0) {
                boolean dispatchListener = sharedPreferences.getBoolean("sorting")
                        && mOnSortingListener != null;
                if(dispatchListener){
                    mTrackList = tracks;
                    notifyDataSetChanged();
                    mOnSortingListener.onFinishSorting();
                }
                else {
                    if(tracks.size() > 255) {
                        if (mPendingUpdates != null) {
                            mPendingUpdates.push(tracks);
                        }
                        updateInBackground(tracks);

                    }
                    else {
                        updateInUIThread(tracks);
                    }
                }
            } else {
                mTrackList = tracks;
                notifyDataSetChanged();
            }
        }
    }

    public Track getTrackById(int id){
        if(getItemCount() > 0){
            for(Track track: mTrackList){
                if(track.getMediaStoreId() == id)
                    return track;
            }
        }

        return null;

    }

    private void updateInUIThread(List<Track> newItems) {
        DiffUtil.DiffResult diffResult = DiffUtil.
                calculateDiff(new DiffCallback(newItems, mTrackList),false);
        diffResult.dispatchUpdatesTo(this);
        mTrackList.clear();
        mTrackList.addAll(newItems);
    }

    private void updateInBackground(List<Track> newItems){
        if (mPendingUpdates != null && mPendingUpdates.size() > 1) {
            return;
        }

        sDiffExecutor = new DiffExecutor(this);
        sDiffExecutor.executeOnExecutor(Executors.newSingleThreadExecutor(), mTrackList, newItems);

    }

    @Override
    public void onAsyncOperationStarted(Void params) {
        if(sharedPreferences.getBoolean("sorting") && mOnSortingListener != null)
            mOnSortingListener.onStartSorting();
    }

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

    public void scrollToPosition(int id) {
        if(id == -1)
            return;

        Track track = getTrackById(id);
        int position = mTrackList.indexOf(track);
        mRecyclerView.scrollToPosition(position);
    }
}



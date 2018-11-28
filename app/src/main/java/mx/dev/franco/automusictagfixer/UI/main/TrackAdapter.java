package mx.dev.franco.automusictagfixer.UI.main;

import android.arch.lifecycle.Observer;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.interfaces.Destructible;
import mx.dev.franco.automusictagfixer.modelsUI.main.AsyncLoaderCover;
import mx.dev.franco.automusictagfixer.modelsUI.main.DiffExecutor;
import mx.dev.franco.automusictagfixer.modelsUI.main.DiffResults;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.persistence.room.TrackState;
import mx.dev.franco.automusictagfixer.services.FixerTrackService;
import mx.dev.franco.automusictagfixer.utilities.GlideApp;
import mx.dev.franco.automusictagfixer.utilities.ServiceUtils;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class TrackAdapter extends RecyclerView.Adapter<AudioItemHolder> implements
        Destructible,
        Observer<List<Track>>,
        AsyncOperation<Void, DiffResults<Track>, Void, Void> {

    public interface OnSortingListener{
        void onStartSorting();
        void onFinishSorting();
    }
    //constants for indicate the sort order
    public static final int ASC = 0;
    public static final int DESC = 1;
    private static final String TAG = TrackAdapter.class.getName();
    @Inject
    public Context context;
    @Inject
    public ServiceUtils serviceUtils;
    @Inject
    public AbstractSharedPreferences sharedPreferences;
    private List<Track> mTrackList = new ArrayList<>();
    private AudioItemHolder.ClickListener mListener;
    private OnSortingListener mOnSortingListener;
    private List<AsyncLoaderCover> mAsyncTaskQueue =  new ArrayList<>();
    private Deque<List<Track>> mPendingUpdates = new ArrayDeque<>();
    private static DiffExecutor sDiffExecutor;

    public TrackAdapter(AudioItemHolder.ClickListener listener){
        mListener = listener;
        AutoMusicTagFixer.getContextComponent().inject(this);
        if(listener instanceof OnSortingListener)
            mOnSortingListener = (OnSortingListener) listener;
    }


    @Override
    public AudioItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.item_list, parent, false);
        Log.d(TAG, "Creating viewHolder");
        return new AudioItemHolder(itemView, mListener);
    }

    @Override
    public void onBindViewHolder(@NonNull final AudioItemHolder holder, int position,
                                 List<Object> payloads) {
        Track track = mTrackList.get(position);
        if(payloads.isEmpty()) {
            super.onBindViewHolder(holder,position,payloads);
        }
        else {
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
                    holder.checkBox.setVisibility(GONE);
                    if (key.equals("processing")) {
                        if (track.processing() == 1) {
                            holder.progressBar.setVisibility(VISIBLE);
                        } else {
                            holder.progressBar.setVisibility(GONE);
                        }
                    }
                }



                if (key.equals("should_reload_cover")){
                    //We need to extracts cover arts in other thread,
                    //because this operation is going to reduce performance
                    //in main thread, making the scroll very laggy
                    if (mAsyncTaskQueue.size() < 9) {
                        final AsyncLoaderCover asyncLoaderCover = new AsyncLoaderCover();
                        mAsyncTaskQueue.add(asyncLoaderCover);
                        asyncLoaderCover.setListener(
                                new AsyncOperation<Void, byte[], byte[], Void>() {
                            @Override
                            public void onAsyncOperationStarted(Void params) {

                            }

                            @Override
                            public void onAsyncOperationFinished(byte[] result) {
                                if (holder.itemView.getContext() != null) {
                                    try {
                                        GlideApp.with(context).
                                                load(result)
                                                .thumbnail(0.5f)
                                                .error(R.drawable.ic_album_white_48px)
                                                .apply(RequestOptions.diskCacheStrategyOf(
                                                        DiskCacheStrategy.NONE))
                                                .apply(RequestOptions.skipMemoryCacheOf(true))
                                                .transition(DrawableTransitionOptions.withCrossFade(150))
                                                .fitCenter()
                                                .listener(new RequestListener<Drawable>() {
                                                    @Override
                                                    public boolean onLoadFailed(@Nullable GlideException e,
                                                                                Object model,
                                                                                Target<Drawable> target,
                                                                                boolean isFirstResource) {
                                                        if (mAsyncTaskQueue != null)
                                                            mAsyncTaskQueue.remove(asyncLoaderCover);
                                                        return false;
                                                    }

                                                    @Override
                                                    public boolean onResourceReady(Drawable resource,
                                                                                   Object model,
                                                                                   Target<Drawable> target,
                                                                                   DataSource dataSource,
                                                                                   boolean isFirstResource) {
                                                        if (mAsyncTaskQueue != null)
                                                            mAsyncTaskQueue.remove(asyncLoaderCover);
                                                        return false;
                                                    }
                                                })
                                                .placeholder(R.drawable.ic_album_white_48px)
                                                .into(holder.cover);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                if (mAsyncTaskQueue != null)
                                    mAsyncTaskQueue.remove(asyncLoaderCover);
                            }

                            @Override
                            public void onAsyncOperationCancelled(byte[] cancellation) {
                                if (mAsyncTaskQueue != null)
                                    mAsyncTaskQueue.remove(asyncLoaderCover);
                            }

                            @Override
                            public void onAsyncOperationError(Void error) {
                                if (mAsyncTaskQueue != null)
                                    mAsyncTaskQueue.remove(asyncLoaderCover);
                            }
                        });
                        asyncLoaderCover.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, track.getPath());
                    }
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

    @Override
    public void onBindViewHolder(final AudioItemHolder holder, final int position) {
        Track track = mTrackList.get(position);
        if(!serviceUtils.checkIfServiceIsRunning(FixerTrackService.CLASS_NAME)) {
            holder.checkBox.setVisibility(VISIBLE);
            holder.progressBar.setVisibility(GONE);
        }
        else {
            holder.checkBox.setVisibility(GONE);
            if (track.processing() == 1) {
                holder.progressBar.setVisibility(View.VISIBLE);
            } else {
                holder.progressBar.setVisibility(View.GONE);
            }
        }
        if(mAsyncTaskQueue.size() < 9){
            final AsyncLoaderCover asyncLoaderCover = new AsyncLoaderCover();
            mAsyncTaskQueue.add(asyncLoaderCover);
            asyncLoaderCover.setListener(new AsyncOperation<Void, byte[], byte[], Void>() {
                @Override
                public void onAsyncOperationStarted(Void params) {
                    holder.cover.setImageDrawable(holder.
                            itemView.
                            getContext().
                            getResources().
                            getDrawable(R.drawable.ic_album_white_48px));
                }

                @Override
                public void onAsyncOperationFinished(byte[] result) {
                    if (holder.itemView.getContext() != null) {
                        try {
                            GlideApp.with(holder.itemView.getContext()).
                                    load(result)
                                    .thumbnail(0.5f)
                                    .error(R.drawable.ic_album_white_48px)
                                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.AUTOMATIC))
                                    .apply(RequestOptions.skipMemoryCacheOf(false))
                                    .transition(DrawableTransitionOptions.withCrossFade(150))
                                    .fitCenter()
                                    .listener(new RequestListener<Drawable>() {
                                        @Override
                                        public boolean onLoadFailed(@Nullable GlideException e,
                                                                    Object model,
                                                                    Target<Drawable> target,
                                                                    boolean isFirstResource) {
                                            if (mAsyncTaskQueue != null)
                                                mAsyncTaskQueue.remove(asyncLoaderCover);
                                            return false;
                                        }

                                        @Override
                                        public boolean onResourceReady(Drawable resource,
                                                                       Object model,
                                                                       Target<Drawable> target,
                                                                       DataSource dataSource,
                                                                       boolean isFirstResource) {
                                            if (mAsyncTaskQueue != null)
                                                mAsyncTaskQueue.remove(asyncLoaderCover);
                                            return false;
                                        }
                                    })
                                    .placeholder(R.drawable.ic_album_white_48px)
                                    .into(holder.cover);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (mAsyncTaskQueue != null)
                        mAsyncTaskQueue.remove(asyncLoaderCover);
                }

                @Override
                public void onAsyncOperationCancelled(byte[] cancellation) {
                    if (mAsyncTaskQueue != null)
                        mAsyncTaskQueue.remove(asyncLoaderCover);
                }

                @Override
                public void onAsyncOperationError(Void error) {
                    if (mAsyncTaskQueue != null)
                        mAsyncTaskQueue.remove(asyncLoaderCover);
                }
            });
            asyncLoaderCover.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, track.getPath());
        }

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

        holder.trackName.setText(track.getTitle());
        holder.artistName.setText(track.getArtist());
        holder.albumName.setText(track.getAlbum());

    }

    @Override
    public void onViewRecycled(AudioItemHolder holder) {
        if(holder.itemView.getContext() != null)
            Glide.with(holder.itemView.getContext()).clear(holder.cover);
    }

    @Override
    public boolean onFailedToRecycleView(@NonNull AudioItemHolder holder) {
        if(holder.itemView.getContext() != null)
            Glide.with(holder.itemView.getContext()).clear(holder.cover);
        return true;
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

    /**
     * Indicates whether each item in the data set
     * can be represented with a unique identifier of type Long.
     * @param hasStableIds
     */
    @Override
    public void setHasStableIds(boolean hasStableIds) {
        super.setHasStableIds(true);
    }

    public List<Track> getDatasource(){
        return mTrackList;
    }


    @Override
    public void destroy() {
        clearLoads();
        mAsyncTaskQueue = null;
        if(sDiffExecutor != null && (sDiffExecutor.getStatus() == AsyncTask.Status.PENDING ||
                sDiffExecutor.getStatus() == AsyncTask.Status.RUNNING)){
            sDiffExecutor.cancel(true);
        }
        sDiffExecutor = null;
        mListener = null;
        mOnSortingListener = null;
    }

    @Override
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
                    if(mPendingUpdates != null) {
                        mPendingUpdates.push(tracks);
                    }
                    updateInBackground(tracks);
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

    private void updateInBackground(List<Track> newItems){
        if (mPendingUpdates != null && mPendingUpdates.size() > 1) {
            return;
        }

        sDiffExecutor = new DiffExecutor(this);
        sDiffExecutor.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mTrackList, newItems);

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
            //Try to perform next latest update.
            if (mPendingUpdates != null && mPendingUpdates.size() > 0) {
                updateInBackground(mPendingUpdates.peek());
            }
        }
    }

    @Override
    public void onAsyncOperationCancelled(Void cancellation) {/*Do nothing*/}

    @Override
    public void onAsyncOperationError(Void error) {/*Do nothing*/}

    private void clearLoads() {
        if(mAsyncTaskQueue != null && mAsyncTaskQueue.size() > 0 ){
            for(AsyncLoaderCover asyncLoaderCover: mAsyncTaskQueue){
                if(asyncLoaderCover.getStatus() == AsyncTask.Status.PENDING ||
                        asyncLoaderCover.getStatus() == AsyncTask.Status.RUNNING)
                    asyncLoaderCover.cancel(true);
            }

            mAsyncTaskQueue.clear();
        }
    }
}



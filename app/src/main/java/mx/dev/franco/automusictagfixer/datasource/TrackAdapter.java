package mx.dev.franco.automusictagfixer.datasource;

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
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.UI.main.ListFragment;
import mx.dev.franco.automusictagfixer.datasource.cover_loader.AsyncLoaderCover;
import mx.dev.franco.automusictagfixer.datasource.cover_loader.CoverLoaderListener;
import mx.dev.franco.automusictagfixer.interfaces.Destructible;
import mx.dev.franco.automusictagfixer.room.Track;
import mx.dev.franco.automusictagfixer.room.TrackState;
import mx.dev.franco.automusictagfixer.services.FixerTrackService;
import mx.dev.franco.automusictagfixer.services.ServiceHelper;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.GlideApp;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class TrackAdapter extends RecyclerView.Adapter<mx.dev.franco.automusictagfixer.datasource.AudioItemHolder> implements
        Destructible,
        Observer<List<Track>>,
        DiffExecutor.DiffCallbackListener{

    //constants for indicate the sort order
    public static final int ASC = 0;
    public static final int DESC = 1;
    private static final String TAG = TrackAdapter.class.getName();
    @Inject
    Context context;
    @Inject
    ServiceHelper serviceHelper;
    @Inject
    AbstractSharedPreferences sharedPreferences;
    private List<Track> mTrackList = new ArrayList<>();
    private AudioItemHolder.ClickListener mListener;
    private List<AsyncLoaderCover> mAsyncTaskQueue =  new ArrayList<>();
    private Deque<List<Track>> mPendingUpdates = new ArrayDeque<>();
    private static DiffExecutor sDiffExecutor;

    public TrackAdapter(AudioItemHolder.ClickListener listener){
        mListener = listener;
        AutoMusicTagFixer.getContextComponent().inject(this);
        Log.d(TAG, "context is null: "+ (context == null));
    }


    @Override
    public mx.dev.franco.automusictagfixer.datasource.AudioItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list, parent, false);

        return new mx.dev.franco.automusictagfixer.datasource.AudioItemHolder(itemView, mListener);
    }

    @Override
    public void onBindViewHolder(@NonNull final AudioItemHolder holder, int position, List<Object> payloads) {
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
                if (key.equals("checked")) {
                    holder.checkBox.setChecked(track.checked() == 1);
                }

                if (key.equals("processing")) {
                    if (track.processing() == 1) {
                        holder.progressBar.setVisibility(VISIBLE);
                        holder.checkBox.setVisibility(View.INVISIBLE);
                    } else {
                        holder.progressBar.setVisibility(GONE);
                        holder.checkBox.setVisibility(VISIBLE);
                    }
                }

                if (key.equals("should_reload_cover")){
                    //We need to extracts cover arts in other thread,
                    //because this operation is going to reduce performance
                    //in main thread, making the scroll very laggy
                    Log.d(TAG, "should_reload_cover");
                    Log.d(TAG, "size asynctaskqueue: " + mAsyncTaskQueue.size());
                    if (mAsyncTaskQueue.size() < 9) {
                        final AsyncLoaderCover asyncLoaderCover = new AsyncLoaderCover();
                        mAsyncTaskQueue.add(asyncLoaderCover);
                        asyncLoaderCover.setListener(new CoverLoaderListener() {
                            @Override
                            public void onLoadingStart() {
                                Log.d(TAG, "on start should_reload_cover");
                            }

                            @Override
                            public void onLoadingFinished(byte[] cover) {
                                Log.d(TAG, "on finish should_reload_cover");
                                if (holder.itemView.getContext() != null) {
                                    try {
                                        Log.d(TAG, "on finish should_reload_cover2");
                                        GlideApp.with(context).
                                                load(cover)
                                                .thumbnail(0.5f)
                                                .error(R.drawable.ic_album_white_48px)
                                                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                                                .apply(RequestOptions.skipMemoryCacheOf(true))
                                                .transition(DrawableTransitionOptions.withCrossFade(150))
                                                .fitCenter()
                                                .listener(new RequestListener<Drawable>() {
                                                    @Override
                                                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                                        Log.d(TAG, "on finish should_reload_cover3");
                                                        if (mAsyncTaskQueue != null)
                                                            mAsyncTaskQueue.remove(asyncLoaderCover);
                                                        return false;
                                                    }

                                                    @Override
                                                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                                        Log.d(TAG, "on finish should_reload_cover4");
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
                            public void onLoadingCancelled() {
                                if (mAsyncTaskQueue != null)
                                    mAsyncTaskQueue.remove(asyncLoaderCover);
                            }

                            @Override
                            public void onLoadingError(String error) {
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
    public void onBindViewHolder(final mx.dev.franco.automusictagfixer.datasource.AudioItemHolder holder, final int position) {
        Track track = mTrackList.get(position);
        if (track.processing() == 1) {
            holder.checkBox.setVisibility(View.GONE);
            holder.progressBar.setVisibility(View.VISIBLE);
        } else {
            holder.progressBar.setVisibility(View.GONE);
            holder.checkBox.setChecked(track.checked() == 1);
        }

        if(mAsyncTaskQueue.size() < 9){
            final AsyncLoaderCover asyncLoaderCover = new AsyncLoaderCover();
            mAsyncTaskQueue.add(asyncLoaderCover);
            asyncLoaderCover.setListener(new CoverLoaderListener() {
                @Override
                public void onLoadingStart() {
                    holder.cover.setImageDrawable(holder.
                            itemView.
                            getContext().
                            getResources().
                            getDrawable(R.drawable.ic_album_white_48px));
                }

                @Override
                public void onLoadingFinished(byte[] cover) {
                    if (holder.itemView.getContext() != null) {
                        try {
                            GlideApp.with(holder.itemView.getContext()).
                                    load(cover)
                                    .thumbnail(0.5f)
                                    .error(R.drawable.ic_album_white_48px)
                                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
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
                public void onLoadingCancelled() {
                    if (mAsyncTaskQueue != null)
                        mAsyncTaskQueue.remove(asyncLoaderCover);
                }

                @Override
                public void onLoadingError(String error) {
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
    public void onViewRecycled(mx.dev.franco.automusictagfixer.datasource.AudioItemHolder holder) {
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
    }

    @Override
    public void onChanged(@Nullable List<Track> tracks) {
        if(tracks != null) {
            Log.d(TAG, tracks.size()+"");
            //Update only if exist items
            if (getItemCount() > 0) {
                updateInBackground(tracks);
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
        if(mPendingUpdates != null) {
            mPendingUpdates.push(newItems);
            Log.d(TAG, "Pushing new incoming list... pushed");
        }

        Log.d(TAG, "trying to execute pending updates");
        if (mPendingUpdates != null && mPendingUpdates.size() > 1) {
            Log.d(TAG, "not executing pending updates");
            return;
        }

        sDiffExecutor = new DiffExecutor(this);
        sDiffExecutor.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mTrackList, newItems);

    }

    @Override
    public void onStartDiff() {
        //Do nothing
        Log.d(TAG, "onStartDiff");
    }

    @Override
    public void onCancelledDiff() {
        //Do nothing
    }

    @Override
    public void onFinishedDiff(DiffExecutor.DiffResults diffResults) {
        Log.d(TAG, "onFinishedDiff");
        if (mPendingUpdates != null)
            mPendingUpdates.remove();

        if (diffResults.diffResult != null) {
            Log.d(TAG, "dispatching results... list" + diffResults.list.size());
            diffResults.diffResult.dispatchUpdatesTo(this);
            Log.d(TAG, "results dispatched.");
            Log.d(TAG, "clearing...");
            mTrackList.clear();
            Log.d(TAG, "cleared.");
            Log.d(TAG, "Adding all.");
            mTrackList.addAll(diffResults.list);
            Log.d(TAG, "Added all.");
            sDiffExecutor = null;

            //Try to perform next latest update.
            if (mPendingUpdates != null && mPendingUpdates.size() > 0) {
                updateInBackground(mPendingUpdates.peek());
            }
        }
    }

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



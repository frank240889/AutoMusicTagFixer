package mx.dev.franco.automusictagfixer.UI.search;

import android.arch.lifecycle.Observer;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
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
import mx.dev.franco.automusictagfixer.utilities.GlideApp;
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
        AutoMusicTagFixer.getContextComponent().inject(this);
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

        final AsyncLoaderCover asyncLoaderCover = new AsyncLoaderCover();
        if(mAsyncTaskQueue.size() < 8) {
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
        holder.trackName.setText(track.getTitle());
        holder.artistName.setText(track.getArtist());
        holder.albumName.setText(track.getAlbum());

    }

    @Override
    public void onBindViewHolder(@NonNull final FoundItemHolder holder, int position, List<Object> payloads) {
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

                if (key.equals("should_reload_cover")){
                    //We need to extracts cover arts in other thread,
                    //because this operation is going to reduce performance
                    //in main thread, making the scroll very laggy
                    if (mAsyncTaskQueue.size() < 9) {
                        final AsyncLoaderCover asyncLoaderCover = new AsyncLoaderCover();
                        mAsyncTaskQueue.add(asyncLoaderCover);
                        asyncLoaderCover.setListener(new AsyncOperation<Void, byte[], byte[], Void>() {
                            @Override
                            public void onAsyncOperationStarted(Void params) {}
                            @Override
                            public void onAsyncOperationFinished(byte[] result) {
                                if (holder.itemView.getContext() != null) {
                                    try {
                                        GlideApp.with(holder.itemView.getContext()).
                                                load(result)
                                                .thumbnail(0.5f)
                                                .error(R.drawable.ic_album_white_48px)
                                                .apply(RequestOptions.
                                                        diskCacheStrategyOf(DiskCacheStrategy.NONE))
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
                        asyncLoaderCover.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                                track.getPath());
                    }
                }
            }
        }
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
}



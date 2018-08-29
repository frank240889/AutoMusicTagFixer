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
    private static Sorter sSorter = Sorter.getInstance();
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
                if(key.equals("checked")){
                    holder.checkBox.setChecked(track.checked() == 1);
                }

                if(key.equals("state")){
                    switch (track.getState()) {
                        case TrackState.TAGS_CORRECTED_BY_SEMIAUTOMATIC_MODE:
                        case TrackState.ALL_TAGS_FOUND:
                        case TrackState.TAGS_EDITED_BY_USER:
                            holder.stateMark.setImageResource(R.drawable.ic_done_all_white);
                            holder.stateMark.setVisibility(VISIBLE);
                            break;
                        case TrackState.ALL_TAGS_NOT_FOUND:
                            holder.stateMark.setImageResource(R.drawable.ic_done_white);
                            holder.stateMark.setVisibility(VISIBLE);
                            break;
                        case TrackState.NO_TAGS_FOUND:
                            holder.stateMark.setImageResource(R.drawable.ic_error_outline_white);
                            holder.stateMark.setVisibility(VISIBLE);
                            break;
                        case TrackState.FILE_ERROR_READ:
                        case TrackState.COULD_NOT_APPLIED_CHANGES:
                        case TrackState.COULD_RESTORE_FILE_TO_ITS_LOCATION:
                        case TrackState.COULD_NOT_CREATE_AUDIOFILE:
                        case TrackState.COULD_NOT_CREATE_TEMP_FILE:
                        case TrackState.FILE_IN_SD_WITHOUT_PERMISSION:
                            holder.stateMark.setImageResource(R.drawable.ic_highlight_off_white_material);
                            holder.stateMark.setVisibility(VISIBLE);
                            break;
                        default:
                            holder.stateMark.setImageResource(0);
                            holder.stateMark.setVisibility(GONE);
                            break;
                    }
                }

                if(key.equals("processing")){
                    boolean processing = o.getBoolean("processing");
                    if(processing){
                        holder.progressBar.setVisibility(VISIBLE);
                        holder.checkBox.setVisibility(View.INVISIBLE);
                    }
                    else {
                        holder.progressBar.setVisibility(GONE);
                        holder.checkBox.setVisibility(VISIBLE);
                    }
                    track.setProcessing(processing);
                }

                //We need to extracts cover arts in other thread,
                //because this operation is going to reduce performance
                //in main thread, making the scroll very laggy
                Log.d(TAG,"Reloading data" + key.equals("should_reload_cover"));
                if(key.equals("should_reload_cover")) {
                    Log.d(TAG,"should_reload_cover");
                    if(mAsyncTaskQueue.size() < 9) {
                        final AsyncLoaderCover asyncLoaderCover = new AsyncLoaderCover();
                        asyncLoaderCover.setListener(new CoverLoaderListener() {
                            @Override
                            public void onLoadingStart() {
                                Log.d(TAG, "on start should_reload_cover");
                            }

                            @Override
                            public void onLoadingFinished(byte[] cover) {
                                if (holder.itemView.getContext() != null && cover != null) {
                                    try {
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
                                                        if (mAsyncTaskQueue != null)
                                                            mAsyncTaskQueue.remove(asyncLoaderCover);
                                                        return false;
                                                    }

                                                    @Override
                                                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
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
            }
        }
    }

    @Override
    public void onBindViewHolder(final mx.dev.franco.automusictagfixer.datasource.AudioItemHolder holder, final int position) {
        Track track = mTrackList.get(position);
        if (track.isProcessing()) {
            holder.checkBox.setVisibility(View.GONE);
            holder.progressBar.setVisibility(View.VISIBLE);
        } else {
            holder.progressBar.setVisibility(View.GONE);
            holder.checkBox.setChecked(track.checked() == 1);
        }
        Log.d(TAG, "size asynctaskqueue: " + mAsyncTaskQueue.size());
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
                    if (holder.itemView.getContext() != null && cover != null) {
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
            case TrackState.TAGS_CORRECTED_BY_SEMIAUTOMATIC_MODE:
            case TrackState.ALL_TAGS_FOUND:
            case TrackState.TAGS_EDITED_BY_USER:
                holder.stateMark.setImageResource(R.drawable.ic_done_all_white);
                holder.stateMark.setVisibility(VISIBLE);
                break;
            case TrackState.ALL_TAGS_NOT_FOUND:
                holder.stateMark.setImageResource(R.drawable.ic_done_white);
                holder.stateMark.setVisibility(VISIBLE);
                break;
            case TrackState.NO_TAGS_FOUND:
                holder.stateMark.setImageResource(R.drawable.ic_error_outline_white);
                holder.stateMark.setVisibility(VISIBLE);
                break;
            case TrackState.FILE_ERROR_READ:
            case TrackState.COULD_NOT_APPLIED_CHANGES:
            case TrackState.COULD_RESTORE_FILE_TO_ITS_LOCATION:
            case TrackState.COULD_NOT_CREATE_AUDIOFILE:
            case TrackState.COULD_NOT_CREATE_TEMP_FILE:
            case TrackState.FILE_IN_SD_WITHOUT_PERMISSION:
                holder.stateMark.setImageResource(R.drawable.ic_highlight_off_white_material);
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
        System.gc();
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

    /**
     * Sort list in desired order
     * @param sortBy the field/column to sort by
     * @param sortType the sort type, may be ascendant or descendant
     */
    public boolean sortBy(String sortBy, int sortType){

        //if no songs, no case sort anything
        if(mTrackList.size() == 0 ){
            return false;
        }

        //wait for sorting while correction task is running
        if(serviceHelper.checkIfServiceIsRunning(FixerTrackService.class.getName())){
            return false;
        }

        sSorter.setSortParams(sortBy, sortType);
        Collections.sort(mTrackList, sSorter);
        notifyDataSetChanged();

        String order;
        if(sortType == ASC){
            order = " " + sortBy + " ASC ";
        }
        else {
            order = " " + sortBy + " DESC ";
        }

        sharedPreferences.putString(Constants.SORT_KEY,order);
        return true;
    }

    private void updateInBackground(List<Track> newItems){
        mPendingUpdates.push(newItems);
        Log.d(TAG, "trying to execute pending updates");
        if (mPendingUpdates.size() > 1) {
            Log.d(TAG, "not executing pending updates");
            return;
        }

        sDiffExecutor = new DiffExecutor(this);
        sDiffExecutor.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mTrackList, newItems);

    }

    @Override
    public void onStartDiff() {
        Log.d(TAG, "onStartDiff");
    }

    @Override
    public void onCancelledDiff() {

    }

    @Override
    public void onFinishedDiff(DiffExecutor.DiffResults diffResults) {
        Log.d(TAG, "onFinishedDiff");
        mPendingUpdates.remove();
        if (diffResults.diffResult != null) {
            Log.d(TAG, "dispatching results");
            diffResults.diffResult.dispatchUpdatesTo(this);
            //mTrackList.clear();
            mTrackList = diffResults.list;
            //mTrackList.addAll(diffResults.list);
            sDiffExecutor = null;

            //Try to perform next update.
            if (mPendingUpdates.size() > 0) {
                updateInBackground(mPendingUpdates.peek());
            }
        }
    }

    private void clearLoads() {
        if(mAsyncTaskQueue != null && mAsyncTaskQueue.size() > 0 ){
            for(AsyncLoaderCover asyncLoaderCover: mAsyncTaskQueue){
                if(asyncLoaderCover.getStatus() == AsyncTask.Status.PENDING || asyncLoaderCover.getStatus() == AsyncTask.Status.RUNNING)
                    asyncLoaderCover.cancel(true);
            }

            mAsyncTaskQueue.clear();
        }
    }
}



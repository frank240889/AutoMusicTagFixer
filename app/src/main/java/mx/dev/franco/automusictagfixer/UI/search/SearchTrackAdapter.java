package mx.dev.franco.automusictagfixer.UI.search;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.interfaces.CoverLoaderListener;
import mx.dev.franco.automusictagfixer.interfaces.Destructible;
import mx.dev.franco.automusictagfixer.modelsUI.main.AsyncLoaderCover;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.GlideApp;
import mx.dev.franco.automusictagfixer.utilities.ServiceUtils;

public class SearchTrackAdapter extends RecyclerView.Adapter<FoundItemHolder> implements Destructible{
    private static final String TAG = SearchTrackAdapter.class.getName();
    @Inject
    Context context;
    @Inject
    ServiceUtils serviceUtils;
    private List<Track> mTrackList = new ArrayList<>();
    private FoundItemHolder.ClickListener mListener;
    private List<AsyncLoaderCover> mAsyncTaskQueue =  new ArrayList<>();


    public SearchTrackAdapter(FoundItemHolder.ClickListener listener){
        mListener = listener;
        AutoMusicTagFixer.getContextComponent().inject(this);
        Log.d(TAG, "context is null: "+ (context == null));
    }


    @Override
    public FoundItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.found_item_list, parent, false);

        return new FoundItemHolder(itemView, mListener);
    }

    @Override
    public void onBindViewHolder(final FoundItemHolder holder, final int position) {
        Track track = mTrackList.get(position);

        final AsyncLoaderCover asyncLoaderCover = new AsyncLoaderCover();
        if(mAsyncTaskQueue.size() < 8) {
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
        holder.trackName.setText(track.getTitle());
        holder.artistName.setText(track.getArtist());
        holder.albumName.setText(track.getAlbum());

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
        context = null;
        mTrackList.clear();
        mTrackList = null;
        mListener = null;
    }

    public void swapData(List<Track> tracks){
        if(tracks.size() > 0){
            if(!mTrackList.isEmpty()) {
                int top = mTrackList.size();
                mTrackList.clear();
                notifyItemRangeRemoved(0, top);
            }
            mTrackList.addAll(tracks);
            notifyItemRangeInserted(0, mTrackList.size());

        }
        else {
            int top = mTrackList.size();
            mTrackList.clear();
            notifyItemRangeRemoved(0, top);
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

    public void updateTrack(Intent data) {
        Track track = getTrackById(data.getIntExtra(Constants.MEDIA_STORE_ID, -1));
        if(track != null){
            String title = data.getStringExtra("title");
            if(title != null && !title.equals(""))
                track.setTitle(title);

            String artist = data.getStringExtra("artist");
            if(artist != null && !artist.equals(""))
                track.setArtist(artist);

            String album = data.getStringExtra("album");
            if(album != null && !album.equals(""))
                track.setAlbum(album);

            String path = data.getStringExtra("path");
            if(path != null && !path.equals(""))
                track.setPath(path);

            int position = mTrackList.indexOf(track);
            notifyItemChanged(position);
        }
    }

    public void reset() {
        if(mAsyncTaskQueue != null && mAsyncTaskQueue.size() > 0 ){
            for(AsyncLoaderCover asyncLoaderCover: mAsyncTaskQueue){
                asyncLoaderCover.cancel(true);
            }
            mAsyncTaskQueue.clear();
        }
    }
}



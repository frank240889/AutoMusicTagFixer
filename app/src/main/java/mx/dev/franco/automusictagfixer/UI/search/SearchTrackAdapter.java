package mx.dev.franco.automusictagfixer.UI.search;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
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
import mx.dev.franco.automusictagfixer.datasource.cover_loader.AsyncLoaderCover;
import mx.dev.franco.automusictagfixer.datasource.cover_loader.CoverLoaderListener;
import mx.dev.franco.automusictagfixer.interfaces.Destructible;
import mx.dev.franco.automusictagfixer.room.Track;
import mx.dev.franco.automusictagfixer.services.ServiceHelper;
import mx.dev.franco.automusictagfixer.utilities.GlideApp;

public class SearchTrackAdapter extends RecyclerView.Adapter<FoundItemHolder> implements Destructible{
    private static final String TAG = SearchTrackAdapter.class.getName();
    @Inject
    Context context;
    @Inject
    ServiceHelper serviceHelper;
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
                if(holder.itemView.getContext() != null && cover != null) {
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
                                        if(mAsyncTaskQueue != null)
                                            mAsyncTaskQueue.remove(asyncLoaderCover);
                                        return false;
                                    }

                                    @Override
                                    public boolean onResourceReady(Drawable resource,
                                                                   Object model,
                                                                   Target<Drawable> target,
                                                                   DataSource dataSource,
                                                                   boolean isFirstResource) {
                                        if(mAsyncTaskQueue != null)
                                            mAsyncTaskQueue.remove(asyncLoaderCover);
                                        return false;
                                    }
                                })
                                .placeholder(R.drawable.ic_album_white_48px)
                                .into(holder.cover);
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }
                if(mAsyncTaskQueue != null)
                    mAsyncTaskQueue.remove(asyncLoaderCover);
            }

            @Override
            public void onLoadingCancelled() {
                if(mAsyncTaskQueue != null)
                    mAsyncTaskQueue.remove(asyncLoaderCover);
            }

            @Override
            public void onLoadingError(String error) {
                if(mAsyncTaskQueue != null)
                    mAsyncTaskQueue.remove(asyncLoaderCover);
            }
        });
        asyncLoaderCover.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, track.getPath());

        holder.trackName.setText(track.getTitle());
        holder.artistName.setText(track.getArtist());
        holder.albumName.setText(track.getAlbum());

    }

    @Override
    public void onViewRecycled(FoundItemHolder holder) {
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
        if(mAsyncTaskQueue != null && mAsyncTaskQueue.size() > 0 ){
            for(AsyncLoaderCover asyncLoaderCover: mAsyncTaskQueue){
                asyncLoaderCover.cancel(true);
            }

            mAsyncTaskQueue.clear();
        }

        mAsyncTaskQueue = null;

        mListener = null;
        System.gc();
    }

    public void swapData(List<Track> tracks){
        mTrackList = tracks;
        notifyDataSetChanged();
    }

}



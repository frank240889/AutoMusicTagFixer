package mx.dev.franco.automusictagfixer.ui.trackdetail;

import android.arch.lifecycle.Observer;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.gracenote.gnsdk.GnImageSize;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.identifier.Identifier;
import mx.dev.franco.automusictagfixer.identifier.Result;
import mx.dev.franco.automusictagfixer.utilities.GlideApp;

public class IdentificationResultsAdapter extends RecyclerView.Adapter<ResultItemHolder> implements Observer<List<Identifier.IdentificationResults>> {
    private List<Identifier.IdentificationResults> mIdentificationResults = new ArrayList<>();

    public IdentificationResultsAdapter(){}

    @NonNull
    @Override
    public ResultItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.result_item_list, parent, false);

        return new ResultItemHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResultItemHolder holder, int position) {
        Result result = (Result) mIdentificationResults.get(position);
        if(result.getTitle() != null && !result.getTitle().isEmpty()) {
            holder.title.setVisibility(View.VISIBLE);
            holder.title.setText(result.getTitle());
        }
        else {
            holder.title.setVisibility(View.GONE);
        }

        if(result.getArtist() != null && !result.getArtist().isEmpty()) {
            holder.artist.setText(result.getArtist());
            holder.artist.setVisibility(View.VISIBLE);
        }
        else {
            holder.artist.setVisibility(View.GONE);
        }

        if(result.getAlbum() != null && !result.getAlbum().isEmpty()) {
            holder.album.setText(result.getAlbum());
            holder.album.setVisibility(View.VISIBLE);
        }
        else {
            holder.album.setVisibility(View.GONE);
        }

        if(result.getGenre() != null && !result.getGenre().isEmpty()) {
            holder.genre.setText(result.getGenre());
            holder.genre.setVisibility(View.VISIBLE);
        }
        else {
            holder.genre.setVisibility(View.GONE);
        }

        if(result.getTrackYear() != null && !result.getTrackYear().isEmpty()) {
            holder.trackYear.setText(result.getTrackYear());
            holder.trackYear.setVisibility(View.VISIBLE);
        }
        else {
            holder.trackYear.setVisibility(View.GONE);
        }

        if(result.getTrackNumber() != null && !result.getTrackNumber().isEmpty()) {
            holder.trackNumber.setText(result.getTrackNumber());
            holder.trackNumber.setVisibility(View.VISIBLE);
        }
        else {
            holder.trackNumber.setVisibility(View.GONE);
        }

        String urlImage = getUrl(result.getCovers());
        if(urlImage != null) {
            GlideApp.with(holder.itemView.getContext()).
                    load(urlImage)
                    .thumbnail(0.5f)
                    .error(R.drawable.ic_album_white_48px)
                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.AUTOMATIC))
                    .apply(RequestOptions.skipMemoryCacheOf(false))
                    .transition(DrawableTransitionOptions.withCrossFade(150))
                    .fitCenter()
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            Log.d("failed", "failed");
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            Log.d("success", "success");
                            return false;
                        }
                    })
                    .placeholder(R.drawable.ic_album_white_48px)
                    .into(holder.cover);
        }
    }

    private String getUrl(Map<GnImageSize, String> covers) {
        Set<Map.Entry<GnImageSize, String>> entries = covers.entrySet();
        for(Map.Entry<GnImageSize, String> entry : entries) {
            if(entry.getValue() != null) {
                String url = entry.getValue();
                if(!url.contains("http")) {
                    url = "http://" + url;
                }
                return url;
            }
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return mIdentificationResults != null ? mIdentificationResults.size() : 0;
    }

    @Override
    public void onChanged(@Nullable List<Identifier.IdentificationResults> identificationResults) {
        mIdentificationResults.addAll(identificationResults);
        notifyDataSetChanged();
    }
}

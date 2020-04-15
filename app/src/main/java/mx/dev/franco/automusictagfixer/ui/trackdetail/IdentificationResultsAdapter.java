package mx.dev.franco.automusictagfixer.ui.trackdetail;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.util.ArrayList;
import java.util.List;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.identifier.Identifier;
import mx.dev.franco.automusictagfixer.identifier.Result;
import mx.dev.franco.automusictagfixer.utilities.GlideApp;

public class IdentificationResultsAdapter extends RecyclerView.Adapter<ResultItemHolder>
        implements Observer<List<? extends Identifier.IdentificationResults>> {
    private List<Identifier.IdentificationResults> mIdentificationResults = new ArrayList<>();

    public IdentificationResultsAdapter(){}

    @NonNull
    @Override
    public ResultItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.result_identification_item, parent, false);

        return new ResultItemHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResultItemHolder holder, int position) {
        Result result = (Result) mIdentificationResults.get(position);
        holder.progressBar.setVisibility(View.VISIBLE);

        GlideApp.with(holder.itemView.getContext()).
                load(result.getCoverArt() != null ? result.getCoverArt().getUrl() : null)
                .thumbnail(0.5f)
                .error(ContextCompat.getDrawable(holder.itemView.getContext(),R.drawable.ic_album_white_48px))
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                .apply(RequestOptions.skipMemoryCacheOf(false))
                .transition(DrawableTransitionOptions.withCrossFade(150))
                .fitCenter()
                .addListener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        holder.progressBar.setVisibility(View.GONE);
                        holder.imageDimensions.setText(R.string.no_cover);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        holder.progressBar.setVisibility(View.GONE);
                        holder.imageDimensions.setText(result.getCoverArt().getSize());
                        return false;
                    }
                })
                .placeholder(ContextCompat.getDrawable(holder.itemView.getContext(),R.drawable.ic_album_white_48px))
                .into(holder.cover);


        if (result.getTitle() != null && !result.getTitle().isEmpty()) {
            holder.title.setVisibility(View.VISIBLE);
            holder.title.setText(result.getTitle());
        }
        else {
            holder.title.setVisibility(View.GONE);
        }

        if (result.getArtist() != null && !result.getArtist().isEmpty()) {
            holder.artist.setText(result.getArtist());
            holder.artist.setVisibility(View.VISIBLE);
        }
        else {
            holder.artist.setVisibility(View.GONE);
        }

        if (result.getAlbum() != null && !result.getAlbum().isEmpty()) {
            holder.album.setText(result.getAlbum());
            holder.album.setVisibility(View.VISIBLE);
        }
        else {
            holder.album.setVisibility(View.GONE);
        }

        if (result.getGenre() != null && !result.getGenre().isEmpty()) {
            holder.genre.setText(result.getGenre());
            holder.genre.setVisibility(View.VISIBLE);
        }
        else {
            holder.genre.setVisibility(View.GONE);
        }

        if (result.getTrackYear() != null && !result.getTrackYear().isEmpty()) {
            holder.trackYear.setText(result.getTrackYear());
            holder.trackYear.setVisibility(View.VISIBLE);
        }
        else {
            holder.trackYear.setVisibility(View.GONE);
        }

        if (result.getTrackNumber() != null && !result.getTrackNumber().isEmpty()) {
            holder.trackNumber.setText(result.getTrackNumber());
            holder.trackNumber.setVisibility(View.VISIBLE);
        }
        else {
            holder.trackNumber.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return mIdentificationResults != null ? mIdentificationResults.size() : 0;
    }

    @Override
    public void onChanged(List<? extends Identifier.IdentificationResults> identificationResults) {
        mIdentificationResults.addAll(identificationResults);
        notifyDataSetChanged();
    }
}

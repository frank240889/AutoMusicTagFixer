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

public class CoverIdentificationResultsAdapter extends RecyclerView.Adapter<CoverResultItemHolder>
        implements Observer<List<? extends Identifier.IdentificationResults>> {
    private List<Identifier.IdentificationResults> mIdentificationResults = new ArrayList<>();

    public CoverIdentificationResultsAdapter(){}

    @NonNull
    @Override
    public CoverResultItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.cover_result_identification_item, parent, false);

        return new CoverResultItemHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CoverResultItemHolder holder, int position) {
        Result result = (Result) mIdentificationResults.get(position);
        holder.progressBar.setVisibility(View.VISIBLE);
        GlideApp.with(holder.itemView.getContext()).
                load(result.getCoverArt() != null ? result.getCoverArt().getUrl() : null)
                .thumbnail(0.5f)
                .error(ContextCompat.getDrawable(holder.itemView.getContext(),R.drawable.ic_album_white_48px))
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.AUTOMATIC))
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

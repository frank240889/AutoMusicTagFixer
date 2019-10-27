package mx.dev.franco.automusictagfixer.ui.trackdetail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.identifier.CoverIdentificationResult;
import mx.dev.franco.automusictagfixer.identifier.Identifier;
import mx.dev.franco.automusictagfixer.utilities.GlideApp;

public class CoverIdentificationResultsAdapter extends RecyclerView.Adapter<CoverResultItemHolder>
        implements Observer<List<? extends Identifier.IdentificationResults>> {
    private List<Identifier.IdentificationResults> mIdentificationResults = new ArrayList<>();

    public CoverIdentificationResultsAdapter(){}

    @NonNull
    @Override
    public CoverResultItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.cover_result_item_list, parent, false);

        return new CoverResultItemHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CoverResultItemHolder holder, int position) {
        CoverIdentificationResult result = (CoverIdentificationResult) mIdentificationResults.get(position);
        holder.imageDimensions.setText(result.getSize());
        GlideApp.with(holder.itemView.getContext()).
                load(result.getCover())
                .thumbnail(0.5f)
                .error(R.drawable.ic_album_white_48px)
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.AUTOMATIC))
                .apply(RequestOptions.skipMemoryCacheOf(false))
                .transition(DrawableTransitionOptions.withCrossFade(150))
                .fitCenter()
                .placeholder(R.drawable.ic_album_white_48px)
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

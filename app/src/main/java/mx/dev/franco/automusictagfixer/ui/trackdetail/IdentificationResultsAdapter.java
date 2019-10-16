package mx.dev.franco.automusictagfixer.ui.trackdetail;

import android.arch.lifecycle.Observer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
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
                inflate(R.layout.layout_results_track_id, parent, false);

        return new ResultItemHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResultItemHolder holder, int position) {
        Result result = (Result) mIdentificationResults.get(position);
        holder.title.setText(result.getTitle());
        holder.artist.setText(result.getArtist());
        holder.album.setText(result.getAlbum());
        holder.genre.setText(result.getGenre());
        holder.trackYear.setText(result.getTrackYear());
        holder.trackNumber.setText(result.getTrackNumber());
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
                    .placeholder(R.drawable.ic_album_white_48px)
                    .into(holder.cover);
        }
    }

    private String getUrl(Map<GnImageSize, String> covers) {
        Set<Map.Entry<GnImageSize, String>> entries = covers.entrySet();
        for(Map.Entry<GnImageSize, String> entry : entries) {
            if(entry.getValue() != null)
                return entry.getValue();
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

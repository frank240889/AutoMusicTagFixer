package mx.dev.franco.automusictagfixer.ui.trackdetail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.identifier.Identifier;
import mx.dev.franco.automusictagfixer.identifier.TrackIdentificationResult;

public class IdentificationResultsAdapter extends RecyclerView.Adapter<ResultItemHolder>
        implements Observer<List<? extends Identifier.IdentificationResults>> {
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
        TrackIdentificationResult result = (TrackIdentificationResult) mIdentificationResults.get(position);
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

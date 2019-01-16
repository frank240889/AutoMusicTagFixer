package mx.dev.franco.automusictagfixer.UI.search;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import mx.dev.franco.automusictagfixer.R;

/**
 * This class helps to maintain the reference to
 * every element of item, avoiding call findViewById()
 * in every element for data source, making a considerable
 * improvement in performance of list
 */
public class FoundItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    public interface ClickListener{
        void onItemClick(int position, View view);
    }

    public TextView trackName;
    public TextView artistName;
    public TextView albumName;
    public ImageView cover;
    public ClickListener mListener;

    public FoundItemHolder(View itemView, ClickListener clickListener) {
        super(itemView);
        cover =  itemView.findViewById(R.id.found_cover_art);
        trackName = itemView.findViewById(R.id.found_track_name);
        artistName = itemView.findViewById(R.id.found_artist_name);
        albumName = itemView.findViewById(R.id.found_album_name);
        mListener = clickListener;
        cover.setOnClickListener(this);
        itemView.setOnClickListener(this);
    }

    /**
     * This method of mListener is implemented in
     * activity that creates the adapter and data source
     * @param v
     */
    @Override
    public void onClick(View v) {
        if (mListener != null) {
            mListener.onItemClick(getLayoutPosition(), v);
        }
    }

}

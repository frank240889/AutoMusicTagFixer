package mx.dev.franco.automusictagfixer.ui.main;

import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.ui.AudioHolder;

/**
 * @author Franco Castillo
 */
public class AudioItemHolder extends AudioHolder implements View.OnClickListener {
    public interface ClickListener{
        void onCoverClick(int position, View view);
        void onCheckboxClick(int position);
        void onItemClick(int position, View view);
    }


    public CheckBox checkBox;
    public TextView trackName;
    public TextView artistName;
    public TextView albumName;
    public ImageButton stateMark;
    public ProgressBar progressBar;
    public ClickListener mListener;

    public AudioItemHolder(View itemView, ClickListener clickListener) {
        super(itemView);
        checkBox = itemView.findViewById(R.id.cb_item_checkable_state);
        checkBox.setChecked(false);
        trackName = itemView.findViewById(R.id.tv_item_track_name);
        //artistName = itemView.findViewById(R.id.artist_name);
        //albumName = itemView.findViewById(R.id.album_name);
        progressBar =  itemView.findViewById(R.id.pb_item_track_progress_correction);
        cover = itemView.findViewById(R.id.iv_cover_art);
        //stateMark = itemView.findViewById(R.id.status_mark);
        mListener = clickListener;
        checkBox.setOnClickListener(this);
        cover.setOnClickListener(this);
        itemView.setOnClickListener(this);
    }

    /**
     * This method of listener is implemented in
     * the host that creates the adapter and data source
     * @param v The view clicked.
     */
    @Override
    public void onClick(View v) {
        if (mListener != null) {
            switch (v.getId()){
                case R.id.cb_item_checkable_state:
                        mListener.onCheckboxClick(getAdapterPosition());
                    break;
                case R.id.iv_cover_art:
                        mListener.onCoverClick(getAdapterPosition(), v);
                    break;
                default:
                        mListener.onItemClick(getAdapterPosition(), cover);
                    break;
            }
        }
    }
}

package mx.dev.franco.automusictagfixer.ui;

import android.view.View;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

public abstract class AudioHolder extends RecyclerView.ViewHolder {
    public ImageView cover;
    public AudioHolder(View itemView) {
        super(itemView);
    }
}

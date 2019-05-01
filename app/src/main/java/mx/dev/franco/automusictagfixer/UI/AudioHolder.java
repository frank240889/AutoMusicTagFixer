package mx.dev.franco.automusictagfixer.UI;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

public abstract class AudioHolder extends RecyclerView.ViewHolder {
    public ImageView cover;
    public AudioHolder(View itemView) {
        super(itemView);
    }
}

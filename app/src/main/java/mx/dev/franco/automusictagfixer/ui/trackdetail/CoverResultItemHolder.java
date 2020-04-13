package mx.dev.franco.automusictagfixer.ui.trackdetail;

import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.ui.AudioHolder;

public class CoverResultItemHolder extends AudioHolder {
    public TextView imageDimensions;
    public ProgressBar progressBar;
    public CoverResultItemHolder(View itemView) {
        super(itemView);
        cover = itemView.findViewById(R.id.trackid_cover);
        imageDimensions = itemView.findViewById(R.id.trackid_cover_dimensions);
        progressBar = itemView.findViewById(R.id.preview_progress);
    }
}

package mx.dev.franco.automusictagfixer.UI.track_detail;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.list.AudioItem;
import mx.dev.franco.automusictagfixer.services.gnservice.GnResponseListener;
import mx.dev.franco.automusictagfixer.utilities.GlideApp;

public class ResultsDialog extends AlertDialog {
    private GnResponseListener.IdentificationResults mResults;
    protected ResultsDialog(Context context, GnResponseListener.IdentificationResults results){
        this(context);
        mResults = results;
    }

    protected ResultsDialog(@NonNull Context context) {
        super(context);
    }

    protected ResultsDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    protected ResultsDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.layout_results_track_id);

        TextView coverDimensions = findViewById(R.id.trackid_cover_dimensions);
        coverDimensions.setText(AudioItem.getStringImageSize(mResults.cover)) ;
        TextView title = findViewById(R.id.track_id_title);
        title.setText(mResults.title);
        TextView artist = findViewById(R.id.track_id_artist);
        artist.setText(mResults.artist);
        TextView album = findViewById(R.id.trackid_album);
        album.setText(mResults.album);
        TextView genre = findViewById(R.id.trackid_genre);
        genre.setText(mResults.genre);
        TextView trackNumber = findViewById(R.id.track_id_number);
        trackNumber.setText(mResults.trackNumber);
        TextView year = findViewById(R.id.track_id_year);
        year.setText(mResults.trackYear);
        ImageView cover = findViewById(R.id.trackid_cover);
        GlideApp.with(getContext()).
                load(mResults.cover).
                diskCacheStrategy(DiskCacheStrategy.NONE).
                skipMemoryCache(true).
                //apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE)).
                //apply(RequestOptions.skipMemoryCacheOf(true)).
                transition(DrawableTransitionOptions.withCrossFade(200)).
                fitCenter().
                into(cover);
    }


    public static class Builder{
        public Builder(){

        }
    }
}

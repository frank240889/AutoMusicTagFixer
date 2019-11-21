package mx.dev.franco.automusictagfixer.utilities;

import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.databinding.BindingAdapter;
import androidx.lifecycle.MutableLiveData;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import mx.dev.franco.automusictagfixer.R;

public class BindingUtils {
    @BindingAdapter({"imageCover"})
    public static void loadImage(ImageView view, MutableLiveData<byte[]> cover) {
        GlideApp.with(view)
                .asBitmap()
                .load(cover.getValue())
                .error(view.getContext().getDrawable(R.drawable.ic_album_white_48px))
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                .theme(view.getContext().getTheme())
                .apply(RequestOptions.skipMemoryCacheOf(true))
                .listener(new RequestListener<Bitmap>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                        ((View)view.getParent().getParent()).setBackgroundColor(ContextCompat.getColor(view.getContext(),
                                R.color.primaryColor));
                        return false;
                    }
                    @Override
                    public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                        if (resource != null) {
                            Palette.from(resource).generate(new Palette.PaletteAsyncListener() {
                                int colorPalette;
                                @Override
                                public void onGenerated(@Nullable Palette palette) {
                                    if(palette != null) {
                                        colorPalette = palette.getDominantColor(ContextCompat.getColor(view.getContext(),
                                                R.color.primaryColor));
                                    }
                                    else {
                                        colorPalette = ContextCompat.getColor(view.getContext(),
                                                R.color.primaryColor);
                                    }

                                    ((View)view.getParent().getParent()).setBackgroundColor(colorPalette);
                                }
                            });
                        }
                        return false;
                    }
                })
                .fitCenter()
                .placeholder(view.getContext().getDrawable(R.drawable.ic_album_white_48px))
                .into(view);
    }
}

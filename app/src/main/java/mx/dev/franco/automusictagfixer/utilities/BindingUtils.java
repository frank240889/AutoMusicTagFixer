package mx.dev.franco.automusictagfixer.utilities;

import android.widget.ImageView;
import androidx.databinding.BindingAdapter;
import androidx.lifecycle.MutableLiveData;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import mx.dev.franco.automusictagfixer.R;

public class BindingUtils {
    @BindingAdapter({"imageCover"})
    public static void loadImage(ImageView view, MutableLiveData<byte[]> cover) {
        GlideApp.with(view)
                .load(cover.getValue())
                .error(view.getContext().getDrawable(R.drawable.ic_album_white_48px))
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                .theme(view.getContext().getTheme())
                .apply(RequestOptions.skipMemoryCacheOf(false))
                .fitCenter()
                .transition(DrawableTransitionOptions.withCrossFade(150))
                .placeholder(view.getContext().getDrawable(R.drawable.ic_album_white_48px))
                .into(view);
    }
}

package mx.dev.franco.automusictagfixer.utilities;

import android.widget.ImageView;

import androidx.databinding.BindingAdapter;
import androidx.lifecycle.MutableLiveData;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.ui.BaseFragment;

public class BindingUtils {
    @BindingAdapter({"imageCover"})
    public static void loadImage(ImageView view, MutableLiveData<byte[]> cover) {
        GlideApp.with(view).
                load(cover.getValue())
                .error(R.drawable.ic_album_white_48px)
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                .apply(RequestOptions.skipMemoryCacheOf(true))
                .transition(DrawableTransitionOptions.withCrossFade(BaseFragment.CROSS_FADE_DURATION))
                .fitCenter()
                .into(view);
    }
}

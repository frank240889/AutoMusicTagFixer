package mx.dev.franco.automusictagfixer.utilities;

import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
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
        DisplayMetrics dm = view.getResources().getDisplayMetrics();
        int screenWidthInPixels = view.getResources().getDisplayMetrics().widthPixels;
        float screenWidthInDp = screenWidthInPixels / dm.density;
        int dpInPixels = (int) (screenWidthInPixels / screenWidthInDp);
        ViewGroup.LayoutParams layoutParams = ((View)view.getParent()).getLayoutParams();
        ViewGroup.LayoutParams layoutParams2 = view.getLayoutParams();
        layoutParams.width = (int) (screenWidthInPixels - (view.getResources().getDimension(R.dimen.default_margin) * dpInPixels));
        layoutParams.height = (int) (screenWidthInPixels - (view.getResources().getDimension(R.dimen.default_margin) * dpInPixels));
        layoutParams2.width = (int) (screenWidthInPixels - (view.getResources().getDimension(R.dimen.default_margin) * dpInPixels));
        layoutParams2.height = (int) (screenWidthInPixels - (view.getResources().getDimension(R.dimen.default_margin) * dpInPixels));
        ((View)view.getParent()).setLayoutParams(layoutParams);
        view.setLayoutParams(layoutParams2);
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

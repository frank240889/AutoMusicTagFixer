package mx.dev.franco.automusictagfixer.utilities

import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import mx.dev.franco.automusictagfixer.R

object BindingUtils {

    @JvmStatic
    @BindingAdapter("imageCover", "attachedFab")
    fun loadImage(
        view: ImageView,
        cover: MutableLiveData<ByteArray?>,
        fab: ExtendedFloatingActionButton?
    ) {
        //fab.hide();
        //fab = null;
        val dm = view.resources.displayMetrics
        val screenWidthInPixels = view.resources.displayMetrics.widthPixels
        val screenWidthInDp = screenWidthInPixels / dm.density
        val dpInPixels = (screenWidthInPixels / screenWidthInDp).toInt()
        val layoutParams = (view.parent as View).layoutParams
        val layoutParams2 = view.layoutParams
        layoutParams.width =
            (screenWidthInPixels - view.resources.getDimension(R.dimen.default_margin) * dpInPixels).toInt()
        layoutParams.height =
            (screenWidthInPixels - view.resources.getDimension(R.dimen.default_margin) * dpInPixels).toInt()
        layoutParams2.width =
            (screenWidthInPixels - view.resources.getDimension(R.dimen.default_margin) * dpInPixels).toInt()
        layoutParams2.height =
            (screenWidthInPixels - view.resources.getDimension(R.dimen.default_margin) * dpInPixels).toInt()
        (view.parent as View).layoutParams = layoutParams
        view.layoutParams = layoutParams2
        GlideApp.with(view)
            .load(cover.value)
            .error(ContextCompat.getDrawable(view.context,R.drawable.ic_album_white_48px))
            .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
            .theme(view.context.theme)
            .apply(RequestOptions.skipMemoryCacheOf(false))
            .fitCenter()
            .transition(DrawableTransitionOptions.withCrossFade(150))
            .placeholder(ContextCompat.getDrawable(view.context,R.drawable.ic_album_white_48px))
            .into(view)
    }
}
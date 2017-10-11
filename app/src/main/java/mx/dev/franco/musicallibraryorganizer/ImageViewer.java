package mx.dev.franco.musicallibraryorganizer;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * Created by franco on 10/10/17.
 */

public class ImageViewer extends Fragment {
    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle savedInstance){
        return layoutInflater.inflate(R.layout.image_viewer, viewGroup, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstance){
        super.onViewCreated(view,savedInstance);
        Bundle bundle = getArguments();
        byte[] cover = bundle.getByteArray("cover");
        ImageView imageView = (ImageView) view.findViewById(R.id.imageContainerViewer);
        imageView.setImageBitmap(BitmapFactory.decodeByteArray(cover,0,cover.length-1));
        /*GlideApp.with(getActivity())
                .load(cover)
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                .apply(RequestOptions.skipMemoryCacheOf(true))
                .fitCenter()
                .into(imageView);*/

    }
}

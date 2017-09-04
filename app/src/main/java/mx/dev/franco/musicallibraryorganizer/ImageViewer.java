package mx.dev.franco.musicallibraryorganizer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

/**
 * Created by franco on 26/08/17.
 */

public class ImageViewer extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceBundle){
        super.onCreate(savedInstanceBundle);
        setContentView(R.layout.image_viewer);
        ImageView image = (ImageView) findViewById(R.id.imageContainerViewer);
        Intent intent = getIntent();
        long id = intent.getLongExtra("id",-1);
        AudioItem audioItem = SelectFolderActivity.getItemByIdOrPath(id,null);
        byte[] cover = audioItem.getCoverArt();
        if(cover != null){
            image.setImageBitmap(audioItem.getBitmapCover());
        }
        /*GlideApp.with(view)
                .load(cover)
                .centerCrop()
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                .apply(RequestOptions.skipMemoryCacheOf(true))
                .into(image);*/
    }

    @Override
    public void onStart(){
        super.onStart();

    }

}

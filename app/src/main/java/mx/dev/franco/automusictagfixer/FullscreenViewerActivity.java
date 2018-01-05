package mx.dev.franco.automusictagfixer;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenViewerActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen_viewer);
        Intent bundle = getIntent();
        byte[] cover = bundle.getByteArrayExtra("cover");
        ImageView imageView = (ImageView) findViewById(R.id.imageContainerViewer);
        imageView.setImageBitmap(BitmapFactory.decodeByteArray(cover,0,cover.length-1));
    }

}

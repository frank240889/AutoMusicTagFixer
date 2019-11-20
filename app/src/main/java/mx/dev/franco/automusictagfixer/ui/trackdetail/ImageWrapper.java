package mx.dev.franco.automusictagfixer.ui.trackdetail;

import android.graphics.Bitmap;
import android.graphics.ImageDecoder;

/**
 * Wrapper class for bitmap class android.
 */
public class ImageWrapper {
    public ImageDecoder.Source source;
    public int width;
    public int height;
    public Bitmap bitmap;
    public int requestCode;
}

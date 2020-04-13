package mx.dev.franco.automusictagfixer.ui.trackdetail;

import android.graphics.Bitmap;
import android.graphics.ImageDecoder;

/**
 * Wrapper class for bitmap class android.
 */
public class ImageWrapper {
    public static final int MAX_WIDTH = 1080;
    public static final int MAX_HEIGHT = 1080;
    public ImageDecoder.Source source;
    public int width;
    public int height;
    public Bitmap bitmap;
    public int requestCode;
}

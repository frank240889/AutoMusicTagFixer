package mx.dev.franco.automusictagfixer.utilities;

/**
 * Created by franco on 19/05/17.
 */

import com.gracenote.gnsdk.GnImageSize;

/**
 * This class help us to store the values from settings in order to
 * do not call SharedPreferences every time we need to check the value.
 * Some have been declared as volatiles because they are accessed by more than
 *  1 thread.
 */
public class Settings {
    //Determine the size of downloaded cover art, default value is not download cover art.
    public static volatile GnImageSize SETTING_SIZE_ALBUM_ART = GnImageSize.kImageSizeUnknown;

    public static GnImageSize setValueImageSize(String preferenceSaved){
        GnImageSize size = null;
        switch (preferenceSaved){
            case "-1":
                size = null;
                break;
            case "0":
                size = GnImageSize.kImageSizeThumbnail;
                break;
            case "1":
                size = GnImageSize.kImageSizeSmall;
                break;
            case "5":
                size = GnImageSize.kImageSizeMedium;
                break;
            case "7":
                size = GnImageSize.kImageSize720;
                break;
            case "10":
                size = GnImageSize.kImageSize1080;
                break;
            case "1000":
                size= GnImageSize.kImageSizeXLarge;
                break;
                default:
                    size = GnImageSize.kImageSize1080;
                    break;
        }
        return size;
    }
}

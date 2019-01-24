package mx.dev.franco.automusictagfixer.utilities;

/**
 * Created by franco on 19/05/17.
 */

import com.gracenote.gnsdk.GnImageSize;
import com.gracenote.gnsdk.GnLanguage;

/**
 * This class help us to store the values og ImageSize from settings.
 */
public class Settings {
    //Determine the size of downloaded cover art, default value is not download cover art.
    public static volatile GnImageSize SETTING_SIZE_ALBUM_ART = GnImageSize.kImageSizeUnknown;
    public static volatile GnLanguage SETTING_LANGUAGE = GnLanguage.kLanguageSpanish;

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

    public static GnLanguage setValueLanguage(String preferenceSaved){
        GnLanguage language= null;
        switch (preferenceSaved){
            case "0":
                language = GnLanguage.kLanguageSpanish;
                break;
            case "1":
                language = GnLanguage.kLanguageEnglish;
                break;
            default:
                language = GnLanguage.kLanguageSpanish;
                break;
        }
        return language;
    }
}

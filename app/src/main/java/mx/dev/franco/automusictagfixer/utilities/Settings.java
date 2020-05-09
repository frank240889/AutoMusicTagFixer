package mx.dev.franco.automusictagfixer.utilities;

/**
 * Created by franco on 19/05/17.
 */

import com.gracenote.gnsdk.GnImageSize;
import com.gracenote.gnsdk.GnLanguage;

/**
 * Helper class to store the values og¡f ImageSize from settings.
 */
public class Settings {
    //Determine the size of downloaded cover art, default value is not download cover art.
    public static volatile GnImageSize SETTING_SIZE_ALBUM_ART = GnImageSize.kImageSizeUnknown;
    public static volatile GnLanguage SETTING_LANGUAGE = GnLanguage.kLanguageSpanish;

    public static GnImageSize setValueImageSize(String preferenceSaved){
        GnImageSize size;
        switch (preferenceSaved){
            case "-1":
                return null;
            case "0":
                return GnImageSize.kImageSizeThumbnail;
            case "1":
                return GnImageSize.kImageSizeSmall;
            case "5":
                return GnImageSize.kImageSizeMedium;
            case "7":
                return GnImageSize.kImageSize720;
            case "10":
                return GnImageSize.kImageSize1080;
            case "1000":
                return GnImageSize.kImageSizeXLarge;
        }
        return GnImageSize.kImageSize1080;
    }

    public static GnLanguage setValueLanguage(String preferenceSaved){
        GnLanguage language = null;
        switch (preferenceSaved){
            case "0":
                return GnLanguage.kLanguageSpanish;
            case "1":
                return GnLanguage.kLanguageEnglish;
            case "2":
                return GnLanguage.kLanguageGerman;
            case "3":
                return GnLanguage.kLanguageFrench;
            case "4":
                return GnLanguage.kLanguageItalian;
            case "5":
                return GnLanguage.kLanguagePortuguese;
            case "6":
                return GnLanguage.kLanguageRussian;
            case "7":
                return GnLanguage.kLanguageChineseTraditional;
            case "8":
                return GnLanguage.kLanguageJapanese;
            case "9":
                return GnLanguage.kLanguageKorean;
        }
        return GnLanguage.kLanguageSpanish;
    }
}

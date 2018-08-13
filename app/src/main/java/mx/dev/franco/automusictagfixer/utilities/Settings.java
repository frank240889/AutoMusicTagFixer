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
    public static boolean BACKGROUND_CORRECTION = false;
    //If true, the automatized process of correction
    //will change the file name with the same name of title song, default value true
    public static volatile boolean SETTING_RENAME_FILE_AUTOMATIC_MODE = true;
    //if true, the trackId title, will be applied as file, this implies
    //that file will be renamed
    public static volatile boolean SETTING_RENAME_FILE_SEMI_AUTOMATIC_MODE = true;
    //If is true, when user edit the metadata of audio file,
    //the title the user types, will be the same for the file name, default value false
    public static volatile boolean SETTING_RENAME_FILE_MANUAL_MODE = true;
    //Determine when user edit manually an audio file, if he types strange
    //characters like "$" or "#", these automatically will be replace by empty character if this setting
    //is true, otherwise it will be shown an alert indicating that data entered by user is not valid
    public static volatile boolean SETTING_REPLACE_STRANGE_CHARS_MANUAL_MODE = true;
    //default sort is by ascendant location
    public static String SETTING_SORT = null;
    public static boolean SETTING_USE_EMBED_PLAYER = true;
    //Determine the size of downloaded cover art, default value is not download cover art.
    public static volatile GnImageSize SETTING_SIZE_ALBUM_ART = GnImageSize.kImageSizeUnknown;
    //if true, will be overwritten all tags identificationFound by service in automatic mode
    //else, only missing tags will be written
    public static volatile boolean SETTING_OVERWRITE_ALL_TAGS_AUTOMATIC_MODE = false;

    public static boolean SETTING_AUTO_UPDATE_LIST = false;

    public static boolean ALL_CHECKED = false;

    public static boolean ENABLE_SD_CARD_ACCESS = false;

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
        }
        return size;
    }
}

package mx.dev.franco.musicallibraryorganizer;

/**
 * Created by franco on 19/05/17.
 */

import com.gracenote.gnsdk.GnImageSize;

import java.util.Set;

/**
 * This class help us to store the values from settings in order to
 * do not call SharedPreferences every time we need to check the value
 */
public class SelectedOptions {
    //If true, the automatized process of correction
    //will change the file name with the same name of title song, default value true
    public static boolean AUTOMATIC_CHANGE_FILENAME = true;
    //If is true, when user edit the metadata of audio file,
    //the title the user types, will be the same for the file name, default value false
    public static boolean MANUAL_CHANGE_FILE = false;
    //Determine when user edit manually an audio file, if he types strange
    //characters like "$" or "#", these automatically will be replace by empty character if this setting
    //is true, otherwise it will be shown an alert indicating that data entered by user is not valid
    public static boolean AUTOMATICALLY_REPLACE_STRANGE_CHARACTERS = false;

    public static int DEFAULT_SORT = 0;
    public static boolean USE_EMBED_PLAYER = true;
    //Determine the size of downloaded cover art, default value is not download cover art.
    public static GnImageSize ALBUM_ART_SIZE = GnImageSize.kImageSizeUnknown;

    public static boolean AUTOMATIC_MODE_OVERWRITE_ALL_TAGS= false;
    public static boolean SEMIAUTOMATIC_MODE_OVERWRITE_ALL_TAGS= false;

    public static boolean TRACK_NUMBER = true;
    public static boolean TRACK_YEAR = true;
    public static boolean TRACK_GENRE = true;

    public static boolean ALL_SELECTED = false;

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

    public static void setValuesExtraDataToDownload(Set<String> set){
        //Default values
        if(set == null){
            SelectedOptions.TRACK_NUMBER = true;
            SelectedOptions.TRACK_YEAR = true;
            SelectedOptions.TRACK_GENRE = true;
        }
        //User selected options
        else if (set.size() > 0){
            SelectedOptions.TRACK_NUMBER = set.contains("number");
            SelectedOptions.TRACK_GENRE = set.contains("genre");
            SelectedOptions.TRACK_YEAR = set.contains("date");
        }
        //None
        else {
            SelectedOptions.TRACK_NUMBER = false;
            SelectedOptions.TRACK_YEAR = false;
            SelectedOptions.TRACK_GENRE = false;
        }
    }
}

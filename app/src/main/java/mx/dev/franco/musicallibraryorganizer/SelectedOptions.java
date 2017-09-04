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
    static boolean MANUAL_CHANGE_FILE = false;
    //Determine when user edit manually an audio file, if he types strange
    //characters like "$" or "#", these automatically will be replace by empty character if this setting
    //is true, otherwise it will be shown an alert indicating that data entered by user is not valid
    static boolean AUTOMATICALLY_REPLACE_STRANGE_CHARACTERS = false;
    //Hide items from list that their duration are lesser than this time, in milliseconds, default value show all
    //static int DURATION = 300000;
    static boolean SHOW_SEPARATORS = false;
    //Determine the size of downloaded cover art, default value is not download cover art.
    public static GnImageSize ALBUM_ART_SIZE = GnImageSize.kImageSizeUnknown;

    static boolean TRACK_NUMBER = true;
    static boolean TRACK_YEAR = true;
    static boolean TRACK_GENRE = true;

    static boolean ALL_SELECTED = false;

    static GnImageSize setValueImageSize(String preferenceSaved){
        GnImageSize size = null;
        switch (preferenceSaved){
            case "75":
                size = GnImageSize.kImageSize75;
                break;
            case "110":
                size = GnImageSize.kImageSize110;
                break;
            case "170":
                size = GnImageSize.kImageSize170;
                break;
            case "220":
                size = GnImageSize.kImageSize220;
                break;
            case "300":
                size= GnImageSize.kImageSize300;
                break;
            case "450":
                size = GnImageSize.kImageSize450;
                break;
            case "720":
                size= GnImageSize.kImageSize720;
                break;
            case "1080":
                size = GnImageSize.kImageSize1080;
                break;
            case "1000":
                size = null;
                break;
            default:
                size = GnImageSize.kImageSizeUnknown;
                break;
        }
        return size;
    }

    static void setValuesExtraDataToDownload(Set<String> set){
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

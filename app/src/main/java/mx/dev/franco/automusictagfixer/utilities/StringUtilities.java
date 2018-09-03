package mx.dev.franco.automusictagfixer.utilities;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mx.dev.franco.automusictagfixer.R;

/**
 * Created by franco on 21/06/17.
 * Helper class containing some useful
 * static methods for validating strings
 */

public final class StringUtilities {
    /**
     *
     * @param str is the input entered by user
     * @return true if string is empty, false otherwise
     */
    public static boolean isFieldEmpty(String str) {
        return str.isEmpty();
    }

    /**
     *
     * @param dirtyString
     * We replace all invalid characters because
     * compatibility problems when showing the information
     * about song
     * @return sanitized string
     */
    public static String sanitizeString(String dirtyString) {
        return dirtyString.replaceAll("[^\\w\\s()&_\\-\\]\\[\'#.:$]", "");
    }

    /**
     *
     * @param str is the input entered by user
     * @return true string contains another chararacter, false otherwise
     */
    public static boolean hasNotAllowedCharacters(String str){
        Pattern pattern = Pattern.compile("[^\\w\\s()&_\\-\\]\\[\'#.:$]");
        Matcher matcher = pattern.matcher(str);
        return matcher.find();
    }

    public static String sanitizeFilename(String dirtyFileName){
        if(dirtyFileName != null && !dirtyFileName.equals(""))
            return dirtyFileName.replaceAll("[^\\w\\s()&_\\-\\]\\[\'#.:$/]", "");

        return "";
    }

    /**
     *
     * @param str is the input entered by user
     * @return trimmed string
     */
    public static String trimString(String str){
        return str.trim();
    }

    /**
     *
     * @param id is the id of element
     * @param str is the input entered by user
     * @return true if string is too long, false otherwise
     */
    public static boolean isTooLong(int id, String str){
        boolean isTooLong = false;
        switch (id){
            case R.id.track_name_details:
                isTooLong = str.length() >= 101;
                break;
            case R.id.artist_name_details:
                isTooLong = str.length() >= 101;
                break;
            case R.id.album_name_details:
                isTooLong = str.length() >= 151;
                break;
            case R.id.track_number:
            case R.id.track_year:
                isTooLong = str.length() >= 5;
                break;
            case R.id.track_genre:
                isTooLong = str.length() >= 81;
                break;
        }
        return isTooLong;
    }
}
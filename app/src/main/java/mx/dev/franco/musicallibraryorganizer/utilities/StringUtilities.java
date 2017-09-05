package mx.dev.franco.musicallibraryorganizer.utilities;

import android.widget.EditText;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mx.dev.franco.musicallibraryorganizer.R;

/**
 * Created by franco on 21/06/17.
 * Helper class containing some useful
 * static methods for validating strings
 */

public final class StringUtilities {
    /**
     *
     * @param editText
     * @return true if string is empty, false otherwise
     */
    public static boolean isFieldEmpty(EditText editText) {
        return editText.getText().toString().isEmpty();
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
        return dirtyString.replaceAll("[^\\w\\s()&_-]", "");
    }

    /**
     *
     * @param editText
     * @return true string contains another chararacter, false otherwise
     */
    public static boolean hasNotAllowedCharacters(EditText editText){
        Pattern pattern = Pattern.compile("[^\\w\\s.()?¿:&_-]");
        Matcher matcher = pattern.matcher(editText.getText().toString());
        return matcher.find();
    }

    /**
     *
     * @param editText
     * @return trimmed string
     */
    public static String trimString(EditText editText){
        return editText.getText().toString().trim();
    }

    /**
     *
     * @param editText
     * @return true if string is too long, false otherwise
     */
    public static boolean isTooLong(EditText editText){
        boolean isTooLong = false;
        switch (editText.getId()){
            case R.id.track_name_details:
            case R.id.artist_name_details:
            case R.id.album_name_details:
                isTooLong = editText.getText().toString().length() >= 81;
                break;
            case R.id.track_number:
            case R.id.track_year:
                isTooLong = editText.getText().toString().length() >= 5;
                break;
            case R.id.track_genre:
                isTooLong = editText.getText().toString().length() >=31;
                break;
        }
        return isTooLong;
    }
}
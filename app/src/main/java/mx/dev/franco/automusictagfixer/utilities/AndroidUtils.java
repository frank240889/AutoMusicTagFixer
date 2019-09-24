package mx.dev.franco.automusictagfixer.utilities;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.ArrayMap;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.gracenote.gnsdk.GnAssetFetch;
import com.gracenote.gnsdk.GnException;
import com.gracenote.gnsdk.GnImageSize;

import org.jaudiotagger.tag.FieldKey;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import mx.dev.franco.automusictagfixer.BuildConfig;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.fixer.AudioMetadataTagger;
import mx.dev.franco.automusictagfixer.identifier.GnApiService;
import mx.dev.franco.automusictagfixer.identifier.Identifier;
import mx.dev.franco.automusictagfixer.identifier.Result;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.utilities.resource_manager.ResourceManager;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class AndroidUtils {

    public static Toast getToast(Context context){
        @SuppressLint("ShowToast") Toast toast = Toast.makeText(context.getApplicationContext(), "", Toast.LENGTH_LONG);
        View view = toast.getView();
        TextView text = view.findViewById(android.R.id.message);
        text.setTextColor(ContextCompat.getColor(context.getApplicationContext(), R.color.grey_900));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            text.setTextAppearance(R.style.CustomToast);
        } else {
            text.setTextAppearance(context.getApplicationContext(), R.style.CustomToast);
        }
        view.setBackground(ContextCompat.getDrawable(context.getApplicationContext(), R.drawable.background_custom_toast));
        toast.setGravity(Gravity.CENTER, 0, 0);
        return toast;
    }

    /**
     * Open files in external app
     * @param path
     */
    public static void openInExternalApp(String path, Context context){
        File file = new File(path);
        String type = TrackUtils.getMimeType(path);
        try {

            Intent intent = new Intent();
            //default action is ACTION_VIEW
            intent.setAction(Intent.ACTION_VIEW);
            //For android >7 we need a file provider to open
            //files in external app
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Uri contentUri = FileProvider.getUriForFile(context.getApplicationContext(), BuildConfig.DOCUMENTS_AUTHORITY, file);
                intent.setDataAndType(contentUri, type);
            } else {
                intent.setDataAndType(Uri.fromFile(file), type);
                intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Crashlytics.logException(e);
            e.printStackTrace();
        }
    }

    public static void openInExternalApp(String action, String msg, Context context){
        Uri uri = Uri.parse(msg);
        Intent intent = new Intent(action, uri);
        context.startActivity(intent);
    }

    public static Snackbar getSnackbar(@NonNull View viewToAttach, @NonNull Context context){
        Snackbar snackbar = Snackbar.make(viewToAttach,"",Snackbar.LENGTH_SHORT);
        TextView tv = snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        snackbar.getView().setBackgroundColor(ContextCompat.getColor(context.getApplicationContext(),R.color.primaryLightColor));
        tv.setTextColor(ContextCompat.getColor(context.getApplicationContext(),R.color.grey_800));
        snackbar.setActionTextColor(ContextCompat.getColor(context.getApplicationContext(),R.color.grey_800));
        return snackbar;
    }

    public static boolean grantPermissionSD(Context context, Intent resultData){

        if(resultData == null)
            return false;

        final int takeFlags = resultData.getFlags()
                & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        context.getContentResolver().takePersistableUriPermission(Objects.requireNonNull(resultData.getData()), takeFlags);

        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.Application.FULL_QUALIFIED_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(Constants.URI_TREE, resultData.getData().toString()).apply();
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("key_enable_sd_card_access",true).apply();
        return true;
    }

    public static void revokePermissionSD(Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.Application.FULL_QUALIFIED_NAME, Context.MODE_PRIVATE);
        String uriString = sharedPreferences.getString(Constants.URI_TREE, null);

        // Writable permission to URI SD Card had not been granted yet, so uriString is null
        if(uriString == null)
            return;

        Uri uri = Uri.parse(uriString);

        //Revoke permission to write to SD card and remove URI from shared preferences.
        if(uri != null) {
            int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
            context.getContentResolver().releasePersistableUriPermission(uri, takeFlags);
            sharedPreferences.edit().remove(Constants.URI_TREE).apply();
            PreferenceManager.getDefaultSharedPreferences(context).
                    edit().
                    putBoolean("key_enable_sd_card_access", false).
                    apply();
        }
    }

    public static Uri getUriSD(Context context){
        String uriString = context.getSharedPreferences(Constants.Application.FULL_QUALIFIED_NAME, Context.MODE_PRIVATE).getString(Constants.URI_TREE, null);
        if(uriString == null)
            return null;
        return Uri.parse(uriString);
    }

    public static byte[] generateCover(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    public static Bundle getBundle(int idTrack, int correctionMode){
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.MEDIA_STORE_ID, idTrack);
        bundle.putInt(Constants.CorrectionModes.MODE, correctionMode);
        return bundle;
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

    public static String createName(Track track) {
        if(!track.getTitle().isEmpty())
            return track.getTitle();
        else if(!track.getPath().isEmpty()) {
            File file = new File(track.getPath());
            return file.getName();
        }
        else return null;
    }

    public static class AudioTaggerErrorDescription {
        public static String getErrorMessage(ResourceManager resourceManager, int errorCode){
            String errorMessage;
            switch (errorCode){
                case Tagger.COULD_NOT_APPLY_COVER:
                    errorMessage = resourceManager.getString(R.string.message_could_not_apply_cover);
                    break;
                case Tagger.COULD_NOT_APPLY_TAGS:
                    errorMessage = resourceManager.getString(R.string.message_could_not_apply_tags);
                    break;
                case Tagger.COULD_NOT_COPY_BACK_TO_ORIGINAL_LOCATION:
                    errorMessage = resourceManager.getString(R.string.message_could_copy_back);
                    break;
                case Tagger.COULD_NOT_CREATE_AUDIOFILE:
                    errorMessage = resourceManager.getString(R.string.message_could_not_create_audio_file);
                    break;
                case Tagger.COULD_NOT_CREATE_TEMP_FILE:
                    errorMessage = resourceManager.getString(R.string.message_could_not_create_temp_file);
                    break;
                case Tagger.COULD_NOT_GET_URI_SD_ROOT_TREE:
                    errorMessage = resourceManager.getString(R.string.message_uri_tree_not_set);
                    break;
                case Tagger.COULD_NOT_READ_TAGS:
                    errorMessage = resourceManager.getString(R.string.message_could_not_read_tags);
                    break;
                case Tagger.COULD_NOT_REMOVE_COVER:
                    errorMessage = resourceManager.getString(R.string.message_could_not_remove_cover);
                    break;
                default:
                    errorMessage = resourceManager.getString(R.string.error);
                    break;
            }

            return errorMessage;
        }

        public static String getErrorMessage(Context context, int errorCode){
            String errorMessage;
            switch (errorCode){
                case Tagger.COULD_NOT_APPLY_COVER:
                    errorMessage = context.getString(R.string.message_could_not_apply_cover);
                    break;
                case Tagger.COULD_NOT_APPLY_TAGS:
                    errorMessage = context.getString(R.string.message_could_not_apply_tags);
                    break;
                case Tagger.COULD_NOT_COPY_BACK_TO_ORIGINAL_LOCATION:
                    errorMessage = context.getString(R.string.message_could_copy_back);
                    break;
                case Tagger.COULD_NOT_CREATE_AUDIOFILE:
                    errorMessage = context.getString(R.string.message_could_not_create_audio_file);
                    break;
                case Tagger.COULD_NOT_CREATE_TEMP_FILE:
                    errorMessage = context.getString(R.string.message_could_not_create_temp_file);
                    break;
                case Tagger.COULD_NOT_GET_URI_SD_ROOT_TREE:
                    errorMessage = context.getString(R.string.message_uri_tree_not_set);
                    break;
                case Tagger.COULD_NOT_READ_TAGS:
                    errorMessage = context.getString(R.string.message_could_not_read_tags);
                    break;
                case Tagger.COULD_NOT_REMOVE_COVER:
                    errorMessage = context.getString(R.string.message_could_not_remove_cover);
                    break;
                default:
                    errorMessage = context.getString(R.string.error);
                    break;
            }

            return errorMessage;
        }
    }


    public static AudioMetadataTagger.InputParams createInputParams(Identifier.IdentificationResults result) {
        Result r = (Result) result;
        Map<FieldKey, Object> tags = new ArrayMap<>();
        if(!r.getTitle().isEmpty())
            tags.put(FieldKey.TITLE, r.getTitle());

        if(!r.getArtist().isEmpty())
            tags.put(FieldKey.ARTIST, r.getArtist());

        if(!r.getAlbum().isEmpty())
            tags.put(FieldKey.ALBUM, r.getAlbum());

        if(!r.getGenre().isEmpty())
            tags.put(FieldKey.GENRE, r.getGenre());

        if(!r.getTrackYear().isEmpty())
            tags.put(FieldKey.YEAR, r.getTrackYear());

        if(!r.getTrackNumber().isEmpty())
            tags.put(FieldKey.TRACK, r.getTrackNumber());

        Map<GnImageSize, String> covers = r.getCovers();

        String coverUrl = null;
        if(covers.size() > 0) {

            //If is selected "De mejor calidad disponible"
            //iterate from higher to lower quality and select the first higher quality identificationFound.
            if (Settings.SETTING_SIZE_ALBUM_ART == GnImageSize.kImageSizeXLarge) {
                coverUrl = getBetterQualityCover(covers);
                if(coverUrl != null)
                    tags.put(FieldKey.COVER_ART, coverUrl);
            }
            //If is selected "De menor calidad disponible"
            //iterate from lower to higher quality and select the first lower quality identificationFound.
            else if (Settings.SETTING_SIZE_ALBUM_ART == GnImageSize.kImageSizeThumbnail) {
                coverUrl = getLowestQualityCover(covers);
                if(coverUrl != null)
                    tags.put(FieldKey.COVER_ART, coverUrl);
            }
            //get the first identificationFound in any of those predefined sizes:
            //"De baja calidad", "De media calidad", "De alta calidad", "De muy alta calidad"
            else {
                coverUrl = covers.get(Settings.SETTING_SIZE_ALBUM_ART);
                if(coverUrl != null)
                    tags.put(FieldKey.COVER_ART, coverUrl);
            }
        }

        return new AudioMetadataTagger.InputParams(tags);
    }

    public static String getBetterQualityCover(Map<GnImageSize, String> covers) {
        Set<Map.Entry<GnImageSize, String>> entries = covers.entrySet();
        for(Map.Entry<GnImageSize, String> entry : entries) {
            if(entry.getValue() != null) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static String getLowestQualityCover(Map<GnImageSize, String> covers) {
        Set<Map.Entry<GnImageSize, String>> entries = covers.entrySet();
        for(Map.Entry<GnImageSize, String> entry : entries) {
            if(entry.getValue() != null) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static byte[] getAsset(String value, Context context) throws GnException {
        return new GnAssetFetch(GnApiService.getInstance(context).getGnUser(), value).data();
    }
}

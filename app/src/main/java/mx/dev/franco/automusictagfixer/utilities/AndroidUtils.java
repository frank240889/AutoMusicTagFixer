package mx.dev.franco.automusictagfixer.utilities;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.ArrayMap;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.crashlytics.android.Crashlytics;
import com.google.android.material.snackbar.Snackbar;
import com.gracenote.gnsdk.GnAssetFetch;
import com.gracenote.gnsdk.GnException;
import com.gracenote.gnsdk.GnImageSize;

import org.jaudiotagger.tag.FieldKey;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import mx.dev.franco.automusictagfixer.BuildConfig;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.common.Action;
import mx.dev.franco.automusictagfixer.fixer.AudioMetadataTagger;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;
import mx.dev.franco.automusictagfixer.identifier.CoverIdentificationResult;
import mx.dev.franco.automusictagfixer.identifier.GnApiService;
import mx.dev.franco.automusictagfixer.identifier.Identifier;
import mx.dev.franco.automusictagfixer.identifier.Result;
import mx.dev.franco.automusictagfixer.identifier.TrackIdentificationResult;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.ui.trackdetail.InputCorrectionParams;
import mx.dev.franco.automusictagfixer.utilities.Constants.CorrectionActions;
import mx.dev.franco.automusictagfixer.utilities.resource_manager.ResourceManager;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class AndroidUtils {

    public static Toast getToast(Context context){
        @SuppressLint("ShowToast") Toast toast = Toast.makeText(context.getApplicationContext(), "", Toast.LENGTH_LONG);
        //View view = toast.getView();
        //TextView text = view.findViewById(android.R.id.message);
        //text.setTextColor(ContextCompat.getColor(context.getApplicationContext(), R.color.primaryColor));
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            text.setTextAppearance(R.style.CustomToast);
        } else {
            text.setTextAppearance(context.getApplicationContext(), R.style.CustomToast);
        }*/
        //view.setBackground(ContextCompat.getDrawable(context.getApplicationContext(), R.drawable.background_custom_toast));
        toast.setGravity(Gravity.CENTER, 0, 0);
        return toast;
    }

    public static void showToast(@NonNull Message message, @NonNull Context context) {
        if(message.getMessage() != null) {
            showToast(message.getMessage(), context);
        }
        else if(message.getIdResourceMessage() != -1) {
            showToast(message.getIdResourceMessage(), context);
        }
    }

    public static void showToast(@NonNull String message, @NonNull Context context) {
        Toast toast = getToast(context);
        toast.setText(message);
        toast.show();
    }

    public static void showToast(@StringRes int message, @NonNull Context context) {
        Toast toast = getToast(context);
        if(message != -1) {
            toast.setText(message);
            toast.show();
        }
    }

    public static Snackbar createSnackbar(@NonNull View view, Message message) {
        Snackbar snackbar = getSnackbar(view, view.getContext());
        if(message.getMessage() != null) {
            snackbar.setText(message.getMessage());
        }
        else if(message.getIdResourceMessage() != -1) {
            snackbar.setText(message.getIdResourceMessage());
        }
        return snackbar;
    }

    public static Snackbar createSnackbar(@NonNull View view, @NonNull String message) {
        Snackbar snackbar = getSnackbar(view, view.getContext());
        snackbar.setText(message);
        return snackbar;
    }

    public static Snackbar createSnackbar(@NonNull View view, @StringRes int message) {
        Snackbar snackbar = getSnackbar(view, view.getContext());
        snackbar.setText(message);
        return snackbar;
    }

    public static Snackbar createActionableSnackbar(@NonNull View view,
                                                        Message message,
                                                        OnClickListener onClickListener) {

        ActionableMessage actionableMessage = (ActionableMessage) message;
        Snackbar snackbar = createSnackbar(view, message);
        String action = getActionName(actionableMessage.getAction());
        snackbar.setAction(action, onClickListener);

        return snackbar;
    }


    public static String getActionName(Action action){
        return null;
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
        snackbar.setTextColor(ContextCompat.getColor(context,R.color.snackbarTextBackgroundColor));
        //TextView tv = snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
        //snackbar.getView().setBackground(ContextCompat.getDrawable(context, R.drawable.bg_toolbar));
        //snackbar.getView().setBackgroundColor(ContextCompat.getColor(context.getApplicationContext(),R.color.snackbarBackgroundColor));
        //tv.setTextColor(ContextCompat.getColor(context.getApplicationContext(),R.color.snackbarTextColor));
        //snackbar.setActionTextColor(ContextCompat.getColor(context.getApplicationContext(),R.color.primaryColor));
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

    public static Bitmap generateBitmap(byte[] data) {
        return BitmapFactory.decodeByteArray(data, 0, data.length - 1, null);
    }

    public static Bundle getBundle(int idTrack, int correctionMode){
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.MEDIA_STORE_ID, idTrack);
        bundle.putInt(CorrectionActions.MODE, correctionMode);
        return bundle;
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static byte[] decodeSampledBitmapFromResource(byte[] data, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data,0, data.length - 1, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        Bitmap downSampledBitmap = BitmapFactory.decodeByteArray(data,0, data.length - 1, options);
        if(downSampledBitmap != null)
            return generateCover(downSampledBitmap);
        return null;
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

    public static Identifier.IdentificationResults  findId(List<? extends Identifier.IdentificationResults> resultList, String idToSearch) {
        for(Identifier.IdentificationResults r : resultList) {
            if(r.getId().equals(idToSearch))
                return r;
        }

        return null;
    }

    public static class AudioTaggerErrorDescription {
        public static String getErrorMessage(ResourceManager resourceManager, int errorCode){
            String errorMessage;
            switch (errorCode){
                case AudioTagger.COULD_NOT_APPLY_COVER:
                    errorMessage = resourceManager.getString(R.string.message_could_not_apply_cover);
                    break;
                case AudioTagger.COULD_NOT_APPLY_TAGS:
                    errorMessage = resourceManager.getString(R.string.message_could_not_apply_tags);
                    break;
                case AudioTagger.COULD_NOT_COPY_BACK_TO_ORIGINAL_LOCATION:
                    errorMessage = resourceManager.getString(R.string.message_could_copy_back);
                    break;
                case AudioTagger.COULD_NOT_CREATE_AUDIOFILE:
                    errorMessage = resourceManager.getString(R.string.message_could_not_create_audio_file);
                    break;
                case AudioTagger.COULD_NOT_CREATE_TEMP_FILE:
                    errorMessage = resourceManager.getString(R.string.message_could_not_create_temp_file);
                    break;
                case AudioTagger.COULD_NOT_GET_URI_SD_ROOT_TREE:
                    errorMessage = resourceManager.getString(R.string.message_uri_tree_not_set);
                    break;
                case AudioTagger.COULD_NOT_READ_TAGS:
                    errorMessage = resourceManager.getString(R.string.message_could_not_read_tags);
                    break;
                case AudioTagger.COULD_NOT_REMOVE_COVER:
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
                case AudioTagger.COULD_NOT_APPLY_COVER:
                    errorMessage = context.getString(R.string.message_could_not_apply_cover);
                    break;
                case AudioTagger.COULD_NOT_APPLY_TAGS:
                    errorMessage = context.getString(R.string.message_could_not_apply_tags);
                    break;
                case AudioTagger.COULD_NOT_COPY_BACK_TO_ORIGINAL_LOCATION:
                    errorMessage = context.getString(R.string.message_could_copy_back);
                    break;
                case AudioTagger.COULD_NOT_CREATE_AUDIOFILE:
                    errorMessage = context.getString(R.string.message_could_not_create_audio_file);
                    break;
                case AudioTagger.COULD_NOT_CREATE_TEMP_FILE:
                    errorMessage = context.getString(R.string.message_could_not_create_temp_file);
                    break;
                case AudioTagger.COULD_NOT_GET_URI_SD_ROOT_TREE:
                    errorMessage = context.getString(R.string.message_uri_tree_not_set);
                    break;
                case AudioTagger.COULD_NOT_READ_TAGS:
                    errorMessage = context.getString(R.string.message_could_not_read_tags);
                    break;
                case AudioTagger.COULD_NOT_REMOVE_COVER:
                    errorMessage = context.getString(R.string.message_could_not_remove_cover);
                    break;
                default:
                    errorMessage = context.getString(R.string.error);
                    break;
            }

            return errorMessage;
        }
    }


    public static void createInputParams(Identifier.IdentificationResults result,
                                                          InputCorrectionParams correctionParams) {
        TrackIdentificationResult r = (TrackIdentificationResult) result;
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

        correctionParams.setFields(tags);
    }

    public static void createCoverInputParams(CoverIdentificationResult r,
                                         InputCorrectionParams correctionParams) {
        Map<FieldKey, Object> tags = null;
        if(correctionParams.getFields() == null)
            tags = new ArrayMap<>();
        else
            tags = correctionParams.getFields();

        tags.put(FieldKey.COVER_ART, r.getCover());
        correctionParams.setFields(tags);
    }

    public static InputCorrectionParams createInputParams(@Nullable String title,
                                                          @Nullable String artist,
                                                          @Nullable String album,
                                                          @Nullable String genre,
                                                          @Nullable String trackNumber,
                                                          @Nullable String trackYear,
                                                          @Nullable byte[] cover
                                                                    ) {

        Map<FieldKey, Object> tags = new ArrayMap<>();
        if(title != null && !title.isEmpty())
            tags.put(FieldKey.TITLE, title);

        if(artist != null && !artist.isEmpty())
            tags.put(FieldKey.ARTIST, artist);

        if(album != null && !album.isEmpty())
            tags.put(FieldKey.ALBUM, album);

        if(genre != null && !genre.isEmpty())
            tags.put(FieldKey.GENRE, genre);

        if(trackYear != null && !trackYear.isEmpty())
            tags.put(FieldKey.YEAR, trackYear);

        if(trackNumber != null && !trackNumber.isEmpty())
            tags.put(FieldKey.TRACK, trackNumber);
        if(cover != null)
            tags.put(FieldKey.COVER_ART, cover);

        return new InputCorrectionParams(tags);
    }

    public static void createInputParams(@Nullable String title,
                                                          @Nullable String artist,
                                                          @Nullable String album,
                                                          @Nullable String genre,
                                                          @Nullable String trackNumber,
                                                          @Nullable String trackYear,
                                                          @Nullable byte[] cover,
    AudioMetadataTagger.InputParams inputParams) {

        Map<FieldKey, Object> tags = new ArrayMap<>();
        if(title != null && !title.isEmpty())
            tags.put(FieldKey.TITLE, title);

        if(artist != null && !artist.isEmpty())
            tags.put(FieldKey.ARTIST, artist);

        if(album != null && !album.isEmpty())
            tags.put(FieldKey.ALBUM, album);

        if(genre != null && !genre.isEmpty())
            tags.put(FieldKey.GENRE, genre);

        if(trackYear != null && !trackYear.isEmpty())
            tags.put(FieldKey.YEAR, trackYear);

        if(trackNumber != null && !trackNumber.isEmpty())
            tags.put(FieldKey.TRACK, trackNumber);
        if(cover != null)
            tags.put(FieldKey.COVER_ART, cover);

        inputParams.setFields(tags);
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

    public static byte[] getAsset(String value, Context context) throws GnException {
        return new GnAssetFetch(GnApiService.getInstance(context).getGnUser(), value).data();
    }

    public static final TrackIdentificationResult createTrackResult(Result result) {
        return new TrackIdentificationResult(result.getTitle(),
                result.getArtist(),
                result.getAlbum(),
                result.getTrackNumber(),
                result.getTrackYear(),
                result.getGenre());
    }

    public static final List<CoverIdentificationResult> createListCoverResult(Result result, Context context) {
        List<CoverIdentificationResult> c = new ArrayList<>();
        Map<GnImageSize, String> covers = result.getCovers();
        Set<Map.Entry<GnImageSize, String>> entries = covers.entrySet();
        for(Map.Entry<GnImageSize, String> entry : entries){
            try {
                byte[] cover = getAsset(entry.getValue(), context);
                Bitmap bitmap = generateBitmap(cover);
                String size = bitmap.getWidth() + " * " + bitmap.getHeight();
                CoverIdentificationResult coverIdentificationResult = new CoverIdentificationResult(cover, size, entry.getKey());
                coverIdentificationResult.setId(result.getId());
                c.add(coverIdentificationResult);
            } catch (IllegalArgumentException | GnException e) {
                e.printStackTrace();
            }
        }
        return c;
    }
    public static final class AsyncBitmapDecoder {
        public interface AsyncBitmapDecoderCallback {
            void onBitmapDecoded(Bitmap bitmap);
            void onDecodingError(Throwable throwable);
        }

        private Handler mHandler;
        public AsyncBitmapDecoder(){
            mHandler = new Handler(Looper.getMainLooper());
        }

        @RequiresApi(api = Build.VERSION_CODES.P)
        public void decodeBitmap(ImageDecoder.Source source, AsyncBitmapDecoderCallback asyncBitmapDecoderCallback) {
            Thread thread = new Thread(() -> {
                try {
                    Bitmap bitmap = ImageDecoder.decodeBitmap(source);
                    mHandler.post(() -> {
                        if(asyncBitmapDecoderCallback != null)
                            asyncBitmapDecoderCallback.onBitmapDecoded(bitmap);
                    });
                } catch (IOException e) {
                    mHandler.post(() -> {
                        if(asyncBitmapDecoderCallback != null)
                            asyncBitmapDecoderCallback.onDecodingError(e);
                    });
                }
            });
            thread.start();
        }

        public void decodeBitmap(ContentResolver cr, Uri uri, AsyncBitmapDecoderCallback asyncBitmapDecoderCallback) {
            Thread thread = new Thread(() -> {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(cr, uri);
                    mHandler.post(() -> {
                        if(asyncBitmapDecoderCallback != null)
                            asyncBitmapDecoderCallback.onBitmapDecoded(bitmap);
                    });
                } catch (Exception e) {
                    mHandler.post(() -> {
                        if(asyncBitmapDecoderCallback != null)
                            asyncBitmapDecoderCallback.onDecodingError(e);
                    });
                }
            });
            thread.start();
        }
    }
}

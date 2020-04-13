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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import org.jaudiotagger.tag.FieldKey;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import mx.dev.franco.automusictagfixer.BuildConfig;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.common.Action;
import mx.dev.franco.automusictagfixer.fixer.AudioTagger;
import mx.dev.franco.automusictagfixer.fixer.CorrectionParams;
import mx.dev.franco.automusictagfixer.identifier.Identifier;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.utilities.Constants.CorrectionActions;
import mx.dev.franco.automusictagfixer.utilities.resource_manager.ResourceManager;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static mx.dev.franco.automusictagfixer.utilities.Constants.DATE_PATTERN;

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

    public static Snackbar createNoDismissibleSnackbar(@NonNull View view, @StringRes int message) {
        Snackbar snackbar = getSnackbar(view, view.getContext());
        snackbar.setBehavior(new BaseTransientBottomBar.Behavior() {
            @Override
            public boolean canSwipeDismissView(View child) {
                return false;
            }
        });
        snackbar.setDuration(Snackbar.LENGTH_INDEFINITE);
        snackbar.setText(message);
        return snackbar;
    }

    public static Snackbar createNoDismissibleSnackbar(@NonNull View view, @NonNull String message) {
        Snackbar snackbar = getSnackbar(view, view.getContext());
        snackbar.setBehavior(new BaseTransientBottomBar.Behavior() {
            @Override
            public boolean canSwipeDismissView(View child) {
                return false;
            }
        });
        snackbar.setDuration(Snackbar.LENGTH_INDEFINITE);
        snackbar.setText(message);
        return snackbar;
    }


    public static Snackbar createSnackbar(@NonNull View view, @StringRes int message) {
        Snackbar snackbar = getSnackbar(view, view.getContext());
        snackbar.setText(message);
        return snackbar;
    }

    public static Snackbar createSnackbar(@NonNull View view, @NonNull String message) {
        Snackbar snackbar = getSnackbar(view, view.getContext());
        snackbar.setText(message);
        return snackbar;
    }

    public static Snackbar createActionableSnackbar(@NonNull View view,
                                                        Message message,
                                                        OnClickListener onClickListener) {

        ActionableMessage actionableMessage = (ActionableMessage) message;
        Snackbar snackbar = createSnackbar(view, message);
        String action = getActionName(actionableMessage.getAction(), view);
        snackbar.setAction(action, onClickListener);

        return snackbar;
    }


    public static String getActionName(Action action,View view){
        if(action == Action.URI_ERROR)
            return view.getContext().getString(R.string.details);
        else if(action == Action.MANUAL_CORRECTION)
            return view.getContext().getString(R.string.edit);
        else if(action == Action.WATCH_IMAGE)
            return view.getContext().getString(R.string.see_image);
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
            try {
                context.getContentResolver().releasePersistableUriPermission(uri, takeFlags);
                sharedPreferences.edit().remove(Constants.URI_TREE).apply();
                PreferenceManager.getDefaultSharedPreferences(context).
                        edit().
                        putBoolean("key_enable_sd_card_access", false).
                        apply();
            }
            catch (SecurityException ignored){ }
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

    public static void createInputParams(@Nullable String title,
                                                          @Nullable String artist,
                                                          @Nullable String album,
                                                          @Nullable String genre,
                                                          @Nullable String trackNumber,
                                                          @Nullable String trackYear,
                                                          @Nullable byte[] cover,
    CorrectionParams inputParams) {

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

        inputParams.setTags(tags);
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

    public static boolean isConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        boolean connected = networkInfo != null && networkInfo.isConnected() && networkInfo.isAvailable();
        boolean hasInternetConnection =  false;

        try {
            URL url = new URL("https://www.google.com");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Android");
            connection.setRequestProperty("Connection", "close");
            connection.setConnectTimeout(500);
            connection.setReadTimeout(500);
            connection.connect();
            hasInternetConnection = connection.getResponseCode() == 200;
            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return  connected && hasInternetConnection;
    }

    public static String generateNameWithDate(String filename) {
        Date date = new Date();
        DateFormat now = new SimpleDateFormat(DATE_PATTERN, Locale.getDefault());
        return filename.trim().replaceAll(" ","_") + "_" +now.format(date);
    }

}

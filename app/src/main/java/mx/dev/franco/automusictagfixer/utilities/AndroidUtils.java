package mx.dev.franco.automusictagfixer.utilities;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
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
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.crashlytics.android.Crashlytics;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Objects;

import mx.dev.franco.automusictagfixer.BuildConfig;
import mx.dev.franco.automusictagfixer.R;

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

    public static AlertDialog createResultsDialog(Context context, GnResponseListener.IdentificationResults results, DialogInterface.OnClickListener... listeners){
        AlertDialog.Builder builder = getBuilder(context, results, false);
        builder.setPositiveButton(R.string.all_tags, listeners[0]).
                setNegativeButton(R.string.missing_tags, listeners[1]);

        return builder.create();
    }

    public static AlertDialog createResultsDialog(Context context, GnResponseListener.IdentificationResults results,
                                                  String message, DialogInterface.OnClickListener... listeners){
        AlertDialog.Builder builder = getBuilder(context, results, false );
        builder.setMessage(message);
        builder.setPositiveButton(R.string.all_tags, listeners[0]).
                setNegativeButton(R.string.missing_tags, listeners[1]);

        return builder.create();
    }

    public static AlertDialog createResultsDialog(Context context, GnResponseListener.IdentificationResults results,
                                                  String title, String message, DialogInterface.OnClickListener... listeners){
        AlertDialog.Builder builder = getBuilder(context, results, false );
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(R.string.all_tags, listeners[0]).
                setNegativeButton(R.string.missing_tags, listeners[1]);

        return builder.create();
    }

    public static AlertDialog createResultsDialog(Context context, GnResponseListener.IdentificationResults results,
                                                  int message, boolean showAll, DialogInterface.OnClickListener... listeners){
        AlertDialog.Builder builder = getBuilder(context, results, showAll);
        builder.setMessage(message);
        builder.setPositiveButton(R.string.all_tags, listeners[0]).
                setNegativeButton(R.string.missing_tags, listeners[1]);

        return builder.create();
    }

    public static AlertDialog createResultsDialog(Context context, GnResponseListener.IdentificationResults results,
                                                  int message, boolean showAll, View customView, DialogInterface.OnClickListener... listeners){
        AlertDialog.Builder builder = getBuilder(context, results, showAll, customView);
        builder.setMessage(message);
        builder.setPositiveButton(R.string.all_tags, listeners[0]).
                setNegativeButton(R.string.missing_tags, listeners[1]);

        return builder.create();
    }

    private static AlertDialog.Builder getBuilder(Context context, GnResponseListener.IdentificationResults results, boolean showAll, View customView){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(customView);
        if(showAll)
            setValues(results, customView, context);
        else
            setCover(results,customView, context);
        return builder;
    }

    public static AlertDialog createResultsDialog(Context context, GnResponseListener.IdentificationResults results,
                                                  int title, int message, DialogInterface.OnClickListener... listeners){
        AlertDialog.Builder builder = getBuilder(context, results, false);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(R.string.all_tags, listeners[0]).
                setNegativeButton(R.string.missing_tags, listeners[1]);

        return builder.create();
    }

    private static AlertDialog.Builder getBuilder(Context context, GnResponseListener.IdentificationResults results, boolean showAll){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.layout_results_track_id, null);
        builder.setView(view);
        view.findViewById(R.id.checkbox_rename).setVisibility(View.GONE);
        view.findViewById(R.id.label_rename_to).setVisibility(View.GONE);
        view.findViewById(R.id.rename_to).setVisibility(View.GONE);
        view.findViewById(R.id.message_rename_hint).setVisibility(View.GONE);
        if(showAll)
            setValues(results, view, context);
        else
            setCover(results,view, context);
        return builder;
    }

    private static void setValues(GnResponseListener.IdentificationResults results, View view, Context context) {
        ImageView cover = view.findViewById(R.id.trackid_cover);
        GlideApp.with(view.getContext()).
                load(results.cover).
                diskCacheStrategy(DiskCacheStrategy.NONE).
                skipMemoryCache(true).
                placeholder(R.drawable.ic_album_white_48px).
                transition(DrawableTransitionOptions.withCrossFade(200)).
                fitCenter().
                into(cover);
        TextView coverDimensions = view.findViewById(R.id.trackid_cover_dimensions);
        coverDimensions.setText(TrackUtils.getStringImageSize(results.cover, context)) ;
        if(!results.title.isEmpty()) {
            TextView title = view.findViewById(R.id.track_id_title);
            title.setVisibility(View.VISIBLE);

            title.setText(results.title);
        }

        if(!results.artist.isEmpty()) {
            TextView artist = view.findViewById(R.id.track_id_artist);
            artist.setVisibility(View.VISIBLE);
            artist.setText(results.artist);
        }

        if(!results.album.isEmpty()) {
            TextView album = view.findViewById(R.id.trackid_album);
            album.setVisibility(View.VISIBLE);
            album.setText(results.album);
        }

        if(!results.genre.isEmpty()) {
            TextView genre = view.findViewById(R.id.trackid_genre);
            genre.setVisibility(View.VISIBLE);
            genre.setText(results.genre);
        }

        if(!results.trackNumber.isEmpty()) {
            TextView trackNumber = view.findViewById(R.id.track_id_number);
            trackNumber.setVisibility(View.VISIBLE);
            trackNumber.setText(results.trackNumber);
        }

        if(!results.trackYear.isEmpty()) {
            TextView year = view.findViewById(R.id.track_id_year);
            year.setVisibility(View.VISIBLE);
            year.setText(results.trackYear);
        }
    }

    private static void setCover(GnResponseListener.IdentificationResults results, View view, Context context){
        ImageView cover = view.findViewById(R.id.trackid_cover);
        GlideApp.with(view.getContext()).
                load(results.cover).
                diskCacheStrategy(DiskCacheStrategy.NONE).
                skipMemoryCache(true).
                placeholder(R.drawable.ic_album_white_48px).
                transition(DrawableTransitionOptions.withCrossFade(200)).
                fitCenter().
                into(cover);
        TextView coverDimensions = view.findViewById(R.id.trackid_cover_dimensions);
        coverDimensions.setText(TrackUtils.getStringImageSize(results.cover, context)) ;

        TextView title = view.findViewById(R.id.track_id_title);
        title.setVisibility(View.GONE);

        TextView artist = view.findViewById(R.id.track_id_artist);
        artist.setVisibility(View.GONE);

        TextView album = view.findViewById(R.id.trackid_album);
        album.setVisibility(View.GONE);

        TextView genre = view.findViewById(R.id.trackid_genre);
        genre.setVisibility(View.GONE);

        TextView trackNumber = view.findViewById(R.id.track_id_number);
        trackNumber.setVisibility(View.GONE);

        TextView year = view.findViewById(R.id.track_id_year);
        year.setVisibility(View.GONE);

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

    public static Animator createRevealWithDelay(View view, int centerX, int centerY, float startRadius, float endRadius) {
        Animator delayAnimator = ViewAnimationUtils.createCircularReveal(view, centerX, centerY, startRadius, startRadius);
        delayAnimator.setDuration(100);
        Animator revealAnimator = ViewAnimationUtils.createCircularReveal(view, centerX, centerY, startRadius, endRadius);
        AnimatorSet set = new AnimatorSet();
        set.playSequentially(delayAnimator, revealAnimator);
        return set;
    }

    public static GnResponseListener.IdentificationResults getResults(Bundle bundle) {
        GnResponseListener.IdentificationResults identificationResults = new GnResponseListener.IdentificationResults();
        identificationResults.title = bundle.getString("title");
        identificationResults.artist = bundle.getString("artist");
        identificationResults.album = bundle.getString("album");
        identificationResults.trackNumber = bundle.getString("track_number");
        identificationResults.trackYear = bundle.getString("track_year");
        identificationResults.genre = bundle.getString("genre");
        identificationResults.cover = bundle.getByteArray("cover");

        return identificationResults;
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

}

package mx.dev.franco.automusictagfixer.utilities;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.crashlytics.android.Crashlytics;

import java.io.File;

import mx.dev.franco.automusictagfixer.BuildConfig;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.list.AudioItem;
import mx.dev.franco.automusictagfixer.services.gnservice.GnResponseListener;

public class ViewUtils {

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
        String type = AudioItem.getMimeType(path);
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

    public static Snackbar getSnackbar(View viewToAttach, Context context){
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
        View view = LayoutInflater.from(context).inflate(R.layout.fragment_results_track_id, null);
        builder.setView(view);
        if(showAll)
            setValues(results, view);
        else
            setCover(results,view);
        return builder;
    }

    private static void setValues(GnResponseListener.IdentificationResults results, View view){
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
        coverDimensions.setText(AudioItem.getStringImageSize(results.cover)) ;
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

    private static void setCover(GnResponseListener.IdentificationResults results, View view){
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
        coverDimensions.setText(AudioItem.getStringImageSize(results.cover)) ;

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
}

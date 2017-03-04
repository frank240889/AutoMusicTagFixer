package mx.dev.franco.musicallibraryorganizer;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by franco on 4/12/16.
 */

class TrackAdapter extends ArrayAdapter<File> {
    private MediaPlayer mp;

    TrackAdapter(Context context, ArrayList<File> songs){
        super(context,0,songs);
    }


    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        // Obtenemos el item en la posicion actual

        File track = getItem(position);
        MediaMetadataRetriever metadataTrack = new MediaMetadataRetriever();
        assert track != null;
        metadataTrack.setDataSource(track.getPath());

        // Vamos reutilizando las vistas que son visibles
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.song_list, parent, false);
        }

        // Aqui llenamos los datos de las canciones en los textviews
        //if(SelectFolderActivity.selectedTracks.contains(position)){
        //    convertView.setBackgroundColor(getContext().getResources().getColor(R.color.colorPrimaryDark));
        //}
        //else{
        //    convertView.setBackgroundColor(getContext().getResources().getColor(R.color.common_google_signin_btn_text_dark_default));
        //}


        ImageButton imageButton = (ImageButton) convertView.findViewById(R.id.playTrack) ;
        TextView trackName = (TextView) convertView.findViewById(R.id.track_name);
        TextView artistName = (TextView) convertView.findViewById(R.id.artist_name);
        TextView albumName = (TextView) convertView.findViewById(R.id.album_name);

        // Extraemos los metadatos y los seteamos en su view correspondiente

        if(metadataTrack.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) != null &&
                metadataTrack.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE).length() > 51){

            String subTitle = metadataTrack.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE).substring(0,50);
            trackName.setText(String.format("%s...", subTitle));
        }
        else {
            trackName.setText(metadataTrack.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
        }

        if(metadataTrack.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR) != null &&
                metadataTrack.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR).length() > 51){

            String subAuthor = metadataTrack.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR).substring(0,50);
            artistName.setText(String.format("%s...", subAuthor));
        }
        else {
            artistName.setText(metadataTrack.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR));
        }

        if(metadataTrack.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST) != null &&
                metadataTrack.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST).length() > 51){

            String subAlbum = metadataTrack.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST).substring(0,50);
            artistName.setText(String.format("%s...", subAlbum));
        }
        else {
            artistName.setText(metadataTrack.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST));
        }

        imageButton.setTag(track.getAbsolutePath());
        trackName.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
        artistName.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
        albumName.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));

        Log.d("El tag del ib",imageButton.getTag().toString());
        if(!imageButton.getTag().toString().equals(SelectFolderActivity.activeTrack)){
            imageButton.setImageResource(getContext().getResources().getIdentifier("android:drawable/ic_media_play", null, null));
            convertView.setBackgroundColor(Color.TRANSPARENT);
        }
        else{
            imageButton.setImageResource(getContext().getResources().getIdentifier("android:drawable/ic_media_pause", null, null));
            convertView.setBackgroundColor(Color.parseColor("#33b5e5"));
        }


        return convertView;

    }

}

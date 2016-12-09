package mx.dev.franco.musicallibraryorganizer;

import android.content.Context;
import android.graphics.Typeface;
import android.media.MediaMetadataRetriever;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by franco on 4/12/16.
 */

class TrackAdapter extends ArrayAdapter<File> {
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
        CheckBox checkBoxSongName = (CheckBox) convertView.findViewById(R.id.checkbox_track_name);
        TextView trackName = (TextView) convertView.findViewById(R.id.track_name);
        TextView pathTrack = (TextView) convertView.findViewById(R.id.track_path);
        TextView artistName = (TextView) convertView.findViewById(R.id.artist_name);
        TextView albumName = (TextView) convertView.findViewById(R.id.album_name);

        // Extraemos los metadatos y los seteamos en su view correspondiente
        checkBoxSongName.setText(track.getName());
        checkBoxSongName.setTextSize((float) 15.0);
        trackName.setText(metadataTrack.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
        trackName.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
        pathTrack.setText(track.getPath());
        pathTrack.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
        artistName.setText(metadataTrack.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR));
        artistName.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
        albumName.setText(metadataTrack.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST));
        albumName.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));


        return convertView;

    }
}

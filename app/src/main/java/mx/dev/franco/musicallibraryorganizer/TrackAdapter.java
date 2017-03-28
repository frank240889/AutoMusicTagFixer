package mx.dev.franco.musicallibraryorganizer;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaMetadataRetriever;
import android.support.annotation.NonNull;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * Created by franco on 4/12/16.
 */

class TrackAdapter extends ArrayAdapter<File>{
    private int position;
    private Context context;
    private ArrayList<File> audioTracks;

    TrackAdapter(Context context, ArrayList<File> songs){
        super(context,0,songs);
        this.context = context;
        this.audioTracks = songs;
    }


    @NonNull
    @Override
    public View getView(final int position, View convertView, final ViewGroup parent){
        // Obtenemos el item en la posicion actual
        CustomAudioFile track = (CustomAudioFile) getItem(position);
        CustomAudioFile track2 = (CustomAudioFile)audioTracks.get(position);
        try {
            assert ((CustomAudioFile)track) != null;
            Log.d("TRACK_TITLE", track.getTitle());
            Log.d("AUDIO_TRACKS", ((CustomAudioFile)audioTracks.get(position)).getTitle());
        }
        catch (Exception e){
            e.printStackTrace();
        }

        DataTrackHolder dataTrackHolder = null;

        // Vamos reutilizando las vistas que son visibles
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.song_list, parent, false);
            dataTrackHolder = new DataTrackHolder();
            dataTrackHolder.imageButton = (ImageButton) convertView.findViewById(R.id.playTrack);
            dataTrackHolder.button = (Button) convertView.findViewById(R.id.contextualMenuButton);
            dataTrackHolder.trackName = (TextView) convertView.findViewById(R.id.track_name);
            dataTrackHolder.artistName = (TextView) convertView.findViewById(R.id.artist_name);
            dataTrackHolder.albumName = (TextView) convertView.findViewById(R.id.album_name);
            convertView.setTag(dataTrackHolder);
        }
        else{
            dataTrackHolder = (DataTrackHolder) convertView.getTag();
        }

        Log.d("GETVIEW",position+"");

        if(SelectFolderActivity.selectedTracksList.containsKey((Integer) position)){
            convertView.setBackgroundColor(Color.parseColor("#ff0099cc"));
        }
        else{
            convertView.setBackgroundColor(Color.TRANSPARENT);
        }
        ((CustomTrackView) convertView).setAbsoluteTrackPath(track.getAbsolutePath());
        // Extraemos los metadatos y los seteamos en su view correspondiente
        try {
            Log.d("EL TITULO EN ADAPTER", track.getTitle());
        }catch (Exception e){
            e.printStackTrace();
        }
        if(track.getTitle() != null && track.getTitle().length() > 51){
            dataTrackHolder.trackName.setText(track.getTitle().substring(0,50));
        }
        else {
            dataTrackHolder.trackName.setText(track.getTitle());
        }

        if(track.getArtist() != null && track.getArtist().length() > 51){
            dataTrackHolder.artistName.setText(track.getArtist().substring(0,50));
        }
        else {
            dataTrackHolder.artistName.setText(track.getArtist());
        }

        if(track.getAlbum() != null && track.getAlbum().length() > 51){
            dataTrackHolder.albumName.setText(track.getAlbum().substring(0,50));
        }
        else {
            dataTrackHolder.albumName.setText(track.getAlbum());
        }

        dataTrackHolder.button.setTag(track.getAbsolutePath());
        final View finalConvertView = convertView;
         //if(!dataTrackHolder.button.hasOnClickListeners()){
              dataTrackHolder.button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showContextualMenu(position,v, finalConvertView);
                }
           });
        //}

        dataTrackHolder.imageButton.setTag(track.getAbsolutePath());
        dataTrackHolder.trackName.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
        dataTrackHolder.artistName.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
        dataTrackHolder.albumName.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));

        if(!dataTrackHolder.imageButton.getTag().toString().equals(SelectFolderActivity.activeTrack)){
            if(track.getDecodedAlbumArt() !=null) {
                dataTrackHolder.imageButton.setImageBitmap(track.getDecodedAlbumArt());
            }
            else{
                dataTrackHolder.imageButton.setImageResource(R.drawable.generic_album);
            }
        }
        else{
            dataTrackHolder.imageButton.setImageResource(R.drawable.circled_pause);
        }


        return convertView;

    }

    private void showContextualMenu(int position, View button, final View parent){
        this.position = position;
        Log.d("CONTEXTUAL MENU",this.position+"");
        PopupMenu trackContextualMenu = new PopupMenu(getContext(),button);
        MenuInflater menuInflater = trackContextualMenu.getMenuInflater();
        menuInflater.inflate(R.menu.track_contextual_menu, trackContextualMenu.getMenu());

        if(!SelectFolderActivity.selectedTracksList.containsKey((Integer) this.position)) {
              trackContextualMenu.getMenu().findItem(R.id.action_select).setTitle(R.string.select_to_process);
        }
        else {
             trackContextualMenu.getMenu().findItem(R.id.action_select).setTitle(R.string.deselect_to_process);
        }

        trackContextualMenu.show();
        trackContextualMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){

            @Override
            public boolean onMenuItemClick(MenuItem item) {

                switch (item.getItemId()){
                    case R.id.action_select:
                        if(!SelectFolderActivity.selectedTracksList.containsKey((Integer) TrackAdapter.this.position)) {
                            SelectFolderActivity.selectedTracksList.put((Integer)TrackAdapter.this.position,((CustomTrackView)parent).getAbsoluteTrackPath());
                            parent.setBackgroundColor(Color.parseColor("#ff0099cc"));
                        }else {
                            SelectFolderActivity.selectedTracksList.remove((Integer) TrackAdapter.this.position);
                            parent.setBackgroundColor(Color.TRANSPARENT);
                        }

                        if(SelectFolderActivity.selectedTracksList.isEmpty()){
                            ((View) parent).getRootView().findViewById(R.id.fab).setEnabled(false);
                            ((View) parent).getRootView().findViewById(R.id.fab).setAlpha(0.8f);
                        }
                        else{
                            ((View) parent).getRootView().findViewById(R.id.fab).setEnabled(true);
                            ((View) parent).getRootView().findViewById(R.id.fab).setAlpha(1);
                        }
                        break;
                    case R.id.action_details:

                        //snackbar.setText("En desarrollo" + "... " +item.getTitle());

                        break;
                    case R.id.action_delete:
                        if(SelectFolderActivity.isPlaying){
                            SelectFolderActivity.mediaPlayer.stop();
                            SelectFolderActivity.isPlaying = false;
                        }
                        TrackAdapter.this.remove(TrackAdapter.this.getItem(TrackAdapter.this.position));
                        SelectFolderActivity.selectedTracks.remove((Integer) TrackAdapter.this.position);
                        Log.d("NUMERO DE ITEMS",TrackAdapter.this.getCount()+"");

                        //snackbar.setText("En desarrollo" + "... " +item.getTitle());

                        break;
                    default:
                        break;
                }

                return false;
            }
        });
    }



    private static class DataTrackHolder extends Object{
        DataTrackHolder(){};
        ImageButton imageButton ;
        Button button ;
        TextView trackName ;
        TextView artistName ;
        TextView albumName ;
    }
}



package mx.dev.franco.musicallibraryorganizer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by franco on 4/12/16.
 */

class TrackAdapter extends ArrayAdapter<AudioItem> implements Filterable{
    private int position;
    private Context context;
    protected ArrayList<AudioItem> audioTracks;
    protected CustomFilter customFilter;
    protected ArrayList<AudioItem> originalAudioTracks = null;

    @SuppressWarnings("unchecked")
    TrackAdapter(Context context, ArrayList<AudioItem> songs){
        super(context,0,songs);
        this.context = context;
        this.audioTracks = songs;
    }


    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull final ViewGroup parent){
        int ignoreRowPosition = position;
        // We get the item from current position of list.
        AudioItem track = (AudioItem) getItem(position);
        //Ignore some rows because of settings app
        if(!track.isVisible()){
            if(position < getCount()) {
                ignoreRowPosition++;
                track = getItem(ignoreRowPosition);
            }
        }


        assert track != null;



            DataTrackHolder dataTrackHolder = null;
            // Vamos reutilizando las vistas que son visibles
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.song_list, parent, false);
                dataTrackHolder = new DataTrackHolder();
                dataTrackHolder.itemList = (LinearLayout) convertView.findViewById(R.id.itemListView);
                dataTrackHolder.checked = (LinearLayout) convertView.findViewById(R.id.checkBoxTrack);
                dataTrackHolder.statusIcon = (LinearLayout) convertView.findViewById(R.id.statusProcess);
                dataTrackHolder.button = (Button) convertView.findViewById(R.id.contextualMenuButton);
                dataTrackHolder.trackName = (TextView) convertView.findViewById(R.id.track_name);
                dataTrackHolder.artistName = (TextView) convertView.findViewById(R.id.artist_name);
                dataTrackHolder.albumName = (TextView) convertView.findViewById(R.id.album_name);
                dataTrackHolder.progressBar = (ProgressBar) convertView.findViewById(R.id.progressProcessingFile);
                dataTrackHolder.progressBarPlaying = (ProgressBar) convertView.findViewById(R.id.progressBarPlaying);
                dataTrackHolder.duration = (TextView) convertView.findViewById(R.id.trackDuration);
                dataTrackHolder.path = (TextView) convertView.findViewById(R.id.path);
                convertView.setTag(dataTrackHolder);
            } else {
                dataTrackHolder = (DataTrackHolder) convertView.getTag();
            }

            //Log.d("SELECTED", String.valueOf(track.isProcessing()));

            if (track.isProcessing()) {
                dataTrackHolder.checked.setBackground(null);
                dataTrackHolder.progressBar.setVisibility(View.VISIBLE);
                convertView.setActivated(false);
            } else {
                convertView.setActivated(true);
                dataTrackHolder.progressBar.setVisibility(View.GONE);

                if (track.isSelected()) {
                    dataTrackHolder.checked.setBackground(this.context.getResources().getDrawable(R.drawable.checked2, null));
                } else {
                    dataTrackHolder.checked.setBackground(this.context.getResources().getDrawable(R.drawable.unchecked2, null));
                }

                if (track.getStatus() == AudioItem.FILE_STATUS_NO_PROCESSED) {
                    dataTrackHolder.statusIcon.setBackground(this.context.getDrawable(R.drawable.audio_file));
                } else {
                    switch (track.getStatus()) {
                        case AudioItem.FILE_STATUS_BAD:
                            dataTrackHolder.statusIcon.setBackground(this.context.getResources().getDrawable(R.drawable.fail, null));
                            break;
                        case AudioItem.FILE_STATUS_INCOMPLETE:
                            dataTrackHolder.statusIcon.setBackground(this.context.getResources().getDrawable(R.drawable.attention, null));
                            break;
                        case AudioItem.FILE_STATUS_OK:
                            dataTrackHolder.statusIcon.setBackground(this.context.getResources().getDrawable(R.drawable.ok, null));
                            break;
                        case AudioItem.FILE_STATUS_EDIT_BY_USER:
                            dataTrackHolder.statusIcon.setBackground(this.context.getResources().getDrawable(R.drawable.ic_edit_black_24dp, null));
                            break;
                        case AudioItem.FILE_STATUS_DOES_NOT_EXIST:
                            dataTrackHolder.statusIcon.setBackground(this.context.getResources().getDrawable(R.drawable.ic_remove_circle_black_24dp, null));
                            break;
                    }
                }

            }


            if (track.isPlayingAudio()) {
                dataTrackHolder.progressBarPlaying.setVisibility(View.VISIBLE);
            } else {
                dataTrackHolder.progressBarPlaying.setVisibility(View.GONE);
            }

            if (track.getTitle() != null) {
                dataTrackHolder.trackName.setText(track.getTitle());
            } else {
                dataTrackHolder.trackName.setText(this.context.getText(R.string.no_available));
            }

            if (track.getArtist() != null) {
                dataTrackHolder.artistName.setText(track.getArtist());
            } else {
                dataTrackHolder.artistName.setText(this.context.getText(R.string.no_available));
            }


            if (track.getAlbum() != null) {
                dataTrackHolder.albumName.setText(track.getAlbum());
            } else {
                dataTrackHolder.albumName.setText(this.context.getText(R.string.no_available));
            }

            dataTrackHolder.duration.setText(track.getHumanReadableDuration());
            dataTrackHolder.button.setTag(position);
            dataTrackHolder.checked.setTag(position);
            dataTrackHolder.statusIcon.setTag(position);
            dataTrackHolder.path.setTag(track.getNewAbsolutePath());




        return convertView;

    }

    @NonNull
    @Override
    public Filter getFilter(){
        if(this.customFilter == null){
            this.customFilter = new CustomFilter();
        }
        return this.customFilter;
    }

    private static class DataTrackHolder {
        DataTrackHolder(){};
        LinearLayout itemList;
        LinearLayout checked ;
        LinearLayout statusIcon;
        Button button ;
        TextView trackName ;
        TextView artistName ;
        TextView albumName ;
        TextView duration;
        TextView path;
        ProgressBar progressBar;
        ProgressBar progressBarPlaying;
    }

    private class CustomFilter extends Filter{
        @SuppressWarnings("unchecked")
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            //Log.d(this.getClass().getName(),constraint.toString());
            if(originalAudioTracks == null){
                originalAudioTracks = (ArrayList<AudioItem>) audioTracks.clone();
            }

            //Log.d("AUDIOTRACKS_SIZE", String.valueOf(originalAudioTracks.size()));
            FilterResults results = new FilterResults();
            if(constraint.toString().length() > 0 || constraint != null){
                ArrayList<AudioItem> resultList = new ArrayList<AudioItem>();
                for(int count = 0 ; count < originalAudioTracks.size() ;count++){
                    AudioItem tempItem = originalAudioTracks.get(count);
                    //Log.d("tempItem.getFileName()",tempItem.getFileName());
                    if(tempItem.getFileName().toLowerCase().contains(constraint.toString().toLowerCase())
                            //||((AudioItem)tempItem).getTitle().toLowerCase().contains(constraint.toString().toLowerCase())
                            //|| ((AudioItem)tempItem).getArtist().toLowerCase().contains(constraint.toString().toLowerCase())
                            //|| ((AUdioItem)tempItem).getAlbum().toLowerCase().contains(constraint.toString().toLowerCase())
                        ){
                        resultList.add(tempItem);
                    }
                }
                results.count = resultList.size();
                results.values = resultList;
            }
            else{
                //synchronized (this) {
                    results.count = originalAudioTracks.size();
                    results.values = originalAudioTracks;
                //}
            }
            return results;
        }
        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            //Log.d(this.getClass().getName(),"Publish results "+ results.count);
            ArrayList<AudioItem> filteredList = (ArrayList<AudioItem>) results.values;
            if(results.count == 0){
                notifyDataSetInvalidated();
            }
            else {
                clear();
                addAll(filteredList);
                notifyDataSetChanged();

            }
        }
    }
}



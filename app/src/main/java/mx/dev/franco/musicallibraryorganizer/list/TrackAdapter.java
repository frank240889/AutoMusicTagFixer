/*
 * Copyright (C) 2017 francocastillo2@hotmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */


package mx.dev.franco.musicallibraryorganizer.list;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import mx.dev.franco.musicallibraryorganizer.GlideApp;
import mx.dev.franco.musicallibraryorganizer.R;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.AudioItemHolder> implements Filterable {//extends SelectableAdapter<TrackAdapter.AudioItemHolder> implements Filterable{
    private final AudioItemHolder.ClickListener clickListener;
    private Context context;
    private List<AudioItem> currentList, currentfilteredList;
    private boolean allSelected = false;
    private CustomFilter customFilter;


    @SuppressWarnings("unchecked")
    public TrackAdapter(Context context, List<AudioItem> list, TrackAdapter.AudioItemHolder.ClickListener clickListener){
        this.context = context;
        this.currentList = list;
        this.currentfilteredList =  currentList;
        this.clickListener = clickListener;

    }

    @Override
    public AudioItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);

        return new AudioItemHolder(itemView, this.clickListener);
    }

    @Override
    public void onBindViewHolder(final AudioItemHolder holder, final int position) {
        AudioItem audioItem = currentList.get(position);
        holder.checkBox.setChecked(audioItem.isChecked());


        holder.progressBar.setVisibility(audioItem.isProcessing()?VISIBLE:GONE);

            //We need to load covers arts in other thread,
            //because this operation can reduce performance
            //in main thread
            new AsyncTask<String,byte[],Void>(){

                @Override
                protected Void doInBackground(String... params) {
                    MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                    String path = params[0];

                    File file = new File(path);
                    if(!file.exists()) {
                        publishProgress(null);
                        file = null;
                        return null;
                    }

                    mediaMetadataRetriever.setDataSource(path);
                    byte[] cover = mediaMetadataRetriever.getEmbeddedPicture();
                    publishProgress(cover);
                    mediaMetadataRetriever.release();
                    mediaMetadataRetriever = null;
                    System.gc();
                    return null;
                }
                @Override
                protected void onPostExecute(Void voids){

                }

                @Override
                protected void onProgressUpdate(byte[]... cover){
                    GlideApp.with(context).
                            load(cover == null ? context.getResources().getDrawable(R.drawable.ic_album_white_48px,null) : cover[0] )
                            .placeholder(R.drawable.ic_album_white_48px)
                            .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                            .apply(RequestOptions.skipMemoryCacheOf(true))
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .fitCenter()
                            .into(holder.imageView);

                }
            }.execute(audioItem.getAbsolutePath());

        if (audioItem.isPlayingAudio()) {
            GlideApp.with(holder.imageView).load(R.drawable.playing).centerInside().into(holder.checkMark);
            holder.playButton.setImageResource(R.drawable.ic_stop_white_24px);
        } else {
            holder.playButton.setImageResource(R.drawable.ic_play_arrow_white_24px);

            switch (audioItem.getStatus()){
                case AudioItem.FILE_STATUS_OK:
                case AudioItem.FILE_STATUS_EDIT_BY_USER:
                    holder.checkMark.setImageResource(R.drawable.ic_done_all_white);
                    break;
                case AudioItem.FILE_STATUS_INCOMPLETE:
                    holder.checkMark.setImageResource(R.drawable.ic_done_white);
                    break;
                case AudioItem.FILE_STATUS_BAD:
                    holder.checkMark.setImageResource(R.drawable.ic_error_outline_white);
                    break;
                default:
                    holder.checkMark.setImageResource(0);
                    break;
            }
        }

        holder.trackName.setText(audioItem.getTitle());
        holder.artistName.setText(audioItem.getArtist());
        holder.albumName.setText(audioItem.getAlbum());

        holder.checkBox.setTag(audioItem.getId());
        holder.playButton.setTag(audioItem.getId());
        holder.absolutePath.setTag(audioItem.getAbsolutePath());

    }

    public boolean areAllSelected(){
        return this.allSelected;
    }

    public TrackAdapter setAllSelected(boolean allSelected){
        this.allSelected = allSelected;
        return this;
    }


    @Override
    public int getItemCount() {
        if(currentList != null)
            return currentList.size();
        return 0;
    }


    @Override
    public void setHasStableIds(boolean hasStableIds) {
        super.setHasStableIds(true);
    }

    public AudioItem getItemByIdOrPath(long id, String path){
        AudioItem audioItem = null;

        if(id != -1){
            for(int t = 0; t < getItemCount() ; t++){
                if(currentList.get(t).getId() == id ){
                    audioItem =  currentList.get(t);
                    audioItem.setPosition(t);
                    break;
                }
            }
            return audioItem;
        }

        if(path != null && !path.equals("")){
            for(int t = 0; t < getItemCount() ; t++){
                if(currentList.get(t).getAbsolutePath().equals(path)){
                    audioItem = currentList.get(t);
                    audioItem.setPosition(t);
                    break;
                }
            }
        }

        return audioItem;
    }


        public static class AudioItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        CheckBox checkBox ;
        TextView trackName ;
        TextView artistName ;
        TextView albumName ;
        TextView absolutePath;
        ProgressBar progressBar;
        ImageView imageView, checkMark;
        ImageButton playButton;
        ClickListener listener;

        public AudioItemHolder(View itemView, ClickListener clickListener) {
            super(itemView);
            checkBox = (CheckBox) itemView.findViewById(R.id.checkBoxTrack);
            checkBox.setChecked(false);
            trackName = (TextView) itemView.findViewById(R.id.track_name);
            artistName = (TextView) itemView.findViewById(R.id.artist_name);
            albumName = (TextView) itemView.findViewById(R.id.album_name);
            progressBar = (ProgressBar) itemView.findViewById(R.id.progressProcessingFile);

            absolutePath = (TextView) itemView.findViewById(R.id.absolute_path);
            imageView = (ImageView) itemView.findViewById(R.id.coverArt);
            checkMark = (ImageView) itemView.findViewById(R.id.checkMark);
            playButton = (ImageButton) itemView.findViewById(R.id.playButton);
            listener = clickListener;
            playButton.setOnClickListener(this);
            checkBox.setOnClickListener(this);
            imageView.setOnClickListener(this);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (listener != null) {
                listener.onItemClicked(getLayoutPosition(),v);
            }
        }

        public interface ClickListener {
            void onItemClicked(int position, View v);
        }
    }


    @NonNull
    @Override
    public Filter getFilter(){
        if(this.customFilter == null){
            this.customFilter = new CustomFilter();
        }
        return this.customFilter;
    }

    private class CustomFilter extends Filter {

        @SuppressWarnings("unchecked")
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            String charString = constraint.toString();

            Log.d("filtering", currentList.size() + "-");
            if (charString.isEmpty()) {
                currentList = currentfilteredList;
            }
            else {
                List<AudioItem> filteredList = new ArrayList<>();

                for (AudioItem audioItem : currentfilteredList) {
                    if (audioItem.getTitle().toLowerCase().contains(charString.toLowerCase())) {

                        filteredList.add(audioItem);
                    }
                }

                currentList = filteredList;

            }

            FilterResults  filterResults = new FilterResults();
            filterResults.values = currentList;
            filterResults.count = currentList.size();

            return filterResults;
        }
        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {

            currentList = (ArrayList<AudioItem>) results.values;
            notifyDataSetChanged();
        }
    }



}



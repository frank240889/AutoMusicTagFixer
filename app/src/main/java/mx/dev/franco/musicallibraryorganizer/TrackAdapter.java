package mx.dev.franco.musicallibraryorganizer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.view.View.GONE;

public class TrackAdapter extends SelectableAdapter<TrackAdapter.AudioItemHolder> implements Filterable{
    private final AudioItemHolder.ClickListener clickListener;
    private Context context;
    private List<AudioItem> audioTracks = new ArrayList<>();
    private CustomFilter customFilter;
    private String previousPath = "";
    private String currentPath = "";
    private String nextPath = "";
    private int nextPosition = 0, previousPosition = 0;
    private boolean sorted = false;
    private boolean allSelected = false;
    private static final int TYPE_INACTIVE = 0;
    private static final int TYPE_ACTIVE = 1;

    public static class AudioItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        RelativeLayout itemList;
        CheckBox checkBox ;
        TextView trackName ;
        TextView artistName ;
        TextView albumName ;
        TextView duration;
        TextView path;
        TextView pathSeparator;
        ProgressBar progressBar;
        ImageView imageView, checkMark;
        View selectedOverlay;
        private ClickListener listener;

        public AudioItemHolder(View itemView, ClickListener clickListener) {
            super(itemView);
            itemList = (RelativeLayout) itemView.findViewById(R.id.itemListView);
            checkBox = (CheckBox) itemView.findViewById(R.id.checkBoxTrack);
            checkBox.setChecked(false);
            trackName = (TextView) itemView.findViewById(R.id.track_name);
            artistName = (TextView) itemView.findViewById(R.id.artist_name);
            albumName = (TextView) itemView.findViewById(R.id.album_name);
            progressBar = (ProgressBar) itemView.findViewById(R.id.progressProcessingFile);
            duration = (TextView) itemView.findViewById(R.id.trackDuration);
            path = (TextView) itemView.findViewById(R.id.path);
            pathSeparator = (TextView) itemView.findViewById(R.id.separator);
            pathSeparator.setVisibility(GONE);
            imageView = (ImageView) itemView.findViewById(R.id.coverArt);
            selectedOverlay = itemView.findViewById(R.id.selectedOverlay);
            checkMark = (ImageView) itemView.findViewById(R.id.checkMark);
            this.listener = clickListener;
            checkBox.setOnClickListener(this);
            imageView.setOnClickListener(this);
            itemList.setOnClickListener(this);
            itemList.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (listener != null) {
                listener.onItemClicked(getLayoutPosition(),v);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (listener != null) {
                return listener.onItemLongClicked(getLayoutPosition(),v);
            }

            return false;
        }

        public interface ClickListener {
            void onItemClicked(int position, View v);
            boolean onItemLongClicked(int position, View v);
        }
    }

    @SuppressWarnings("unchecked")
    TrackAdapter(Context context, List<AudioItem> songs, TrackAdapter.AudioItemHolder.ClickListener clickListener){
        super();
        this.context = context;
        this.audioTracks = songs;
        this.clickListener = clickListener;
    }


    @Override
    public AudioItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.song_list, parent, false);

        return new AudioItemHolder(itemView, this.clickListener);
    }

    @Override
    public void onBindViewHolder(AudioItemHolder holder, int position) {
        final AudioItem track =  audioTracks.get(position);
        assert track != null;
        //Log.d("adapter_position",position+"");
        currentPath = track.getPath();

        AudioItem nextItem, previousItem;

         //If option "Mostrar separadores" is active
        if(SelectedOptions.SHOW_SEPARATORS) {
            if (audioTracks.size() <= 2) {
                nextPath = "";
                previousPath = "";
            } else {
                if (position == 0) { //first item? get next item too
                    nextPosition = position + 1;
                    nextItem = audioTracks.get(nextPosition);
                    nextPath = nextItem.getPath();
                    previousItem = null;
                    previousPath = "";

                } else if (position >= audioTracks.size() - 1) { //last item? get previous item too
                    nextItem = null;
                    nextPath = "";
                    previousPosition = position - 1;
                    previousItem = audioTracks.get(previousPosition);
                    previousPath = previousItem.getPath();

                } else {                                    //another intermediate item? get previuos and next item respect to current
                    nextPosition = position + 1;
                    nextItem = audioTracks.get(nextPosition);
                    nextPath = nextItem.getPath();

                    previousPosition = position - 1;
                    previousItem = audioTracks.get(previousPosition);
                    previousPath = previousItem.getPath();
                }
            }


            if (position == 0) { //First audio item
                holder.pathSeparator.setText(AudioItem.getRelativePath(currentPath));
                holder.pathSeparator.setVisibility(View.VISIBLE);
            } else { //subsequent audio items
                if (!currentPath.equals(previousPath) && currentPath.equals(nextPath)) { //Scrolling down
                    holder.pathSeparator.setText(AudioItem.getRelativePath(nextPath));
                    holder.pathSeparator.setVisibility(View.VISIBLE);
                } else {
                    holder.pathSeparator.setVisibility(GONE);
                    previousPath = "";
                    nextPath = "";
                    currentPath = "";
                }
            }
        }
        else {
            holder.pathSeparator.setVisibility(GONE);
        }

        if (track.isProcessing()) {
            holder.checkBox.setEnabled(false);
            holder.progressBar.setVisibility(View.VISIBLE);
        } else {
            holder.checkBox.setEnabled(true);
            holder.progressBar.setVisibility(GONE);
        }


        if (track.isChecked()) {
            holder.checkBox.setChecked(true);
        } else {
            holder.checkBox.setChecked(false);
        }

        holder.selectedOverlay.setVisibility(isSelected(position) ? View.VISIBLE : View.INVISIBLE);

        //Log.d("is activated",track.isSelected()+"");

        if (track.isPlayingAudio()) {
            GlideApp.with(holder.imageView).load(R.drawable.playing).centerInside().into(holder.imageView);
        } else {
            if (track.getCoverArt() != null) {
                GlideApp.with(holder.imageView).
                        load(track.getCoverArt())
                        .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                        .apply(RequestOptions.skipMemoryCacheOf(true))
                        .centerCrop()
                        .into(holder.imageView);
            }
            else {
                GlideApp.with(holder.imageView).load(R.drawable.nocoverart).fitCenter().into(holder.imageView);
            }
        }

        if (track.getTitle() != null) {
            holder.trackName.setText(track.getTitle());
        } else {
            holder.trackName.setText(this.context.getText(R.string.no_available));
        }

        if (track.getArtist() != null) {
            holder.artistName.setText(track.getArtist());
        } else {
            holder.artistName.setText(this.context.getText(R.string.no_available));
        }


        if (track.getAlbum() != null) {
            holder.albumName.setText(track.getAlbum());
        } else {
            holder.albumName.setText(this.context.getText(R.string.no_available));
        }


        switch (track.getStatus()){
            case AudioItem.FILE_STATUS_OK:
            case AudioItem.FILE_STATUS_EDIT_BY_USER:
                holder.checkMark.setImageResource(R.drawable.ic_star_black_24dp);
                holder.checkMark.setBackground(context.getResources().getDrawable(R.drawable.ic_star_black_24dp,null));
                break;
            case AudioItem.FILE_STATUS_INCOMPLETE:
                holder.checkMark.setImageResource(R.drawable.ic_star_half_black_24dp);
                holder.checkMark.setBackground(context.getResources().getDrawable(R.drawable.ic_star_half_black_24dp,null));
                break;
            case AudioItem.FILE_STATUS_BAD:
                holder.checkMark.setImageResource(R.drawable.ic_error_outline_black_24dp);
                holder.checkMark.setBackground(context.getResources().getDrawable(R.drawable.ic_error_outline_black_24dp,null));
                break;
            default:
                holder.checkMark.setImageResource(R.drawable.ic_star_border_black_24dp);
                holder.checkMark.setBackground(context.getResources().getDrawable(R.drawable.ic_star_border_black_24dp,null));
                break;
        }

        holder.duration.setText(track.getHumanReadableDuration());
        holder.checkBox.setTag(track.getId());
        holder.path.setTag(track.getId());

    }

    @Override
    public int getItemCount() {
        return audioTracks.size();
    }

    /** This method sorts data by path field,
     * in ascendent order
     */
    void sortByPath() {
        Comparator<AudioItem> comparator = new Comparator<AudioItem>() {

            @Override
            public int compare(AudioItem audioItem1, AudioItem audioItem2) {
                return audioItem1.getPath().compareToIgnoreCase(audioItem2.getPath());
            }
        };
        Collections.sort(audioTracks, comparator);
        renewItemsPositions();
        setSorted(true);
    }

    @Override
    public int getItemViewType(int position) {
        final AudioItem audioItem = audioTracks.get(position);

        return audioItem.isSelected() ? TYPE_ACTIVE : TYPE_INACTIVE;
    }

    public boolean isSorted() {
        return this.sorted;
    }

    public TrackAdapter setSorted(boolean sorted){
        this.sorted = sorted;
        return this;
    }

    public boolean areAllSelected(){
        return this.allSelected;
    }

    public TrackAdapter setAllSelected(boolean allSelected){
        this.allSelected = allSelected;
        return this;
    }

    /**
     * This method renew positions of
     * every item in ascendent order
     */
    void renewItemsPositions(){
        int count =  getItemCount();
        for (int i = 0 ; i < count ; i++){
            Log.d("old_position",audioTracks.get(i).getPosition()+"");
            audioTracks.get(i).setPosition(i);
        }
    }

    public void removeItem(int position) {
        audioTracks.remove(position);
        notifyItemRemoved(position);
    }

    public void removeItems(List<Integer> positions) {
        // Reverse-sort the list
        Collections.sort(positions, new Comparator<Integer>() {
            @Override
            public int compare(Integer lhs, Integer rhs) {
                return rhs - lhs;
            }
        });

        // Split the list in ranges
        while (!positions.isEmpty()) {
            if (positions.size() == 1) {
                removeItem(positions.get(0));
                positions.remove(0);
            } else {
                int count = 1;
                while (positions.size() > count && positions.get(count).equals(positions.get(count - 1) - 1)) {
                    ++count;
                }

                if (count == 1) {
                    removeItem(positions.get(0));
                } else {
                    removeRange(positions.get(count - 1), count);
                }

                for (int i = 0; i < count; ++i) {
                    positions.remove(0);
                }
            }
        }
    }

    private void removeRange(int positionStart, int itemCount) {
        for (int i = 0; i < itemCount; ++i) {
            audioTracks.remove(positionStart);
        }
        notifyItemRangeRemoved(positionStart, itemCount);
    }

    @NonNull
    @Override
    public Filter getFilter(){
        if(this.customFilter == null){
            this.customFilter = new CustomFilter();
        }
        return this.customFilter;
    }

    private class CustomFilter extends Filter{
        List<AudioItem> audioItemList;

        CustomFilter(){
            if(this.audioItemList == null) {
                audioItemList = new ArrayList<>();
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            FilterResults results = new FilterResults();
            if(constraint.toString().length() == 0){
                results.values = audioTracks;
                results.count = audioTracks.size();
            }
            else{
                List<AudioItem> resultList = new ArrayList<>();
                for(int count = 0 ; count < SelectFolderActivity.audioItemList.size() ;count++){
                    AudioItem tempItem = SelectFolderActivity.audioItemList.get(count);
                    //Log.d("tempItem.getFileName()",tempItem.getFileName());
                    if(tempItem.getFileName().toLowerCase().contains(constraint.toString().toLowerCase())){
                        resultList.add(tempItem);
                    }
                }
                results.count = resultList.size();
                results.values = resultList;
            }
            return results;
        }
        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            //Log.d(this.getClass().getName(),"Publish results "+ results.count);
            ArrayList<AudioItem> filteredList = (ArrayList<AudioItem>) results.values;
            if(results.count == 0){
                //notifyDataSetInvalidated();
            }
            else {
                audioItemList.clear();
                audioItemList.addAll(filteredList);
                //TrackAdapter.this.
                //addAll(filteredList);
                notifyDataSetChanged();

            }
        }
    }

}



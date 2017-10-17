package mx.dev.franco.musicallibraryorganizer.list;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
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
import java.util.Comparator;
import java.util.List;

import mx.dev.franco.musicallibraryorganizer.R;
import mx.dev.franco.musicallibraryorganizer.database.TrackContract;
import mx.dev.franco.musicallibraryorganizer.utilities.GlideApp;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.AudioItemHolder> implements Filterable {//extends SelectableAdapter<TrackAdapter.AudioItemHolder> implements Filterable{
    //constants for indacate the sort order
    public static final int ASC = 0;
    public static final int DESC = 1;
    //listener attached to every item in listview
    private final AudioItemHolder.ClickListener clickListener;
    //we need the context to access some features like R class
    private Context context;
    //List of audioitems, we need to references: one for the filtered one
    //and the other one to original list
    private List<AudioItem> currentList, currentFilteredList;
    //this property indicate us if all items were selected
    //not used, for now
    private boolean allSelected = false;
    //filter for filteriing search results
    private CustomFilter customFilter;
    //Comparator for ordering the items in list
    private static Sorter sorter;

    @SuppressWarnings("unchecked")
    public TrackAdapter(Context context, List<AudioItem> list, TrackAdapter.AudioItemHolder.ClickListener clickListener){
        this.context = context;
        this.currentList = list;
        this.currentFilteredList =  currentList;
        this.clickListener = clickListener;

    }

    /**
     * Called when RecyclerView needs a new RecyclerView.ViewHolder of the given type to represent an item
     * This new ViewHolder should be constructed with a new View that can represent the items of the given type.
     * You can either create a new View manually or inflate it from an XML layout file.
     * The new ViewHolder will be used to display items of the adapter using onBindViewHolder(ViewHolder, int, List).
     * Since it will be re-used to display different items in the data set,
     * it is a good idea to cache references to sub views of the View to avoid unnecessary findViewById(int) calls.
     * @param parent
     * @param viewType
     * @return
     */
    @Override
    public AudioItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list, parent, false);

        return new AudioItemHolder(itemView, this.clickListener);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * This method should update the contents of the itemView to reflect the item at the given position.
     * Note that unlike ListView, RecyclerView will not call this method again if the position of
     * the item changes in the data set unless the item itself is invalidated or
     * the new position cannot be determined. For this reason, you should only use
     * the position parameter while acquiring the related data item inside
     * this method and should not keep a copy of it. If you need the position of an
     * item later on (e.g. in a click listener), use getAdapterPosition()
     * which will have the updated adapter position. Override onBindViewHolder(ViewHolder, int, List)
     * instead if Adapter can handle efficient partial bind.
     * @param holder
     * @param position
     */
    @Override
    public void onBindViewHolder(final AudioItemHolder holder, final int position) {
        AudioItem audioItem = currentList.get(position);
        holder.checkBox.setChecked(audioItem.isChecked());

        holder.progressBar.setVisibility(audioItem.isProcessing()?VISIBLE:GONE);

            //We need to load covers arts in other thread,
            //because this operation is going reduce performance
            //in main thread, making the scroll very laggy
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

                switch (audioItem.getStatus()) {
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

    /**
     * Getter for allSelected property
     * @return true if are all selected, false otherwise
     */
    public boolean areAllSelected(){
        return this.allSelected;
    }

    /**
     * Setter for allSelected property
     * @return this object adapter.
     */
    public TrackAdapter setAllSelected(boolean allSelected){
        this.allSelected = allSelected;
        return this;
    }

    /**
     * Get size of data source
     * @return zero if data source is null, otherwise size of data source
     */
    @Override
    public int getItemCount() {
        if(currentList != null)
            return currentList.size();
        return 0;
    }

    /**
     * Indicates whether each item in the data set
     * can be represented with a unique identifier of type Long.
     * @param hasStableIds
     */
    @Override
    public void setHasStableIds(boolean hasStableIds) {
        super.setHasStableIds(true);
    }

    /**
     * Return the corresponding audio item
     * that has the id or path passed as parameter
     * @param id
     * @param path
     * @return
     */
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


    /**
     * This class helps to maintain the reference to
     * every element of item, avoiding call findViewById()
     * in every element for data source, making a considerable
     * improvement in performance of list
     */
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

        /**
         * This method of listener is implemented in
         * activity that creates the adapter and data source
         * @param v
         */
        @Override
        public void onClick(View v) {
            if (listener != null) {
                listener.onItemClicked(getLayoutPosition(),v);
            }
        }

        /**
         * This method is implemented
         * in activity that needs to handle
         * clicks
         */

        public interface ClickListener {
            void onItemClicked(int position, View v);
        }
    }

    /**
     * The instance of this class handles the sort
     * of the list
     */
    public static class Sorter implements Comparator<AudioItem> {
        //default sort if no provided
        private int sortType = ASC;
        //default sort field if no provided
        private String sortByField = TrackContract.TrackData.DATA;

        //don't instantiate objects, we only need one
        private Sorter(){}

        public static Sorter getInstance(){
            if(sorter == null){
                sorter = new Sorter();
            }
            return sorter;
        }

        /**
         * Seta sort params: order by field and ascendent 0
         * or descendant 1
         * @param sortByField
         * @param sortType
         */
        public void setSortParams(String sortByField, int sortType){
            this.sortType = sortType;
            this.sortByField = sortByField;
        }

        @Override
        public int compare(AudioItem audioItem1, AudioItem audioItem2) {
            String str1 = null;
            String str2 = null;
            String str1ToCompare = null;
            String str2ToCompare = null;

            switch (sortByField) {
                case TrackContract.TrackData.TITLE:
                    str1 = audioItem1.getTitle();
                    str2 = audioItem2.getTitle();
                    break;
                case TrackContract.TrackData.ARTIST:
                    str1 = audioItem1.getArtist();
                    str2 = audioItem2.getArtist();
                    break;
                case TrackContract.TrackData.ALBUM:
                    str1 = audioItem1.getAlbum();
                    str2 = audioItem2.getAlbum();
                    break;
                default:
                    str1 = audioItem1.getAbsolutePath();
                    str2 = audioItem2.getAbsolutePath();
                    break;
            }

            if(sortType == DESC) {
                str1ToCompare = str1;
                str2ToCompare = str2;
            }
            else{
                str1ToCompare = str2;
                str2ToCompare = str1;
            }
            return str2ToCompare.compareToIgnoreCase(str1ToCompare);
        }
    }


    /**
     * This method creates a filter for filtering results when
     * an item y searched in search widget
     * @return a unique instance of filter
     */
    @NonNull
    @Override
    public Filter getFilter(){
        if(this.customFilter == null){
            this.customFilter = new CustomFilter();
        }
        return this.customFilter;
    }

    /**
     * class that filters a results of search widget
     */
    private class CustomFilter extends Filter {

        @SuppressWarnings("unchecked")
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            String charString = constraint.toString();

            //is empty then is not needed to search
            //so we reference to the original list a return all results
            if (charString.isEmpty()) {
                currentList = currentFilteredList;
            }
            else {
                //creates a temporal filtered list
                List<AudioItem> filteredList = new ArrayList<>();

                //here we define the parameter in which the results are based
                for (AudioItem audioItem : currentFilteredList) {
                    if (audioItem.getTitle().toLowerCase().contains(charString.toLowerCase())) {
                        //if this item satisfy the criteria of search then add to temporal list
                        filteredList.add(audioItem);
                    }
                }
                //now our current list references to filtered list
                //but we don't lose the orinal reference, because when the search
                //finish we need the original list.
                currentList = filteredList;

            }

            FilterResults  filterResults = new FilterResults();
            filterResults.values = currentList;
            filterResults.count = currentList.size();

            return filterResults;
        }

        /**
         * Callback to publish results
         * @param constraint
         * @param results
         */
        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {

            currentList = (ArrayList<AudioItem>) results.values;
            //inform to recycler view to update the views.
            notifyDataSetChanged();
        }
    }



}



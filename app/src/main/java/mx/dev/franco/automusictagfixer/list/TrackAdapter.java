package mx.dev.franco.automusictagfixer.list;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import mx.dev.franco.automusictagfixer.MainActivity;
import mx.dev.franco.automusictagfixer.R;
import mx.dev.franco.automusictagfixer.SplashActivity;
import mx.dev.franco.automusictagfixer.database.TrackContract;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.GlideApp;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.AudioItemHolder> implements Filterable {
    //constants for indacate the sort order
    public static final int ASC = 0;
    public static final int DESC = 1;
    //mListener attached to every item in listview
    private AudioItemHolder.ClickListener mClickListener;
    //we need the context to access some features like R class
    private Context mContext;
    //List of audioitems, we need to references: one for the filtered one
    //and the other one to original list
    private List<AudioItem> mCurrentList, mCurrentFilteredList;
    //this property indicate us if all items were selected
    //not used, for now
    private boolean mAllSelected = false;
    //filters search results
    private CustomFilter mCustomFilter;
    //Comparator for ordering list
    private static Sorter mSorter;

    private static AsyncLoadCover asyncLoadCover;
    private int mVerticalScrollSpeed = 0;
    private int mScrollingState = 0;

    @SuppressWarnings("unchecked")
    public TrackAdapter(Context context, List<AudioItem> list, TrackAdapter.AudioItemHolder.ClickListener clickListener){
        this.mContext = context;
        this.mCurrentList = list;
        this.mCurrentFilteredList = mCurrentList;
        this.mClickListener = clickListener;

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

        return new AudioItemHolder(itemView, this.mClickListener);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * This method should update the contents of the itemView to reflect the item at the given position.
     * Note that unlike ListView, RecyclerView will not call this method again if the position of
     * the item changes in the data set unless the item itself is invalidated or
     * the new position cannot be determined. For this reason, you should only use
     * the position parameter while acquiring the related data item inside
     * this method and should not keep a copy of it. If you need the position of an
     * item later on (e.g. in a click mListener), use getAdapterPosition()
     * which will have the updated adapter position. Override onBindViewHolder(ViewHolder, int, List)
     * instead if Adapter can handle efficient partial bind.
     * @param holder
     * @param position
     */
    @Override
    public void onBindViewHolder(final AudioItemHolder holder, int position) {
        AudioItem audioItem = mCurrentList.get(position);
        holder.mCheckBox.setChecked(audioItem.isChecked());

        if (audioItem.isProcessing()) {
            holder.itemView.setEnabled(false);
            holder.mProgressBar.setVisibility(VISIBLE);
        } else {
            holder.itemView.setEnabled(true);
            holder.mProgressBar.setVisibility(GONE);
        }

        //don't load covers if user is scrolling too fast
       if(this.mVerticalScrollSpeed <= 450 && this.mVerticalScrollSpeed >= -450) {
           //We need to read cover arts in other thread,
           //because this operation is going to reduce performance
           //in main thread, making the scroll very laggy
            asyncLoadCover = new AsyncLoadCover(holder, mContext);
            asyncLoadCover.execute(audioItem.getAbsolutePath());
       }

            switch (audioItem.getStatus()) {
                case AudioItem.STATUS_TAGS_CORRECTED_BY_SEMIAUTOMATIC_MODE:
                case AudioItem.STATUS_ALL_TAGS_FOUND:
                case AudioItem.STATUS_TAGS_EDITED_BY_USER:
                    holder.mCheckMark.setImageResource(R.drawable.ic_done_all_white);
                    break;
                case AudioItem.STATUS_ALL_TAGS_NOT_FOUND:
                    holder.mCheckMark.setImageResource(R.drawable.ic_done_white);
                    break;
                case AudioItem.STATUS_NO_TAGS_FOUND:
                    holder.mCheckMark.setImageResource(R.drawable.ic_error_outline_white);
                    break;
                case AudioItem.FILE_ERROR_READ:
                    holder.mCheckMark.setImageResource(R.drawable.ic_highlight_off_white_material);
                    break;
                default:
                    holder.mCheckMark.setImageResource(0);
                    break;
            }



        holder.mTrackName.setText(audioItem.getTitle());
        holder.mArtistName.setText(audioItem.getArtist());
        holder.mAlbumName.setText(audioItem.getAlbum());

        holder.mCheckBox.setTag(audioItem.getId());
        holder.mAbsolutePath.setTag(audioItem.getAbsolutePath());

    }

    @Override
    public void onViewRecycled(TrackAdapter.AudioItemHolder holder) {
        super.onViewRecycled(holder);
        if(mContext != null)
            Glide.with(mContext).clear(holder.mImageView);
    }

    public void setVerticalSpeedScroll(int dy){
        mVerticalScrollSpeed = dy;
    }

    public void setScrollState(int state){
        mScrollingState = state;

    }

    /**
     * Check if all items were previously checked
     * @return true if are all selected, false otherwise
     */
    public boolean areAllChecked(){
        return mContext.getSharedPreferences(SplashActivity.APP_SHARED_PREFERENCES, Context.MODE_PRIVATE).
                getBoolean(Constants.ALL_ITEMS_CHECKED, false);
    }

    /**
     * Sets new state to ALL_ITEMS_CHECKED
     * @param allChecked
     */
    public void setAllChecked(boolean allChecked){
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(SplashActivity.APP_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(Constants.ALL_ITEMS_CHECKED, allChecked);
        editor.apply();
        editor = null;
        sharedPreferences = null;
    }

    /**
     * Get size of data source
     * @return zero if data source is null, otherwise size of data source
     */
    @Override
    public int getItemCount() {
        if(mCurrentList != null)
            return mCurrentList.size();
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
     * that has the id passed as parameter
     * @param id The id to search
     * @return An AudioItem object if was found, null otherwise
     */
    public AudioItem getAudioItemByIdOrPath(long id){
        AudioItem audioItem = null;

        for(int t = 0; t < getItemCount() ; t++){
            if(mCurrentList.get(t).getId() == id ){
                audioItem =  mCurrentList.get(t);
                audioItem.setPosition(t);
                break;
            }
        }

        return audioItem;
    }

    /**
     * Return the corresponding audio item
     * that has the path passed as parameter
     * @param path The path to search
     * @return An AudioItem object if was found, null otherwise
     */
    public AudioItem getAudioItemByIdOrPath(String path){
        AudioItem audioItem = null;
        if(path != null && !path.equals("")){
            for(int t = 0; t < getItemCount() ; t++){
                if(mCurrentList.get(t).getAbsolutePath().equals(path)){
                    audioItem = mCurrentList.get(t);
                    audioItem.setPosition(t);
                    break;
                }
            }
        }

        return audioItem;
    }





    public AudioItem getAudioItemByPosition(int position){
        AudioItem audioItem = null;
        for(int t = 0; t < getItemCount() ; t++){
            if(position == t ){
                audioItem =  mCurrentList.get(t);
                break;
            }
        }
        return audioItem;
    }

    /**
     * Sets as false their processing state
     * of items were processing by FixerTrackService service
     * and updates UI
     */
    public void cancelProcessing(){
        for (int t = 0; t < mCurrentList.size(); t++) {
            AudioItem audioItem = mCurrentList.get(t);
            if (audioItem.isProcessing()) {
                audioItem.setProcessing(false);
                notifyItemChanged(t);
            }
        }
    }

    public void checkAudioItem(long id, boolean checked){
        int position;
        if(id != -1){
            AudioItem audioItem = getAudioItemByIdOrPath(id);
            audioItem.setChecked(checked);
            position = audioItem.getPosition();
            notifyItemChanged(position);
            audioItem.setPosition(-1);
        }
        else {
            for (int t = 0; t < mCurrentList.size(); t++) {
                AudioItem audioItem = mCurrentList.get(t);
                    audioItem.setChecked(checked);
                    notifyItemChanged(t);
            }
        }

    }

    /**
     * Count how many items were checked
     * in list
     * @return Number of checked items
     */
    public int getCountSelectedItems(){
        int numberOfSelectedItems = 0;
        if(mCurrentList != null) {
            for (int t = 0; t < mCurrentList.size(); t++) {
                if (mCurrentList.get(t).isChecked()) {
                    numberOfSelectedItems++;
                }
            }
        }

        return numberOfSelectedItems;
    }

    /**
     * This class helps to maintain the reference to
     * every element of item, avoiding call findViewById()
     * in every element for data source, making a considerable
     * improvement in performance of list
     */
    public static class AudioItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public CheckBox mCheckBox;
        public TextView mTrackName;
        public TextView mArtistName;
        public TextView mAlbumName;
        public TextView mAbsolutePath;
        public ProgressBar mProgressBar;
        public ImageView mImageView;
        public ImageView mCheckMark;
        public ClickListener mListener;

        public AudioItemHolder(View itemView, ClickListener clickListener) {
            super(itemView);
            mCheckBox = (CheckBox) itemView.findViewById(R.id.checkBoxTrack);
            mCheckBox.setChecked(false);
            mTrackName = (TextView) itemView.findViewById(R.id.track_name);
            mArtistName = (TextView) itemView.findViewById(R.id.artist_name);
            mAlbumName = (TextView) itemView.findViewById(R.id.album_name);
            mProgressBar = (ProgressBar) itemView.findViewById(R.id.progressProcessingFile);

            mAbsolutePath = (TextView) itemView.findViewById(R.id.absolute_path);
            mImageView = (ImageView) itemView.findViewById(R.id.coverArt);
            mCheckMark = (ImageView) itemView.findViewById(R.id.checkMark);
            mListener = clickListener;
            mCheckBox.setOnClickListener(this);
            mImageView.setOnClickListener(this);
            itemView.setOnClickListener(this);
        }

        /**
         * This method of mListener is implemented in
         * activity that creates the adapter and data source
         * @param v
         */
        @Override
        public void onClick(View v) {
            if (mListener != null) {
                mListener.onItemClicked(getLayoutPosition(),v);
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
     * This class should be static for avoiding memory leaks,
     * then we use weak references to resources needed inside
     */
    public static class AsyncLoadCover extends AsyncTask<String, byte[], Void> {

        private WeakReference<AudioItemHolder> mHolder;
        private WeakReference<Context> mContext;

        private AsyncLoadCover(AudioItemHolder holder, Context context) {
            this.mHolder = new WeakReference<>(holder);
            this.mContext = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(String... params) {
            String path = params[0];
            File file = new File(path);
            if(!file.exists()) {
                publishProgress(null);
                file = null;
                return null;
            }

            try {
                AudioFile audioTaggerFile = AudioFileIO.read(new File(path));
                Tag tag = null;
                byte[] cover = null;
                if (audioTaggerFile.getTag() == null) {
                    publishProgress(null);
                    return null;

                }

                tag = audioTaggerFile.getTag();

                if (tag.getFirstArtwork() == null) {
                    publishProgress(null);
                    return null;
                }

                if(tag.getFirstArtwork().getBinaryData() == null){
                    publishProgress(null);
                    return null;
                }

                //Log.d("cover", (cover == null) + " - " + path);
                cover = tag.getFirstArtwork().getBinaryData();
                publishProgress(cover);
                return null;

            }
            catch(IOException | CannotReadException | ReadOnlyFileException | InvalidAudioFrameException | TagException e){
                e.printStackTrace();
                publishProgress(null);
                System.gc();
                return null;
            }
        }
        @Override
        protected void onPostExecute(Void voids){
            this.mContext.clear();
            this.mHolder = null;
            this.mContext = null;
            asyncLoadCover = null;
            System.gc();
        }

        @Override
        public void onCancelled(Void voids){
            this.mHolder = null;
            if(mContext != null && !((MainActivity) mContext.get()).isDestroyed() ) {
                this.mContext.clear();
                this.mContext = null;
            }
            asyncLoadCover = null;
            System.gc();
        }

        @Override
        protected void onProgressUpdate(byte[]... cover){
            if(mContext != null && !((MainActivity) mContext.get()).isDestroyed() ) {
                GlideApp.with(mContext.get()).
                        load(cover == null ? null : cover[0])
                        .thumbnail(0.1f)
                        .error(R.drawable.ic_album_white_48px)
                        .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
                        .apply(RequestOptions.skipMemoryCacheOf(true))
                        .transition(DrawableTransitionOptions.withCrossFade(100))
                        .fitCenter()
                        .placeholder(R.drawable.ic_album_white_48px)
                        .into(mHolder.get().mImageView);
            }
            System.gc();
        }
    }

    /**
     * The instance of this class handles the sort
     * of the list
     */
    public static class Sorter implements Comparator<AudioItem> {
        //default sort if no provided
        private int mSortType = ASC;
        //default sort field if no provided
        private String mSortByField = TrackContract.TrackData.DATA;

        //don't instantiate objects, we only need one
        private Sorter(){}

        public static Sorter getInstance(){
            if(mSorter == null){
                mSorter = new Sorter();
            }
            return mSorter;
        }

        /**
         * Set sort params: order by field and ascendent 0
         * or descendant 1
         * @param sortByField
         * @param sortType
         */
        public void setSortParams(String sortByField, int sortType){
            this.mSortType = sortType;
            this.mSortByField = sortByField;
        }

        @Override
        public int compare(AudioItem audioItem1, AudioItem audioItem2) {
            String str1 = null;
            String str2 = null;
            String str1ToCompare = null;
            String str2ToCompare = null;

            switch (mSortByField) {
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

            if(mSortType == DESC) {
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
        if(this.mCustomFilter == null){
            this.mCustomFilter = new CustomFilter(this);
        }
        return this.mCustomFilter;
    }

    public void releaseResources(){
            mClickListener = null;
            mContext = null;
            mCurrentList = null;
            mCurrentFilteredList = null;
            mCustomFilter = null;
            mSorter = null;
            if(null != asyncLoadCover){
                asyncLoadCover.cancel(true);
                asyncLoadCover = null;
            }
            System.gc();
    }

    /**
     * class that filters a results of search widget
     */
    public static class CustomFilter extends Filter {
        private WeakReference<TrackAdapter> mWeakReferenceTrackAdapter;
        public CustomFilter(TrackAdapter trackAdapter){
            mWeakReferenceTrackAdapter = new WeakReference<>(trackAdapter);
        }

        @SuppressWarnings("unchecked")
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            String charString = constraint.toString();

            //is empty then is not needed to search
            //so we reference to the original list a return all results
            if (charString.isEmpty()) {
                mWeakReferenceTrackAdapter.get(). mCurrentList = mWeakReferenceTrackAdapter.get().mCurrentFilteredList;
            }
            else {
                //creates a temporal filtered list
                List<AudioItem> filteredList = new ArrayList<>();

                //here we define the parameter in which the results are based
                for (AudioItem audioItem : mWeakReferenceTrackAdapter.get().mCurrentFilteredList) {
                    if (audioItem.getTitle().toLowerCase().contains(charString.toLowerCase())) {
                        //if this item satisfy the criteria of search then add to temporal list
                        filteredList.add(audioItem);
                    }
                }
                //now our current list references to filtered list
                //but we don't lose the orinal reference, because when the search
                //finish we need the original list.
                mWeakReferenceTrackAdapter.get().mCurrentList = filteredList;

            }

            FilterResults  filterResults = new FilterResults();
            filterResults.values = mWeakReferenceTrackAdapter.get().mCurrentList;
            filterResults.count = mWeakReferenceTrackAdapter.get().mCurrentList.size();

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

            mWeakReferenceTrackAdapter.get().mCurrentList = (ArrayList<AudioItem>) results.values;
            //inform to recycler view to update the views.
            mWeakReferenceTrackAdapter.get().notifyDataSetChanged();
        }
    }



}



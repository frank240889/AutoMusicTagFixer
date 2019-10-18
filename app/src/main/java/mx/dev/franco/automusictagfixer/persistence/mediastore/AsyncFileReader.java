package mx.dev.franco.automusictagfixer.persistence.mediastore;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.persistence.room.TrackDAO;
import mx.dev.franco.automusictagfixer.persistence.room.TrackRoomDatabase;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;

//Todo: extract removing tracks functionality from here.
public class AsyncFileReader extends AsyncTask<Void, Void, Void> {
    public static final int INSERT_ALL = 0;
    public static final int UPDATE_LIST = 1;

    @Inject
    TrackRoomDatabase trackRoomDatabase;
    @Inject
    AbstractSharedPreferences sharedPreferences;
    @Inject
    Context context;
    private AsyncOperation<Void, Boolean, Void, Void> mListener;
    private AsyncOperation<Void, List<Track>, Void, Void> mCallback;
    private int mTask;
    private boolean mEmptyList = true;

    public AsyncFileReader(AsyncOperation<Void, List<Track>, Void, Void> listener){
        this();
        mCallback = listener;
    }

    public AsyncFileReader(){}

    public void setListener(AsyncOperation<Void, Boolean, Void, Void> mediaStoreRetrieverListener){
        mListener = mediaStoreRetrieverListener;
    }

    public AsyncFileReader setTask(int task){
        mTask = task;
        return this;
    }

    @Override
    protected void onPreExecute() {
        if(mListener != null)
            mListener.onAsyncOperationStarted(null);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        Cursor cursor = null;
        if(mTask == INSERT_ALL){
            cursor = MediaStoreRetriever.getAllFromDevice(context);
            insertAll(cursor);
            //Save process of reading identificationCompleted
            sharedPreferences.putBoolean(Constants.COMPLETE_READ, true);
        }
        else if(mTask == UPDATE_LIST){
            cursor = MediaStoreRetriever.getAllFromDevice(context);
            addNewTracks(cursor);
            removeInexistentTracks();
        }

        if(cursor != null && !cursor.isClosed())
            cursor.close();

        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        if(mListener != null)
            mListener.onAsyncOperationFinished(mEmptyList);
        mListener = null;
    }

    @Override
    public void onCancelled(){
        if(mListener != null)
            mListener.onAsyncOperationCancelled(null);
        mListener = null;
    }

    /**
     * Inserts a bulk of data from media store to app Database
     * @param cursor The data as a cursor
     */
    private void insertAll(Cursor cursor){
        List<Track> tracks = new ArrayList<>();
        TrackDAO trackDAO = trackRoomDatabase.trackDao();
        while (cursor.moveToNext()){
            Track track = buildTrack(cursor);
            tracks.add(track);
        }

        if(tracks.size() > 0) {
            trackDAO.insert(tracks);
            mEmptyList = false;
        }
        else {
            mEmptyList = true;
        }

        cursor.close();
    }

    private void addNewTracks(Cursor cursor){
        List<Track> tracks = new ArrayList<>();
        TrackDAO trackDAO = trackRoomDatabase.trackDao();
        while (cursor.moveToNext()){
            int mediaStoreId = cursor.getInt(0);//mediastore id
            if(!trackDAO.findTrackById(mediaStoreId)){
                Track track = buildTrack(cursor);
                tracks.add(track);
            }

        }
        if(tracks.size() > 0) {
            trackDAO.insert(tracks);
            mEmptyList = false;
        }
        else {
            mEmptyList = true;
        }
        cursor.close();
    }

    private void removeInexistentTracks(){
        List<Track> tracksToRemove = new ArrayList<>();
        TrackDAO trackDAO = trackRoomDatabase.trackDao();
        List<Track> currentTracks = trackDAO.getTracks();
        if(currentTracks == null || currentTracks.size() == 0)
            return;

        for(Track track:currentTracks){
            File file = new File(track.getPath());
            if(!file.exists()){
                tracksToRemove.add(track);
            }
        }
        if(tracksToRemove.size() > 0)
            trackDAO.deleteBatch(tracksToRemove);
    }

    /**
     * Builds a Track object from cursor input
     * @param cursor The iterable data source
     * @return A Track object
     */
    private Track buildTrack(Cursor cursor){
        //mediastore id
        int mediaStoreId = cursor.getInt(0);
        String title = null;
        title = new String(cursor.getString(1).getBytes(), StandardCharsets.UTF_8);
        String artist = null;
        artist = new String(cursor.getString(2).getBytes(), StandardCharsets.UTF_8);
        String album = null;
        album = new String(cursor.getString(3).getBytes(), StandardCharsets.UTF_8);
        //MediaStore.Audio.Media.DATA column is the path of file
        String fullPath = Uri.parse(cursor.getString(4)).toString();
        Track track = new Track(title,artist,album,fullPath);
        track.setMediaStoreId(mediaStoreId);
        return track;
    }

}
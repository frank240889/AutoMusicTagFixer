package mx.dev.franco.automusictagfixer.persistence.mediastore;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.persistence.room.TrackDAO;
import mx.dev.franco.automusictagfixer.persistence.room.TrackRoomDatabase;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;

public class AsyncFileReader extends AsyncTask<Void, Void, Void> {
    public interface IRetriever {
        void onStart();
        void onFinish(boolean emptyList);
        void onCancel();
    }

    public static final int INSERT_ALL = 0;
    public static final int UPDATE_LIST = 1;

    @Inject
    TrackRoomDatabase trackRoomDatabase;
    @Inject
    AbstractSharedPreferences sharedPreferences;
    @Inject
    Context context;
    private AsyncOperation<Void, Boolean, Void, Void> mListener;
    private int mTask;
    private boolean mEmptyList = true;

    public AsyncFileReader(){
        AutoMusicTagFixer.getContextComponent().inject(this);
    }

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
        if(mTask == INSERT_ALL){
            Cursor cursor = MediaStoreRetriever.getAllFromDevice(context);
            insertAll(cursor);
            //Save process of reading identificationCompleted
            sharedPreferences.putBoolean(Constants.COMPLETE_READ, true);
        }
        else if(mTask == UPDATE_LIST){
            Cursor cursor = MediaStoreRetriever.getAllFromDevice(context);
            addNewTracks(cursor);
            removeInexistentTracks();
        }

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
            trackDAO.insertAll(tracks);
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
            trackDAO.insertAll(tracks);
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
        int mediaStoreId = cursor.getInt(0);//mediastore id
        String title = null;
        try {
            title = new String(cursor.getString(1).getBytes(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            title = cursor.getString(1);
            e.printStackTrace();
        }
        String artist = null;
        try {
            artist = new String(cursor.getString(2).getBytes(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            artist = cursor.getString(2);
        }
        String album = null;
        try {
            album = new String(cursor.getString(3).getBytes(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            album = cursor.getString(3);
        }
        String fullPath = Uri.parse(cursor.getString(4)).toString(); //MediaStore.Audio.Media.DATA column is the path of file
        Track track = new Track(title,artist,album,fullPath);
        track.setMediaStoreId(mediaStoreId);
        return track;
    }

}
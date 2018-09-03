package mx.dev.franco.automusictagfixer.media_store_retriever;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
import mx.dev.franco.automusictagfixer.room.Track;
import mx.dev.franco.automusictagfixer.room.TrackDAO;
import mx.dev.franco.automusictagfixer.room.TrackRoomDatabase;
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
    public static final int UPDATE_SINGLE = 2;
    public static final int GET_TRACK = 3;

    @Inject
    TrackRoomDatabase trackRoomDatabase;
    @Inject
    AbstractSharedPreferences sharedPreferences;
    @Inject
    Context context;
    private IRetriever mListener;
    private int mTask;
    private int mMediaStoreId;
    private boolean mEmptyList = true;

    public AsyncFileReader(){
        AutoMusicTagFixer.getContextComponent().inject(this);
    }

    public void setListener(IRetriever mediaStoreRetrieverListener){
        mListener = mediaStoreRetrieverListener;
    }

    public AsyncFileReader setTask(int task){
        mTask = task;
        return this;
    }

    public void setIdToUpdate(int mediaStoreId){
        mMediaStoreId = mediaStoreId;
    }

    @Override
    protected void onPreExecute() {
        if(mListener != null)
            mListener.onStart();
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
        else {
            Cursor cursor = MediaStoreRetriever.getFromDevice(context, mMediaStoreId);
            updateSingleTrack(cursor);
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Void... items) {
    }

    @Override
    protected void onPostExecute(Void result) {
        if(mListener != null)
            mListener.onFinish(mEmptyList);
        mListener = null;
    }

    @Override
    public void onCancelled(){
        if(mListener != null)
            mListener.onCancel();
        mListener = null;
    }

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

    private void updateSingleTrack(Cursor cursor){
        TrackDAO trackDAO = trackRoomDatabase.trackDao();
        if(cursor.moveToFirst()){
            Track track = buildTrack(cursor);
            trackDAO.update(track);
        }

        cursor.close();

    }


    private Track buildTrack(Cursor cursor){
        int mediaStoreId = cursor.getInt(0);//mediastore id
        String title = cursor.getString(1);
        String artist = cursor.getString(2);
        String album = cursor.getString(3);
        String fullPath = Uri.parse(cursor.getString(4)).toString(); //MediaStore.Audio.Media.DATA column is the path of file
        Track track = new Track(title,artist,album,fullPath);
        track.setMediaStoreId(mediaStoreId);
        return track;
    }

}
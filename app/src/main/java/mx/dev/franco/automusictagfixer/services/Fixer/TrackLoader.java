package mx.dev.franco.automusictagfixer.services.Fixer;

import android.os.AsyncTask;

import java.util.List;

import mx.dev.franco.automusictagfixer.room.Track;
import mx.dev.franco.automusictagfixer.room.TrackRoomDatabase;

public class TrackLoader extends AsyncTask<Integer, Void, List<Track>> {
    private DataTrackLoader<List<Track>> mDataLoader;
    private TrackRoomDatabase mTrackRoomDatabase;

    public TrackLoader(DataTrackLoader<List<Track>> dataLoader, TrackRoomDatabase trackRoomDatabase){
        mDataLoader = dataLoader;
        mTrackRoomDatabase = trackRoomDatabase;
    }

    @Override
    protected List<Track> doInBackground(Integer... integers) {
        return mTrackRoomDatabase.trackDao().getSelectedTrack(integers[0]);
    }

    @Override
    protected void onPostExecute(List<Track> list){
        if(mDataLoader != null)
            mDataLoader.onTrackDataLoaded(list);

        mDataLoader = null;
        mTrackRoomDatabase = null;
    }

    @Override
    public void onCancelled(List<Track> list){
        super.onCancelled();
        mDataLoader = null;
        mTrackRoomDatabase = null;
    }
}
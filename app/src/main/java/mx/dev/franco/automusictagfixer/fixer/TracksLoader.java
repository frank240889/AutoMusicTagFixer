package mx.dev.franco.automusictagfixer.fixer;

import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;

import java.util.List;

import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.persistence.room.TrackRoomDatabase;

public class TracksLoader extends AsyncTask<Integer, Void, List<Track>> {
    public interface DataLoader{
        void onDataLoaded(LiveData<List<Track>> tracks);
    }
    private DataLoader mDataLoader;
    private TrackRoomDatabase mTrackRoomDatabase;

    public TracksLoader(DataLoader dataLoader, TrackRoomDatabase trackRoomDatabase){
        mDataLoader = dataLoader;
        mTrackRoomDatabase = trackRoomDatabase;
    }

    @Override
    protected List<Track> doInBackground(Integer... integers) {
        List<Track> tracks;
        if(integers[0] == -1){
            tracks = mTrackRoomDatabase.trackDao().getSelectedTracks();
        }
        else {
            tracks = mTrackRoomDatabase.trackDao().getSelectedTrack(integers[0]);
        }

        return tracks;
    }

    @Override
    protected void onPostExecute(List<Track> list){
        if(mDataLoader != null)
            //mDataLoader.onDataLoaded(list);

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
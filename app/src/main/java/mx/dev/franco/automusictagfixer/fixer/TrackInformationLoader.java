package mx.dev.franco.automusictagfixer.fixer;

import android.os.AsyncTask;

import java.util.List;

import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.persistence.room.TrackRoomDatabase;

public class TrackInformationLoader extends AsyncTask<Integer, Void, List<Track>> {
    private AsyncOperation<Void, List<Track>, Void, Void> mDataLoader;
    private TrackRoomDatabase mTrackRoomDatabase;

    public TrackInformationLoader(AsyncOperation<Void, List<Track>, Void, Void> dataLoader, TrackRoomDatabase trackRoomDatabase){
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
            mDataLoader.onAsyncOperationFinished(list);

        mDataLoader = null;
        mTrackRoomDatabase = null;
    }

    @Override
    public void onCancelled(List<Track> list){
        if(mDataLoader != null)
            mDataLoader.onAsyncOperationCancelled(null);
        onCancelled();
        mDataLoader = null;
        mTrackRoomDatabase = null;
    }
}
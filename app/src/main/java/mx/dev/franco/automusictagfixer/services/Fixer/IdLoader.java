package mx.dev.franco.automusictagfixer.services.Fixer;

import android.os.AsyncTask;

import java.util.List;

import mx.dev.franco.automusictagfixer.room.TrackRoomDatabase;

public class IdLoader extends AsyncTask<Integer, Void, List<Integer>> {
    private DataLoader<List<Integer>> mDataLoader;
    private TrackRoomDatabase mTrackRoomDatabase;

    public IdLoader(DataLoader<List<Integer>> dataLoader, TrackRoomDatabase trackRoomDatabase){
        mDataLoader = dataLoader;
        mTrackRoomDatabase = trackRoomDatabase;
    }

    @Override
    protected List<Integer> doInBackground(Integer... integers) {
        return mTrackRoomDatabase.trackDao().getCheckedTracks();
    }

    @Override
    protected void onPostExecute(List<Integer> list){
        if(mDataLoader != null)
            mDataLoader.onDataLoaded(list);

        mDataLoader = null;
        mTrackRoomDatabase = null;
    }

    @Override
    public void onCancelled(List<Integer> list){
        super.onCancelled();
        mDataLoader = null;
        mTrackRoomDatabase = null;
    }
}
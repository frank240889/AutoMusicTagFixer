package mx.dev.franco.automusictagfixer.fixer;

import android.arch.persistence.db.SimpleSQLiteQuery;
import android.arch.persistence.db.SupportSQLiteQuery;
import android.os.AsyncTask;

import java.util.List;

import mx.dev.franco.automusictagfixer.interfaces.TrackListLoader;
import mx.dev.franco.automusictagfixer.persistence.room.TrackRoomDatabase;

public class IdLoader extends AsyncTask<String, Void, List<Integer>> {
    private TrackListLoader<List<Integer>> mTrackListLoader;
    private TrackRoomDatabase mTrackRoomDatabase;

    public IdLoader(TrackListLoader<List<Integer>> trackListLoader, TrackRoomDatabase trackRoomDatabase){
        mTrackListLoader = trackListLoader;
        mTrackRoomDatabase = trackRoomDatabase;
    }

    @Override
    protected List<Integer> doInBackground(String... sort) {
        String order = sort[0];
        if(order == null)
            order = " title COLLATE NOCASE ASC ";
        String query = "SELECT mediastore_id FROM track_table WHERE selected = 1 ORDER BY" + order;
        SupportSQLiteQuery sqLiteQuery = new SimpleSQLiteQuery(query);
        return mTrackRoomDatabase.trackDao().getCheckedTracks(sqLiteQuery);
    }

    @Override
    protected void onPostExecute(List<Integer> list){
        if(mTrackListLoader != null)
            mTrackListLoader.onDataLoaded(list);

        mTrackListLoader = null;
        mTrackRoomDatabase = null;
    }

    @Override
    public void onCancelled(List<Integer> list){
        super.onCancelled();
        mTrackListLoader = null;
        mTrackRoomDatabase = null;
    }
}
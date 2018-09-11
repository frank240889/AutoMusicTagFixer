package mx.dev.franco.automusictagfixer.fixer;

import android.arch.persistence.db.SimpleSQLiteQuery;
import android.arch.persistence.db.SupportSQLiteQuery;
import android.os.AsyncTask;

import java.util.List;

import mx.dev.franco.automusictagfixer.interfaces.DataLoader;
import mx.dev.franco.automusictagfixer.persistence.room.TrackRoomDatabase;

public class IdLoader extends AsyncTask<String, Void, List<Integer>> {
    private DataLoader<List<Integer>> mDataLoader;
    private TrackRoomDatabase mTrackRoomDatabase;

    public IdLoader(DataLoader<List<Integer>> dataLoader, TrackRoomDatabase trackRoomDatabase){
        mDataLoader = dataLoader;
        mTrackRoomDatabase = trackRoomDatabase;
    }

    @Override
    protected List<Integer> doInBackground(String... sort) {
        String order = sort[0];
        if(order == null)
            order = " title ASC ";
        String query = "SELECT mediastore_id FROM track_table WHERE selected = 1 ORDER BY ?";
        SupportSQLiteQuery sqLiteQuery = new SimpleSQLiteQuery(query, new Object[]{order});
        return mTrackRoomDatabase.trackDao().getCheckedTracks(sqLiteQuery);
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
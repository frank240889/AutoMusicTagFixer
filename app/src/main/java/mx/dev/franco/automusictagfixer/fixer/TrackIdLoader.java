package mx.dev.franco.automusictagfixer.fixer;

import android.os.AsyncTask;

import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import java.util.List;

import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.persistence.room.TrackRoomDatabase;

public class TrackIdLoader extends AsyncTask<String, Void, List<Integer>> {
    private AsyncOperation<Void, List<Integer>, Void, Void> mTrackListLoader;
    private TrackRoomDatabase mTrackRoomDatabase;

    public TrackIdLoader(AsyncOperation<Void, List<Integer>, Void, Void> trackListLoader, TrackRoomDatabase trackRoomDatabase){
        mTrackListLoader = trackListLoader;
        mTrackRoomDatabase = trackRoomDatabase;
    }

    @Override
    protected void onPreExecute() {
        if(mTrackListLoader != null)
            mTrackListLoader.onAsyncOperationStarted(null);
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
            mTrackListLoader.onAsyncOperationFinished(list);

        mTrackListLoader = null;
        mTrackRoomDatabase = null;
    }

    @Override
    public void onCancelled(List<Integer> list){
        mTrackListLoader.onAsyncOperationCancelled(null);
        onCancelled();
        mTrackListLoader = null;
        mTrackRoomDatabase = null;
    }
}
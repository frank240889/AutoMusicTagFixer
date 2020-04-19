package mx.dev.franco.automusictagfixer.fixer;

import android.os.AsyncTask;

import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.persistence.room.TrackDAO;
import mx.dev.franco.automusictagfixer.utilities.Constants;
import mx.dev.franco.automusictagfixer.utilities.shared_preferences.AbstractSharedPreferences;

public class TrackLoader extends AsyncTask<AbstractSharedPreferences, Void, Track> {
    private AsyncOperation<Void, Track, Void, Void> mTrackListLoader;
    private TrackDAO mTrackDao;

    public TrackLoader(AsyncOperation<Void, Track, Void, Void> trackListLoader, TrackDAO trackDAO){
        mTrackListLoader = trackListLoader;
        mTrackDao= trackDAO;
    }

    @Override
    protected void onPreExecute() {
        if(mTrackListLoader != null)
            mTrackListLoader.onAsyncOperationStarted(null);
    }

    @Override
    protected Track doInBackground(AbstractSharedPreferences... sharedPreferences) {
        String currentOrder = sharedPreferences[0].getString(Constants.SORT_KEY);

        // Check the last sort order saved.
        if(currentOrder == null || "".equals(currentOrder))
            currentOrder = TrackDAO.DEFAULT_ORDER;

        String query = "SELECT * FROM track_table WHERE selected = 1 ORDER BY" +currentOrder + " LIMIT 1";

        SupportSQLiteQuery sqLiteQuery = new SimpleSQLiteQuery(query);
        return mTrackDao.findNextSelected(sqLiteQuery);
    }

    @Override
    protected void onPostExecute(Track track){
        if(mTrackListLoader != null)
            mTrackListLoader.onAsyncOperationFinished(track);

        mTrackListLoader = null;
        mTrackDao = null;
    }
}
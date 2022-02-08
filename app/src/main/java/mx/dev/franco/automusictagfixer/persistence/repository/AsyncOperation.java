package mx.dev.franco.automusictagfixer.persistence.repository;

import android.os.AsyncTask;

import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.persistence.room.TrackDAO;

public class AsyncOperation {

    public static class TrackUpdater extends AsyncTask<Track, Void, Void> {

        private TrackDAO mAsyncTaskDao;

        public TrackUpdater(TrackDAO dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Track... track) {
            mAsyncTaskDao.update(track[0]);
            mAsyncTaskDao = null;
            return null;
        }
    }
}

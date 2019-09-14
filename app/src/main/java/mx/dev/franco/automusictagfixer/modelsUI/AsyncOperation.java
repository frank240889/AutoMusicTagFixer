package mx.dev.franco.automusictagfixer.modelsUI;

import android.os.AsyncTask;

import java.util.List;

import mx.dev.franco.automusictagfixer.persistence.room.Track;
import mx.dev.franco.automusictagfixer.persistence.room.TrackDAO;

public class AsyncOperation {

    public static class TrackChecker extends AsyncTask<Void, Void, Void> {

        private TrackDAO mAsyncTaskDao;

        public TrackChecker(TrackDAO dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Void... params) {
            mAsyncTaskDao.checkAll();
            mAsyncTaskDao = null;
            return null;
        }
    }

    public static class TrackUnchecker extends AsyncTask<Void, Void, Void> {

        private TrackDAO mAsyncTaskDao;

        public TrackUnchecker(TrackDAO dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Void... params) {
            mAsyncTaskDao.uncheckAll();
            mAsyncTaskDao = null;
            return null;
        }
    }

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


    public static class TrackRemover extends AsyncTask<Track, Void, Void> {

        private TrackDAO mAsyncTaskDao;

        public TrackRemover(TrackDAO dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Track... track) {
            mAsyncTaskDao.delete(track[0]);
            mAsyncTaskDao = null;
            return null;
        }
    }

    public static class TrackInserter extends AsyncTask<List<Track>, Void, Void> {
        private TrackDAO mAsyncTaskDao;

        public TrackInserter(TrackDAO trackDAO) {
            mAsyncTaskDao = trackDAO;
        }

        @SafeVarargs
        @Override
        protected final Void doInBackground(List<Track>... lists) {
            mAsyncTaskDao.insert(lists[0]);
            mAsyncTaskDao = null;
            return null;
        }
    }
}

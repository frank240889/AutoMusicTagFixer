package mx.dev.franco.automusictagfixer.persistence.repository;

import android.os.AsyncTask;

import java.io.File;
import java.util.ArrayList;
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

    public static class TrackUpdaterSync extends AsyncTask<Track, Void, Integer> {
        private mx.dev.franco.automusictagfixer.interfaces.AsyncOperation<Void, Integer, Void, Void> mCallback;
        private TrackDAO mAsyncTaskDao;

        public TrackUpdaterSync(mx.dev.franco.automusictagfixer.interfaces.AsyncOperation<Void, Integer, Void, Void> callback, TrackDAO dao) {
            mAsyncTaskDao = dao;
            mCallback = callback;
        }

        @Override
        protected Integer doInBackground(final Track... track) {
            try {
                Thread.sleep(3000);
                mAsyncTaskDao.update(track[0]);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mAsyncTaskDao = null;
            return null;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            if (mCallback != null)
                mCallback.onAsyncOperationFinished(integer);
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
            removeInexistentTracks();
            mAsyncTaskDao.insert(lists[0]);
            mAsyncTaskDao = null;
            return null;
        }

        private void removeInexistentTracks(){
            List<Track> tracksToRemove = new ArrayList<>();
            List<Track> currentTracks = mAsyncTaskDao.getTracks();
            if(currentTracks == null || currentTracks.size() == 0)
                return;

            for(Track track:currentTracks){
                File file = new File(track.getPath());
                if(!file.exists()){
                    tracksToRemove.add(track);
                }
            }

            if(tracksToRemove.size() > 0)
                mAsyncTaskDao.deleteBatch(tracksToRemove);
        }
    }
}

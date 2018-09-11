package mx.dev.franco.automusictagfixer.modelsUI.search;

import android.os.AsyncTask;

import java.util.List;

import mx.dev.franco.automusictagfixer.persistence.repository.TrackRepository;
import mx.dev.franco.automusictagfixer.persistence.room.Track;

public class AsyncSearch extends AsyncTask<String, Void, List<Track>> {
    private static final String TAG = AsyncSearch.class.getName();
    public interface ResultsSearchListener{
        void onStartSearch();
        void onFinished(List<Track> results);
        void onCancelled();
    }

    private TrackRepository mTrackRepository;
    private ResultsSearchListener mListener;

    public AsyncSearch(ResultsSearchListener listener, TrackRepository trackRepository){
        mListener = listener;
        mTrackRepository = trackRepository;
    }

    @Override
    protected void onPreExecute(){
        if(mListener != null)
            mListener.onStartSearch();
    }
    @Override
    protected List<Track> doInBackground(String... params) {
        return mTrackRepository.search(params[0]);
    }

    @Override
    protected void onPostExecute(List<Track> results){
        if(mListener != null)
            mListener.onFinished(results);
        clear();
    }

    @Override
    public void onCancelled(List<Track> results){
        super.onCancelled(results);
        if(mListener != null)
            mListener.onCancelled();

        clear();
    }

    private void clear(){
        mListener = null;
        mTrackRepository = null;
    }
}

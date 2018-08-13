package mx.dev.franco.automusictagfixer.UI.search;

import android.os.AsyncTask;
import android.util.Log;

import java.util.List;

import mx.dev.franco.automusictagfixer.repository.TrackRepository;
import mx.dev.franco.automusictagfixer.room.Track;

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
        Log.d(TAG, "Searching..." + params[0]);
        return mTrackRepository.search(params[0]);
    }

    @Override
    protected void onPostExecute(List<Track> results){
        Log.d(TAG, "Results size:" + results.size());
        if(mListener != null)
            mListener.onFinished(results);
        clear();
    }

    @Override
    public void onCancelled(List<Track> results){
        if(mListener != null)
            mListener.onCancelled();

        clear();
        super.onCancelled(results);
    }

    private void clear(){
        mListener = null;
        mTrackRepository = null;
    }
}

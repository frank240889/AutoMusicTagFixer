package mx.dev.franco.automusictagfixer.datasource;

import android.os.AsyncTask;
import android.support.v7.util.DiffUtil;

import java.util.List;

import mx.dev.franco.automusictagfixer.room.Track;

public class DiffExecutor extends AsyncTask<List<Track>, Void, DiffExecutor.DiffResults>{
    public interface DiffCallbackListener {
        void onStartDiff();
        void onCancelledDiff();
        void onFinishedDiff(DiffResults diffResults);
    }
    private DiffCallbackListener mListener;

    public DiffExecutor(DiffCallbackListener diffExecutor){
        mListener = diffExecutor;
    }
    @Override
    protected void onPreExecute(){
        if(mListener != null)
            mListener.onStartDiff();
    }

    @Override
    protected DiffResults doInBackground(List<Track>... lists) {
        DiffUtil.DiffResult diffResult = DiffUtil.
                calculateDiff(new DiffCallback(lists[0], lists[1]));
        DiffResults diffResults = new DiffResults();
        diffResults.diffResult = diffResult;
        diffResults.list = lists[1];
        return diffResults;
    }

    @Override
    protected void onPostExecute(DiffResults diffResults){
        if(mListener != null)
            mListener.onFinishedDiff(diffResults);

        mListener = null;
    }

    public static class DiffResults{
        List<Track> list;
        DiffUtil.DiffResult diffResult;
    }
}

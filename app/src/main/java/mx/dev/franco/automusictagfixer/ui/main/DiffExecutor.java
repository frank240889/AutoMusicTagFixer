package mx.dev.franco.automusictagfixer.ui.main;

import android.os.AsyncTask;
import android.support.v7.util.DiffUtil;
import java.util.List;
import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.persistence.room.Track;

public class DiffExecutor extends AsyncTask<List<Track>, Void, DiffResults<Track>>{
    private AsyncOperation<Void, DiffResults<Track>, Void, Void> mListener;
    public DiffExecutor(AsyncOperation<Void, DiffResults<Track>, Void, Void> diffExecutor){
        mListener = diffExecutor;
    }
    @Override
    protected void onPreExecute(){
        if(mListener != null)
            mListener.onAsyncOperationStarted(null);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected DiffResults doInBackground(List<Track>... lists) {
        DiffUtil.DiffResult diffResult = DiffUtil.
                calculateDiff(new DiffCallback(lists[0], lists[1]),false);
        DiffResults diffResults = new DiffResults();
        diffResults.diffResult = diffResult;
        diffResults.list = lists[1];
        return diffResults;
    }

    @Override
    protected void onPostExecute(DiffResults diffResults){
        if(mListener != null)
            mListener.onAsyncOperationFinished(diffResults);

        mListener = null;
    }
}

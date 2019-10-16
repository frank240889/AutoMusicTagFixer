package mx.dev.franco.automusictagfixer.persistence.mediastore;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.persistence.room.Track;

public class MediaStoreReader extends AsyncTask<Context, Void, List<Track>> {
    private AsyncOperation<Void, List<Track>, Void, Void> mCallback;

    public MediaStoreReader(AsyncOperation<Void, List<Track>, Void, Void> listener){
        this();
        mCallback = listener;
    }

    public MediaStoreReader(){}

    @Override
    protected void onPreExecute() {
        if(mCallback != null)
            mCallback.onAsyncOperationStarted(null);
    }

    @Override
    protected List<Track> doInBackground(Context... context) {
        Cursor cursor = MediaStoreRetriever.getAllFromDevice(context[0]);
        List<Track> tracks = new ArrayList<>();
        if(cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                Track track = buildTrack(cursor);
                tracks.add(track);
            }
        }


        return tracks;
    }

    @Override
    protected void onPostExecute(List<Track> result) {
        if(mCallback != null)
            mCallback.onAsyncOperationFinished(result);
    }

    @Override
    public void onCancelled(){
        if(mCallback != null)
            mCallback.onAsyncOperationFinished(new ArrayList<>());
    }

    /**
     * Builds a Track object from cursor input
     * @param cursor The iterable data source
     * @return A Track object
     */
    private Track buildTrack(Cursor cursor){
        //mediastore id
        int mediaStoreId = cursor.getInt(0);
        String title = null;
        title = new String(cursor.getString(1).getBytes(), StandardCharsets.UTF_8);
        String artist = null;
        artist = new String(cursor.getString(2).getBytes(), StandardCharsets.UTF_8);
        String album = null;
        album = new String(cursor.getString(3).getBytes(), StandardCharsets.UTF_8);
        //MediaStore.Audio.Media.DATA column is the path of file
        String fullPath = Uri.parse(cursor.getString(4)).toString();
        Track track = new Track(title,artist,album,fullPath);
        track.setMediaStoreId(mediaStoreId);
        return track;
    }

}
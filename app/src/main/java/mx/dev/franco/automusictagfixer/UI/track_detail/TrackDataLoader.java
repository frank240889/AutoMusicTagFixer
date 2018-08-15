package mx.dev.franco.automusictagfixer.UI.track_detail;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import com.crashlytics.android.Crashlytics;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;

import java.io.IOException;

import javax.inject.Inject;

import mx.dev.franco.automusictagfixer.AutoMusicTagFixer;
import mx.dev.franco.automusictagfixer.room.TrackRoomDatabase;
import mx.dev.franco.automusictagfixer.utilities.Tagger;

public class TrackDataLoader extends AsyncTask<Integer, Void, TrackDataLoader.TrackDataItem> {
    public interface TrackLoader{
        void onStartedLoad();
        void onFinishedLoad(TrackDataItem trackDataItem);
        void onCancelledLoad();
        void onLoadError(String error);
    }

    private TrackLoader mListener;
    @Inject
    Tagger mTagger;
    @Inject
    TrackRoomDatabase trackRoomDatabase;

    public TrackDataLoader(){
        AutoMusicTagFixer.getContextComponent().inject(this);
    }

    public void setListener(TrackLoader listener){
        mListener = listener;
    }

    protected void onPreExecute(){
        if(mListener != null)
            mListener.onStartedLoad();
    }

    @Override
    protected TrackDataItem doInBackground(Integer... integers) {
        String path = trackRoomDatabase.trackDao().getPath(integers[0]);
        TrackDataItem trackDataItem = null;
        try {
            trackDataItem = mTagger.readFile(path);
        }
        catch (ReadOnlyFileException | IOException | InvalidAudioFrameException | CannotReadException | TagException e) {
            e.printStackTrace();
            Crashlytics.logException(e);
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {
                if(mListener != null)
                    mListener.onLoadError(e.getMessage());
                mListener = null;
            });
        }
        return trackDataItem;
    }

    @Override
    protected void onPostExecute(TrackDataItem trackDataItem) {
        if(mListener != null) {
            mListener.onFinishedLoad(trackDataItem);
        }
        mListener = null;
    }

    @Override
    protected void onCancelled(TrackDataItem trackDataItem) {
        super.onCancelled(trackDataItem);
        if(mListener != null)
            mListener.onCancelledLoad();
        mListener = null;
    }


    public static class TrackDataItem{
        public String title = "";
        public String artist = "";
        public String album = "";
        public String trackNumber = "";
        public String trackYear = "";
        public String genre = "";
        public byte[] cover = null;

        public String fileName = "";
        public String path = "";

        public String duration = "";
        public String bitrate = "";
        public String frequency = "";
        public String resolution = "";
        public String channels = "";
        public String fileType = "";
        public String extension = "";
        public String mimeType = "";
        public String imageSize = "Sin carátula.";
        public String fileSize = "";

    }
}
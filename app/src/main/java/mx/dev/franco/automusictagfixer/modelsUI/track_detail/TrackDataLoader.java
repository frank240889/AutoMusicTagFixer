package mx.dev.franco.automusictagfixer.modelsUI.track_detail;

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
import mx.dev.franco.automusictagfixer.interfaces.AsyncOperation;
import mx.dev.franco.automusictagfixer.persistence.room.TrackRoomDatabase;
import mx.dev.franco.automusictagfixer.utilities.Tagger;

/**
 * This class  extracts asynchronously the data from track making use
 * of {@link Tagger} class.
 */
public class TrackDataLoader extends AsyncTask<Integer, Void, TrackDataLoader.TrackDataItem> {

    private AsyncOperation<Void, TrackDataItem, Void, String> mListener;
    @Inject
    public Tagger mTagger;
    @Inject
    public TrackRoomDatabase trackRoomDatabase;

    public TrackDataLoader(){
        AutoMusicTagFixer.getContextComponent().inject(this);
    }

    public void setListener(AsyncOperation<Void, TrackDataItem ,Void, String> listener){
        mListener = listener;
    }

    protected void onPreExecute(){
        if(mListener != null)
            mListener.onAsyncOperationStarted(null);
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
                    mListener.onAsyncOperationError(e.getMessage());
                mListener = null;
            });
        }
        return trackDataItem;
    }

    @Override
    protected void onPostExecute(TrackDataItem trackDataItem) {
        if(mListener != null) {
            mListener.onAsyncOperationFinished(trackDataItem);
        }
        mListener = null;
    }

    @Override
    protected void onCancelled(TrackDataItem trackDataItem) {
        super.onCancelled(trackDataItem);
        if(mListener != null)
            mListener.onAsyncOperationCancelled(null);
        mListener = null;
    }


    public static class TrackDataItem implements Cloneable{
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

        @Override
        protected Object clone() throws CloneNotSupportedException {
            Object obj = null;
            try {
                obj = super.clone();
            }
            catch (CloneNotSupportedException ignored) {}
            return obj;
        }
    }
}

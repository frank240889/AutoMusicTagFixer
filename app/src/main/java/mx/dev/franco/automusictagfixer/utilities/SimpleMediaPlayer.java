package mx.dev.franco.automusictagfixer.utilities;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.PowerManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by franco on 29/03/17.
 */

public final class SimpleMediaPlayer extends MediaPlayer implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
    public interface OnMediaPlayerEventListener {
        void onStartPlaying();
        void onStopPlaying();
        void onCompletedPlaying();
        void onErrorPlaying(int what, int extra);
    }

    private List<OnMediaPlayerEventListener> mListeners = new ArrayList<>();
    private static SimpleMediaPlayer sMediaPlayer;
    private OnMediaPlayerEventListener mListener;
    private String mCurrentPath;

    /**
     * Don't let instantiate this class, we need only one instance,
     * so we use a singleton pattern in order to make this.
     */
    private SimpleMediaPlayer(Context context){
        super();
        setVolume(1f,1f);
        setWakeMode(context.getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        setOnCompletionListener(this);
        setOnErrorListener(this);
    }

    public void addListener(OnMediaPlayerEventListener listener){
        mListeners.add(listener);
    }

    private void removeListener(OnMediaPlayerEventListener listener) {
        mListeners.remove(listener);
    }

    public void removeListeners(){
        mListeners.clear();
        mCurrentPath = null;
    }

    /**
     *
     * @param context The context, needed for access Android resources
     * @return An unique instance of SimpleMediaPlayer.
     */
    public static SimpleMediaPlayer getInstance(Context context){
        if(sMediaPlayer == null){
            sMediaPlayer = new SimpleMediaPlayer(context);
        }
        return sMediaPlayer;
    }

    /**
     * Play a preview of audiofile.
     * @param path The path of the file to play
     * @throws IOException
     */

    public void playPreview(String path) throws IOException {
        if(path == null || path.equals(""))
            return;

        File file = new File(path);
        if(!file.exists() || !file.canRead() || file.length() == 0)
            return;

        //Stops current audio
        setDataSource(path);
        prepare();
        start();
        if(mListener != null)
            mListener.onStartPlaying();
    }

    public void playPreview() throws IOException {

        if(mCurrentPath == null || mCurrentPath.equals(""))
            return;

        File file = new File(mCurrentPath);
        if(!file.exists() || !file.canRead() || file.length() == 0)
            return;

        //Stops current audio
        setDataSource(mCurrentPath);
        prepare();
        start();
        if(mListener != null)
            mListener.onStartPlaying();
    }

    public void setPath(String path) {
        mCurrentPath = path;
    }


    public void stopPreview(){
        if(isPlaying()) {
            stop();
            reset();
            if(mListener != null)
                mListener.onStopPlaying();
        }
    }

    /**
     * Implementation of completion interface for
     * handling correctly the ends of song if is playing.
     * @param mp
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        stop();
        reset();
        if(mListener != null)
            mListener.onCompletedPlaying();
    }


    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if(mListener != null)
            mListener.onErrorPlaying(what,extra);
        return false;
    }
}

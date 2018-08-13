package mx.dev.franco.automusictagfixer.utilities;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * Created by franco on 29/03/17.
 */

public final class SimpleMediaPlayer extends MediaPlayer implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
    public interface OnEventDispatchedListener{
        void onStartPlaying();
        void onStopPlaying();
        void onCompletionPlaying();
        void onErrorPlaying(int what, int extra);
    }


    private static SimpleMediaPlayer sMediaPlayer;
    private OnEventDispatchedListener mListener;

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

    public void addListener(OnEventDispatchedListener listener){
        mListener = listener;
    }

    public void removeListener(){
        mListener = null;
    }

    /**
     *
     * @param context
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
     * @param path
     * @throws IOException
     * @throws InterruptedException
     */

    public void playPreview(String path) throws IOException {
        File file = new File(path);
        if(path == null || path.equals("") || !file.exists() || !file.canRead() || file.length() == 0)
            return;

        //Stops current audio
        setDataSource(path);
        prepare();
        start();
        if(mListener != null)
            mListener.onStartPlaying();
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
        Log.d("OnCompletion","OnCompletion");
        stop();
        reset();
        if(mListener != null)
            mListener.onCompletionPlaying();
    }


    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if(mListener != null)
            mListener.onErrorPlaying(what,extra);
        return false;
    }
}

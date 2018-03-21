package mx.dev.franco.automusictagfixer.utilities;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import java.io.IOException;

import mx.dev.franco.automusictagfixer.list.AudioItem;
import mx.dev.franco.automusictagfixer.list.TrackAdapter;

/**
 * Created by franco on 29/03/17.
 */

public final class SimpleMediaPlayer extends MediaPlayer implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    private static SimpleMediaPlayer sMediaPlayer;
    private AudioItem mCurrentAudioItem;
    private long mCurrentPosition = -1;
    private TrackAdapter mAdapter;

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

    public void setAdapter(TrackAdapter mAdapter){
        this.mAdapter = mAdapter;
    }

    public RecyclerView.Adapter<TrackAdapter.AudioItemHolder> getAdapter(){
        return this.mAdapter;
    }

    /**
     * Play a preview of audiofile.
     * @param position
     * @throws IOException
     * @throws InterruptedException
     */

    public void playPreview(int position) throws IOException, InterruptedException {
        //Stops current audio
        if(mCurrentPosition == position && sMediaPlayer.isPlaying()) {
            sMediaPlayer.stop();
            sMediaPlayer.reset();
            return;
        }

        //If user plays different audio file
        //search in adapter, if not, take the current mCurrentAudioItem
        if(mCurrentPosition != position){
            mCurrentAudioItem = mAdapter.getAudioItemByPosition(position);
            mCurrentPosition = position;
        }
        sMediaPlayer.setDataSource(mCurrentAudioItem.getAbsolutePath());
        sMediaPlayer.prepare();

        sMediaPlayer.start();
    }


    /**
     * Get the position from current item_list playing.
     * @return
     */
    public long getCurrentPosition2() {
        return mCurrentPosition;
    }
    /**
     * Implementation of completion interface for
     * handling correctly the ends of song if is playing.
     * @param mp
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d("OnCompletion","OnCompletion");
        onCompletePlayback();
    }

    /**
     * If song reaches its end, then we stop and reset player
     * for having it ready for next playback, and doesn't throw
     * any error.
     */

    public void onCompletePlayback(){
        mAdapter.notifyItemChanged((int)getCurrentPosition2());
        sMediaPlayer.stop();
        sMediaPlayer.reset();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        try{
            onCompletePlayback();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }
}

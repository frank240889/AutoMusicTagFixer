package mx.dev.franco.automusictagfixer.covermanager;

import java.lang.ref.WeakReference;

import mx.dev.franco.automusictagfixer.ui.AudioHolder;

public class CoverTask implements ICoverRunnable {
    private WeakReference<AudioHolder> mAudioItemHolderWeakReference;
    Thread mThreadThis;
    private Runnable mExtractionRunnable;
    private String mPath;
    private static CoverManager sCoverManager;
    private byte[] mCover;

    CoverTask(){
        mExtractionRunnable = new CoverExtractionRunnable(this);
    }

    void startFetching(
            CoverManager coverManager, AudioHolder audioItemHolder, String path) {

        mAudioItemHolderWeakReference = new WeakReference<>(audioItemHolder);
        mPath = path;
        sCoverManager = coverManager;
    }

    @Override
    public String getPath() {
        return mPath;
    }

    byte[] getCover() {
        return mCover;
    }

    @Override
    public void handleExtractionState(int state) {
        int outState;

        // Converts the decode state to the overall state.
        switch(state) {
            case CoverExtractionRunnable.EXTRACTION_STATE_COMPLETED:
                outState = CoverManager.EXTRACTION_FINISHED;
                break;
            case CoverExtractionRunnable.EXTRACTION_STATE_FAILED:
                outState = CoverManager.EXTRACTION_ERROR;
                break;
            default:
                outState = CoverManager.EXTRACTION_STARTED;
                break;
        }

        // Passes the state to the ThreadPool object.
        handleState(outState);
    }

    @Override
    public void setCover(byte[] cover) {
        mCover = cover;
    }

    // Returns the ImageView that's being constructed.
    public AudioHolder getAudioHolder() {
        if ( null != mAudioItemHolderWeakReference) {
            return mAudioItemHolderWeakReference.get();
        }
        return null;
    }

    Runnable getExtractionRunnable() {
        return mExtractionRunnable;
    }

    void handleState(int state) {
        sCoverManager.handleState(this, state);
    }

    void recycle() {

        // Deletes the weak reference to the imageView
        if ( null != mAudioItemHolderWeakReference ) {
            mAudioItemHolderWeakReference.clear();
            mAudioItemHolderWeakReference = null;
        }

        mPath = null;
    }
}

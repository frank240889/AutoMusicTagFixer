package mx.dev.franco.automusictagfixer.covermanager;

import mx.dev.franco.automusictagfixer.utilities.Tagger;

public class CoverExtractionRunnable implements Runnable {
    // Sets the log tag
    private static final String TAG = CoverExtractionRunnable.class.getName();

    // Constants for indicating the state of the decode
    static final int EXTRACTION_STATE_FAILED = -1;
    static final int EXTRACTION_STATE_STARTED = 0;
    static final int EXTRACTION_STATE_COMPLETED = 1;

    final ICoverRunnable mCoverRunnable;

    CoverExtractionRunnable(ICoverRunnable iCoverRunnable) {
        mCoverRunnable = iCoverRunnable;
    }

    @Override
    public void run() {
        byte[] cover = Tagger.getCover(mCoverRunnable.getPath());
        mCoverRunnable.setCover(cover);
        if(cover == null) {
            mCoverRunnable.handleExtractionState(EXTRACTION_STATE_FAILED);
        }
        else {
            mCoverRunnable.handleExtractionState(EXTRACTION_STATE_COMPLETED);
        }
        Thread.interrupted();
    }
}

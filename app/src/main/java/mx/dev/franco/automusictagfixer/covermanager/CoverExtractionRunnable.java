package mx.dev.franco.automusictagfixer.covermanager;

import mx.dev.franco.automusictagfixer.fixer.AudioTagger;
import mx.dev.franco.automusictagfixer.utilities.AndroidUtils;

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
        byte[] cover = AudioTagger.getCover(mCoverRunnable.getPath());
        if(cover != null) {
            byte[] downscaledCover = AndroidUtils.decodeSampledBitmapFromResource(cover, 128, 128);
            mCoverRunnable.setCover(downscaledCover);
            mCoverRunnable.handleExtractionState(EXTRACTION_STATE_COMPLETED);
        }
        else {
            mCoverRunnable.handleExtractionState(EXTRACTION_STATE_FAILED);
        }

        Thread.interrupted();
    }
}

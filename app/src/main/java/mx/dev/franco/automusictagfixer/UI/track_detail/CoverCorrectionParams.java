package mx.dev.franco.automusictagfixer.UI.track_detail;

public class CoverCorrectionParams extends SemiAutoCorrectionParams {
    public static final int SAVE_AS_IMAGE_FILE = 0;
    public static final int SAVE_AS_COVER = 1;
    private int saveAs;

    public CoverCorrectionParams() {}

    public CoverCorrectionParams(int saveAs) {
        this();
        this.saveAs = saveAs;
    }

    public int getSaveAs() {
        return saveAs;
    }

    public void setSaveAs(int saveAs) {
        this.saveAs = saveAs;
    }
}

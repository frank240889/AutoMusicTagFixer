package mx.dev.franco.automusictagfixer.ui.trackdetail;

public class CoverCorrectionParams extends SemiAutoCorrectionParams {
    public static final int SAVE_AS_IMAGE_FILE = 0;
    public static final int SAVE_AS_COVER = 1;
    private int saveAs;
    private String gnImageSize;

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

    public String getGnImageSize() {
        return gnImageSize;
    }

    public void setGnImageSize(String gnImageSize) {
        this.gnImageSize = gnImageSize;
    }
}

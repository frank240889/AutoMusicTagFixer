package mx.dev.franco.automusictagfixer.UI.track_detail;

public class SemiAutoCorrectionParams extends ManualCorrectionParams {
    private String position;

    public SemiAutoCorrectionParams() {
        super();
    }

    public SemiAutoCorrectionParams(String fileName) {
        this();
        setNewName(fileName);
    }

    public SemiAutoCorrectionParams(String fileName, String position) {
        this();
        setNewName(fileName);
        this.position = position;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }
}

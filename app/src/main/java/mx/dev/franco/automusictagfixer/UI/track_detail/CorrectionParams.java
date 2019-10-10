package mx.dev.franco.automusictagfixer.UI.track_detail;

public class CorrectionParams extends UIInputParams {
    private String fileName;
    private String id;

    public CorrectionParams() {
        super();
    }

    public CorrectionParams(String fileName) {
        this();
        this.fileName = fileName;
    }

    public CorrectionParams(String fileName, String id) {
        this();
        this.fileName = fileName;
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}

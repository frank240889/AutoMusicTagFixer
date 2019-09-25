package mx.dev.franco.automusictagfixer.UI.track_detail;

import mx.dev.franco.automusictagfixer.fixer.AudioMetadataTagger;

public class CorrectionParams extends AudioMetadataTagger.InputParams {
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

package mx.dev.franco.automusictagfixer.ui.trackdetail;

import org.jaudiotagger.tag.FieldKey;

import java.util.Map;

import mx.dev.franco.automusictagfixer.fixer.AudioMetadataTagger;

public class InputCorrectionParams extends AudioMetadataTagger.InputParams {
    private int correctionMode;
    private boolean renameFile;

    public InputCorrectionParams(){}

    public InputCorrectionParams(int correctionMode) {
        this();
        setCorrectionMode(correctionMode);
    }

    public InputCorrectionParams(Map<FieldKey, Object> tags) {
        super(tags);
    }

    public int getCorrectionMode() {
        return correctionMode;
    }

    public void setCorrectionMode(int correctionMode) {
        this.correctionMode = correctionMode;
    }

    public boolean renameFile() {
        return renameFile;
    }

    public void setRenameFile(boolean renameFile) {
        this.renameFile = renameFile;
    }
}

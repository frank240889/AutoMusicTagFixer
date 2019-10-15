package mx.dev.franco.automusictagfixer.UI.track_detail;

import mx.dev.franco.automusictagfixer.fixer.AudioMetadataTagger;

public class ManualCorrectionParams extends AudioMetadataTagger.InputParams {
  private boolean renameFile;
  private int correctionMode;

  public ManualCorrectionParams(){}

  public boolean renameFile() {
    return renameFile;
  }

  public void setRenameFile(boolean renameFile) {
    this.renameFile = renameFile;
  }

  public int getCorrectionMode() {
    return correctionMode;
  }

  public void setCorrectionMode(int correctionMode) {
    this.correctionMode = correctionMode;
  }

}

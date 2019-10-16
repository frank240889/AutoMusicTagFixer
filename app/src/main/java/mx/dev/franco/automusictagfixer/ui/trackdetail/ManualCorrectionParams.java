package mx.dev.franco.automusictagfixer.ui.trackdetail;

public class ManualCorrectionParams extends InputCorrectionParams {
  private boolean renameFile;


  public ManualCorrectionParams(){
      super();
  }

  public boolean renameFile() {
    return renameFile;
  }

  public void setRenameFile(boolean renameFile) {
    this.renameFile = renameFile;
  }

}

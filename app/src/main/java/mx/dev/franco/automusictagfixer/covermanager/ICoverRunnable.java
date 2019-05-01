package mx.dev.franco.automusictagfixer.covermanager;

public interface ICoverRunnable {
    String getPath();
    /**
     * Sets the actions for each state of the PhotoTask instance.
     * @param state The state being handled.
     */
    void handleExtractionState(int state);

    /**
     * Set the cover as byte array to display.
     * @param cover
     */
    void setCover(byte[] cover);
}

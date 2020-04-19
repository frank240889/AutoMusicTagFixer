package mx.dev.franco.automusictagfixer.interfaces;

/**
 * Interface representing the general state of long running task when starts and when finishes.
 */
public interface AutomaticTaskListener {
    /**
     * Called when long running task starts.
     */
    void onStartAutomaticTask();

    /**
     * Call when long running finishes
     */
    void onFinishedAutomaticTask();

    void onStartProcessingFor(int id);

    interface MessageListener {
        void onIncomingMessageListener(String message);
    }
}

package mx.dev.franco.automusictagfixer.interfaces;

/**
 * Interface representing the general state of long running task when starts and when finishes.
 */
public interface LongRunningTaskListener {
    /**
     * Called when long running task starts.
     */
    void onLongRunningTaskStarted();

    /**
     * Call when long running finishes
     */
    void onLongRunningTaskFinish();

    /**
     * Called when long running task is cancelled.
     */
    default void onLongRunningTaskCancelled(){}

    /**
     * Called when and error occur and long running task is interrupted.
     */

    default void onLongRunningTaskMessage(String error){}
}

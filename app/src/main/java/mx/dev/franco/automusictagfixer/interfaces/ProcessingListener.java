package mx.dev.franco.automusictagfixer.interfaces;

/**
 * @author Franco Castillo
 * Interface to indicate that some processing is occurring for the id.
 */
public interface ProcessingListener {
    /**
     * Called when processing starts.
     */
    void onStartProcessingFor(int id);
}

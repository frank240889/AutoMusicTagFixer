package mx.dev.franco.automusictagfixer.identifier;

/**
 * @author Franco Castillo.
 * Identifier interface, identifiers must implement it.
 * @param <I> The params of input.
 * @param <R> The params of output.
 */
public interface Identifier<I, R> {
    /**
     * Identify the input passed as parameter.
     * @param input The input to identify.
     */
    void identify(I input);

    /**
     * Cancel the identification.
     */
    void cancel();

    /**
     * Set a callback to listen identification events.
     * @param identificationListener The listener to inform events.
     */
    void registerCallback(IdentificationListener<R, I> identificationListener);

    /**
     * Interface to define some events to inform when a identification is in progress.
     * @param <R> The type of result.
     * @param <I> The type of entity being identifying.
     */
    interface IdentificationListener<R, I> {
        void onIdentificationStart(I file);
        void onIdentificationFinished(R result);
        void onIdentificationError(I file, String error);
        void onIdentificationCancelled(I file);
        void onIdentificationNotFound(I file);
    }

    /**
     * Interface that implement the classes to hold the results.
     */
    interface IdentificationResults {}
}

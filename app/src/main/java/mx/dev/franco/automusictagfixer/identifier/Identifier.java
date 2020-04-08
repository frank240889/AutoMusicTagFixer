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
    void registerCallback(IdentificationListener<R> identificationListener);

    /**
     * Interface to define some events to inform when a identification is in progress.
     * @param <R> The type of result.
     * @param <I> The type of entity being identifying.
     */
    interface IdentificationListener<R> {
        void onIdentificationStart();
        void onIdentificationFinished(R result);
        default void onIdentificationError(Throwable error) {};
        void onIdentificationCancelled();
        void onIdentificationNotFound();
    }

    /**
     * Interface that implement the classes to hold the results.
     */
    abstract class IdentificationResults {
        protected String id;
        public String getId() {
            return id;
        }
        public void setId(String id) {
            this.id = id;
        }
    }


    enum IdentificationState {
        STARTING_IDENTIFICATION,
        IDENTIFICATION_FINISHED,
        IDENTIFICATION_ERROR,
        IDENTIFICATION_CANCELLED,
        IDENTIFICATION_NOT_FOUND
    }

    class IdentificationStatus {
        private IdentificationState identificationState;
        private String message;

        public IdentificationStatus() {}

        public IdentificationStatus(IdentificationState identificationState, String message) {
            this.identificationState = identificationState;
            this.message = message;
        }

        public IdentificationState getIdentificationState() {
            return identificationState;
        }

        public void setIdentificationState(IdentificationState identificationState) {
            this.identificationState = identificationState;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }


    /**
     * Represent the fields the identifier can return.
     */
    enum Field {
        FILENAME,
        TITLE,
        ARTIST,
        ALBUM,
        GENRE,
        TRACK_NUMBER,
        TRACK_YEAR,
        COVER_ART
    }

    static class IdentificationException extends Exception {
        public IdentificationException(String message) {
            super(message);
        }
    }
}

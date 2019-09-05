package mx.dev.franco.automusictagfixer.identifier;

public interface Identifier<I, R> {
    void identify(I input);
    void cancel();
    void registerCallback(IdentificationListener<R, I> identificationListener);

    interface IdentificationListener<R, I> {
        void onIdentificationStart(I file);
        void onIdentificationFinished(R result);
        void onIdentificationError(I file, String error);
        void onIdentificationCancelled(I file);
        void onIdentificationNotFound(I file);
    }

    interface IdentificationResults {}
}

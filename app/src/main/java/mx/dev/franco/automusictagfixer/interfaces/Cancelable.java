package mx.dev.franco.automusictagfixer.interfaces;

public interface Cancelable<R> {
    void cancel();
    default void onCancelled(R res) {}
}

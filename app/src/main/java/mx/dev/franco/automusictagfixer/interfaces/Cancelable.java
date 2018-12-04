package mx.dev.franco.automusictagfixer.interfaces;

public interface Cancelable<R> {
    void cancel();
    void onCancelled(R res);
}

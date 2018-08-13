package mx.dev.franco.automusictagfixer.interfaces;

public interface ItemHandler<T> {
    void read(long id);
    void readAll();
}

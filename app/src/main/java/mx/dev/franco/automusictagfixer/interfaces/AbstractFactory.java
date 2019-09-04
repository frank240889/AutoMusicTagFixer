package mx.dev.franco.automusictagfixer.interfaces;

public interface AbstractFactory<T, C> {
    T create(C type);
}

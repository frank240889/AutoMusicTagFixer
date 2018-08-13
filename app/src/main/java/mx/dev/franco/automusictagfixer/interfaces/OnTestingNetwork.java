package mx.dev.franco.automusictagfixer.interfaces;

public interface OnTestingNetwork{
    void onStartTestingNetwork();

    interface OnTestingResult<T>{
        void onNetworkConnected(T param);
        void onNetworkDisconnected(T param);
    }
}

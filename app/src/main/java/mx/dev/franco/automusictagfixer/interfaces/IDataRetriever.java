package mx.dev.franco.automusictagfixer.interfaces;

public interface IDataRetriever {
    void onBeforeRetrieving();
    void onStartRetrieving();
    void onRetrieving();
    void onFinishRetrieving();
    void onCancelRetrieving();

}

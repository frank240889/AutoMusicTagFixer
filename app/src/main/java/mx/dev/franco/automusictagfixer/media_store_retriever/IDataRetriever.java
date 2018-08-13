package mx.dev.franco.automusictagfixer.media_store_retriever;

public interface IDataRetriever {
    void onBeforeRetrieving();
    void onStartRetrieving();
    void onRetrieving();
    void onFinishRetrieving();
    void onCancelRetrieving();

}
